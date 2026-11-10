from celery import Celery

from app.config.settings import get_settings


_settings = get_settings()

celery_app = Celery(
    "malele",
    broker=str(_settings.CELERY_BROKER_URL),
    backend=str(_settings.CELERY_RESULT_BACKEND),
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_acks_late=True,
    task_reject_on_worker_lost=True,
    worker_prefetch_multiplier=1,
    task_track_started=True,
    task_time_limit=300,
    task_soft_time_limit=240,
)

# Task modules are discovered from here
celery_app.autodiscover_tasks(["app.workers"])
