#!/usr/bin/env python3
"""Verify workflow migrations in disposable PostgreSQL; no project DB access."""
import pathlib
import subprocess
import time
import uuid

root = pathlib.Path(__file__).resolve().parents[1]
name = 'eshop-workflow-schema-test-' + uuid.uuid4().hex[:8]


def command(args, payload=None):
    result = subprocess.run(args, input=payload, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if result.returncode:
        raise RuntimeError(result.stderr.decode()[-2500:])
    return result.stdout


def sql(statement):
    return command(['docker', 'exec', '-i', name, 'psql', '-U', 'workflow_test', '-d', 'workflow_test',
                    '-v', 'ON_ERROR_STOP=1', '-q'], statement.encode())


try:
    command(['docker', 'run', '--rm', '-d', '--name', name, '--network', 'none',
             '-e', 'POSTGRES_HOST_AUTH_METHOD=trust', '-e', 'POSTGRES_USER=workflow_test', 'postgres:18'])
    for _ in range(40):
        if subprocess.run(['docker', 'exec', name, 'pg_isready', '-h', '127.0.0.1', '-U', 'workflow_test'], capture_output=True).returncode == 0:
            break
        time.sleep(0.25)
    else:
        raise RuntimeError('Disposable PostgreSQL did not become ready')
    for _ in range(2):
        for migration in ['20261020_workflow_drafts.sql', '20261020_workflow_manual_execution.sql', '20261025_workflow_list_filters.sql', '20261025_workflow_triggers.sql']:
            sql((root / 'ai-service/src/main/resources/db/upgrade' / migration).read_text())
    for _ in range(2):
        sql((root / 'auth-service/src/main/resources/db/upgrade/20261025_workflow_authorization.sql').read_text())
    print('1. PASSED: workflow and authorization migrations applied twice on real PostgreSQL 18')
    sql("""
INSERT INTO ai_workflow_drafts(id,owner_id,name,definition,updated_at)
VALUES('00000000-0000-4000-8000-000000000001',1,'Real schema check','{}',now());
INSERT INTO ai_workflow_published_versions(id,workflow_id,draft_revision,owner_id,name,definition,published_at)
VALUES('00000000-0000-4000-8000-000000000002','00000000-0000-4000-8000-000000000001',0,1,'Real schema check','{}',now());
INSERT INTO ai_workflow_runs(id,published_version_id,owner_id,status,request_id,started_at)
VALUES('00000000-0000-4000-8000-000000000003','00000000-0000-4000-8000-000000000002',1,'RUNNING','schema-check',now());
INSERT INTO ai_workflow_run_steps(id,run_id,position,step_key,name,tool,status)
VALUES('00000000-0000-4000-8000-000000000004','00000000-0000-4000-8000-000000000003',0,'read','Read product','PRODUCT_GET','PENDING');
DO $$ BEGIN
 BEGIN
  UPDATE ai_workflow_published_versions SET definition='{ "changed":true }';
  RAISE EXCEPTION 'published version update accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN
  DELETE FROM ai_workflow_published_versions;
  RAISE EXCEPTION 'published version delete accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN
  INSERT INTO ai_workflow_runs SELECT * FROM ai_workflow_runs;
  RAISE EXCEPTION 'duplicate execution key accepted';
 EXCEPTION WHEN unique_violation THEN NULL; END;
 BEGIN
  INSERT INTO ai_workflow_run_steps(id,run_id,position,step_key,name,tool,status)
  VALUES('00000000-0000-4000-8000-000000000005','00000000-0000-4000-8000-000000000003',0,'duplicate','Duplicate','PRODUCT_GET','PENDING');
  RAISE EXCEPTION 'duplicate step position accepted';
 EXCEPTION WHEN unique_violation THEN NULL; END;
 BEGIN
  INSERT INTO ai_workflow_runs(id,published_version_id,owner_id,status,request_id,started_at)
  VALUES('00000000-0000-4000-8000-000000000006','00000000-0000-4000-8000-000000000099',1,'RUNNING','schema-check',now());
  RAISE EXCEPTION 'missing published version accepted';
 EXCEPTION WHEN foreign_key_violation THEN NULL; END;
END $$;
""")
    print('2. PASSED: workflow, version, run and step records persisted')
    print('3. PASSED: published update and delete rejected')
    print('4. PASSED: duplicate execution ID and step position rejected')
    print('5. PASSED: missing published-version reference rejected')
    sql("""UPDATE ai_workflow_drafts SET definition='{"module":"catalog-service","triggerType":"MANUAL"}',module_code=NULL,trigger_type=NULL;""")
    for _ in range(2):
        sql((root / 'ai-service/src/main/resources/db/upgrade/20261025_workflow_list_filters.sql').read_text())
    sql("""DO $$ BEGIN
      IF NOT EXISTS (SELECT 1 FROM ai_workflow_drafts WHERE module_code='catalog-service' AND trigger_type='MANUAL' AND revision=0)
      THEN RAISE EXCEPTION 'metadata backfill failed or changed the revision'; END IF;
    END $$;""")
    print('6. PASSED: list-filter metadata backfilled without changing revision tokens')

finally:
    subprocess.run(['docker', 'rm', '-f', name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
