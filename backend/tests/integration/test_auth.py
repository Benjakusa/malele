import pytest
from httpx import AsyncClient

pytestmark = pytest.mark.asyncio


async def test_health(client: AsyncClient) -> None:
    r = await client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


async def test_me_without_auth_returns_401(client: AsyncClient) -> None:
    r = await client.get("/v1/me")
    assert r.status_code == 401
    assert r.json()["error"]["code"] == "AUTH_MISSING_TOKEN"


async def test_register_success(client: AsyncClient) -> None:
    r = await client.post("/v1/auth/register", json={
        "email": "new@malele.app",
        "password": "password123",
        "display_name": "New User",
    })
    assert r.status_code == 201, r.text
    body = r.json()
    assert body["user"]["email"] == "new@malele.app"
    assert body["user"]["role"] == "user"
    assert "access_token" in body["tokens"]
    assert "refresh_token" in body["tokens"]
    assert body["tokens"]["token_type"] == "bearer"


async def test_register_duplicate_email_returns_409(client: AsyncClient) -> None:
    payload = {"email": "dup@malele.app", "password": "password123"}
    r1 = await client.post("/v1/auth/register", json=payload)
    assert r1.status_code == 201
    r2 = await client.post("/v1/auth/register", json=payload)
    assert r2.status_code == 409
    assert r2.json()["error"]["code"] == "AUTH_EMAIL_TAKEN"


async def test_register_invalid_body_returns_422(client: AsyncClient) -> None:
    r = await client.post("/v1/auth/register", json={"email": "bad", "password": "short"})
    assert r.status_code == 422


async def test_login_success(client: AsyncClient) -> None:
    await client.post("/v1/auth/register", json={
        "email": "login@malele.app",
        "password": "password123",
    })
    r = await client.post("/v1/auth/login", json={
        "email": "login@malele.app",
        "password": "password123",
    })
    assert r.status_code == 200, r.text
    assert r.json()["user"]["email"] == "login@malele.app"
    assert "access_token" in r.json()["tokens"]


async def test_login_wrong_password_returns_401(client: AsyncClient) -> None:
    await client.post("/v1/auth/register", json={
        "email": "wrong@malele.app",
        "password": "password123",
    })
    r = await client.post("/v1/auth/login", json={
        "email": "wrong@malele.app",
        "password": "WRONG",
    })
    assert r.status_code == 401
    assert r.json()["error"]["code"] == "AUTH_INVALID_CREDENTIALS"


async def test_refresh_rotates_token(client: AsyncClient) -> None:
    reg = await client.post("/v1/auth/register", json={
        "email": "refresh@malele.app",
        "password": "password123",
    })
    first_refresh = reg.json()["tokens"]["refresh_token"]
    r = await client.post("/v1/auth/refresh", json={"refresh_token": first_refresh})
    assert r.status_code == 200, r.text
    second_refresh = r.json()["refresh_token"]
    assert second_refresh != first_refresh
    # old token now revoked
    r2 = await client.post("/v1/auth/refresh", json={"refresh_token": first_refresh})
    assert r2.status_code == 401
    # new token works
    r3 = await client.post("/v1/auth/refresh", json={"refresh_token": second_refresh})
    assert r3.status_code == 200
