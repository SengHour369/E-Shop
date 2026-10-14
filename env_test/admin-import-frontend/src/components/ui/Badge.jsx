import { cn } from '@/utils/cn';

const variantClasses = {
  success: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200',
  warning: 'bg-amber-50 text-amber-700 ring-1 ring-amber-200',
  danger:  'bg-red-50 text-red-700 ring-1 ring-red-200',
  info:    'bg-blue-50 text-blue-700 ring-1 ring-blue-200',
  default: 'bg-slate-100 text-slate-600 ring-1 ring-slate-200',
};

const dotColors = {
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger:  'bg-red-500',
  info:    'bg-blue-500',
  default: 'bg-slate-400',
};

/**
 * Badge component for status indicators.
 * @param {'success'|'warning'|'danger'|'info'|'default'} variant
 * @param {boolean} dot - show a dot indicator
 */
export default function Badge({ children, variant = 'default', dot = false, className }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium',
        variantClasses[variant] || variantClasses.default,
        className
      )}
    >
      {dot && (
        <span
          className={cn('h-1.5 w-1.5 rounded-full', dotColors[variant] || dotColors.default)}
        />
      )}
      {children}
    </span>
  );
}
