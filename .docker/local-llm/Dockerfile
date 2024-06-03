FROM bitnami/pytorch:2.3.0

COPY ./app /app

WORKDIR /app

RUN pip install -U pip
RUN pip install -r requirements.txt

EXPOSE 8000

CMD ["gunicorn", "main:app", "--workers", "16", "--worker-class", "uvicorn.workers.UvicornWorker", "--bind", "0.0.0.0:8000"]