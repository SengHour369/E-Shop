import { HiOutlineMagnifyingGlass, HiOutlineFunnel, HiOutlineArrowDownTray } from 'react-icons/hi2';

export default function CategoriesFilter({
  search,
  onSearch,
  filters,
  onFilter
}) {
  return (
    <div className="flex flex-col gap-4 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 md:flex-row md:items-center md:justify-between rounded-t-xl">
      {/* Left side: Search */}
      <div className="relative w-full md:w-80">
        <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          placeholder="Search categories..."
          value={search}
          onChange={(e) => onSearch(e.target.value)}
          className="w-full rounded-lg border border-slate-200 py-2 pl-10 pr-4 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      {/* Right side: Actions */}
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={filters?.status || ''}
          onChange={(e) => onFilter('status', e.target.value)}
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="">All Status</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
          <option value="deleted">Deleted</option>
        </select>

        <button className="flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors">
          <HiOutlineFunnel className="h-4 w-4" />
          Filter
        </button>

        <button className="flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors">
          <HiOutlineArrowDownTray className="h-4 w-4" />
          Export
        </button>
      </div>
    </div>
  );
}
