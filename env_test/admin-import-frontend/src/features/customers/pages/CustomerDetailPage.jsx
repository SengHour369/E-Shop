import { useParams, useNavigate } from 'react-router-dom';
import CustomerDetail from '../components/CustomerDetail';
import {  customers, orders  } from '@/features/dashboard/services/dashboard.service';
import PageContainer from '@/components/layouts/PageContainer';


export default function CustomerDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const customer = customers.find(c => c.id === parseInt(id));

  if (!customer) {
    return (
      <PageContainer>
        <h3 className="text-lg font-semibold text-slate-700">Customer not found</h3>
        <button onClick={() => navigate('/dashboard/customers')} className="mt-4 text-sm text-indigo-600 hover:underline">
          ← Back to Customers
        </button>
      </PageContainer>
    );
  }

  const customerOrders = orders.filter(o => o.customerId === customer.id);

  return <CustomerDetail customer={customer} orders={customerOrders} />;
}
