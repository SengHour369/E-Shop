import { cn } from '@/utils/cn';
import { formatCompact, formatPercent } from '@/utils/formatCurrency';
import { HiArrowTrendingUp, HiArrowTrendingDown } from 'react-icons/hi2';

const colorMap = {
  indigo:  { bg: 'bg-gradient-to-br from-indigo-500 to-indigo-600', icon: 'bg-white/20 text-white', text: 'text-indigo-600', border: 'border-indigo-100', shadow: 'shadow-indigo-500/20' },
  emerald: { bg: 'bg-gradient-to-br from-emerald-500 to-emerald-600', icon: 'bg-white/20 text-white', text: 'text-emerald-600', border: 'border-emerald-100', shadow: 'shadow-emerald-500/20' },
  amber:   { bg: 'bg-gradient-to-br from-amber-500 to-amber-600',   icon: 'bg-white/20 text-white', text: 'text-amber-600', border: 'border-amber-100', shadow: 'shadow-amber-500/20' },
  violet:  { bg: 'bg-gradient-to-br from-violet-500 to-violet-600',  icon: 'bg-white/20 text-white', text: 'text-violet-600', border: 'border-violet-100', shadow: 'shadow-violet-500/20' },
};

/**
 * Dashboard stats card.
 * @param {string} title
 * @param {number} value
 * @param {number} growth - percentage growth (positive or negative)
 * @param {React.ReactNode} icon
 * @param {'indigo'|'emerald'|'amber'|'violet'} color
 * @param {boolean} isCurrency
 */
export default function StatsCard({ title, value, growth, icon, color = 'indigo', isCurrency = false }) {
  const colors = colorMap[color] || colorMap.indigo;
  const isPositive = growth >= 0;

  return (
    <div className={cn(
      "group relative overflow-hidden rounded-2xl p-6 transition-all duration-300",
      "border bg-white hover:-translate-y-1 hover:shadow-xl",
      colors.border
    )}>
      {/* Background decoration */}
      <div className={cn("absolute -right-6 -top-6 h-32 w-32 rounded-full opacity-10 blur-2xl transition-transform duration-500 group-hover:scale-150", colors.bg)} />
      
      <div className="relative z-10 flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-semibold tracking-wide text-slate-500 uppercase">{title}</p>
          <div className="mt-3 flex items-baseline gap-2">
            <p className="text-3xl font-extrabold text-slate-900 tracking-tight">
              {isCurrency ? '$' : ''}{formatCompact(value || 0)}
            </p>
          </div>
          
          <div className="mt-4 flex items-center gap-2">
            <div className={cn(
              "flex items-center gap-1 rounded-full px-2 py-1 text-xs font-bold",
              isPositive ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-700"
            )}>
              {isPositive ? <HiArrowTrendingUp className="h-3.5 w-3.5" /> : <HiArrowTrendingDown className="h-3.5 w-3.5" />}
              {formatPercent(growth || 0)}
            </div>
            <span className="text-xs font-medium text-slate-400">vs last month</span>
          </div>
        </div>

        <div className={cn(
          "flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3",
          colors.bg,
          colors.shadow
        )}>
          <span className={colors.icon}>{icon}</span>
        </div>
      </div>
    </div>
  );
}
