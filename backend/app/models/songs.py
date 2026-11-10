from uuid import UUID

from sqlalchemy import BigInteger, Float, ForeignKey, Integer, String, Text
from sqlalchemy.dialects.postgresql import JSONB, UUID as PGUUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base, TimestampMixin, UUIDMixin


class Song(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "songs"

    user_id: Mapped[UUID] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False, index=True,
    )
    job_id: Mapped[UUID | None] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("generation_jobs.id", ondelete="SET NULL"),
        nullable=True, index=True,
    )
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    genre: Mapped[str | None] = mapped_column(String(50), nullable=True)
    mood: Mapped[str | None] = mapped_column(String(50), nullable=True)
    language: Mapped[str | None] = mapped_column(String(20), nullable=True)
    is_favourite: Mapped[bool] = mapped_column(default=False, nullable=False)
    is_public: Mapped[bool] = mapped_column(default=False, nullable=False, index=True)

    versions: Mapped[list["SongVersion"]] = relationship(
        back_populates="song", cascade="all, delete-orphan", order_by="SongVersion.created_at"
    )


class SongVersion(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "song_versions"

    song_id: Mapped[UUID] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("songs.id", ondelete="CASCADE"),
        nullable=False, index=True,
    )
    version_number: Mapped[int] = mapped_column(Integer, nullable=False)
    parent_version_id: Mapped[UUID | None] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("song_versions.id", ondelete="SET NULL"),
        nullable=True,
    )
    operation: Mapped[str] = mapped_column(String(50), nullable=False, default="generate")
    audio_asset_id: Mapped[UUID | None] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("audio_assets.id", ondelete="SET NULL"),
        nullable=True,
    )
    duration_seconds: Mapped[float | None] = mapped_column(Float, nullable=True)
    metadata_json: Mapped[dict] = mapped_column(JSONB, default=dict, nullable=False)

    song: Mapped["Song"] = relationship(back_populates="versions")


class AudioAsset(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "audio_assets"

    user_id: Mapped[UUID] = mapped_column(
        PGUUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False, index=True,
    )
    storage_key: Mapped[str] = mapped_column(String(500), nullable=False, unique=True)
    content_type: Mapped[str] = mapped_column(String(50), nullable=False)
    size_bytes: Mapped[int] = mapped_column(BigInteger, nullable=False)
    waveform_json: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
