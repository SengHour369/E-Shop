import { toScanFormat } from './scannerLogic';

const NATIVE_FORMATS = ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128', 'code_39', 'qr_code'];

export function cameraSupport() {
  const mediaDevices = typeof navigator !== 'undefined' && !!navigator.mediaDevices?.getUserMedia;
  return { mediaDevices, detector: typeof globalThis.BarcodeDetector === 'function' };
}

/** Opens the rear camera. Call this only after the user asks to scan. */
export function openRearCamera() {
  if (!cameraSupport().mediaDevices) {
    const error = new Error('Camera API is unavailable');
    error.name = 'NotSupportedError';
    throw error;
  }
  return navigator.mediaDevices.getUserMedia({
    audio: false,
    video: { facingMode: { ideal: 'environment' } },
  });
}

export function stopStream(stream) {
  stream?.getTracks?.().forEach((track) => track.stop());
}

/**
 * Watches a live video element and reports decoded values.
 * Frames stay in the browser. Nothing is uploaded for barcode detection.
 */
export async function watchBarcodes(video, onCode) {
  if (cameraSupport().detector) {
    try {
      return watchNative(video, onCode);
    } catch {
      // The browser advertised the API but rejected the format list.
    }
  }
  return watchZxing(video, onCode);
}

function watchNative(video, onCode) {
  const detector = new globalThis.BarcodeDetector({ formats: NATIVE_FORMATS });
  let stopped = false;
  const timer = setInterval(async () => {
    if (stopped || video.readyState < 2) return;
    try {
      const codes = await detector.detect(video);
      const code = codes?.[0];
      if (code?.rawValue) onCode(code.rawValue, toScanFormat(code.format));
    } catch {
      // A single frame can fail while the camera is still starting.
    }
  }, 250);
  return () => {
    stopped = true;
    clearInterval(timer);
  };
}

async function watchZxing(video, onCode) {
  const [{ BrowserMultiFormatReader, DecodeHintType }, { BarcodeFormat }] = await Promise.all([
    import('@zxing/browser'),
    import('@zxing/library'),
  ]);
  const hints = new Map();
  hints.set(DecodeHintType.POSSIBLE_FORMATS, [
    BarcodeFormat.EAN_13,
    BarcodeFormat.EAN_8,
    BarcodeFormat.UPC_A,
    BarcodeFormat.UPC_E,
    BarcodeFormat.CODE_128,
    BarcodeFormat.CODE_39,
    BarcodeFormat.QR_CODE,
  ]);
  const reader = new BrowserMultiFormatReader(hints, { delayBetweenScanAttempts: 250 });
  const controls = await reader.decodeFromVideoElement(video, (result) => {
    if (result) onCode(result.getText(), toScanFormat(result.getBarcodeFormat?.() || result.barcodeFormat));
  });
  return () => controls.stop();
}

export function captureFrame(video) {
  const canvas = document.createElement('canvas');
  canvas.width = video.videoWidth || 640;
  canvas.height = video.videoHeight || 480;
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.85));
}
