import test from 'node:test';
import assert from 'node:assert/strict';
import { canApprove, canExecute, editable, fieldsFor, mappingProblem, objectValues, errorMessage } from './importLogic.js';

const access = { userId: 2, permissions: ['IMPORT_REVIEW', 'IMPORT_APPROVE', 'IMPORT_EXECUTE'] };
const pending = { status: 'SUBMITTED', assignedReviewerId: 2, ownerId: 1, lastEditorId: 1, revision: 3 };
test('approval requires all grants and the independent assigned reviewer', () => {
  assert.equal(canApprove(pending, access), true);
  for (const change of [{ ownerId: 2 }, { lastEditorId: 2 }, { assignedReviewerId: 3 }, { status: 'DRAFT' }]) assert.equal(canApprove({ ...pending, ...change }, access), false);
  for (const permission of access.permissions) assert.equal(canApprove(pending, { ...access, permissions: access.permissions.filter(code => code !== permission) }), false);
});
test('execution requires the approving reviewer and exact approved version', () => {
  const approved = { ...pending, status: 'APPROVED', reviewerId: 2, approvedRevision: 3 };
  assert.equal(canExecute(approved, access), true);
  assert.equal(canExecute({ ...approved, approvedRevision: 2 }, access), false);
  assert.equal(canExecute({ ...approved, reviewerId: 3 }, access), false);
  assert.equal(canExecute({ ...approved, status: 'COMPLETED' }, access), false);
  assert.equal(canExecute(approved, { ...access, permissions: ['IMPORT_EXECUTE'] }), false);
});
test('mapping requires every required field and rejects ambiguous destinations', () => {
  const tool = { enabled: true, schema: { name: { required: true }, status: { required: true } } };
  assert.equal(mappingProblem(tool, { Title: 'name', Active: 'status' }), '');
  assert.match(mappingProblem(tool, { Title: 'name', Other: 'name' }), /only once/);
  assert.match(mappingProblem(tool, { Title: 'name' }), /status/);
  assert.match(mappingProblem({ ...tool, enabled: false }, {}), /available/);
});
test('correction editor includes required fields missing in source row', () => {
  assert.deepEqual(fieldsFor({ sheetId: 's', valuesJson: '{"name":"Example"}' }, [{ id: 's', tool: 'CATEGORY' }], [{ name: 'CATEGORY', schema: { name: {}, status: {} } }]), ['name', 'status']);
});
test('execution locks editing while approved drafts can be revised', () => {
  assert.ok(editable({ status: 'APPROVED' }));
  for (const status of ['PROCESSING', 'COMPLETED', 'PARTIAL_SUCCESS', 'FAILED', 'REJECTED']) assert.equal(editable({ status }), false);
});
test('malformed row data cannot crash preview and unknown outcomes advise reload', () => {
  assert.deepEqual(objectValues('invalid'), {}); assert.deepEqual(objectValues('[]'), {});
  assert.match(errorMessage({}), /could not be confirmed/);
  assert.match(errorMessage({ response: { status: 409 } }), /Reload/);
});
