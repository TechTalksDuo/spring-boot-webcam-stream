# Use a pipeline as a high-level helper
from transformers import pipeline
from PIL import Image

pipe = pipeline("image-classification", model="MahmoudWSegni/swin-tiny-patch4-window7-224-finetuned-face-emotion-v12_right")
image = Image.open('images.jpeg')
image.show()

result = pipe(image)

print(result)
