#!/usr/bin/env python3
"""Validate guest upgrades against schema-only copies in a disposable PostgreSQL container.
Requires the existing local database containers; never copies customer rows or writes to them.
"""
import json
import pathlib
import subprocess
import time
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[1]
NAME = "eshop-guest-migration-test-" + uuid.uuid4().hex[:8]

def command(args, data=None):
    result = subprocess.run(args, input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError(result.stderr.decode()[-3000:])
    return result.stdout

def sql(database, statement):
    return command(["docker", "exec", "-i", NAME, "psql", "-U", "guest_test", "-d", database,
                    "-v", "ON_ERROR_STOP=1", "-q"], statement.encode())

checks = {
    "admin": """
DO $$ BEGIN
  BEGIN UPDATE storefront_access_settings SET guest_payment=true; RAISE EXCEPTION 'unsafe payment accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
  BEGIN UPDATE storefront_access_settings SET require_delivery_address=false; RAISE EXCEPTION 'address bypass accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
""",
    "order": """
INSERT INTO guest_sessions(id,token_hash,csrf_hash,created_at,expires_at,rotated_at)
VALUES('00000000-0000-4000-8000-000000000001',repeat('a',64),repeat('b',64),now(),now()+interval '7 days',now());
INSERT INTO carts(guest_session_id,total_items,total_price) VALUES('00000000-0000-4000-8000-000000000001',0,0);
DO $$ BEGIN
  BEGIN INSERT INTO carts(guest_session_id) VALUES('00000000-0000-4000-8000-000000000001'); RAISE EXCEPTION 'duplicate cart accepted'; EXCEPTION WHEN unique_violation THEN NULL; END;
  BEGIN INSERT INTO carts(user_id,guest_session_id) VALUES(99,'00000000-0000-4000-8000-000000000001'); RAISE EXCEPTION 'dual owner accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
INSERT INTO order_details(guest_session_id,checkout_key,shipping_address_snapshot,order_number,order_date,status,total_amount,catalog_completed)
VALUES('00000000-0000-4000-8000-000000000001','duplicate-key','{}','GUEST-TEST-1',now(),'PENDING',10,false);
DO $$ BEGIN
  BEGIN INSERT INTO order_details(guest_session_id,checkout_key,shipping_address_snapshot,order_number,order_date,status,total_amount,catalog_completed)
    VALUES('00000000-0000-4000-8000-000000000001','duplicate-key','{}','GUEST-TEST-2',now(),'PENDING',10,false);
    RAISE EXCEPTION 'duplicate checkout accepted'; EXCEPTION WHEN unique_violation THEN NULL; END;
  BEGIN INSERT INTO order_details(order_number,order_date,status,total_amount,catalog_completed)
    VALUES('UNOWNED',now(),'PENDING',10,false); RAISE EXCEPTION 'unowned order accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
""",
    "catalog": """
INSERT INTO checkout_reservations(order_id,guest_session_id,status,snapshot)
VALUES(1,'00000000-0000-4000-8000-000000000001','RESERVED','{}');
DO $$ BEGIN
  BEGIN INSERT INTO checkout_reservations(order_id,status,snapshot) VALUES(2,'RESERVED','{}');
  RAISE EXCEPTION 'unowned reservation accepted'; EXCEPTION WHEN check_violation THEN NULL; END;
END $$;
""",
    "payment": """
INSERT INTO payment_transactions(transaction_no,order_id,customer_id,payment_method,amount,currency,status,verification_attempts)
VALUES('GUEST-TEST-QR',1,NULL,'BAKONG',10,'USD','PENDING',0);
""",
    "auth": """
DO $$ BEGIN
 IF (SELECT count(*) FROM function_permissions WHERE func_code IN ('STOREFRONT_ACCESS_VIEW','STOREFRONT_ACCESS_UPDATE','PRODUCT_REVIEW_MODERATE')) <> 3
 THEN RAISE EXCEPTION 'missing permission definitions'; END IF;
END $$;
""",
}

try:
    command(["docker", "run", "--rm", "-d", "--name", NAME, "--network", "none",
             "-e", "POSTGRES_HOST_AUTH_METHOD=trust", "-e", "POSTGRES_USER=guest_test", "postgres:18"])
    for attempt in range(40):
        ready = subprocess.run(["docker", "exec", NAME, "pg_isready", "-U", "guest_test"], capture_output=True)
        if ready.returncode == 0:
            break
        time.sleep(0.25)
    else:
        raise RuntimeError("PostgreSQL test container did not start")
    for service in ["admin", "order", "catalog", "payment", "auth"]:
        source = service + "-db"
        config = json.loads(command(["docker", "inspect", source]))[0]
        env = dict(value.split("=", 1) for value in config["Config"]["Env"])
        dump = command(["docker", "exec", source, "pg_dump", "-U", env["POSTGRES_USER"],
                        "-d", env["POSTGRES_DB"], "--schema-only", "--no-owner", "--no-privileges"])
        database = "guest_test_" + service
        command(["docker", "exec", NAME, "createdb", "-U", "guest_test", database])
        command(["docker", "exec", "-i", NAME, "psql", "-U", "guest_test", "-d", database,
                 "-v", "ON_ERROR_STOP=1", "-q"], dump)
        migrations = sorted((ROOT / (service + "-service") / "src/main/resources/db/upgrade").glob("20261019_*.sql"))
        for repeat in range(2):
            for migration in migrations:
                sql(database, migration.read_text())
        sql(database, checks[service])
        print(json.dumps({"service": service, "migrations": len(migrations), "idempotent": True, "constraints": "passed"}), flush=True)
finally:
    subprocess.run(["docker", "rm", "-f", NAME], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
