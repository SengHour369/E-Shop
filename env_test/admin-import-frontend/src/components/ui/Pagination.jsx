import React from 'react';
import { HiChevronLeft, HiChevronRight } from 'react-icons/hi2';
import { cn } from '@/utils/cn';

/**
 * Pagination footer — pairs with Table.jsx's visual language
 * (indigo-600 active state, rounded-lg buttons, slate-50 hovers).
 *
 * @param {number} pageNumber - 1-indexed current page
 * @param {number} totalPages
 * @param {number} pageSize
 * @param {number} totalResults
 * @param {function} onPageChange - (page) => void
 */
export default function Pagination({
  pageNumber = 1,
  totalPages = 0,
  pageSize,
  totalResults,
  onPageChange,
}) {
  const showResultsSummary = typeof pageSize === 'number' && typeof totalResults === 'number';
  
  if (totalPages <= 1 && !showResultsSummary) return null;

  const pages = getPageNumbers(pageNumber, Math.max(1, totalPages));

  const rangeStart = totalResults === 0 ? 0 : (pageNumber - 1) * pageSize + 1;
  const rangeEnd = Math.min(pageNumber * pageSize, totalResults);

  return (
    <div className="rounded-b-xl flex flex-col gap-3 border-t border-slate-100 bg-white px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between">
      {/* Mobile: simple prev/next */}
      <div className="flex items-center justify-between gap-3 sm:hidden">
        <button
          onClick={() => onPageChange(pageNumber - 1)}
          disabled={pageNumber === 1}
          className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        >
          <HiChevronLeft className="h-4 w-4" />
          Previous
        </button>
        <span className="text-sm text-slate-500">
          {pageNumber} / {totalPages}
        </span>
        <button
          onClick={() => onPageChange(pageNumber + 1)}
          disabled={pageNumber === totalPages}
          className="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        >
          Next
          <HiChevronRight className="h-4 w-4" />
        </button>
      </div>

      {/* Desktop */}
      <p className="hidden text-sm text-slate-500 sm:block">
        {showResultsSummary ? (
          <>
            Showing <span className="font-medium text-slate-700">{rangeStart}</span> to{' '}
            <span className="font-medium text-slate-700">{rangeEnd}</span> of{' '}
            <span className="font-medium text-slate-700">{totalResults}</span> results
          </>
        ) : (
          <>
            Showing page <span className="font-medium text-slate-700">{pageNumber}</span> of{' '}
            <span className="font-medium text-slate-700">{totalPages}</span>
          </>
        )}
      </p>

      <nav className="hidden items-center gap-1 sm:flex" aria-label="Pagination">
        <button
          onClick={() => onPageChange(pageNumber - 1)}
          disabled={pageNumber === 1}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        >
          <span className="sr-only">Previous</span>
          <HiChevronLeft className="h-4 w-4" aria-hidden="true" />
        </button>

        {pages.map((page, i) =>
          page === '...' ? (
            <span
              key={`ellipsis-${i}`}
              className="flex h-8 min-w-8 items-center justify-center px-1 text-sm text-slate-400"
            >
              …
            </span>
          ) : (
            <button
              key={page}
              onClick={() => onPageChange(page)}
              aria-current={pageNumber === page ? 'page' : undefined}
              className={cn(
                'flex h-8 min-w-8 items-center justify-center rounded-lg px-2 text-sm font-medium transition-colors',
                pageNumber === page
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-600 hover:bg-slate-100'
              )}
            >
              {page}
            </button>
          )
        )}

        <button
          onClick={() => onPageChange(pageNumber + 1)}
          disabled={pageNumber === totalPages}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
        >
          <span className="sr-only">Next</span>
          <HiChevronRight className="h-4 w-4" aria-hidden="true" />
        </button>
      </nav>
    </div>
  );
}

/** Builds a compact 1-indexed page list, e.g. 1 2 3 ... 13, with no adjacent duplicate ellipses. */
function getPageNumbers(current, total, siblings = 1) {
  const totalNumbers = siblings * 2 + 5;
  if (total <= totalNumbers) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  const left = Math.max(current - siblings, 1);
  const right = Math.min(current + siblings, total);

  const pages = [1];
  if (left > 2) pages.push('...');
  for (let i = left; i <= right; i++) {
    if (i !== 1 && i !== total) pages.push(i);
  }
  if (right < total - 1) pages.push('...');
  if (total > 1) pages.push(total);

  return pages;
}