import sys
from datetime import datetime, timezone
from fastapi import APIRouter
from app.core import config

router = APIRouter(tags=["Health"])


@router.get("/health")
@router.get("/api/v1/health")
async def health_check():
    """
    Service health check endpoint.
    Exposes service identity, version, timestamp, and runtime info.
    """
    return {
        "status": "UP",
        "service": config.SERVICE_NAME,
        "version": config.VERSION,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "runtime": {
            "python_version": sys.version.split()[0],
            "model_configured": config.MODEL_NAME,
        }
    }


@router.get("/api/v1/ping")
async def ping():
    """Simple connectivity verification probe."""
    return {"message": "pong", "service": config.SERVICE_NAME}
