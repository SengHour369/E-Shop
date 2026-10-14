import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-[80vh] flex-col items-center justify-center text-center">
      {/* Animated 404 */}
      <div className="relative">
        <div className="absolute -inset-4 rounded-full bg-indigo-100 opacity-50 blur-2xl" />
        <span className="relative text-[120px] font-black text-slate-200 leading-none select-none">
          404
        </span>
      </div>

      <div className="mt-4 space-y-2">
        <h2 className="text-2xl font-bold text-slate-900">Page not found</h2>
        <p className="max-w-sm text-slate-500">
          Sorry, we couldn't find the page you're looking for. It may have been moved or doesn't exist.
        </p>
      </div>

      <div className="mt-8 flex items-center gap-3">
        <Link
          to="/dashboard"
          className="rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700 transition-colors"
        >
          ← Back to Dashboard
        </Link>
        <Link
          to="/dashboard/orders"
          className="rounded-xl border border-slate-200 px-5 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
        >
          View Orders
        </Link>
      </div>

      {/* Decorative dots */}
      <div className="mt-12 flex gap-2">
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className="h-2 w-2 rounded-full bg-indigo-200"
            style={{ animationDelay: `${i * 0.1}s` }}
          />
        ))}
      </div>
    </div>
  );
}
