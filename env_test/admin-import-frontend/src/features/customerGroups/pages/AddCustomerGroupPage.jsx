// Customer-group create/edit route.
import { useState, useEffect } from 'react';
import { useNavigate, Link, useParams } from 'react-router-dom';
import { HiOutlineUserGroup, HiOutlineInformationCircle } from 'react-icons/hi2';
import Button from '@/components/ui/Button';
import PageContainer from '@/components/layouts/PageContainer';


export default function AddCustomerGroupPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);

  const [form, setForm] = useState({
    name: '',
    description: '',
    type: 'VIP',
    status: 'active',
  });

  const allTypes = ['VIP', 'Loyalty', 'Wholesale', 'Default', 'Behavioral', 'Segment', 'Corporate'];

  // Fetch initial data if isEdit
  useEffect(() => {
    if (isEdit) {
      // Mock fetch
      setForm({
        name: 'Platinum Members',
        description: 'Highest tier customers with special privileges.',
        type: 'VIP',
        status: 'active',
      });
    }
  }, [isEdit, id]);

  return (
    <PageContainer>
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 py-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900">{isEdit ? 'Edit Customer Group' : 'Add Customer Group'}</h2>
          {/* Breadcrumbs */}
          <div className="mt-1 flex items-center gap-2 text-xs font-medium text-slate-500">
            <Link to="/dashboard" className="hover:text-blue-600 transition-colors">Dashboard</Link>
            <span className="text-slate-300">›</span>
            <Link to="/dashboard/customers" className="hover:text-blue-600 transition-colors">Customers</Link>
            <span className="text-slate-300">›</span>
            <Link to="/dashboard/customer-groups" className="hover:text-blue-600 transition-colors">Customer Groups</Link>
            <span className="text-slate-300">›</span>
            <span className="text-slate-400">{isEdit ? 'Edit Group' : 'Add Group'}</span>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="secondary" onClick={() => navigate('/dashboard/customer-groups')} className="bg-white border-slate-200 text-slate-700 hover:bg-slate-50 shadow-sm">
            Cancel
          </Button>
          <Button className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm">
            {isEdit ? 'Update Group' : 'Save Group'}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        {/* Left Column: Form */}
        <div className="lg:col-span-2 space-y-6">
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Group Information</h3>
            <p className="mb-6 mt-1 text-sm text-slate-500">Define the details for this customer group.</p>

            <div className="space-y-5">
              {/* Name */}
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Group Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. VIP Customers"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
              </div>

              {/* Description */}
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Description
                </label>
                <textarea
                  placeholder="Group description…"
                  value={form.description}
                  onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
                  rows={4}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 placeholder-slate-400 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
              </div>

              {/* Group Type */}
              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Group Type
                </label>
                <select
                  value={form.type}
                  onChange={e => setForm(f => ({ ...f, type: e.target.value }))}
                  className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 transition-colors focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                >
                  {allTypes.map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </select>
              </div>

              {/* Status */}
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-700">
                  Status <span className="text-red-500">*</span>
                </label>
                <div className="flex items-center gap-6">
                  <label className="flex cursor-pointer items-center gap-2">
                    <div className="relative flex h-4 w-4 items-center justify-center rounded-full border border-slate-300">
                      {form.status === 'active' && <div className="h-2 w-2 rounded-full bg-blue-600" />}
                    </div>
                    <input
                      type="radio"
                      name="status"
                      value="active"
                      checked={form.status === 'active'}
                      onChange={e => setForm(f => ({ ...f, status: e.target.value }))}
                      className="hidden"
                    />
                    <span className="text-sm font-medium text-slate-900">Active</span>
                  </label>
                  <label className="flex cursor-pointer items-center gap-2">
                    <div className="relative flex h-4 w-4 items-center justify-center rounded-full border border-slate-300">
                      {form.status === 'inactive' && <div className="h-2 w-2 rounded-full bg-blue-600" />}
                    </div>
                    <input
                      type="radio"
                      name="status"
                      value="inactive"
                      checked={form.status === 'inactive'}
                      onChange={e => setForm(f => ({ ...f, status: e.target.value }))}
                      className="hidden"
                    />
                    <span className="text-sm text-slate-600">Inactive</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column */}
        <div className="space-y-6">
          {/* Preview */}
          <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Preview</h3>
            <div className="mt-4 flex flex-col items-center gap-3 rounded-xl border border-slate-100 bg-slate-50 p-6 text-center">
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-blue-100 text-blue-600 shadow-inner">
                <HiOutlineUserGroup className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm font-bold text-slate-900">{form.name || 'Group Name'}</p>
                <span className="mt-2 inline-flex items-center rounded-md bg-white px-2 py-1 text-xs font-semibold text-slate-600 border border-slate-200 shadow-sm">
                  {form.type}
                </span>
              </div>
            </div>
          </div>

          {/* Alert */}
          <div className="flex items-start gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4">
            <HiOutlineInformationCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-blue-500" />
            <div>
              <p className="text-sm font-semibold text-blue-800">Group Management</p>
              <p className="mt-1 text-xs text-blue-600/90 leading-relaxed">
                Customer groups help you segment users for marketing campaigns, special discounts, and reporting.
              </p>
            </div>
          </div>
        </div>
      </div>
    </PageContainer>
  );
}
