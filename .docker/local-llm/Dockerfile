FROM bitnami/pytorch:2.3.0

COPY ./app /app

WORKDIR /app

RUN pip install -U pip
RUN pip install -r requirements.txt

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]