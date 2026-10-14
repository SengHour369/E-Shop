import { useState } from 'react';
import Button from '@/components/ui/Button';
import { headersFor, mappingProblem, objectValues, TOOL_LABELS } from '../importLogic';

export default function ImportMapping({ sheet, mapping, tools, permissions, disabled, onConfirm }) {
  const [toolName, setToolName] = useState(sheet.tool || '');
  const [columns, setColumns] = useState(() => objectValues(sheet.confirmed ? mapping?.confirmedValues : mapping?.suggestions));
  const [error, setError] = useState('');
  const tool = tools.find(item => item.name === toolName);
  const confirm = () => {
    const cleaned = Object.fromEntries(Object.entries(columns).filter(([, value]) => value));
    const problem = mappingProblem(tool, cleaned); setError(problem);
    if (!problem) onConfirm(sheet, toolName, cleaned);
  };
  return <fieldset disabled={disabled} className="rounded-xl border border-slate-200 p-4 space-y-3">
    <legend className="px-2 text-sm font-semibold text-slate-800">{sheet.name} · {sheet.confirmed ? 'Confirmed' : 'Confirm mapping'}</legend>
    <label className="block text-sm">Import type<select className="mt-1 block w-full rounded-lg border border-slate-200 p-2" value={toolName} onChange={event => { setToolName(event.target.value); setColumns({}); }}>
      <option value="">Choose import type</option>{tools.filter(item => TOOL_LABELS[item.name]).map(item => <option key={item.name} value={item.name} disabled={!item.enabled || !permissions.includes(item.permission)}>{TOOL_LABELS[item.name]}{!item.enabled ? ' — unavailable' : ''}</option>)}
    </select></label>
    {tool?.unavailableReason && <p className="text-sm text-amber-800">{tool.unavailableReason}</p>}
    <div className="grid gap-3 sm:grid-cols-2">{headersFor(sheet).map(header => <label key={header} className="text-sm text-slate-600">{header}<select className="mt-1 block w-full rounded-lg border border-slate-200 p-2" value={columns[header] || ''} onChange={event => setColumns(current => ({ ...current, [header]: event.target.value }))}><option value="">Ignore column</option>{Object.entries(tool?.schema || {}).map(([field, spec]) => <option key={field} value={field}>{field}{spec.required ? ' *' : ''}</option>)}</select></label>)}</div>
    <details className="text-xs text-slate-500"><summary>AI suggestions · review before confirming</summary><pre className="whitespace-pre-wrap break-words">{JSON.stringify(objectValues(mapping?.suggestions), null, 2)}</pre></details>
    {error && <p role="alert" className="text-sm text-rose-700">{error}</p>}
    <Button variant="secondary" size="sm" onClick={confirm} disabled={!tool?.enabled || !permissions.includes(tool?.permission)}>Confirm mapping</Button>
  </fieldset>;
}
