/** Camera scanner state. This module has no browser or network side effects. */

export const DUPLICATE_WINDOW_MS = 1500;

const FORMATS = new Set(['EAN_13', 'EAN_8', 'UPC_A', 'UPC_E', 'CODE_128', 'CODE_39', 'QR_CODE', 'SKU']);

export function initialScanState() {
  return {
    phase: 'idle',
    code: '',
    format: '',
    result: null,
    candidates: [],
    error: '',
    lastAccepted: null,
  };
}

export function toScanFormat(raw) {
  if (!raw) return '';
  const key = String(raw).trim().toUpperCase().replace(/[\s-]+/g, '_');
  return FORMATS.has(key) ? key : '';
}

export function classifyCameraError(error, support = {}) {
  if (support.mediaDevices === false) return 'camera-unsupported';
  const name = error?.name || '';
  if (name === 'NotAllowedError' || name === 'PermissionDeniedError' || name === 'SecurityError') {
    return 'permission-denied';
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError' || name === 'OverconstrainedError') {
    return 'camera-unavailable';
  }
  if (name === 'NotSupportedError' || name === 'NotReadableError') return 'camera-unsupported';
  return 'camera-unavailable';
}

export function reduceScan(state, action, now = Date.now()) {
  switch (action.type) {
    case 'permission-granted':
      return { ...state, phase: 'live', error: '', result: null, candidates: [] };
    case 'permission-denied':
      return { ...state, phase: 'denied', error: 'Camera permission was denied. Enter the code manually.' };
    case 'camera-unavailable':
      return { ...state, phase: 'unavailable', error: 'No camera was found on this device.' };
    case 'camera-unsupported':
      return { ...state, phase: 'unsupported', error: 'This browser cannot open a camera. Enter the code manually.' };
    case 'barcode': {
      const code = String(action.code || '').trim();
      if (!code || state.phase === 'loading') return state;
      if (state.lastAccepted && state.lastAccepted.code === code && now - state.lastAccepted.at < DUPLICATE_WINDOW_MS) {
        return state;
      }
      return {
        ...state,
        phase: 'loading',
        code,
        format: toScanFormat(action.format),
        error: '',
        result: null,
        candidates: [],
        lastAccepted: { code, at: now },
      };
    }
    case 'found':
      return { ...state, phase: 'found', result: action.result, candidates: [], error: '' };
    case 'missing':
      return { ...state, phase: 'missing', result: action.result, candidates: [], error: '' };
    case 'candidates':
      return { ...state, phase: 'candidates', result: null, candidates: action.candidates || [], error: '' };
    case 'backend-error':
      return { ...state, phase: 'error', error: action.message || 'The product lookup failed.', result: null };
    case 'scan-again':
      return { ...initialScanState(), phase: 'live' };
    default:
      return state;
  }
}

export function outcomeFromScan(body) {
  const result = body?.data;
  if (!body || body.success === false || !result) {
    return { type: 'backend-error', message: body?.message || 'The product lookup failed.' };
  }
  return result.found ? { type: 'found', result } : { type: 'missing', result };
}

export function outcomeFromRecognition(body) {
  const result = body?.data;
  if (!body || body.success === false || !result || result.matchType === 'AI_UNAVAILABLE') {
    return { type: 'backend-error', message: result?.message || body?.message || 'Product recognition is unavailable.' };
  }
  if (result.matchType === 'NO_CATALOG_MATCH' || !result.candidates?.length) {
    return { type: 'missing', result: { found: false, code: '', message: result.message || 'No matching catalog product' } };
  }
  if (result.exact && result.candidates.length === 1) {
    return { type: 'candidates', candidates: result.candidates, exact: true };
  }
  return { type: 'candidates', candidates: result.candidates };
}
