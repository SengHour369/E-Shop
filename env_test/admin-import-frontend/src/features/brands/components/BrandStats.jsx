import React, { useMemo } from 'react';
import { HiOutlineBriefcase, HiOutlineCheckCircle, HiOutlineXCircle, HiOutlineTrash } from 'react-icons/hi2';
import { StatCardSkeleton } from '@/components/ui/Skeleton';

export default function BrandStats({ brands = [], totalElements = 0, globalStats = { total: 0, active: 0, inactive: 0, deleted: 0 }, loading }) {
  const stats = useMemo(() => {
    let total = globalStats.total > 0 ? globalStats.total : (totalElements > 0 ? totalElements : brands.length);
    let active = globalStats.active || brands.filter(b => b.status === 'active').length;
    let inactive = globalStats.inactive || brands.filter(b => b.status === 'inactive').length;
    let deleted = globalStats.deleted || brands.filter(b => b.status === 'deleted').length;

    return [
      { id: 1, title: 'Total Brands', value: total, icon: HiOutlineBriefcase, color: 'text-blue-600', bg: 'bg-blue-50' },
      { id: 2, title: 'Active Brands', value: active, icon: HiOutlineCheckCircle, color: 'text-emerald-600', bg: 'bg-emerald-50' },
      { id: 3, title: 'Inactive Brands', value: inactive, icon: HiOutlineXCircle, color: 'text-amber-600', bg: 'bg-amber-50' },
      { id: 4, title: 'Deleted Brands', value: deleted, icon: HiOutlineTrash, color: 'text-rose-600', bg: 'bg-rose-50' }
    ];
  }, [brands, totalElements, globalStats]);

  return (
    <div className="mb-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
      {loading ? (
        Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-xl border border-slate-100 bg-white p-6 shadow-sm">
            <StatCardSkeleton />
          </div>
        ))
      ) : (
        stats.map(stat => {
        const Icon = stat.icon;
        return (
          <div key={stat.id} className="rounded-xl border border-slate-100 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{stat.title}</p>
                <p className="mt-2 text-3xl font-bold text-slate-800">{stat.value}</p>
              </div>
              <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${stat.bg}`}>
                <Icon className={`h-6 w-6 ${stat.color}`} />
              </div>
            </div>
          </div>
        );
      }))}
    </div>
  );
}
