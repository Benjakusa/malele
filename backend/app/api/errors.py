from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse


class ApiError(Exception):
    """Application-level error with a stable code and HTTP status."""
    def __init__(self, code: str, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.status_code = status_code


def install_error_handlers(app: FastAPI) -> None:
    """Register global exception handlers on the FastAPI app."""

    @app.exception_handler(ApiError)
    async def _api_error(_: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": {"code": exc.code, "message": exc.message}},
        )

    @app.exception_handler(Exception)
    async def _unhandled(_: Request, exc: Exception) -> JSONResponse:
        # Never leak stack traces to the client
        return JSONResponse(
            status_code=500,
            content={"error": {"code": "INTERNAL_ERROR", "message": "An unexpected error occurred"}},
        )


# Stable error code registry (per ARCHITECTURE.md §7)
ERROR_CODES = {
    "AUTH_INVALID_CREDENTIALS": 401,
    "AUTH_EMAIL_TAKEN": 409,
    "AUTH_ACCOUNT_DISABLED": 403,
    "AUTH_INVALID_REFRESH": 401,
    "AUTH_MISSING_TOKEN": 401,
    "AUTH_INVALID_TOKEN": 401,
    "VALIDATION_ERROR": 422,
    "RATE_LIMITED": 429,
    "RESOURCE_NOT_FOUND": 404,
    "FORBIDDEN": 403,
    "INTERNAL_ERROR": 500,
}
