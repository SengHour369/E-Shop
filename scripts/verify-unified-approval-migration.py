#!/usr/bin/env python3
"""Verify import upgrade and backfill in disposable PostgreSQL, never project databases."""
import pathlib, subprocess, time, uuid
root=pathlib.Path(__file__).resolve().parents[1]
name='eshop-unified-approval-test-'+uuid.uuid4().hex[:8]
def run(args,data=None):
 result=subprocess.run(args,input=data,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
 if result.returncode: raise RuntimeError(result.stderr.decode()[-2500:])
 return result.stdout
def sql(value): return run(['docker','exec','-i',name,'psql','-U','approval_test','-d','approval_test','-v','ON_ERROR_STOP=1','-q'],value.encode())
try:
 run(['docker','run','--rm','-d','--name',name,'--network','none','-e','POSTGRES_HOST_AUTH_METHOD=trust','-e','POSTGRES_USER=approval_test','postgres:18'])
 for _ in range(40):
  if subprocess.run(['docker','exec',name,'pg_isready','-h','127.0.0.1','-U','approval_test'],capture_output=True).returncode==0:break
  time.sleep(.25)
 else:raise RuntimeError('PostgreSQL did not start')
 upgrades=root/'ai-service/src/main/resources/db/upgrade'
 sql((upgrades/'20261013_ai_excel_import.sql').read_text())
 sql("""
INSERT INTO ai_import_batch(id,lock_version,owner_id,revision,status,checksum,created_at)
VALUES('00000000-0000-4000-8000-000000000001',0,1,2,'SUBMITTED','unique-source',now());
INSERT INTO ai_import_file(id,batch_id,filename,content)
VALUES('00000000-0000-4000-8000-000000000002','00000000-0000-4000-8000-000000000001','products.xlsx','\\x01');
INSERT INTO ai_import_sheet(id,batch_id,name,tool,headers,confirmed)
VALUES('00000000-0000-4000-8000-000000000003','00000000-0000-4000-8000-000000000001','Products','PRODUCT_BULK_UPDATE','[]',true);
INSERT INTO ai_import_audit_history(id,batch_id,revision,action,created_at)
VALUES('00000000-0000-4000-8000-000000000004','00000000-0000-4000-8000-000000000001',2,'SUBMITTED',now());
""")
 for _ in range(2):sql((upgrades/'20261020_unified_product_approval.sql').read_text())
 sql("""
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM ai_import_batch WHERE filename='products.xlsx' AND source_type='EXCEL_IMPORT' AND entity_type='PRODUCT' AND submitted_at IS NOT NULL)
 THEN RAISE EXCEPTION 'Actual source metadata was not preserved'; END IF;
 IF (SELECT count(*) FROM ai_import_batch)<>1 THEN RAISE EXCEPTION 'Upgrade changed request count'; END IF;
END $$;
""")
 print('PASSED: additive upgrade applied twice to existing PostgreSQL import schema')
 print('PASSED: real stored filename, entity and submission history backfilled')
 print('PASSED: existing request retained; no business database accessed')
finally: subprocess.run(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
