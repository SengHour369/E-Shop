// src/features/brands/components/BrandsGrid.jsx
// Previously: features/subcategorie/components/SubCategoryTable.jsx
import { HiOutlinePencilSquare, HiOutlineTag } from 'react-icons/hi2';
import Badge from '@/components/ui/Badge';
import DeleteButton from '@/components/ui/DeleteButton';

const FALLBACK_LOGO = 'https://placehold.co/64x64/1e293b/fff?text=?';

export default function BrandsGrid({ brands, onEdit, onDelete }) {
  if (!brands.length) {
    return (
      <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-slate-200 bg-slate-50 py-16 text-center">
        <HiOutlineTag className="h-10 w-10 text-slate-300" />
        <p className="mt-3 text-sm font-medium text-slate-500">...Loading...</p>
        <p className="mt-1 text-xs text-slate-400">Click &quot;Add Brand&quot; to create a new one</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
      {brands.map(brand => (
        <div
          key={brand.id}
          className="card group flex flex-col gap-4 transition-all duration-200 hover:shadow-md hover:-translate-y-0.5"
        >
          <div className="flex items-start justify-between">
            <img
              src={brand.logo}
              alt={brand.name}
              onError={(e) => { e.currentTarget.src = FALLBACK_LOGO; }}
              className="h-14 w-14 rounded-xl object-cover border border-slate-100 bg-slate-50"
            />
            <div className="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
              <button
                onClick={() => onEdit?.(brand)}
                title="Edit brand"
                className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 bg-indigo-50 hover:bg-indigo-100 hover:text-indigo-600 transition-colors"
              >
                <HiOutlinePencilSquare className="h-4 w-4" />
              </button>
              <DeleteButton
                onConfirm={() => onDelete?.(brand.id)}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 bg-red-50 hover:bg-red-100 hover:text-red-600 transition-colors"
              />
            </div>
          </div>

          <div className="flex-1">
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-slate-900">{brand.name}</h3>
              <Badge variant={brand.status === 'active' ? 'success' : 'default'}>
                {brand.status}
              </Badge>
            </div>
            <p className="mt-0.5 text-xs text-slate-500">{brand.category_name}</p>
            <p className="mt-2 text-sm text-slate-600 line-clamp-2">
              {brand.description || 'No description provided.'}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
}
