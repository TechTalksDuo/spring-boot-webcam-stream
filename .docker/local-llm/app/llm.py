# Use a pipeline as a high-level helper
from transformers import pipeline

pipe = pipeline("image-classification", model="MahmoudWSegni/swin-tiny-patch4-window7-224-finetuned-face-emotion-v12_right")

def run_inference(payload):
  result = pipe(payload)
  return result