import os
import sys
from pathlib import Path
from fastapi.testclient import TestClient
import pytest
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app import APP, db

@pytest.fixture
def authorized(tmp_path, monkeypatch):
    monkeypatch.setenv("RCAR_AUTH_SECRET", "very-long-test-only-authentication-secret-123")
    monkeypatch.setenv("RCAR_DB_PATH", str(tmp_path / "plates-test.sqlite3"))
    with TestClient(APP) as api:
        api.post("/v1/auth/register", json={"email": "test@example.com", "password": "secure-password-123"})
        token = api.post("/v1/auth/login", json={"email": "test@example.com", "password": "secure-password-123"}).json()["access_token"]
        yield api, {"Authorization": f"Bearer {token}"}


def test_auth_format_and_missing_provider(authorized):
    api, headers = authorized
    assert api.post("/v1/vehicles/lookup", json={"plate": "ABC1D23"}).status_code == 401
    assert api.post("/v1/vehicles/lookup", json={"plate": "A123456"}, headers=headers).status_code == 422
    assert api.post("/v1/vehicles/lookup", json={"plate": "ABC1D23"}, headers=headers).status_code == 503
    assert api.post("/v1/vehicles/lookup", json={"plate": "ABC1234"}, headers=headers).status_code == 503


def test_authorized_provider_filters_personal_data(authorized, monkeypatch):
    api, headers = authorized
    monkeypatch.setenv("RCAR_PLATE_PROVIDER_URL", "https://provider.example/plate")
    monkeypatch.setenv("RCAR_PLATE_PROVIDER_TOKEN", "provider-secret")
    import app
    captured = {}
    class Reply:
        def raise_for_status(self): pass
        def json(self):
            return {"brand":"Volkswagen", "model":"Gol", "year":2020, "engine":"1.0 MPI", "owner":"DO NOT EXPOSE"}
    class Client:
        def __init__(self, *args, **kwargs): pass
        async def __aenter__(self): return self
        async def __aexit__(self, *args): pass
        async def post(self, url, headers, json):
            captured.update({"url": url, "headers": headers, "payload": json})
            return Reply()
    monkeypatch.setattr(app.httpx, "AsyncClient", Client)
    res = api.post("/v1/vehicles/lookup", json={"plate": "ABC1D23"}, headers=headers)
    assert res.status_code == 200, res.text
    assert res.json() == {"brand":"Volkswagen", "model":"Gol", "year":2020, "engine":"1.0 MPI", "source":"licensed_provider"}
    assert captured["payload"] == {"plate":"ABC1D23"}
    with db() as con:
        cols = [c[1] for c in con.execute("PRAGMA table_info(vehicle_lookup_audit)").fetchall()]
    assert "plate" not in cols
    for _ in range(7):
        assert api.post("/v1/vehicles/lookup", json={"plate":"ABC1234"},headers=headers).status_code == 200
    assert api.post("/v1/vehicles/lookup", json={"plate":"ABC1D23"},headers=headers).status_code == 429
