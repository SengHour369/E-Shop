import { Link } from 'react-router-dom';
import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import { formatCurrency } from '@/utils/formatCurrency';
import { formatDate } from '@/utils/formatDate';
import { CUSTOMER_STATUSES } from '@/constants/status';
import { HiOutlineEye } from 'react-icons/hi2';

export default function CustomersTable({ customers, loading }) {
  const columns = [
    {
      key: 'name', label: 'Customer', sortable: true,
      render: (val, row) => (
        <div className="flex items-center gap-3">
          <img src={row.avatar} alt={val} className="h-9 w-9 rounded-full object-cover" />
          <div>
            <p className="font-medium text-slate-900">{val}</p>
            <p className="text-xs text-slate-400">{row.email}</p>
          </div>
        </div>
      ),
    },
    { key: 'phone', label: 'Phone',
      render: val => <span className="text-slate-600">{val}</span> },
    { key: 'city', label: 'City',
      render: val => <span className="text-slate-600">{val}</span> },
    { key: 'orders', label: 'Orders', align: 'center', sortable: true,
      render: val => <span className="font-semibold text-slate-700">{val}</span> },
    { key: 'totalSpent', label: 'Total Spent', align: 'right', sortable: true,
      render: val => <span className="font-semibold text-slate-900">{formatCurrency(val)}</span> },
    { key: 'status', label: 'Status',
      render: val => {
        const s = CUSTOMER_STATUSES[val] || CUSTOMER_STATUSES.active;
        return <Badge variant={s.variant} dot>{s.label}</Badge>;
      },
    },
    { key: 'joinDate', label: 'Joined',
      render: val => <span className="text-slate-500">{formatDate(val)}</span> },
    { key: 'actions', label: '', align: 'right',
      render: (_, row) => (
        <Link
          to={`/dashboard/customers/${row.id}`}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 bg-cyan-100 hover:bg-indigo-50 hover:text-indigo-600 transition-colors"
          title="View customer"
        >
          <HiOutlineEye className="h-4 w-4" />
        </Link>
      ),
    },
  ];

  return <Table columns={columns} data={customers} loading={loading} />;
}
