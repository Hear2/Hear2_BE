from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI, OpenAIError
from typing import List, Optional

import json
import os

app = FastAPI()


# Emotion DTO

class EmotionRequest(BaseModel):
    message: str


# Judge DTO

class JudgeMessage(BaseModel):
    messageId: Optional[int] = None
    senderId: int
    content: str
    emotionType: Optional[str] = None
    emotionScore: Optional[float] = None
    negativeScore: Optional[float] = None
    emotionEmoji: Optional[str] = None
    riskLevel: Optional[str] = None
    createdAt: Optional[str] = None


class JudgeRequest(BaseModel):
    coupleId: Optional[int] = None
    triggerMessageId: Optional[int] = None
    requestedByUserId: Optional[int] = None
    messages: List[JudgeMessage]



# OpenAI Client

def get_openai_client() -> OpenAI:
    api_key = os.getenv("OPENAI_API_KEY")

    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="OPENAI_API_KEY environment variable is not set.",
        )

    return OpenAI(api_key=api_key)



# Emotion Analysis API

@app.post("/analyze")
def analyze_emotion(request: EmotionRequest):

    prompt = f"""
    Analyze the emotion of this message.

    Message:
    {request.message}

    Return JSON only:

    {{
      "emotionType": "HAPPY | SAD | ANGRY | ANXIOUS | NEUTRAL",
      "emotionScore": 0.0,
      "negativeScore": 0.0,
      "emotionEmoji": "🙂",
      "riskLevel": "NONE | CAUTION | WARNING | DANGER",
      "riskDetected": true,
      "riskReason": "short reason"
    }}
    """

    try:
        response = get_openai_client().chat.completions.create(
            model="gpt-4o",
            response_format={"type": "json_object"},
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are an emotion analyzer. "
                        "Return valid JSON only."
                    ),
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
        )

    except OpenAIError as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        ) from error

    content = response.choices[0].message.content

    try:
        return json.loads(content)

    except json.JSONDecodeError as error:
        raise HTTPException(
            status_code=502,
            detail=f"OpenAI returned invalid JSON: {content}",
        ) from error



# AI Judge API

@app.post("/judge")
def judge_chat(request: JudgeRequest):
    if not request.messages:
        raise HTTPException(
            status_code=400,
            detail="messages are required.",
        )

    participant_labels = {}
    label_names = ["A", "B"]
    next_label_index = 0

    def label_for(sender_id: int) -> str:
        nonlocal next_label_index

        if sender_id not in participant_labels:
            if next_label_index < len(label_names):
                participant_labels[sender_id] = label_names[next_label_index]
            else:
                participant_labels[sender_id] = f"User{sender_id}"
            next_label_index += 1
        return participant_labels[sender_id]

    def score_text(score: Optional[float]) -> str:
        if score is None:
            return "UNKNOWN"
        bounded_score = max(0.0, min(1.0, score))
        return f"{round(bounded_score * 100)}%"

    conversation = "\n".join(
        [
            (
                f"{label_for(message.senderId)}"
                f"(userId={message.senderId}, "
                f"emotion={message.emotionType or 'UNKNOWN'}, "
                f"emotionScore={score_text(message.emotionScore)}, "
                f"negativeScore={score_text(message.negativeScore)}, "
                f"risk={message.riskLevel or 'UNKNOWN'}): "
                f"{message.content}"
            )
            for message in request.messages
        ]
    )
    requested_label = (
        label_for(request.requestedByUserId)
        if request.requestedByUserId is not None
        else "the user who tapped the button"
    )

    prompt = f"""
    You are an AI relationship judge for a couple chat service.

    Analyze the following recent conversation and produce a short card that can
    be displayed inside the chat room.

    coupleId: {request.coupleId}
    triggerMessageId: {request.triggerMessageId}
    requestedBy: {requested_label}
    Conversation:
    {conversation}

    Rules:
    - Write every user-facing value in Korean.
    - Be witty, but do not mock either person.
    - Use negativeScore to identify why the AI judge button appeared.
    - summaryA and summaryB must neutrally summarize each side's position.
    - judgement must be 2 or 3 short sentences.
    - solution must contain 2 or 3 concrete reconciliation actions.
    - reconciliationMessage must be one short message that {requested_label} can send immediately.
    - reconciliationMessage must sound sincere, calm, and natural in chat.
    - conflictType must be exactly one of:
      COMMUNICATION, JEALOUSY, TRUST, REPLY_DELAY, DAILY, OTHER.
    - judgeTone must be exactly WITTY.

    Return JSON only with exactly these keys:

    {{
      "summaryA": "A측 입장 요약",
      "summaryB": "B측 입장 요약",
      "judgement": "위트 있지만 따뜻한 판결문",
      "solution": "화해 방안",
      "reconciliationMessage": "바로 보낼 수 있는 화해 메시지",
      "conflictType": "COMMUNICATION | JEALOUSY | TRUST | REPLY_DELAY | DAILY | OTHER",
      "judgeTone": "WITTY"
    }}
    """

    try:
        response = get_openai_client().chat.completions.create(
            model="gpt-4o",
            response_format={"type": "json_object"},
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a witty but warm AI relationship judge. "
                        "Return valid JSON only. Do not include markdown."
                    ),
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
            temperature=0.7,
        )

    except OpenAIError as error:
        raise HTTPException(
            status_code=502,
            detail=str(error)
        ) from error

    content = response.choices[0].message.content

    try:
        return json.loads(content)

    except json.JSONDecodeError as error:
        raise HTTPException(
            status_code=502,
            detail=f"OpenAI returned invalid JSON: {content}",
        ) from error
