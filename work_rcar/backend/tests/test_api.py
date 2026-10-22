import os
import pytest
from fastapi.testclient import TestClient
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from app import APP

@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("RCAR_AUTH_SECRET", "unit-test-only-very-long-and-random-secret-key")
    monkeypatch.setenv("RCAR_DB_PATH", str(tmp_path / "marketplace.sqlite3"))
    with TestClient(APP) as test:
        yield test


def test_signup_login_admin_control_and_order_draft(client, monkeypatch):
    r = client.post("/v1/auth/register", json={"email":"buyer@example.com","password":"buyer-pass-123"})
    assert r.status_code == 201
    assert r.json()["role"] == "buyer"
    assert client.post("/v1/auth/register", json={"email":"buyer@example.com","password":"buyer-pass-123"}).status_code == 409
    token = client.post("/v1/auth/login", json={"email":"buyer@example.com","password":"buyer-pass-123"}).json()["access_token"]
    headers={"Authorization":f"Bearer {token}"}
    assert client.post("/v1/vendors", json={"name":"Loja A","city":"São Paulo","state":"SP"},headers=headers).status_code == 403
    assert client.post("/v1/orders/draft", json={"items":[{"product_id":"fake","quantity":1}]},headers=headers).status_code == 409
    assert client.get("/v1/catalog").json() == []
    assert client.get("/v1/orders/me",headers=headers).json() == []
    assert client.post("/v1/assistant", json={"question":"Preciso de um filtro"},headers=headers).status_code == 503


def test_admin_catalog_and_draft(client):
    import sqlite3
    from app import database_path, password_hash
    with sqlite3.connect(database_path()) as con:
        con.execute("INSERT INTO users VALUES (?,?,?,?,?)",("admin1","ceo@example.com",password_hash("super-secret-admin"),"admin",123))
    token = client.post("/v1/auth/login",json={"email":"ceo@example.com","password":"super-secret-admin"}).json()["access_token"]
    hdr={"Authorization":f"Bearer {token}"}
    vendor = client.post("/v1/vendors", json={"name":"RCar Teste","city":"Campinas","state":"SP","latitude":-22.9,"longitude":-47.06},headers=hdr)
    assert vendor.status_code == 201
    vendor_id=vendor.json()["id"]
    product=client.post("/v1/products",json={"vendor_id":vendor_id,"sku":"R-111","name":"Pastilha Premium","category":"Freios","price_cents":14990,"stock":3},headers=hdr)
    assert product.status_code==201
    pid=product.json()["id"]
    assert client.get("/v1/catalog",params={"q":"Freios"}).json()[0]["price_cents"]==14990
    draft=client.post("/v1/orders/draft",json={"items":[{"product_id":pid,"quantity":2}]},headers=hdr)
    assert draft.status_code==201
    assert draft.json()["subtotal_cents"]==29980
    assert draft.json()["status"]=="DRAFT_NOT_PAID"
    assert client.post("/v1/orders/draft",json={"items":[{"product_id":pid,"quantity":5}]},headers=hdr).status_code==409
    assert len(client.get("/v1/orders/me", headers=hdr).json())==1
