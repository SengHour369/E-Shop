// src/components/ui/Card.js
export const Card = ({ title, children }) => (
  <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
    {title && (
      <div className="px-6 py-4 border-b border-slate-100 bg-slate-50/50">
        <h2 className="text-sm font-bold text-slate-700 uppercase tracking-wider">
          {title}
        </h2>
      </div>
    )}
    <div className="p-6">
      {children}
    </div>
  </div>
);