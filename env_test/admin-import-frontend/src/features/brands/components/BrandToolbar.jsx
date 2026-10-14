import React from 'react';
import { HiOutlineMagnifyingGlass } from 'react-icons/hi2';
import { useCategories } from '@/features/categories/hooks/useCategories';
import SearchableSelect from '@/components/ui/SearchableSelect';

export default function BrandToolbar({
  search,
  onSearch,
  filters,
  onFilter,
}) {
  const { categories } = useCategories();
  
  const hasActive = !!search || !!filters.category || !!filters.status;

  const handleReset = () => {
    onSearch('');
    onFilter('category', '');
    onFilter('status', '');
  };

  return (
    <div className="flex flex-col gap-4 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 md:flex-row md:items-center md:justify-between rounded-t-xl">
      {/* Left side: Search */}
      <div className="relative w-full md:w-80">
        <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          placeholder="Search brands..."
          value={search}
          onChange={(e) => onSearch(e.target.value)}
          className="w-full rounded-lg border border-slate-200 py-2 pl-10 pr-4 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
      </div>

      {/* Right side: Actions / Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="w-[200px]">
          <SearchableSelect
            options={[
              { label: 'All Categories', value: '' },
              ...categories.map(cat => ({ label: cat.name, value: cat.id }))
            ]}
            value={filters.category || ''}
            onChange={val => onFilter('category', val)}
            placeholder="All Categories"
          />
        </div>

        <select
          value={filters.status || ''}
          onChange={(e) => onFilter('status', e.target.value)}
          className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-600 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        >
          <option value="">All Status</option>
          <option value="active">Active</option>
          <option value="inactive">Inactive</option>
          <option value="deleted">Deleted</option>
        </select>

        {hasActive && (
          <button 
            onClick={handleReset}
            className="flex items-center gap-2 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50 transition-colors"
          >
            Clear filters
          </button>
        )}
      </div>
    </div>
  );
}
