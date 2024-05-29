from fastapi import APIRouter
from fastapi.responses import JSONResponse
from fastapi.encoders import jsonable_encoder
from pydantic import BaseModel
from llm import run_inference

router = APIRouter(prefix="/api", tags=["api"])


class ImageSchema(BaseModel):
    image: str


@router.post("/analyze")
def analyze(payload: ImageSchema):
    result = run_inference(payload.image)
    json = jsonable_encoder(result)
    return JSONResponse(content=json)
