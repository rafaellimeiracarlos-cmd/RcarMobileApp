"""Cria conta do administrador remota no servidor (independente do CEO local)."""
import getpass
import os
import sqlite3
import time
import uuid
from app import database_path, init_db, password_hash

if __name__ == "__main__":
    init_db()
    email = input("E-mail do administrador remoto: ").strip().lower()
    password = getpass.getpass("Senha forte (mínimo 12 caracteres): ")
    if len(password) < 12 or "@" not in email:
        raise SystemExit("Informe e-mail válido e senha com pelo menos 12 caracteres.")
    with sqlite3.connect(database_path()) as connection:
        connection.execute("INSERT INTO users (id,email,password_hash,role,created_at) VALUES(?,?,?,?,?)",
                           (uuid.uuid4().hex, email, password_hash(password), "admin", int(time.time())))
    print("Administrador remoto cadastrado. Guarde as credenciais com segurança.")
