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
    partnerUserId: Optional[int] = None
    requestedByName: Optional[str] = None
    partnerName: Optional[str] = None
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

    def label_for(sender_id: int) -> str:
        if request.requestedByUserId is not None and sender_id == request.requestedByUserId:
            return request.requestedByName or "한 사람"
        if request.partnerUserId is not None and sender_id == request.partnerUserId:
            return request.partnerName or "다른 한 사람"
        return f"User{sender_id}"

    def score_text(score: Optional[float]) -> str:
        if score is None:
            return "UNKNOWN"
        bounded_score = max(0.0, min(1.0, score))
        return f"{round(bounded_score * 100)}%"

    def message_line(message: JudgeMessage) -> str:
        return (
            f"{label_for(message.senderId)}"
            f"(userId={message.senderId}, "
            f"emotion={message.emotionType or 'UNKNOWN'}, "
            f"emotionScore={score_text(message.emotionScore)}, "
            f"negativeScore={score_text(message.negativeScore)}, "
            f"risk={message.riskLevel or 'UNKNOWN'}): "
            f"{message.content}"
        )

    def latest_emotion_summary(sender_id: Optional[int]) -> str:
        if sender_id is None:
            return "UNKNOWN"

        for message in reversed(request.messages):
            if message.senderId == sender_id:
                return (
                    f"emotion={message.emotionType or 'UNKNOWN'}, "
                    f"negativeScore={score_text(message.negativeScore)}, "
                    f"risk={message.riskLevel or 'UNKNOWN'}, "
                    f"latestMessage={message.content}"
                )
        return "UNKNOWN"

    recent_messages = request.messages[-20:]
    priority_messages = request.messages[-10:]

    trigger_index = next(
        (index for index, message in enumerate(request.messages)
         if request.triggerMessageId is not None and message.messageId == request.triggerMessageId),
        len(request.messages) - 1
    )
    conflict_start = max(0, trigger_index - 2)
    conflict_end = min(len(request.messages), trigger_index + 3)
    conflict_messages = request.messages[conflict_start:conflict_end]

    conversation = "\n".join(
        [message_line(message) for message in request.messages]
    )
    recent_context = "\n".join([message_line(message) for message in recent_messages])
    priority_context = "\n".join([message_line(message) for message in priority_messages])
    conflict_context = "\n".join([message_line(message) for message in conflict_messages])
    requested_label = request.requestedByName or (
        label_for(request.requestedByUserId)
        if request.requestedByUserId is not None
        else "한 사람"
    )
    partner_label = request.partnerName or (
        label_for(request.partnerUserId)
        if request.partnerUserId is not None
        else "다른 한 사람"
    )
    last_message = request.messages[-1]
    last_message_summary = message_line(last_message)
    requested_latest_emotion = latest_emotion_summary(request.requestedByUserId)
    partner_latest_emotion = latest_emotion_summary(request.partnerUserId)

    prompt = f"""
    You are an AI relationship judge for a couple chat service.

    Analyze the following recent conversation and produce a short card that can
    be displayed inside the chat room.

    coupleId: {request.coupleId}
    triggerMessageId: {request.triggerMessageId}
    requestedBy: {requested_label}
    partner: {partner_label}
    Full Conversation:
    {conversation}

    Most Important Recent Context (last 10 messages, highest priority):
    {priority_context}

    Supporting Recent Context (last 20 messages):
    {recent_context}

    Direct Conflict Excerpt (trigger-centered):
    {conflict_context}

    Latest Emotional State:
    - {requested_label}: {requested_latest_emotion}
    - {partner_label}: {partner_latest_emotion}
    - Final message in conversation: {last_message_summary}

    Rules:
    - Write every user-facing value in Korean.
    - Be witty, but do not mock either person.
    - Use negativeScore to identify why the AI judge button appeared.
    - Prioritize the last 10 to 20 messages over older context when writing reconciliation messages.
    - The reconciliation messages must feel clearly connected to the direct conflict excerpt and the final message in the conversation.
    - Identify the direct trigger of the argument and reflect that exact issue in the reconciliation messages.
    - Use each person's latest emotional state and wording style when deciding tone and responsibility.
    - Treat {requested_label} and {partner_label} as the two participants of this same case.
    - summaryA must summarize {requested_label}'s position and start with "{requested_label}는".
    - summaryB must summarize {partner_label}'s position and start with "{partner_label}는".
    - Never use A, B, userA, or userB in any output field.
    - judgement must be 2 or 3 short sentences.
    - solution must contain 2 or 3 concrete reconciliation actions.
    - requestedReconciliationMessage must be a 2 to 4 sentence message that {requested_label} can send to {partner_label} right now.
    - partnerReconciliationMessage must be a 2 to 4 sentence message that {partner_label} can send to {requested_label} right now.
    - The two reconciliation messages must be meaningfully different from each other.
    - Both reconciliation messages must reflect the recent chat, the main conflict trigger, each person's emotional state, and each person's position.
    - Both reconciliation messages must stay consistent with judgement and solution.
    - The reconciliation messages must mention the concrete subject of the latest conflict, not abstract relationship language.
    - Reflect each side's responsibility share. The side with more responsibility should acknowledge a more concrete mistake or action.
    - The messages should sound like something a real person would actually send in this exact chat.
    - If a message could fit many unrelated fights, it is too generic and must be rewritten.
    - Avoid template-like wording, generic apologies, or copy-pasted sentence patterns.
    - Avoid vague lines such as "우리 사이좋게 지내자", "앞으로 더 잘할게", "잘 지내보자", "좋은 관계를 만들자".
    - Both reconciliation messages must sound sincere, calm, natural in chat, and immediately sendable.
    - conflictType must be exactly one of:
      COMMUNICATION, JEALOUSY, TRUST, REPLY_DELAY, DAILY, OTHER.
    - judgeTone must be exactly WITTY.

    Return JSON only with exactly these keys:

    {{
      "summaryA": "{requested_label}는 입장 요약",
      "summaryB": "{partner_label}는 입장 요약",
      "judgement": "위트 있지만 따뜻한 판결문",
      "solution": "화해 방안",
      "requestedReconciliationMessage": "{requested_label}가 바로 보낼 수 있는 화해 메시지",
      "partnerReconciliationMessage": "{partner_label}가 바로 보낼 수 있는 화해 메시지",
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
