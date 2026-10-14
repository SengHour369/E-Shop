import { cn } from '@/utils/cn';

function SkeletonBlock({ className }) {
  return (
    <div className={cn("animate-pulse bg-slate-200 rounded", className)} />
  );
}

/**
 * Skeleton card for stats cards.
 */
export function StatCardSkeleton() {
  return (
    <div className="card flex items-center gap-4">
      <SkeletonBlock className="h-12 w-12 rounded-xl" />
      <div className="flex-1 space-y-2">
        <SkeletonBlock className="h-3 w-24" />
        <SkeletonBlock className="h-6 w-32" />
        <SkeletonBlock className="h-3 w-16" />
      </div>
    </div>
  );
}

/**
 * Skeleton row for tables.
 */
export function TableRowSkeleton({ cols = 5 }) {
  return (
    <tr>
      {Array.from({ length: cols }).map((_, i) => (
        <td key={i} className="px-4 py-3">
          <SkeletonBlock className="h-4 w-full rounded" />
        </td>
      ))}
    </tr>
  );
}

/**
 * Skeleton table (header + rows).
 */
export function TableSkeleton({ rows = 5, cols = 5 }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-slate-100">
      <table className="w-full">
        <thead>
          <tr className="border-b border-slate-100 bg-slate-50">
            {Array.from({ length: cols }).map((_, i) => (
              <th key={i} className="px-4 py-3">
                <SkeletonBlock className="h-3 w-20 rounded" />
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }).map((_, i) => (
            <TableRowSkeleton key={i} cols={cols} />
          ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * Full-page skeleton for the Edit Product page.
 * Mirrors the layout: header | [basic info + variants] | [images sidebar]
 */
export function EditProductSkeleton() {
  return (
    <div className="px-6 py-6 space-y-6">
      {/* Page header */}
      <div className="flex items-center justify-between">
        <div className="space-y-2">
          <SkeletonBlock className="h-6 w-36" />
          <SkeletonBlock className="h-4 w-56" />
        </div>
        <div className="flex gap-3">
          <SkeletonBlock className="h-9 w-20 rounded-lg" />
          <SkeletonBlock className="h-9 w-32 rounded-lg" />
        </div>
      </div>

      <div className="grid grid-cols-12 gap-6">
        {/* Left column — Basic info + Variants */}
        <div className="col-span-12 lg:col-span-7 space-y-6">
          {/* Basic Information card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-4">
            <SkeletonBlock className="h-5 w-40 mb-2" />
            <SkeletonBlock className="h-10 w-full rounded-lg" />
            <div className="grid grid-cols-2 gap-4">
              <SkeletonBlock className="h-10 rounded-lg" />
              <SkeletonBlock className="h-10 rounded-lg" />
            </div>
            <SkeletonBlock className="h-24 w-full rounded-lg" />
            <div className="grid grid-cols-2 gap-4">
              <SkeletonBlock className="h-10 rounded-lg" />
              <SkeletonBlock className="h-10 rounded-lg" />
            </div>
          </div>

          {/* Product Variants card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-4">
            <SkeletonBlock className="h-5 w-40 mb-2" />
            <SkeletonBlock className="h-4 w-72" />

            {/* SKU row 1 */}
            <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 space-y-3">
              <div className="flex items-center justify-between">
                <SkeletonBlock className="h-5 w-16" />
                <SkeletonBlock className="h-8 w-20 rounded-lg" />
              </div>
              <SkeletonBlock className="h-10 w-full rounded-lg" />
              <div className="grid grid-cols-2 gap-3">
                <SkeletonBlock className="h-10 rounded-lg" />
                <SkeletonBlock className="h-10 rounded-lg" />
                <SkeletonBlock className="h-10 rounded-lg" />
                <SkeletonBlock className="h-10 rounded-lg" />
              </div>
              <SkeletonBlock className="h-8 w-40 rounded-lg" />
            </div>

            <SkeletonBlock className="h-9 w-24 rounded-lg" />
          </div>
        </div>

        {/* Right column — Images */}
        <div className="col-span-12 lg:col-span-5 space-y-6">
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-3">
            <SkeletonBlock className="h-5 w-36 mb-2" />
            <div className="grid grid-cols-3 gap-3">
              <SkeletonBlock className="aspect-square rounded-xl" />
              <SkeletonBlock className="aspect-square rounded-xl" />
              <SkeletonBlock className="aspect-square rounded-xl" />
            </div>
          </div>
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-3">
            <SkeletonBlock className="h-5 w-36" />
            <SkeletonBlock className="h-48 w-full rounded-xl" />
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Full-page skeleton for the Product Detail page.
 * Mirrors: header | [General Info + SKUs] | [Status/Org + Images]
 */
export function ProductDetailSkeleton() {
  return (
    <div className="px-6 py-6 space-y-6">
      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-2">
          <SkeletonBlock className="h-6 w-40" />
          <SkeletonBlock className="h-4 w-64" />
        </div>
        <SkeletonBlock className="h-9 w-28 rounded-lg" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        {/* Left column — General Info + SKUs */}
        <div className="col-span-1 xl:col-span-2 space-y-6">
          {/* General Information card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-5">
            <SkeletonBlock className="h-5 w-44" />
            <div className="space-y-2">
              <SkeletonBlock className="h-3 w-16" />
              <SkeletonBlock className="h-5 w-56" />
            </div>
            <div className="space-y-2">
              <SkeletonBlock className="h-3 w-24" />
              <SkeletonBlock className="h-4 w-full" />
              <SkeletonBlock className="h-4 w-4/5" />
              <SkeletonBlock className="h-4 w-3/5" />
            </div>
          </div>

          {/* Variants (SKUs) card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-4">
            <SkeletonBlock className="h-5 w-36" />
            {/* SKU row */}
            {[0, 1].map((i) => (
              <div key={i} className="rounded-xl border border-slate-200 p-5 bg-white shadow-sm space-y-4">
                <div className="flex items-center justify-between">
                  <SkeletonBlock className="h-5 w-32" />
                  <SkeletonBlock className="h-6 w-24 rounded-full" />
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <SkeletonBlock className="h-16 rounded-lg" />
                  <SkeletonBlock className="h-16 rounded-lg" />
                  <SkeletonBlock className="h-16 rounded-lg" />
                </div>
                <div className="flex flex-wrap gap-2 pt-3 border-t border-slate-100">
                  <SkeletonBlock className="h-7 w-24 rounded-md" />
                  <SkeletonBlock className="h-7 w-28 rounded-md" />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right column — Status & Images */}
        <div className="col-span-1 space-y-6">
          {/* Status & Organization card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-5">
            <SkeletonBlock className="h-5 w-44" />
            <div className="space-y-2">
              <SkeletonBlock className="h-3 w-16" />
              <SkeletonBlock className="h-6 w-20 rounded-full" />
            </div>
            <div className="pt-4 border-t border-slate-100 space-y-2">
              <SkeletonBlock className="h-3 w-20" />
              <SkeletonBlock className="h-5 w-36" />
            </div>
            <div className="pt-4 border-t border-slate-100 space-y-2">
              <SkeletonBlock className="h-3 w-24" />
              <SkeletonBlock className="h-5 w-28" />
            </div>
          </div>

          {/* Product Images card */}
          <div className="rounded-xl border border-slate-100 bg-white p-5 space-y-3">
            <SkeletonBlock className="h-5 w-36" />
            <div className="grid grid-cols-2 gap-3">
              <SkeletonBlock className="aspect-square rounded-lg" />
              <SkeletonBlock className="aspect-square rounded-lg" />
              <SkeletonBlock className="aspect-square rounded-lg" />
              <SkeletonBlock className="aspect-square rounded-lg" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Full-page skeleton for the Payment Detail page.
 * Mirrors: header | [Main Info + Transaction + Action] | [Order + Method + Timeline]
 */
export function PaymentDetailSkeleton() {
  return (
    <div className="space-y-6">
      {/* Page header */}
      <div className="space-y-2 mb-8">
        <SkeletonBlock className="h-8 w-48" />
        <SkeletonBlock className="h-4 w-64" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column */}
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-2xl border border-slate-100 bg-white p-6">
             <SkeletonBlock className="h-5 w-40 mb-4" />
             <SkeletonBlock className="h-10 w-48 mb-2" />
             <SkeletonBlock className="h-4 w-80 mb-6" />
             <SkeletonBlock className="h-6 w-32 rounded-full" />
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-6 space-y-4">
             <SkeletonBlock className="h-6 w-40 mb-4" />
             <SkeletonBlock className="h-8 w-full border-b border-slate-50" />
             <SkeletonBlock className="h-8 w-full border-b border-slate-50" />
             <SkeletonBlock className="h-8 w-full border-b border-slate-50" />
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-6 space-y-4">
             <SkeletonBlock className="h-6 w-32 mb-4" />
             <SkeletonBlock className="h-24 w-full rounded-xl" />
          </div>
        </div>
        
        {/* Right Column */}
        <div className="space-y-6">
          <div className="rounded-2xl border border-slate-100 bg-white p-6 space-y-4">
            <SkeletonBlock className="h-6 w-40 mb-4" />
            <SkeletonBlock className="h-6 w-full" />
            <SkeletonBlock className="h-6 w-full" />
            <SkeletonBlock className="h-6 w-full" />
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-6 space-y-4">
            <SkeletonBlock className="h-6 w-40 mb-4" />
            <SkeletonBlock className="h-6 w-full" />
            <SkeletonBlock className="h-6 w-full" />
            <SkeletonBlock className="h-6 w-full" />
          </div>
          <div className="rounded-2xl border border-slate-100 bg-white p-6 space-y-4">
            <SkeletonBlock className="h-6 w-40 mb-4" />
            <div className="pl-6 space-y-6 relative before:absolute before:inset-y-0 before:left-2.5 before:w-px before:bg-slate-200">
               <SkeletonBlock className="h-12 w-full" />
               <SkeletonBlock className="h-12 w-full" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Full-page skeleton for the Order Detail page.
 * Mirrors: header | top actions | 4 info cards | [items table] + [timeline]
 */
export function OrderDetailSkeleton() {
  return (
    <div className="space-y-6">
      {/* Title & Breadcrumbs */}
      <div className="flex flex-col mb-4 space-y-2">
        <SkeletonBlock className="h-7 w-40" />
        <SkeletonBlock className="h-4 w-56" />
      </div>

      {/* Top Header Card */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <SkeletonBlock className="w-10 h-10 rounded-full" />
          <div>
            <div className="flex items-center gap-3 mb-2">
              <SkeletonBlock className="h-6 w-32" />
              <SkeletonBlock className="h-5 w-20 rounded" />
            </div>
            <SkeletonBlock className="h-4 w-64" />
          </div>
        </div>
        <div className="flex items-center gap-2 w-full sm:w-auto">
          <SkeletonBlock className="h-9 w-32 rounded-lg" />
          <SkeletonBlock className="h-9 w-36 rounded-lg" />
          <SkeletonBlock className="h-9 w-32 rounded-lg" />
        </div>
      </div>

      {/* 4 Top Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
        {[0, 1, 2, 3].map(i => (
          <div key={i} className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 flex flex-col h-full space-y-4">
            <div className="flex items-center gap-2">
              <SkeletonBlock className="w-8 h-8 rounded-full" />
              <SkeletonBlock className="h-5 w-28" />
            </div>
            <div className="flex items-center gap-3">
              <SkeletonBlock className="w-10 h-10 rounded-full" />
              <div className="space-y-2 flex-1">
                <SkeletonBlock className="h-4 w-3/4" />
                <SkeletonBlock className="h-4 w-1/2" />
              </div>
            </div>
            <div className="pt-2 border-t border-slate-100 space-y-2">
              <SkeletonBlock className="h-4 w-full" />
              <SkeletonBlock className="h-4 w-2/3" />
            </div>
          </div>
        ))}
      </div>

      {/* Grid: Items & Timeline */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 space-y-6">
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 space-y-4">
            <SkeletonBlock className="h-6 w-32 mb-4" />
            <div className="space-y-4">
              <SkeletonBlock className="h-12 w-full" />
              <SkeletonBlock className="h-12 w-full" />
              <SkeletonBlock className="h-12 w-full" />
            </div>
            <div className="pt-4 border-t border-slate-200 flex justify-end">
               <div className="space-y-3 w-64">
                 <SkeletonBlock className="h-4 w-full" />
                 <SkeletonBlock className="h-4 w-full" />
                 <SkeletonBlock className="h-5 w-full" />
               </div>
            </div>
          </div>
        </div>
        <div className="xl:col-span-1">
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-5 space-y-4">
            <SkeletonBlock className="h-6 w-40 mb-4" />
            <div className="pl-4 space-y-6 relative before:absolute before:inset-y-0 before:left-2 before:w-px before:bg-slate-200">
               <SkeletonBlock className="h-10 w-full" />
               <SkeletonBlock className="h-10 w-full" />
               <SkeletonBlock className="h-10 w-full" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/**
 * Generic skeleton block.
 */
export default SkeletonBlock;
