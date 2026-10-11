\set ON_ERROR_STOP on
BEGIN;
DO $$
DECLARE dataset UUID; example UUID; count_rows INT;
BEGIN
 INSERT INTO ai_training.datasets(code,name,created_by) VALUES ('test-' || gen_random_uuid(), 'Training storage tests','test') RETURNING id INTO dataset;
 INSERT INTO ai_training.examples(dataset_id,language,prompt,response,source,created_by)
 VALUES (dataset,'en','Test prompt','Test response','synthetic:test','test') RETURNING id INTO example;
 IF EXISTS (SELECT 1 FROM ai_training.approved_examples WHERE id=example) THEN RAISE EXCEPTION 'Draft leaked into export'; END IF;
 BEGIN
  INSERT INTO ai_training.examples(dataset_id,language,prompt,response,source,created_by) VALUES (dataset,'en','Test prompt','Test response','synthetic:test','test');
  RAISE EXCEPTION 'Duplicate accepted';
 EXCEPTION WHEN unique_violation THEN NULL; END;
 BEGIN
  INSERT INTO ai_training.examples(dataset_id,language,prompt,response,source,created_by) VALUES (dataset,'fr','Question','Answer','synthetic:test','test');
  RAISE EXCEPTION 'Invalid language accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN
  UPDATE ai_training.examples SET review_status='APPROVED',reviewed_by='test',reviewed_at=now() WHERE id=example;
  RAISE EXCEPTION 'Unverified example approved';
 EXCEPTION WHEN check_violation THEN NULL; END;
 UPDATE ai_training.examples SET source_verified=true,consent_confirmed=true WHERE id=example;
 BEGIN
  UPDATE ai_training.examples SET review_status='APPROVED' WHERE id=example;
  RAISE EXCEPTION 'Anonymous approval accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 UPDATE ai_training.examples SET review_status='APPROVED',reviewed_by='test-reviewer',reviewed_at=now() WHERE id=example;
 SELECT count(*) INTO count_rows FROM ai_training.approved_examples WHERE id=example AND training_record->'messages'->0->>'content'='Test prompt';
 IF count_rows<>1 THEN RAISE EXCEPTION 'Approved JSONL record missing'; END IF;
 UPDATE ai_training.examples SET response='Changed answer' WHERE id=example;
 IF EXISTS (SELECT 1 FROM ai_training.approved_examples WHERE id=example) THEN RAISE EXCEPTION 'Stale approval retained after edit'; END IF;
 IF NOT EXISTS (SELECT 1 FROM ai_training.examples WHERE id=example AND review_status='DRAFT' AND reviewed_by IS NULL AND reviewed_at IS NULL) THEN RAISE EXCEPTION 'Review reset failed'; END IF;
 BEGIN
  INSERT INTO ai_training.runs(dataset_id,base_model,dataset_sha256,created_by) VALUES(dataset,'local-model','invalid-hash','test');
  RAISE EXCEPTION 'Invalid snapshot hash accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 BEGIN
  INSERT INTO ai_training.runs(dataset_id,base_model,dataset_sha256,created_by,status) VALUES(dataset,'local-model',repeat('a',64),'test','COMPLETED');
  RAISE EXCEPTION 'Incomplete completed run accepted';
 EXCEPTION WHEN check_violation THEN NULL; END;
 INSERT INTO ai_training.runs(dataset_id,base_model,dataset_sha256,created_by,status,started_at,finished_at,artifact_uri)
 VALUES(dataset,'local-model',repeat('b',64),'test','COMPLETED',now(),now(),'local:test-model');
 RAISE NOTICE 'PASS: drafts excluded, duplicates blocked, language validated, consent/source required, reviewer required, approved export, edit invalidates review, snapshot hash validated, run completion validated, valid run saved';
END $$;
ROLLBACK;
