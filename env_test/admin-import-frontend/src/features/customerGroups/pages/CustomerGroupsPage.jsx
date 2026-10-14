// Customer-group listing route.
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/components/ui';
import CustomerGroupsTable from '../components/CustomerGroupsTable';
import {  customerGroups, customerGroupStats  } from '@/features/customerGroups/services/customerGroup.service';
import { HiPlus } from 'react-icons/hi2';
import PageContainer from '@/components/layouts/PageContainer';


export default function CustomerGroupsPage() {
  const navigate = useNavigate();
  return (
    <PageContainer>
      {/* ── Page header ── */}
      <PageHeader
        title="Customer Groups"
        crumbs={[
          { label: 'Dashboard', path: '/' },
          { label: 'Customers', path: '/customers' },
          { label: 'Customer Groups' },
        ]}
      >
        <button 
          onClick={() => navigate('/dashboard/customer-groups/add')}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 transition-colors"
        >
          <HiPlus className="h-4 w-4" />
          Add Group
        </button>
      </PageHeader>

      {/* ── Table + stats ── */}
      <CustomerGroupsTable
        groups={customerGroups}
        stats={customerGroupStats}
        onEdit={(group) => navigate(`/dashboard/customer-groups/edit/${group.id}`)}
      />
    </PageContainer>
  );
}
