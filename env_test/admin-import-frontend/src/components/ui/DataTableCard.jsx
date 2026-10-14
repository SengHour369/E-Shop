import React from 'react';

export default function DataTableCard({ toolbar, children, footer, loading, error, loadingMessage = 'Loading...' }) {
  return (
    <div className="flex flex-col rounded-xl border border-slate-200 bg-white shadow-sm">
      {toolbar}
      
      {error && (
        <div className="m-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="overflow-x-auto bg-white">
        {children}
      </div>

      {footer && (
        <div className="flex flex-col sm:flex-row items-center justify-between border-t border-slate-100 bg-white px-5 sm:px-6 py-4 rounded-b-xl gap-4">
          {footer}
        </div>
      )}
    </div>
  );
}
