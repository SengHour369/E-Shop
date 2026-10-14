import { Package, AlertTriangle, XCircle, CheckCircle2, Layers } from 'lucide-react';

/**
 * StatCards — driven by real data passed from InventoryPage.
 *
 * Props:
 *   summary: {
 *     total:      number   — total inventory records (from API totalElements)
 *     inStock:    number   — count of "In Stock" records on current page
 *     lowStock:   number   — count of "Low Stock" records on current page
 *     outOfStock: number   — count of "Out of Stock" records on current page
 *   }
 */
import { StatCardSkeleton } from '@/components/ui/Skeleton';

export default function StatCards({ summary = {}, loading }) {
  const { total = 0, inStock = 0, lowStock = 0, outOfStock = 0 } = summary;

  const stats = [
    {
      title:   'Total Records',
      value:   total.toLocaleString(),
      subtext: 'All inventory records',
      icon:    Layers,
      color:   'text-blue-600',
      bgColor: 'bg-blue-50',
    },
    {
      title:   'In Stock',
      value:   inStock.toLocaleString(),
      subtext: 'Items available',
      icon:    CheckCircle2,
      color:   'text-green-600',
      bgColor: 'bg-green-50',
    },
    {
      title:   'Low Stock',
      value:   lowStock.toLocaleString(),
      subtext: 'Below threshold',
      icon:    AlertTriangle,
      color:   'text-amber-500',
      bgColor: 'bg-amber-50',
    },
    {
      title:   'Out of Stock',
      value:   outOfStock.toLocaleString(),
      subtext: 'No stock available',
      icon:    XCircle,
      color:   'text-red-600',
      bgColor: 'bg-red-50',
    },
    {
      title:   'Total Items (Page)',
      value:   (inStock + lowStock + outOfStock).toLocaleString(),
      subtext: 'Loaded this page',
      icon:    Package,
      color:   'text-purple-600',
      bgColor: 'bg-purple-50',
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-5 mb-6">
      {loading ? (
        Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="rounded-xl border border-slate-100 bg-white p-6 shadow-sm">
            <StatCardSkeleton />
          </div>
        ))
      ) : (
        stats.map((stat, index) => (
          <div key={index} className="bg-white rounded-xl shadow-sm border border-slate-100 p-5 flex items-center gap-4">
            <div className={`h-12 w-12 rounded-full flex items-center justify-center flex-shrink-0 ${stat.bgColor}`}>
              <stat.icon className={`h-6 w-6 ${stat.color}`} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-500 mb-1">{stat.title}</p>
              <h3 className="text-2xl font-bold text-slate-900 leading-tight">{stat.value}</h3>
              <p className="text-xs text-slate-400 mt-1">{stat.subtext}</p>
            </div>
          </div>
        ))
      )}
    </div>
  );
}
