// Local visual-test fixture only; never copied to the frontend application.
import http from 'node:http';
const batch = { id: '7faeb0ed-041c-4c03-8ddf-74c3edb2e5f1', revision: 2, status: 'SUBMITTED', ownerId: 1, lastEditorId: 1, assignedReviewerId: 2, reviewerId: null, approvedRevision: null };
const sheet = { id: 'sheet-category', name: 'Categories', tool: 'CATEGORY_BULK_CREATE', confirmed: true, headers: '["Category name","Active"]' };
const tools = [{ name: 'CATEGORY_BULK_CREATE', enabled: true, permission: 'CATEGORY_CREATE', schema: { name: { required: true, type: 'text' }, status: { required: true, type: 'boolean' }, description: { required: false, type: 'text' } } }, { name: 'BRANCH_BULK_CREATE', permission: 'BRANCH_CREATE', enabled: false, schema: { name: { required: true } }, unavailableReason: 'Branch service and branch access contract are unavailable' }];
const rows = [
  { id: 'row-1', sheetId: sheet.id, rowNumber: 2, originalValues: '{"Category name":"Headphones","Active":"true"}', valuesJson: '{"name":"Headphones","status":"true"}', status: 'VALID', edited: false, skipped: false },
  { id: 'row-2', sheetId: sheet.id, rowNumber: 3, originalValues: '{"Category name":"Accessories","Active":"yes"}', valuesJson: '{"name":"Accessories","status":"yes"}', status: 'INVALID', edited: true, skipped: false },
  { id: 'row-3', sheetId: sheet.id, rowNumber: 4, originalValues: '{"Category name":"Cables","Active":"true"}', valuesJson: '{"name":"Cables","status":"true"}', status: 'SKIPPED', edited: false, skipped: true },
];
const page = content => ({ content, totalElements: content.length, totalPages: content.length ? 1 : 0 });
http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost'); let data;
  let body = ''; for await (const part of req) body += part;
  if (url.pathname === '/api/v1/user/access') data = { userId: 2, administrator: true, permissions: ['IMPORT_UPLOAD', 'IMPORT_REVIEW', 'IMPORT_APPROVE', 'IMPORT_EXECUTE', 'CATEGORY_CREATE'] };
  else if (url.pathname.endsWith('/tools')) data = tools;
  else if (url.pathname === '/api/ai/imports') data = page([batch]);
  else if (url.pathname.endsWith('/preview')) {
    const status = url.searchParams.get('status'); const filtered = rows.filter(row => !status || row.status === status || status === 'EDITED' && row.edited);
    data = { batch, sheets: [sheet], mappings: [{ sheetId: sheet.id, suggestions: '{"Category name":"name","Active":"status"}', confirmedValues: '{"Category name":"name","Active":"status"}' }], rows: page(filtered), errors: [{ id: 'err-1', rowId: 'row-2', message: 'INVALID_VALUE: status', explanation: 'Use true or false for this field.' }], summary: { totalRows: 3, editedRows: 1, statuses: { VALID: 1, INVALID: 1, SKIPPED: 1 } } };
  } else if (url.pathname.endsWith('/executions')) data = { executions: [], rows: [] };
  else if (url.pathname.endsWith('/history')) data = { audit: [{ id: 'audit-1', action: 'SUBMITTED', revision: 2, actorId: 1, createdAt: '2026-10-14T06:00:00Z', requestId: 'visual-test', traceId: 'fixture' }], revisions: [], approvals: [] };
  else if (url.pathname.endsWith('/chat')) { res.statusCode = 422; data = { conversationId: 'visual-conversation', result: { status: 'NEEDS_INPUT', errorCode: 'CONFIRMATION_REQUIRED', message: 'Review this category before confirming.', data: { confirmationId: 'test-confirmation', parameters: { name: 'Example' } } } }; }
  else if (url.pathname.endsWith('/test-confirmation')) data = { status: 'SUCCESS', message: 'Visual test confirmation received.' };
  else if (req.method !== 'GET') { if (body.startsWith('{')) { const patch = JSON.parse(body); if (patch.values) { rows[1].valuesJson = JSON.stringify(patch.values); batch.revision++; batch.status = 'DRAFT'; } } data = batch; }
  else { res.statusCode = 404; data = { message: 'Visual fixture endpoint unavailable' }; }
  res.setHeader('Content-Type', 'application/json'); res.end(JSON.stringify(data));
}).listen(5191, '127.0.0.1', () => process.stdout.write('Visual-test API listening on 5191\n'));
