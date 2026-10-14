import { HiOutlineMagnifyingGlass, HiOutlineFunnel, HiArrowUpTray } from 'react-icons/hi2';

export default function PaymentToolbar({ search, onSearch, filters, onFilter }) {
  return (
    <div className="flex flex-col gap-3 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 sm:flex-row sm:items-center flex-wrap rounded-t-xl">
      {/* Search */}
      <div className="relative flex-1 min-w-[220px]">
        <HiOutlineMagnifyingGlass className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          placeholder="Search by ID, customer..."
          value={search}
          onChange={e => onSearch(e.target.value)}
          className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-700 placeholder:text-slate-400 outline-none transition-colors focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
        />
      </div>

      <div className="flex flex-wrap items-center gap-2 sm:ml-auto">
        {/* Status filter */}
        <select
          value={filters.status || ''}
          onChange={e => onFilter('status', e.target.value)}
          className="rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-3 pr-8 text-sm font-medium text-slate-600 outline-none transition-colors hover:bg-slate-100 focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
        >
          <option value="">All Status</option>
          <option value="PAID">Success</option>
          <option value="PENDING">Pending</option>
          <option value="FAILED">Failed</option>
          <option value="REFUNDED">Refunded</option>
        </select>

        {/* Method filter */}
        <select
          value={filters.method || ''}
          onChange={e => onFilter('method', e.target.value)}
          className="rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-3 pr-8 text-sm font-medium text-slate-600 outline-none transition-colors hover:bg-slate-100 focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
        >
          <option value="">All Payment Methods</option>
          <option value="BAKONG">Bakong</option>
          <option value="VISA">Visa</option>
          <option value="MASTERCARD">Mastercard</option>
          <option value="PAYPAL">PayPal</option>
          <option value="APPLEPAY">Apple Pay</option>
          <option value="UPI">UPI</option>
          <option value="STRIPE">Stripe</option>
        </select>

        {/* Filters button */}
        <button className="relative flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50">
          <HiOutlineFunnel className="h-4 w-4" />
          Filters
        </button>

        {/* Export button */}
        <button className="flex items-center gap-2 px-4 py-2.5 text-sm font-medium text-white bg-indigo-600 rounded-lg shadow-sm hover:bg-indigo-700 transition-colors">
          <HiArrowUpTray className="h-4 w-4" />
          Export
        </button>
      </div>
    </div>
  );
}
