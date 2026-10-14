import { Search, ChevronDown, RotateCcw, Download, Upload, Filter } from 'lucide-react';

export default function FilterBar() {
  return (
    <div className="flex flex-col lg:flex-row gap-4 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 md:items-center justify-between rounded-t-xl">
      {/* Left side: Search & Dropdowns */}
      <div className="flex-grow flex flex-col md:flex-row gap-3">
        {/* Search */}
        <div className="relative flex-grow min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by product name, SKU, or barcode..."
            className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 bg-white"
          />
        </div>

        {/* Dropdowns */}
        <div className="flex gap-3 overflow-x-auto pb-1 md:pb-0 hide-scrollbar">
          <Dropdown label="All Categories" />
          <Dropdown label="All Brands" />
          <Dropdown label="All Warehouses" />
          <Dropdown label="Stock Status" />
          <button className="flex items-center gap-1.5 px-3 py-2 text-sm text-slate-500 hover:text-slate-800 hover:bg-slate-50 border border-transparent rounded-lg flex-shrink-0 transition-colors">
            <RotateCcw className="h-4 w-4" />
            Reset
          </button>
        </div>
      </div>

      {/* Right side: Actions */}
      <div className="flex gap-2 flex-shrink-0">
        <ActionButton icon={Download} label="Export" />
        <ActionButton icon={Upload} label="Import" />
        <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors">
          <Filter className="h-4 w-4" />
          Filters
        </button>
      </div>
    </div>
  );
}

function Dropdown({ label }) {
  return (
    <button className="flex items-center justify-between gap-2 px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-700 bg-white hover:bg-slate-50 min-w-[140px] flex-shrink-0">
      <span>{label}</span>
      <ChevronDown className="h-4 w-4 text-slate-400" />
    </button>
  );
}

function ActionButton({ icon: Icon, label }) {
  return (
    <button className="flex items-center justify-between gap-2 px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-700 bg-white hover:bg-slate-50 transition-colors">
      <Icon className="h-4 w-4 text-slate-400" />
      <span>{label}</span>
      <ChevronDown className="h-4 w-4 text-slate-400 ml-1" />
    </button>
  );
}
