import { useEffect, useRef, useState } from 'react';
import PageContainer from '@/components/layouts/PageContainer';
import Button from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import Input from '@/components/ui/Input';
import { formatCurrency } from '@/utils/formatCurrency';
import { cameraSupport, captureFrame, openRearCamera, stopStream, watchBarcodes } from '../barcodeCapture';
import { classifyCameraError, initialScanState, outcomeFromRecognition, outcomeFromScan, reduceScan } from '../scannerLogic';
import { scannerService } from '../services/scanner.service';

export default function ProductScannerPage() {
  const [state, setState] = useState(initialScanState);
  const [manualCode, setManualCode] = useState('');
  const stateRef = useRef(state);
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  stateRef.current = state;

  const dispatch = (action) => {
    setState((current) => reduceScan(current, action));
  };

  const lookup = async (code, format) => {
    try {
      const body = await scannerService.scan(code, format);
      dispatch(outcomeFromScan(body));
    } catch (error) {
      const message = error?.response?.data?.message || 'The product lookup failed.';
      dispatch({ type: 'backend-error', message });
    }
  };

  const onCode = (code, format) => {
    const next = reduceScan(stateRef.current, { type: 'barcode', code, format });
    if (next === stateRef.current) return;
    stateRef.current = next;
    setState(next);
    lookup(next.code, next.format);
  };

  useEffect(() => {
    if (state.phase !== 'live') return undefined;
    let cancelWatch = () => {};
    let stopped = false;
    (async () => {
      try {
        const stream = await openRearCamera();
        if (stopped) {
          stopStream(stream);
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
          cancelWatch = await watchBarcodes(videoRef.current, onCode);
        }
      } catch (error) {
        if (!stopped) dispatch({ type: classifyCameraError(error, cameraSupport()) });
      }
    })();
    return () => {
      stopped = true;
      cancelWatch();
      stopStream(streamRef.current);
      streamRef.current = null;
    };
    // The camera is opened only when the user moves the page into the live phase.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.phase]);

  const submitManual = (event) => {
    event.preventDefault();
    onCode(manualCode, '');
  };

  const recognize = async () => {
    if (!videoRef.current) return;
    setState((current) => ({ ...current, phase: 'loading', error: '' }));
    try {
      const blob = await captureFrame(videoRef.current);
      if (!blob) throw new Error('frame');
      const body = await scannerService.recognize(new File([blob], 'scan.jpg', { type: 'image/jpeg' }));
      dispatch(outcomeFromRecognition(body));
    } catch (error) {
      dispatch({ type: 'backend-error', message: error?.response?.data?.message || 'Product recognition is unavailable.' });
    }
  };

  const showCamera = state.phase === 'live' || state.phase === 'loading';

  return (
    <PageContainer>
      <div>
        <h1 className="text-xl font-bold text-slate-900">Product Scanner</h1>
        <p className="text-sm text-slate-500">Point the camera at a barcode or QR code. The photo stays on this device until you ask for image recognition.</p>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card title="Camera">
          {state.phase === 'idle' && (
            <div className="flex min-h-64 flex-col items-center justify-center gap-4 text-center">
              <p className="text-sm text-slate-600">The camera stays off until you start a scan.</p>
              <Button type="button" onClick={() => dispatch({ type: 'permission-granted' })}>Start camera</Button>
            </div>
          )}
          {showCamera && (
            <div className="relative overflow-hidden rounded-xl bg-slate-900">
              <video ref={videoRef} className="aspect-[3/4] w-full object-cover sm:aspect-video" playsInline muted />
              <div className="pointer-events-none absolute inset-0 flex items-center justify-center">
                <div className="h-36 w-64 rounded-lg border-2 border-white/90 shadow-[0_0_0_999px_rgba(15,23,42,0.35)]" />
              </div>
              <p className="absolute inset-x-0 bottom-3 text-center text-sm text-white">Point camera at barcode</p>
              {state.phase === 'loading' && (
                <div className="absolute inset-0 flex items-center justify-center bg-slate-900/50 text-sm font-medium text-white">Looking up product…</div>
              )}
            </div>
          )}
          {state.error && <p className="mt-4 text-sm text-red-600">{state.error}</p>}
          {showCamera && state.phase === 'live' && (
            <div className="mt-4">
              <Button type="button" variant="secondary" onClick={recognize}>Recognize from photo</Button>
            </div>
          )}
          <form onSubmit={submitManual} className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="flex-1">
              <Input label="Enter code manually" value={manualCode} onChange={(event) => setManualCode(event.target.value)} placeholder="Barcode, QR value, or SKU" />
            </div>
            <Button type="submit" variant="secondary">Look up</Button>
          </form>
        </Card>

        <div className="space-y-4">
          {state.phase === 'found' && state.result && <ResultCard result={state.result} onAgain={() => dispatch({ type: 'scan-again' })} />}
          {state.phase === 'missing' && (
            <Card title="No product">
              <p className="text-sm text-slate-700">{state.result?.message || 'Product not found'}</p>
              {state.result?.code && <p className="mt-2 text-sm text-slate-500">Code: {state.result.code}</p>}
              <div className="mt-4"><Button type="button" onClick={() => dispatch({ type: 'scan-again' })}>Scan again</Button></div>
            </Card>
          )}
          {state.phase === 'candidates' && (
            <Card title="Select a product">
              <p className="mb-3 text-sm text-slate-600">Image recognition found more than one possible product. Choose the correct one.</p>
              <ul className="space-y-2">
                {state.candidates.map((candidate) => (
                  <li key={candidate.productId}>
                    <button type="button" className="w-full rounded-lg border border-slate-200 px-3 py-2 text-left text-sm hover:bg-slate-50" onClick={() => candidate.sku ? onCode(candidate.sku, 'SKU') : dispatch({ type: 'found', result: candidateResult(candidate) })}>
                      <span className="font-medium text-slate-900">{candidate.name}</span>
                      {candidate.sku && <span className="mt-1 block text-slate-500">SKU: {candidate.sku}</span>}
                    </button>
                  </li>
                ))}
              </ul>
              <div className="mt-4"><Button type="button" variant="secondary" onClick={() => dispatch({ type: 'scan-again' })}>Scan again</Button></div>
            </Card>
          )}
          {state.phase === 'error' && (
            <Card title="Lookup failed">
              <p className="text-sm text-red-600">{state.error}</p>
              <div className="mt-4"><Button type="button" onClick={() => dispatch({ type: 'scan-again' })}>Scan again</Button></div>
            </Card>
          )}
        </div>
      </div>
    </PageContainer>
  );
}

function candidateResult(candidate) {
  return {
    found: true,
    productId: candidate.productId,
    productSkuId: candidate.productSkuId,
    name: candidate.name,
    sku: candidate.sku,
    imageUrl: candidate.imageUrl,
    message: 'Confirm this image match before using it.',
    variant: [],
  };
}

function ResultCard({ result, onAgain }) {
  const promoted = result.hasPromotion && result.finalPrice != null && result.originalPrice != null && result.finalPrice !== result.originalPrice;
  return (
    <Card title="Product">
      {result.imageUrl && <img src={result.imageUrl} alt="" className="mb-4 h-40 w-full rounded-lg object-contain bg-slate-50" />}
      <h2 className="text-lg font-semibold text-slate-900">{result.name}</h2>
      {result.sku && <p className="mt-1 text-sm text-slate-500">SKU: {result.sku}</p>}
      {result.barcode && <p className="text-sm text-slate-500">Barcode: {result.barcode}</p>}
      {result.message && <p className="mt-2 text-sm text-slate-600">{result.message}</p>}
      <div className="mt-4 flex items-baseline gap-3">
        {promoted && <span className="text-slate-400 line-through">{formatCurrency(result.originalPrice)}</span>}
        {result.finalPrice != null && <span className="text-2xl font-semibold text-slate-900">{formatCurrency(result.finalPrice)}</span>}
        {promoted && result.discountPercentage != null && <span className="text-sm font-medium text-emerald-600">{result.discountPercentage}% OFF</span>}
      </div>
      <dl className="mt-4 grid grid-cols-2 gap-2 text-sm">
        <div><dt className="text-slate-500">Stock</dt><dd className="font-medium">{result.availableQuantity ?? 0}{result.inStock ? '' : ' (out of stock)'}</dd></div>
        {result.lowStock && <div><dt className="text-slate-500">Low stock</dt><dd className="font-medium">Yes</dd></div>}
        {(result.variant || []).map((item) => (
          <div key={`${item.name}-${item.value}`}><dt className="text-slate-500">{item.name}</dt><dd className="font-medium">{item.value}</dd></div>
        ))}
      </dl>
      <div className="mt-6"><Button type="button" onClick={onAgain}>Scan again</Button></div>
    </Card>
  );
}
