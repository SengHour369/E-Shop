import { cn } from '@/utils/cn';
import { HiOutlineInbox } from 'react-icons/hi2';

/**
 * Responsive table with striped rows, hover effects, and sortable header UI.
 *
 * @param {Array} columns - [{ key, label, sortable, align, render, width }]
 * @param {Array} data - row data
 * @param {string} sortKey - current sort key
 * @param {'asc'|'desc'} sortDir
 * @param {function} onSort - (key) => void
 * @param {boolean} loading - shows skeleton rows
 * @param {React.ReactNode} emptyState - custom empty state content
 * @param {boolean} stickyHeader
 */
export default function Table({
  columns = [],
  data = [],
  sortKey,
  sortDir = 'asc',
  onSort,
  loading = false,
  emptyState,
  stickyHeader = false,
  className,
  onRowClick,
}) {
  const isEmpty = !loading && data.length === 0;

  return (
    <div
      className={cn(
        'overflow-hidden rounded-t-2xl border border-slate-200/70 bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]',
        className
      )}
    >
      <div className="overflow-x-auto">
        <table className="w-full min-w-full border-collapse">
          <thead className={cn(stickyHeader && 'sticky top-0 z-10')}>
            <tr className="border-b border-slate-100 bg-slate-50/80 backdrop-blur-sm">
              {columns.map(col => (
                <th
                  key={col.key}
                  style={col.width ? { width: col.width } : undefined}
                  className={cn(
                    'whitespace-nowrap px-4 py-3.5 text-left text-[11px] font-semibold uppercase tracking-wider text-slate-500',
                    col.align === 'right' && 'text-right',
                    col.align === 'center' && 'text-center',
                    col.sortable && 'cursor-pointer select-none transition-colors hover:text-slate-800'
                  )}
                  onClick={() => col.sortable && onSort?.(col.key)}
                >
                  <span
                    className={cn(
                      'inline-flex items-center gap-1.5',
                      col.align === 'right' && 'justify-end',
                      col.align === 'center' && 'justify-center'
                    )}
                  >
                    {col.label}
                    {col.sortable && (
                      <span className="flex flex-col -space-y-0.5">
                        <svg
                          className={cn(
                            'h-2.5 w-2.5 transition-colors',
                            sortKey === col.key && sortDir === 'asc' ? 'text-indigo-600' : 'text-slate-300'
                          )}
                          viewBox="0 0 16 16" fill="currentColor"
                        >
                          <path d="M8 4l4 6H4z" />
                        </svg>
                        <svg
                          className={cn(
                            'h-2.5 w-2.5 transition-colors',
                            sortKey === col.key && sortDir === 'desc' ? 'text-indigo-600' : 'text-slate-300'
                          )}
                          viewBox="0 0 16 16" fill="currentColor"
                        >
                          <path d="M8 12l4-6H4z" />
                        </svg>
                      </span>
                    )}
                  </span>
                </th>
              ))}
            </tr>
          </thead>

          <tbody className="divide-y divide-slate-50 bg-white">
            {loading ? (
              Array.from({ length: 6 }).map((_, rowIdx) => (
                <tr key={rowIdx} className={cn(rowIdx % 2 === 1 && 'bg-slate-50/50')}>
                  {columns.map((col, colIdx) => (
                    <td key={col.key} className="px-4 py-4">
                      <div
                        className="h-3.5 animate-pulse rounded-full bg-slate-100"
                        style={{ width: colIdx === 0 ? '70%' : `${55 + ((colIdx * 13) % 35)}%` }}
                      />
                    </td>
                  ))}
                </tr>
              ))
            ) : isEmpty ? (
              <tr>
                <td colSpan={columns.length} className="py-20 text-center">
                  {emptyState ?? (
                    <div className="flex flex-col items-center gap-3">
                      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-50 ring-1 ring-slate-100">
                        <HiOutlineInbox className="h-5 w-5 text-slate-300" />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-sm font-medium text-slate-600">No results found</p>
                        <p className="text-xs text-slate-400">Try adjusting your filters or search terms</p>
                      </div>
                    </div>
                  )}
                </td>
              </tr>
            ) : (
              data.map((row, rowIdx) => (
                <tr
                  key={row.id ?? rowIdx}
                  onClick={() => onRowClick?.(row)}
                  className={cn(
                    'group transition-colors duration-150 hover:bg-indigo-50/50',
                    rowIdx % 2 === 1 && 'bg-slate-50/40',
                    onRowClick && 'cursor-pointer'
                  )}
                >
                  {columns.map(col => (
                    <td
                      key={col.key}
                      className={cn(
                        'px-4 py-3.5 text-sm text-slate-700',
                        col.align === 'right' && 'text-right',
                        col.align === 'center' && 'text-center'
                      )}
                    >
                      {col.render ? col.render(row[col.key], row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}