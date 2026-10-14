import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { FileSpreadsheet, Upload, ShieldCheck, History, RefreshCw } from 'lucide-react';
import PageContainer from '@/components/layouts/PageContainer';
import { Card } from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import ImportStatus from '../components/ImportStatus';
import ImportMapping from '../components/ImportMapping';
import ImportRows from '../components/ImportRows';
import AssistantChat from '../components/AssistantChat';
import { importService } from '../services/import.service';
import { canApprove, canExecute, editable, errorMessage, ROW_FILTERS } from '../importLogic';

const links = [['assistant', '/dashboard/ai-assistant', 'AI Assistant'], ['review', '/dashboard/import-approvals', 'Approvals'], ['executions', '/dashboard/import-executions', 'Tool executions']];
const inputStyle = 'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm';

export default function AiImportsPage({ mode = 'assistant' }) {
  const [access, setAccess] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    const controller = new AbortController();
    importService.access(controller.signal).then(setAccess).catch(failure => { if (!controller.signal.aborted) setError(errorMessage(failure)); });
    return () => controller.abort();
  }, []);
  if (error) return <p role="alert" className="rounded-xl bg-rose-50 p-6 text-rose-700">{error}</p>;
  if (!access) return <p role="status" className="p-8 text-slate-500">Checking import permissions…</p>;
  const allowed = mode === 'review' ? access.permissions?.includes('IMPORT_REVIEW') : access.permissions?.some(code => ['IMPORT_UPLOAD', 'IMPORT_REVIEW'].includes(code));
  if (!allowed) return <Card title="Excel imports"><p className="text-slate-600">Your account does not have access to this import screen.</p></Card>;
  return <ImportWorkspace key={mode} mode={mode} access={access}/>;
}

