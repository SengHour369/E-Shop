import { HiOutlineCreditCard, HiOutlineCheckCircle, HiOutlineClock, HiOutlineXCircle } from 'react-icons/hi2';
import { cn } from '@/utils/cn';

function OverviewCard({ icon, iconBg, iconRing, label, value, growth }) {
  const isPositive = growth >= 0;
  return (
    <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5 flex items-start gap-4">
      <div className={cn(
        'flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full text-xl',
        iconBg, iconRing, 'ring-4'
      )}>
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-slate-400 mb-0.5">{label}</p>
        <p className="text-xl font-bold text-slate-900 tabular-nums">
          ${typeof value === 'number'
            ? value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
            : value}
        </p>
        {growth !== undefined && (
          <p className={cn(
            'mt-1 text-xs font-medium flex items-center gap-1',
            isPositive ? 'text-emerald-600' : 'text-red-500'
          )}>
            <span>{isPositive ? '↑' : '↓'} {Math.abs(growth)}%</span>
            <span className="text-slate-400 font-normal">from last 7 days</span>
          </p>
        )}
      </div>
    </div>
  );
}

import { StatCardSkeleton } from '@/components/ui/Skeleton';

export default function PaymentStats({ stats }) {
  if (!stats) {
    return (
      <div className="grid grid-cols-2 gap-4 xl:grid-cols-4 mb-6">
        <StatCardSkeleton />
        <StatCardSkeleton />
        <StatCardSkeleton />
        <StatCardSkeleton />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4 xl:grid-cols-4 mb-6">
      <OverviewCard
        icon={<HiOutlineCreditCard className="h-6 w-6 text-blue-600" />}
        iconBg="bg-blue-50"
        iconRing="ring-blue-100"
        label="Total Transactions"
        value={stats.total?.value || 0}
        growth={stats.total?.growth}
      />
      <OverviewCard
        icon={<HiOutlineCheckCircle className="h-6 w-6 text-emerald-600" />}
        iconBg="bg-emerald-50"
        iconRing="ring-emerald-100"
        label="Successful"
        value={stats.successful?.value || 0}
        growth={stats.successful?.growth}
      />
      <OverviewCard
        icon={<HiOutlineClock className="h-6 w-6 text-amber-600" />}
        iconBg="bg-amber-50"
        iconRing="ring-amber-100"
        label="Pending"
        value={stats.pending?.value || 0}
        growth={stats.pending?.growth}
      />
      <OverviewCard
        icon={<HiOutlineXCircle className="h-6 w-6 text-red-600" />}
        iconBg="bg-red-50"
        iconRing="ring-red-100"
        label="Failed"
        value={stats.failed?.value || 0}
        growth={stats.failed?.growth}
      />
    </div>
  );
}
