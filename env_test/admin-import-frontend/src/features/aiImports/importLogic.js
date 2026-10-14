export const ROW_FILTERS = ['', 'VALID', 'INVALID', 'DUPLICATE', 'EDITED', 'CREATED', 'FAILED', 'SKIPPED', 'UNVALIDATED'];
export const TOOL_LABELS = {
  CATEGORY_BULK_CREATE: 'Categories', BRANCH_BULK_CREATE: 'Branches', PRODUCT_BULK_CREATE: 'Products',
  PRODUCT_SKU_BULK_CREATE: 'Product SKUs', INVENTORY_STOCK_IN_BULK: 'Stock in',
  INVENTORY_STOCK_ADJUST_BULK: 'Stock adjustment', PROMOTION_BULK_CREATE: 'Promotions',
};
export function objectValues(value) {
  try { const result = typeof value === 'string' ? JSON.parse(value) : value; return result && typeof result === 'object' && !Array.isArray(result) ? result : {}; }
  catch { return {}; }
}
export function headersFor(sheet) {
  try { const result = JSON.parse(sheet.headers); return Array.isArray(result) ? result : []; } catch { return []; }
}
export function fieldsFor(row, sheets, tools) {
  const tool = tools.find(item => item.name === sheets.find(sheet => sheet.id === row.sheetId)?.tool);
  return [...new Set([...Object.keys(tool?.schema || {}), ...Object.keys(objectValues(row.valuesJson))])];
}
export function mappingProblem(tool, columns) {
  if (!tool?.enabled) return 'Choose an available import type.';
  const fields = Object.values(columns).filter(Boolean);
  if (new Set(fields).size !== fields.length) return 'Map each destination field only once.';
  const missing = Object.entries(tool.schema).filter(([name, field]) => field.required && !fields.includes(name)).map(([name]) => name);
  return missing.length ? `Map required fields: ${missing.join(', ')}.` : '';
}
export function editable(batch) { return batch && !['PROCESSING', 'COMPLETED', 'PARTIAL_SUCCESS', 'FAILED', 'REJECTED'].includes(batch.status); }
export function canApprove(batch, access) {
  const has = permission => access.permissions?.includes(permission);
  return batch?.status === 'SUBMITTED' && batch.assignedReviewerId === access.userId && batch.ownerId !== access.userId
    && batch.lastEditorId !== access.userId && has('IMPORT_REVIEW') && has('IMPORT_APPROVE') && has('IMPORT_EXECUTE');
}
export function canExecute(batch, access) {
  return ['APPROVED', 'PROCESSING', 'PARTIAL_SUCCESS', 'FAILED'].includes(batch?.status)
    && batch.reviewerId === access.userId && batch.approvedRevision === batch.revision
    && ['IMPORT_EXECUTE', 'IMPORT_APPROVE', 'IMPORT_REVIEW'].every(code => access.permissions?.includes(code));
}
export function errorMessage(error) {
  if (error?.response?.status === 409) return 'This import changed or the workbook is already staged. Reload the current version before continuing.';
  if (error?.response?.status === 403) return 'You do not have permission for this action.';
  if (error?.response?.status === 413) return 'This workbook exceeds the server upload limit.';
  if (!error?.response) return 'The result could not be confirmed. Reload the import before retrying; successful rows are protected against duplicate execution.';
  return error.response.data?.message || 'The action failed. Check the workbook, permissions and current import status.';
}