function ImportWorkspace({ mode, access }) {
  const [search, setSearch] = useSearchParams();
  const batchId = search.get('batch') || '';
  const [tools, setTools] = useState([]);
  const [batches, setBatches] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [listPage, setListPage] = useState(0);
  const [assigned, setAssigned] = useState(mode === 'executions' && access.permissions.includes('IMPORT_REVIEW') && !access.administrator);
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState('');
  const [preview, setPreview] = useState(null);
  const [execution, setExecution] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [working, setWorking] = useState(false);
  const [loading, setLoading] = useState(Boolean(batchId));
  const [listLoading, setListLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [refresh, setRefresh] = useState(0);
  const [reviewerId, setReviewerId] = useState('');
  const [reason, setReason] = useState('');
  const inFlight = useRef(false);
  const fileInput = useRef(null);
  const has = code => access.permissions.includes(code);
  const busy = working || loading;
  const batch = preview?.batch;
  const currentPreview = batch?.id === batchId;
  const isEditable = currentPreview && editable(batch) && !busy;
  const reviewList = mode === 'review';
  const assignedList = !reviewList && (assigned || !has('IMPORT_UPLOAD'));

  useEffect(() => {
    const controller = new AbortController();
    importService.tools(controller.signal).then(setTools).catch(failure => { if (!controller.signal.aborted) setError(errorMessage(failure)); });
    return () => controller.abort();
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    importService.list(reviewList, listPage, assignedList, controller.signal).then(result => { setBatches(result); setListLoading(false); }).catch(failure => { if (!controller.signal.aborted) { setError(errorMessage(failure)); setListLoading(false); } });
    return () => controller.abort();
  }, [reviewList, assignedList, listPage, refresh]);
  useEffect(() => {
    if (!batchId) return undefined;
    const controller = new AbortController();
    importService.preview(batchId, page, filter, controller.signal).then(result => { setPreview(result); setLoading(false); }).catch(failure => { if (!controller.signal.aborted) { setError(errorMessage(failure)); setLoading(false); } });
    return () => controller.abort();
  }, [batchId, page, filter, refresh]);

  const select = id => {
    if (busy) return;
    setPage(0); setFilter(''); setPreview(null); setExecution(null); setHistory(null); setReason(''); setError(''); setNotice(''); setLoading(true); setSearch({ batch: id });
    if (id === batchId) setRefresh(value => value + 1);
  };
  const reload = () => { setLoading(Boolean(batchId)); setListLoading(true); setRefresh(value => value + 1); setError(''); };
  const mutate = async operation => {
    if (inFlight.current || busy) return;
    inFlight.current = true; setWorking(true); setError(''); setNotice('');
    try {
      const result = await operation();
      setNotice('Saved. Review the current import status and validation results.');
      setExecution(null); setHistory(null); setLoading(true); setListLoading(true);
      if (result.id !== batchId) { setPreview(null); setPage(0); setFilter(''); setSearch({ batch: result.id }); }
      setRefresh(value => value + 1);
    } catch (failure) { setError(errorMessage(failure)); }
    finally { inFlight.current = false; setWorking(false); }
  };
  const upload = event => {
    const file = event.target.files?.[0]; event.target.value = '';
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) { setError('Choose an .xlsx or .xls workbook.'); return; }
    mutate(() => importService.upload(file));
  };
  const action = name => mutate(() => importService.action(batch, name, name === 'submit' ? { reviewerId: Number(reviewerId) } : ['approve', 'reject', 'request-changes'].includes(name) ? { reason: reason.trim() } : {}));
  const download = async kind => {
    if (busy) return; setWorking(true); setError('');
    try {
      const blob = await importService.download(batchId, kind); const url = URL.createObjectURL(blob);
      const link = document.createElement('a'); link.href = url; link.download = kind === 'report' ? `import-${batchId}.csv` : `original-${batchId}.bin`;
      document.body.appendChild(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (failure) { setError(errorMessage(failure)); } finally { setWorking(false); }
  };
  const showHistory = useCallback(async () => {
    if (!batchId) return;
    setHistoryLoading(true); setError('');
    try { const [attempts, audit] = await Promise.all([importService.executions(batchId), importService.history(batchId)]); setExecution({ batchId, ...attempts }); setHistory({ batchId, ...audit }); }
    catch (failure) { setError(errorMessage(failure)); } finally { setHistoryLoading(false); }
  }, [batchId]);
  const movePage = next => { setLoading(true); setPage(next); };
  const title = mode === 'review' ? 'Excel import approvals' : mode === 'executions' ? 'Import execution history' : 'AI Assistant';

  return <PageContainer>
    <header className="flex flex-wrap items-start justify-between gap-4"><div><p className="mb-2 text-xs font-semibold uppercase tracking-widest text-indigo-600">E-Shop workspace</p><h1 className="text-2xl font-bold text-slate-900">{title}</h1><p className="mt-2 text-sm text-slate-500">Prepare your data, review each change, and track confirmed results.</p></div><div className="rounded-xl bg-indigo-50 p-3"><FileSpreadsheet className="h-7 w-7 text-indigo-600"/></div></header>
    <nav aria-label="AI import screens" className="flex flex-wrap gap-2">{links.filter(([key]) => key !== 'review' || has('IMPORT_REVIEW')).map(([key, path, label]) => <Link key={key} to={path} className={`rounded-lg px-4 py-2 text-sm font-medium ${mode === key ? 'bg-indigo-600 text-white' : 'border border-slate-200 bg-white text-slate-600'}`}>{label}</Link>)}</nav>
    {mode === 'assistant' && <Card title="Your assistant"><AssistantChat administrator={access.administrator}/></Card>}
    {error && <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"><p>{error}</p><Button variant="secondary" size="sm" disabled={busy} onClick={reload}>Reload current data</Button></div>}
    {notice && <p role="status" className="rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700">{notice}</p>}
    {mode === 'assistant' && has('IMPORT_UPLOAD') && <Card title="Attach an Excel workbook"><div className="flex flex-wrap items-center justify-between gap-4 rounded-xl border-2 border-dashed border-indigo-200 bg-indigo-50/40 p-6"><div className="flex items-center gap-4"><Upload className="h-8 w-8 text-indigo-500"/><div><h2 className="font-semibold text-slate-800">Start a new import</h2><p className="mt-1 text-sm text-slate-500">.xlsx or .xls · multiple worksheets · literal values only</p><p className="mt-1 text-xs text-slate-400">Default server limit: 5 MB and 10,000 rows. Each mapping needs confirmation.</p></div></div><input ref={fileInput} type="file" accept=".xlsx,.xls" className="sr-only" aria-label="Attach Excel workbook" onChange={upload} disabled={busy}/><Button disabled={busy} onClick={() => fileInput.current?.click()}>Choose workbook</Button></div></Card>}
    <Card title={mode === 'review' ? 'Pending review' : 'Import batches'}><div className="mb-4 flex items-center justify-between text-sm"><span className="text-slate-500">{batches.totalElements} imports</span><Button variant="secondary" size="sm" disabled={busy || listLoading} onClick={reload}><RefreshCw size={14}/>Refresh</Button></div>
      {mode !== 'review' && !access.administrator && has('IMPORT_UPLOAD') && has('IMPORT_REVIEW') && <label className="mb-4 block text-sm text-slate-500">Show <select className="ml-2 rounded-lg border border-slate-200 p-2" value={assigned ? 'assigned' : 'owned'} disabled={busy || listLoading} onChange={event => { setAssigned(event.target.value === 'assigned'); setListPage(0); setListLoading(true); }}><option value="owned">My uploads</option><option value="assigned">Assigned to me · all statuses</option></select></label>}
      {!batches.content.length && <p className="py-6 text-center text-sm text-slate-500">{listLoading ? 'Loading imports…' : mode === 'review' ? 'No imports are waiting for review.' : 'No imports yet. Attach a workbook to get started.'}</p>}
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">{batches.content.map(item => <button key={item.id} disabled={busy} onClick={() => select(item.id)} className={`rounded-xl border p-4 text-left transition hover:border-indigo-300 ${item.id === batchId ? 'border-indigo-400 bg-indigo-50/40' : 'border-slate-200'}`}><div className="flex items-center justify-between gap-2"><FileSpreadsheet size={18} className="text-indigo-500"/><ImportStatus status={item.status}/></div><p className="mt-3 truncate text-xs font-mono text-slate-600" title={item.id}>{item.id}</p><p className="mt-2 text-xs text-slate-400">Version {item.revision} · Owner #{item.ownerId}</p></button>)}</div>
      <Pagination page={listPage} pages={batches.totalPages} disabled={busy || listLoading} onChange={next => { setListLoading(true); setListPage(next); }}/>
    </Card>
    {loading && <p role="status" className="text-sm text-slate-500">Loading the current import…</p>}
    {currentPreview && <>
      <Card title="Import analysis"><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="break-all text-sm font-mono text-slate-500">{batch.id}</p><p className="mt-2 text-sm text-slate-500">Version {batch.revision} · {preview.sheets.length} worksheets</p></div><ImportStatus status={batch.status}/></div>
        <div className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-4"><Metric label="Total rows" value={preview.summary?.totalRows ?? preview.rows.totalElements}/><Metric label="Valid" value={preview.summary?.statuses?.VALID || 0}/><Metric label="Needs correction" value={(preview.summary?.statuses?.INVALID || 0) + (preview.summary?.statuses?.DUPLICATE || 0)}/><Metric label="Confirmed results" value={preview.summary?.statuses?.CREATED || 0}/></div>
        <div className="mt-4 flex flex-wrap gap-3 text-xs text-slate-500">{Object.entries(preview.summary?.statuses || {}).map(([status, count]) => <span key={status}>{status.replaceAll('_', ' ')}: {count}</span>)}<span>Edited: {preview.summary?.editedRows || 0}</span></div>
        <div className="mt-4 flex flex-wrap gap-2"><Button variant="secondary" size="sm" disabled={busy} onClick={reload}>Reload version</Button><Button variant="secondary" size="sm" disabled={busy} onClick={() => download('report')}>Download report</Button><Button variant="ghost" size="sm" disabled={busy} onClick={() => download('original')}>Original workbook</Button></div>
      </Card>
      {mode !== 'executions' && <Card title="Worksheet mappings"><p className="mb-4 text-sm text-slate-500">AI suggestions are advisory. Confirm the import type and every required field before validating.</p><div className="grid gap-4 lg:grid-cols-2">{preview.sheets.map(sheet => <ImportMapping key={`${sheet.id}:${batch.revision}`} sheet={sheet} mapping={preview.mappings.find(item => item.sheetId === sheet.id)} tools={tools} permissions={access.permissions} disabled={!isEditable} onConfirm={(item, tool, columns) => mutate(() => importService.mapping(batch, item, tool, columns))}/>)}</div><Button className="mt-4" variant="secondary" disabled={!isEditable} onClick={() => action('analyze')}>Suggest mappings with AI</Button></Card>}
      <Card title="Row preview"><div className="mb-4 flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-slate-500">{preview.rows.totalElements} matching rows · original values are preserved</p><label className="text-sm text-slate-500">Filter <select value={filter} disabled={busy} onChange={event => { setLoading(true); setPage(0); setFilter(event.target.value); }} className="ml-2 rounded-lg border border-slate-200 p-2">{ROW_FILTERS.map(status => <option key={status} value={status}>{status || 'All rows'}</option>)}</select></label></div><ImportRows preview={preview} tools={tools} permissions={access.permissions} disabled={!isEditable || mode === 'executions'} onSave={(row, values) => mutate(() => importService.edit(batch, row, values))} onSkip={row => mutate(() => importService.skip(batch, row))}/><Pagination page={page} pages={preview.rows.totalPages} disabled={busy} onChange={movePage}/></Card>
      {mode !== 'executions' && <Card title="Review and approval"><div className="flex items-start gap-3 rounded-lg bg-slate-50 p-4"><ShieldCheck className="h-5 w-5 shrink-0 text-indigo-500"/><p className="text-sm text-slate-600">An independent assigned reviewer must approve the current version. Editing a row or mapping invalidates earlier approval.</p></div><div className="mt-4 space-y-4">
        <Button variant="secondary" disabled={!isEditable} onClick={() => action('validate')}>Validate current rows</Button>
        {has('IMPORT_UPLOAD') && batch.ownerId === access.userId && editable(batch) && <div className="flex flex-wrap items-end gap-3"><label className="text-sm text-slate-600">Reviewer user ID<input type="number" min="1" step="1" value={reviewerId} disabled={busy} onChange={event => setReviewerId(event.target.value)} className={inputStyle}/></label><Button disabled={busy || !Number.isSafeInteger(Number(reviewerId)) || Number(reviewerId) <= 0 || Number(reviewerId) === access.userId || batch.status !== 'VALIDATED'} onClick={() => action('submit')}>Submit for approval</Button><p className="text-xs text-slate-500">Validate all included rows before submitting.</p></div>}
        {batch.status === 'SUBMITTED' && has('IMPORT_REVIEW') && batch.assignedReviewerId === access.userId && <div className="space-y-3"><label className="block text-sm text-slate-600">Review reason<textarea rows={3} maxLength={2000} value={reason} onChange={event => setReason(event.target.value)} disabled={busy} className={inputStyle}/></label><div className="flex flex-wrap gap-2"><Button variant="success" disabled={busy || !reason.trim() || !canApprove(batch, access)} onClick={() => action('approve')}>Approve version {batch.revision}</Button><Button variant="secondary" disabled={busy || !reason.trim()} onClick={() => action('request-changes')}>Request changes</Button><Button variant="danger" disabled={busy || !reason.trim()} onClick={() => action('reject')}>Reject import</Button></div></div>}
      </div></Card>}
      {canExecute(batch, access) && <Card title="Execute approved import"><p className="mb-4 text-sm text-slate-600">Each action processes up to 10 rows. Continue while processing; failed rows can be retried. Confirmed rows are never repeated.</p><Button loading={working} disabled={busy} onClick={() => action('execute')}>{['PARTIAL_SUCCESS', 'FAILED'].includes(batch.status) ? 'Retry failed rows' : batch.status === 'PROCESSING' ? 'Process next 10 rows' : 'Execute approved rows'}</Button></Card>}
      <Card title="Execution and audit history"><Button variant="secondary" size="sm" disabled={busy || historyLoading} onClick={showHistory}><History size={16}/>{historyLoading ? 'Loading history…' : 'Load execution and audit history'}</Button>
        {execution?.batchId === batchId && <div className="mt-4 overflow-x-auto"><table className="w-full text-left text-sm"><thead className="text-xs uppercase text-slate-500"><tr><th className="p-3">Row</th><th className="p-3">Status</th><th className="p-3">Confirmed ID</th><th className="p-3">Error</th></tr></thead><tbody>{execution.rows?.map(item => <tr key={item.id} className="border-t border-slate-100"><td className="p-3 text-xs font-mono">{item.rowId}</td><td className="p-3"><ImportStatus status={item.status}/></td><td className="p-3">{item.resourceId || 'Unconfirmed'}</td><td className="p-3 text-rose-700">{item.errorCode}</td></tr>)}</tbody></table>{!execution.rows?.length && <p className="py-4 text-sm text-slate-500">Execution has not started.</p>}<details className="mt-3 text-sm text-slate-500"><summary>Execution attempts</summary><pre className="whitespace-pre-wrap break-words text-xs">{JSON.stringify(execution.executions, null, 2)}</pre></details></div>}
        {history?.batchId === batchId && <div className="mt-4 space-y-3">{history.audit?.map(item => <details key={item.id} className="rounded-lg border border-slate-100 p-3"><summary className="cursor-pointer text-sm text-slate-600">{item.action} · Version {item.revision} · User #{item.actorId} · {item.createdAt}</summary><p className="mt-2 break-all text-xs text-slate-400">Request: {item.requestId || '—'} · Trace: {item.traceId || '—'}</p><pre className="mt-2 whitespace-pre-wrap break-words text-xs">{item.details}</pre></details>)}<details className="text-sm text-slate-500"><summary>Approval decisions and immutable revisions</summary><pre className="whitespace-pre-wrap break-words text-xs">{JSON.stringify({ approvals: history.approvals, revisions: history.revisions }, null, 2)}</pre></details></div>}
      </Card>
    </>}
  </PageContainer>;
}
function Metric({ label, value }) { return <div className="rounded-xl border border-slate-100 bg-slate-50/60 p-4"><p className="text-xs text-slate-500">{label}</p><p className="mt-2 text-2xl font-bold text-slate-800">{value}</p></div>; }
function Pagination({ page, pages, disabled, onChange }) { return <div className="mt-4 flex items-center justify-end gap-3 text-xs text-slate-500"><Button variant="secondary" size="sm" disabled={disabled || page <= 0} onClick={() => onChange(page - 1)}>Previous</Button><span>Page {page + 1} of {pages || 1}</span><Button variant="secondary" size="sm" disabled={disabled || page + 1 >= pages} onClick={() => onChange(page + 1)}>Next</Button></div>; }
