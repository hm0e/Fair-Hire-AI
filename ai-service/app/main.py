from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core import config
from app.api.health import router as health_router

app = FastAPI(
    title="FairHire AI - AI/NLP Microservice",
    description="Dedicated microservice for resume parsing, skill extraction, embeddings, and fairness auditing.",
    version=config.VERSION,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register routers
app.include_router(health_router)


@app.get("/")
async def root():
    return {
        "service": config.SERVICE_NAME,
        "version": config.VERSION,
        "status": "UP",
        "docs": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host=config.HOST, port=config.PORT, reload=config.DEBUG)
