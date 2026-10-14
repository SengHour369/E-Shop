import { Link } from 'react-router-dom';
import { cn } from '@/utils/cn';

import { StatCardSkeleton } from '@/components/ui/Skeleton';

export default function PageHeader({ title, description, crumbs, stats = [], loading, children }) {
  return (
    <div className="mb-6 space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">{title}</h2>
          {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
          {crumbs && crumbs.length > 0 && (
            <div className="mt-2 flex items-center gap-2 text-xs font-medium text-slate-500">
              {crumbs.map((crumb, idx) => {
                const isLast = idx === crumbs.length - 1;
                return (
                  <span key={idx} className="flex items-center gap-2">
                    {crumb.path && !isLast ? (
                      <Link to={crumb.path} className="hover:text-blue-600 transition-colors">
                        {crumb.label}
                      </Link>
                    ) : (
                      <span className={isLast ? "text-slate-400" : "text-slate-500"}>
                        {crumb.label}
                      </span>
                    )}
                    {!isLast && <span className="text-slate-300">›</span>}
                  </span>
                );
              })}
            </div>
          )}
        </div>
        {children && (
          <div className="flex items-center gap-3">
            {children}
          </div>
        )}
      </div>

      {loading && stats && stats.length > 0 ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-2">
          {Array.from({ length: stats.length }).map((_, idx) => (
            <div key={idx} className="bg-white rounded-2xl shadow-sm border border-slate-100 p-5">
              <StatCardSkeleton />
            </div>
          ))}
        </div>
      ) : stats && stats.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-2">
          {stats.map((stat, idx) => (
            <div key={idx} className="bg-white rounded-2xl shadow-sm border border-slate-100 p-5 flex items-start gap-4 transition-all hover:shadow-md">
              <div className={cn('flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl ring-4 ring-white', stat.iconBgColorClass)}>
                {stat.icon}
              </div>
              <div className="min-w-0">
                <p className="text-xs font-medium text-slate-400">{stat.label}</p>
                <div className="mt-1 flex items-baseline gap-2">
                  <p className="text-2xl font-bold text-slate-900 tabular-nums">{stat.value}</p>
                  <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide', stat.badgeColorClass)}>
                    {stat.badgeText}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
