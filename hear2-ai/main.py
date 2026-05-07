from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI, OpenAIError
import json
import os


app = FastAPI()


class EmotionRequest(BaseModel):
    message: str


def get_openai_client() -> OpenAI:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(
            status_code=500,
            detail="OPENAI_API_KEY environment variable is not set.",
        )
    return OpenAI(api_key=api_key)


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
      "riskLevel": "NONE | CAUTION | WARNING | DANGER",
      "riskDetected": true
    }}
    """

    try:
        response = get_openai_client().chat.completions.create(
            model="gpt-4o-mini",
            response_format={"type": "json_object"},
            messages=[
                {
                    "role": "system",
                    "content": "You are an emotion analyzer. Return valid JSON only.",
                },
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
        )
    except OpenAIError as error:
        raise HTTPException(status_code=502, detail=str(error)) from error

    content = response.choices[0].message.content

    try:
        return json.loads(content)
    except json.JSONDecodeError as error:
        raise HTTPException(
            status_code=502,
            detail=f"OpenAI returned invalid JSON: {content}",
        ) from error
