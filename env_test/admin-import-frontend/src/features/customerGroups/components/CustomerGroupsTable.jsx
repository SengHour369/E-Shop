// Customer-group table.
import { useState } from 'react';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { cn } from '@/utils/cn';
import {
  HiOutlineMagnifyingGlass,
  HiOutlineFunnel,
  HiOutlineArrowDownTray,
  HiOutlineChevronDown,
  HiOutlineEye,
  HiOutlinePencil,
  HiOutlineTrash,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineUserGroup,
  HiOutlineShoppingBag,
  HiOutlineCurrencyDollar,
  HiOutlineUsers,
} from 'react-icons/hi2';
import DeleteButton from '@/components/ui/DeleteButton';

// ── Group type badge colors ────────────────────────────────────────────────────
const TYPE_STYLES = {
  VIP: 'bg-purple-100 text-purple-700',
  Loyalty: 'bg-blue-100 text-blue-700',
  Wholesale: 'bg-violet-100 text-violet-700',
  Default: 'bg-slate-100 text-slate-600',
  Behavioral: 'bg-orange-100 text-orange-700',
  Segment: 'bg-teal-100 text-teal-700',
  Corporate: 'bg-indigo-100 text-indigo-700',
};

// ── Summary stat card ─────────────────────────────────────────────────────────
function StatCard({ icon: Icon, iconBg, label, value, sub, isCurrency }) {
  return (
    <div className="card flex items-center gap-4 p-5">
      <div className={cn('flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-xl', iconBg)}>
        <Icon className="h-6 w-6" />
      </div>
      <div>
        <p className="text-xs font-medium text-slate-400">{label}</p>
        <p className="mt-0.5 text-2xl font-bold text-slate-900 tabular-nums">
          {isCurrency
            ? `$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`
            : value.toLocaleString()}
        </p>
        <p className="mt-0.5 flex items-center gap-1 text-xs text-emerald-600">
          <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 inline-block" />
          {sub}
        </p>
      </div>
    </div>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────
const PAGE_SIZE = 10;

export default function CustomerGroupsTable({ groups, stats, onEdit }) {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [page, setPage] = useState(1);

  const allTypes = [...new Set(groups.map(g => g.type))];

  const filtered = groups.filter(g => {
    const q = search.toLowerCase();
    const matchQ = !q || g.name.toLowerCase().includes(q) || g.description.toLowerCase().includes(q);
    const matchS = !statusFilter || g.status === statusFilter;
    const matchT = !typeFilter || g.type === typeFilter;
    return matchQ && matchS && matchT;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(page, totalPages);
  const paginated = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE);

  const formatDate = (d) =>
    new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

  return (
    <div className="space-y-5">

      {/* ── Summary Stats ─────────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          icon={HiOutlineUserGroup} iconBg="bg-blue-100 text-blue-600"
          label="Total Groups" value={stats.totalGroups.count}
          sub={stats.totalGroups.label}
        />
        <StatCard
          icon={HiOutlineUsers} iconBg="bg-green-100 text-green-600"
          label="Total Customers" value={stats.totalCustomers.count}
          sub={stats.totalCustomers.label}
        />
        <StatCard
          icon={HiOutlineShoppingBag} iconBg="bg-amber-100 text-amber-600"
          label="Total Orders" value={stats.totalOrders.count}
          sub={stats.totalOrders.label}
        />
        <StatCard
          icon={HiOutlineCurrencyDollar} iconBg="bg-purple-100 text-purple-600"
          label="Total Spent" value={stats.totalSpent.value}
          sub={stats.totalSpent.label} isCurrency
        />
      </div>

      {/* ── Filter bar ───────────────────────────────────────────────────── */}
      <div className="card p-4">
        <div className="flex flex-wrap items-center gap-3">
          {/* Search */}
          <div className="relative flex-1 min-w-[220px]">
            <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by group name, description..."
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(1); }}
              className="w-full rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>

          {/* Status filter */}
          <div className="relative">
            <select
              value={statusFilter}
              onChange={e => { setStatusFilter(e.target.value); setPage(1); }}
              className="appearance-none rounded-lg border border-slate-200 bg-white py-2 pl-3 pr-8 text-sm text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
              <option value="">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
            <HiOutlineChevronDown className="pointer-events-none absolute right-2 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          </div>

          {/* Group type filter */}
          <div className="relative">
            <select
              value={typeFilter}
              onChange={e => { setTypeFilter(e.target.value); setPage(1); }}
              className="appearance-none rounded-lg border border-slate-200 bg-white py-2 pl-3 pr-8 text-sm text-slate-600 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            >
              <option value="">All Group Types</option>
              {allTypes.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
            <HiOutlineChevronDown className="pointer-events-none absolute right-2 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          </div>

          {/* Filters btn */}
          <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50 transition-colors">
            <HiOutlineFunnel className="h-4 w-4" />
            Filters
          </button>

          {/* Export btn */}
          <button className="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 hover:bg-slate-50 transition-colors">
            <HiOutlineArrowDownTray className="h-4 w-4" />
            Export
            <HiOutlineChevronDown className="h-3.5 w-3.5 text-slate-400" />
          </button>
        </div>
      </div>

      {/* ── Table ────────────────────────────────────────────────────────── */}
      <div className="card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-100">
            <thead>
              <tr className="bg-slate-50">
                {['Group Name', 'Description', 'Group Type', 'Customers', 'Orders', 'Total Spent', 'Status', 'Created At', 'Action'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500 whitespace-nowrap">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 bg-white">
              {paginated.length === 0 ? (
                <tr>
                  <td colSpan={9} className="py-16 text-center text-sm text-slate-400">
                    No customer groups found.
                  </td>
                </tr>
              ) : paginated.map(g => (
                <tr key={g.id} className="hover:bg-slate-50 transition-colors">

                  {/* Group Name */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-3">
                      <div className={cn('flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg text-base', g.iconBg)}>
                        {g.icon}
                      </div>
                      <span className="text-sm font-semibold text-slate-800">{g.name}</span>
                    </div>
                  </td>

                  {/* Description */}
                  <td className="px-4 py-3 max-w-[220px]">
                    <span className="text-sm text-slate-500 line-clamp-1">{g.description}</span>
                  </td>

                  {/* Group Type */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <span className={cn(
                      'inline-flex items-center rounded-md px-2.5 py-0.5 text-xs font-semibold',
                      TYPE_STYLES[g.type] || 'bg-slate-100 text-slate-600'
                    )}>
                      {g.type}
                    </span>
                  </td>

                  {/* Customers */}
                  <td className="px-4 py-3 whitespace-nowrap text-sm font-medium text-slate-700 tabular-nums">
                    {g.customers.toLocaleString()}
                  </td>

                  {/* Orders */}
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-slate-600 tabular-nums">
                    {g.orders.toLocaleString()}
                  </td>

                  {/* Total Spent */}
                  <td className="px-4 py-3 whitespace-nowrap text-sm font-semibold text-slate-800 tabular-nums">
                    {formatCurrency(g.totalSpent)}
                  </td>

                  {/* Status */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <Badge
                      variant={g.status === 'active' ? 'success' : 'default'}
                      dot
                    >
                      {g.status === 'active' ? 'Active' : 'Inactive'}
                    </Badge>
                  </td>

                  {/* Created At */}
                  <td className="px-4 py-3 whitespace-nowrap text-sm text-slate-500">
                    {formatDate(g.createdAt)}
                  </td>

                  {/* Actions */}
                  <td className="px-4 py-3 whitespace-nowrap">
                    <div className="flex items-center gap-1">
                      <button
                        title="View"
                        className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-cyan-100 hover:bg-blue-50 hover:text-blue-600 transition-colors"
                      >
                        <HiOutlineEye className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => onEdit?.(g)}
                        title="Edit"
                        className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-yellow-100 hover:bg-amber-50 hover:text-amber-600 transition-colors"
                      >
                        <HiOutlinePencil className="h-4 w-4" />
                      </button>
                      <DeleteButton
                        onConfirm={() => onDelete?.(g.id)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-pink-100 hover:bg-red-50 hover:text-red-600 transition-colors"
                      />
                    </div>
                  </td>

                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* ── Pagination ── */}
        <div className="flex items-center justify-between border-t border-slate-100 bg-white px-4 py-3">
          <p className="text-sm text-slate-500">
            Showing {filtered.length === 0 ? 0 : (safePage - 1) * PAGE_SIZE + 1} to{' '}
            {Math.min(safePage * PAGE_SIZE, filtered.length)} of{' '}
            {filtered.length} results
          </p>

          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={safePage === 1}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronLeft className="h-4 w-4" />
            </button>

            {[...Array(Math.min(totalPages, 3))].map((_, i) => {
              const pg = i + 1;
              return (
                <button
                  key={pg}
                  onClick={() => setPage(pg)}
                  className={cn(
                    'inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-medium border transition-colors',
                    safePage === pg
                      ? 'border-blue-600 bg-blue-600 text-white'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {pg}
                </button>
              );
            })}

            {totalPages > 3 && (
              <>
                <span className="px-1 text-slate-400">…</span>
                <button
                  onClick={() => setPage(totalPages)}
                  className={cn(
                    'inline-flex h-8 w-8 items-center justify-center rounded-lg text-sm font-medium border transition-colors',
                    safePage === totalPages
                      ? 'border-blue-600 bg-blue-600 text-white'
                      : 'border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {totalPages}
                </button>
              </>
            )}

            <button
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={safePage === totalPages}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-500 disabled:opacity-40 hover:bg-slate-50 transition-colors"
            >
              <HiOutlineChevronRight className="h-4 w-4" />
            </button>

            <select
              readOnly
              defaultValue="10"
              className="ml-2 rounded-lg border border-slate-200 bg-white px-2 py-1 text-sm text-slate-600"
            >
              <option>10 / page</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}
