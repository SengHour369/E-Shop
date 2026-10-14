import { useState, useEffect } from 'react';
import {
  HiOutlineMagnifyingGlass,
  HiOutlineChevronDown,
  HiOutlineFunnel,
  HiOutlineXMark,
} from 'react-icons/hi2';
import { cn } from '@/utils/cn';

import { OrderStatus, PaymentStatus, PaymentMethod } from '../types/orderTypes';

const ORDER_STATUS_OPTIONS = [
  { value: 'ALL', label: 'All Order Status' },
  { value: OrderStatus.PENDING, label: 'Pending' },
  { value: OrderStatus.PROCESSING, label: 'Processing' },
  { value: OrderStatus.CONFIRMED, label: 'Confirmed' },
  { value: OrderStatus.SHIPPED, label: 'Shipped' },
  { value: OrderStatus.DELIVERED, label: 'Delivered' },
  { value: OrderStatus.COMPLETED, label: 'Completed' },
  { value: OrderStatus.CANCELLED, label: 'Cancelled' },
];



const PAYMENT_METHOD_OPTIONS = [
  { value: 'all', label: 'All Payment Methods' },
  {
    label: 'KHQR network',
    options: [
      { value: PaymentMethod.BAKONG, label: 'Bakong' },
      { value: PaymentMethod.ABA, label: 'ABA' },
      { value: PaymentMethod.ACLEDA, label: 'ACLEDA' },
    ]
  },
  {
    label: 'Cards & other',
    options: [
      { value: PaymentMethod.RAZORPAY, label: 'Razorpay' },
      { value: PaymentMethod.SINGE, label: 'Singe' },
    ]
  }
];

export default function OrderFilter({
  search = '',
  onSearchChange,
  orderStatus = 'ALL',
  onOrderStatusChange,

  paymentMethod = 'all',
  onPaymentMethodChange,
  date = '',
  onDateChange,
  onClearFilters,
  onFiltersClick,
}) {
  const [localSearch, setLocalSearch] = useState(search);

  // Sync prop changes (e.g. clear filters)
  useEffect(() => {
    setLocalSearch(search);
  }, [search]);

  // Debounce effect
  useEffect(() => {
    const handler = setTimeout(() => {
      if (localSearch !== search) {
        onSearchChange?.(localSearch);
      }
    }, 500);
    return () => clearTimeout(handler);
  }, [localSearch, search, onSearchChange]);

  const handleSearch = (e) => {
    setLocalSearch(e.target.value);
  };

  const activeFiltersCount = [
    orderStatus !== 'ALL',
    paymentMethod !== 'all',
    !!date,
    !!search
  ].filter(Boolean).length;

  return (
    <div className="flex flex-col gap-3 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 sm:flex-row sm:items-center flex-wrap rounded-t-xl">
      {/* Search */}
      <div className="relative flex-1 min-w-[220px]">
        <HiOutlineMagnifyingGlass className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
        <input
          type="text"
          value={localSearch}
          onChange={handleSearch}
          placeholder="Search by order ID, customer, product..."
          className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm text-slate-700 placeholder:text-slate-400 outline-none transition-colors focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
        />
      </div>

      {/* Order Status */}
      <SelectField
        value={orderStatus}
        onChange={onOrderStatusChange}
        options={ORDER_STATUS_OPTIONS}
        className="sm:w-44"
      />



      {/* Payment Method */}
      <SelectField
        value={paymentMethod}
        onChange={onPaymentMethodChange}
        options={PAYMENT_METHOD_OPTIONS}
        className="sm:w-48"
      />

      {/* Date (Single Calendar Day) */}
      <div className="flex gap-2">
        <input
          type="date"
          value={date}
          onChange={(e) => onDateChange?.(e.target.value)}
          className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-medium text-slate-600 transition-colors hover:bg-slate-100 w-full sm:w-auto outline-none focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
        />
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 ml-auto">
        {activeFiltersCount > 0 && (
          <button
            type="button"
            onClick={onClearFilters}
            className="flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-slate-700 transition-colors px-2"
          >
            <HiOutlineXMark className="h-4 w-4" />
            Clear
          </button>
        )}

        <button
          type="button"
          onClick={onFiltersClick}
          className="relative flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
        >
          <HiOutlineFunnel className="h-4 w-4" />
          Filters
          {activeFiltersCount > 0 && (
            <span className="absolute -top-1.5 -right-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-[10px] font-bold text-white">
              {activeFiltersCount}
            </span>
          )}
        </button>
      </div>
    </div>
  );
}

function SelectField({ value, onChange, options, className }) {
  return (
    <div className={cn('relative', className)}>
      <select
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        className="w-full appearance-none rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-3 pr-8 text-sm font-medium text-slate-600 outline-none transition-colors hover:bg-slate-100 focus:border-indigo-400 focus:bg-white focus:ring-2 focus:ring-indigo-100"
      >
        {options.map((opt, index) => {
          if (opt.options) {
            return (
              <optgroup key={index} label={opt.label}>
                {opt.options.map(subOpt => (
                  <option key={subOpt.value} value={subOpt.value}>
                    {subOpt.label}
                  </option>
                ))}
              </optgroup>
            );
          }
          return (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          );
        })}
      </select>
      <HiOutlineChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
    </div>
  );
}