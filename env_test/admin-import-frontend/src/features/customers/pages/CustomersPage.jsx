import CustomersTable from '../components/CustomersTable';
import {  customers  } from '@/features/dashboard/services/dashboard.service';
import { HiOutlineMagnifyingGlass } from 'react-icons/hi2';
import { useState } from 'react';
import { PageHeader, DataTableCard } from '@/components/ui';
import {  customerStats  } from '@/features/reports/components/PageStats';
import PageContainer from '@/components/layouts/PageContainer';


export default function CustomersPage() {
  const [search, setSearch] = useState('');

  const filtered = customers.filter(c => {
    const q = search.toLowerCase();
    return !q || c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q) || c.city.toLowerCase().includes(q);
  });

  return (
    <PageContainer>
      <PageHeader
        title="Customers"
        description="View and manage customer profiles and activity."
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Customers' }]}
        stats={customerStats}
      />

      <DataTableCard
        toolbar={
          <div className="flex flex-col gap-4 border-b border-slate-200 bg-white px-5 py-5 sm:px-6 md:flex-row md:items-center md:justify-between rounded-t-xl">
            <div className="relative max-w-sm flex-1">
              <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="Search customers..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full rounded-lg border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm placeholder-slate-400 focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-100"
              />
            </div>
          </div>
        }
      >
        <CustomersTable customers={filtered} />
      </DataTableCard>
    </PageContainer>
  );
}
