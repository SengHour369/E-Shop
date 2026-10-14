import { useState } from 'react';
import Button from '@/components/ui/Button';
import ImportStatus from './ImportStatus';
import { fieldsFor, objectValues } from '../importLogic';

function Row({ row, sheets, tools, permissions, errors, disabled, onSave, onSkip }) {
  const [values, setValues] = useState(() => objectValues(row.valuesJson));
  const sheet = sheets.find(item => item.id === row.sheetId);
  const tool = tools.find(item => item.name === sheet?.tool);
  const canEdit = sheet?.confirmed && tool?.enabled && permissions.includes(tool.permission);
  return <tr className="border-t border-slate-100 align-top">
    <td className="p-4 text-sm"><strong>{sheet?.name}</strong><p className="text-slate-500">Row {row.rowNumber}</p></td>
    <td className="p-4 min-w-48"><dl className="text-xs space-y-2">{Object.entries(objectValues(row.originalValues)).map(([key, value]) => <div key={key}><dt className="font-semibold text-slate-500">{key}</dt><dd className="break-words whitespace-pre-wrap">{String(value)}</dd></div>)}</dl></td>
    <td className="p-4 min-w-60 space-y-2">{fieldsFor(row, sheets, tools).map(field => <label key={field} className="block text-xs text-slate-500">{field}<input className="mt-1 block w-full rounded-lg border border-slate-200 p-2 text-sm text-slate-800" value={values[field] ?? ''} maxLength={2000} disabled={disabled || !canEdit} onChange={event => setValues(current => ({ ...current, [field]: event.target.value }))}/></label>)}
      <div className="flex gap-2"><Button size="sm" variant="secondary" disabled={disabled || !canEdit} onClick={() => onSave(row, values)}>Save</Button><Button size="sm" variant="ghost" disabled={disabled} onClick={() => onSkip(row)}>{row.skipped ? 'Include' : 'Skip'}</Button></div>
    </td>
    <td className="p-4 min-w-52"><ImportStatus status={row.status}/>{row.edited && <p className="mt-2 text-xs text-indigo-600">Edited</p>}{errors.filter(error => error.rowId === row.id).map(error => <div key={error.id || error.message} className="mt-2 text-xs"><p className="text-rose-700">{error.message}</p>{error.explanation && <p className="mt-1 text-slate-500">AI suggestion: {error.explanation}</p>}</div>)}</td>
    <td className="p-4 text-xs text-slate-500 min-w-36">{row.affectedResourceId && <p>Affected: {row.affectedResourceId}</p>}<p>{row.resourceId ? `Confirmed: ${row.resourceId}` : 'No confirmed result'}</p></td>
  </tr>;
}
export default function ImportRows({ preview, tools, permissions, disabled, onSave, onSkip }) {
  return <div className="overflow-x-auto"><table className="w-full text-left"><thead className="bg-slate-50 text-xs uppercase text-slate-500"><tr>{['Sheet / row', 'Original values', 'Corrections', 'Validation', 'Resource'].map(title => <th key={title} className="p-4">{title}</th>)}</tr></thead><tbody>{preview.rows.content.map(row => <Row key={`${row.id}:${preview.batch.revision}:${row.valuesJson}`} row={row} sheets={preview.sheets} tools={tools} permissions={permissions} errors={preview.errors} disabled={disabled} onSave={onSave} onSkip={onSkip}/>)}</tbody></table>{!preview.rows.content.length && <p className="p-8 text-center text-sm text-slate-500">No rows match this filter.</p>}</div>;
}
