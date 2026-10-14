import { useParams, useNavigate, useLocation } from 'react-router-dom';
import OrderDetail from '../components/OrderDetail';
import { useOrderDetail } from '../hooks/useOrderDetail';
import { PageHeader, OrderDetailSkeleton } from '@/components/ui';
import PageContainer from '@/components/layouts/PageContainer';


export default function OrderDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const userId = location.state?.userId || 1; // Fallback to 1 if not navigated from table

  const { order, loading, error, updateStatus, cancelOrder } = useOrderDetail(id, userId);

  if (loading) {
    return (
      <PageContainer>
        <OrderDetailSkeleton />
      </PageContainer>
    );
  }

  if (error || !order) {
    return (
      <PageContainer>
        <p className="text-slate-500 mb-4">{error || 'Order not found.'}</p>
        <button 
          onClick={() => navigate('/dashboard/orders')}
          className="text-indigo-600 hover:underline font-medium"
        >
          Back to Orders
        </button>
      </PageContainer>
    );
  }

  return (
    <OrderDetail 
      order={order} 
      onUpdateStatus={(status) => updateStatus(status)}
      onCancel={(userId, reason) => cancelOrder(userId, reason)}
    />
  );
}
