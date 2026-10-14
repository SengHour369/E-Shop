import test from 'node:test';
import assert from 'node:assert/strict';
import {
  classifyCameraError,
  initialScanState,
  outcomeFromRecognition,
  outcomeFromScan,
  reduceScan,
  toScanFormat,
} from './scannerLogic.js';

test('permission granted, denied, and missing camera', () => {
  const idle = initialScanState();
  assert.equal(reduceScan(idle, { type: 'permission-granted' }).phase, 'live');
  assert.equal(reduceScan(idle, { type: 'permission-denied' }).phase, 'denied');
  assert.equal(reduceScan(idle, { type: 'camera-unavailable' }).phase, 'unavailable');
  assert.equal(reduceScan(idle, { type: 'camera-unsupported' }).phase, 'unsupported');
  assert.equal(classifyCameraError({ name: 'NotAllowedError' }), 'permission-denied');
  assert.equal(classifyCameraError({ name: 'NotFoundError' }), 'camera-unavailable');
  assert.equal(classifyCameraError({ name: 'NotSupportedError' }, { mediaDevices: false }), 'camera-unsupported');
});

test('barcode detection starts one lookup and ignores a rapid duplicate', () => {
  const live = reduceScan(initialScanState(), { type: 'permission-granted' });
  const first = reduceScan(live, { type: 'barcode', code: '8850123456787', format: 'ean_13' }, 1_000);
  assert.equal(first.phase, 'loading');
  assert.equal(first.format, 'EAN_13');
  const settled = { ...first, phase: 'found' };
  const duplicate = reduceScan(settled, { type: 'barcode', code: '8850123456787', format: 'EAN_13' }, 1_200);
  assert.equal(duplicate, settled);
  const later = reduceScan({ ...first, phase: 'found' }, { type: 'barcode', code: '8850123456787' }, 3_000);
  assert.equal(later.phase, 'loading');
});

test('scan again returns to the camera', () => {
  const found = reduceScan(initialScanState(), { type: 'found', result: { found: true, name: 'Nike' } });
  assert.equal(reduceScan(found, { type: 'scan-again' }).phase, 'live');
});

test('backend error and product not found stay on screen', () => {
  const loading = reduceScan(initialScanState(), { type: 'barcode', code: '96385074' }, 1);
  const missing = reduceScan(loading, outcomeFromScan({ success: true, data: { found: false, message: 'Product not found' } }));
  assert.equal(missing.phase, 'missing');
  const failed = reduceScan(loading, { type: 'backend-error', message: 'The product lookup failed.' });
  assert.equal(failed.phase, 'error');
  assert.equal(outcomeFromScan({ success: false, message: 'down' }).type, 'backend-error');
});

test('image recognition keeps catalog candidates and reports an unavailable provider', () => {
  const low = outcomeFromRecognition({
    success: true,
    data: { matchType: 'AI_IMAGE', exact: false, candidates: [{ productId: 100, name: 'Nike Air Max' }] },
  });
  assert.equal(low.type, 'candidates');
  assert.equal(outcomeFromRecognition({ success: true, data: { matchType: 'NO_CATALOG_MATCH', candidates: [] } }).type, 'missing');
  assert.equal(outcomeFromRecognition({ success: true, data: { matchType: 'AI_UNAVAILABLE', candidates: [] } }).type, 'backend-error');
  assert.equal(toScanFormat('qr-code'), 'QR_CODE');
  assert.equal(toScanFormat('pdf417'), '');
});
