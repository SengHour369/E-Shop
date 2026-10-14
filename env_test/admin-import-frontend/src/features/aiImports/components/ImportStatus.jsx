const tones = {
  COMPLETED: 'bg-emerald-50 text-emerald-700', CREATED: 'bg-emerald-50 text-emerald-700', VALID: 'bg-emerald-50 text-emerald-700', VALIDATED: 'bg-emerald-50 text-emerald-700',
  INVALID: 'bg-rose-50 text-rose-700', FAILED: 'bg-rose-50 text-rose-700', REJECTED: 'bg-rose-50 text-rose-700',
  DUPLICATE: 'bg-amber-50 text-amber-800', PARTIAL_SUCCESS: 'bg-amber-50 text-amber-800', CHANGES_REQUESTED: 'bg-amber-50 text-amber-800',
  SUBMITTED: 'bg-indigo-50 text-indigo-700', APPROVED: 'bg-indigo-50 text-indigo-700', PROCESSING: 'bg-blue-50 text-blue-700',
};
export default function ImportStatus({ status }) {
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${tones[status] || 'bg-slate-100 text-slate-600'}`}>{String(status || 'DRAFT').replaceAll('_', ' ')}</span>;
}
