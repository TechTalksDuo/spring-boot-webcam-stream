from fastapi import FastAPI
from router import router

# Server
app = FastAPI()
app.include_router(router)
