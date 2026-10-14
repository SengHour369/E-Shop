import { useState } from 'react';
import { HiOutlineCalendarDays } from 'react-icons/hi2';

const PRESETS = ['Today', 'Last 7 days', 'Last 30 days', 'Last 90 days', 'This year', 'Custom'];

export default function DateRangePicker({ value, onChange }) {
  const [selected, setSelected] = useState(value || 'Last 30 days');
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');

  const handleSelect = (preset) => {
    setSelected(preset);
    onChange?.(preset);
  };

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex items-center gap-1 rounded-xl border border-slate-200 bg-white p-1">
        {PRESETS.filter(p => p !== 'Custom').map(preset => (
          <button
            key={preset}
            onClick={() => handleSelect(preset)}
            className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-all duration-150 ${
              selected === preset
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            }`}
          >
            {preset}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2">
        <div className="relative">
          <HiOutlineCalendarDays className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="date"
            value={customStart}
            onChange={e => { setCustomStart(e.target.value); setSelected('Custom'); }}
            className="rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100"
          />
        </div>
        <span className="text-sm text-slate-400">to</span>
        <div className="relative">
          <HiOutlineCalendarDays className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="date"
            value={customEnd}
            onChange={e => { setCustomEnd(e.target.value); setSelected('Custom'); }}
            className="rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100"
          />
        </div>
      </div>
    </div>
  );
}
