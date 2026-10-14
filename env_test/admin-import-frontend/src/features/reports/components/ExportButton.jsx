import { useState } from 'react';
import Button from '@/components/ui/Button';
import { HiOutlineDocumentArrowDown } from 'react-icons/hi2';

export default function ExportButton({ onExport }) {
  const [loading, setLoading] = useState(false);
  const [format, setFormat] = useState('CSV');

  const handleExport = async () => {
    setLoading(true);
    // UI-only: simulate export delay
    await new Promise(r => setTimeout(r, 1200));
    setLoading(false);
    onExport?.(format);
    // Show a toast placeholder
    alert(`Exported as ${format} (UI placeholder — no file generated)`);
  };

  return (
    <div className="flex items-center gap-2">
      <select
        value={format}
        onChange={e => setFormat(e.target.value)}
        className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-100"
      >
        <option value="CSV">CSV</option>
        <option value="PDF">PDF</option>
        <option value="XLSX">XLSX</option>
      </select>
      <Button
        onClick={handleExport}
        loading={loading}
        variant="secondary"
      >
        <HiOutlineDocumentArrowDown className="h-4 w-4" />
        Export {format}
      </Button>
    </div>
  );
}
