import { useState } from 'react';
import UsersTable from '../components/UsersTable';
import RolesTable from '../components/RolesTable';
import PermissionMatrix from '../components/PermissionMatrix';
import { Button, Modal, PageHeader } from '@/components/ui';
import {  adminUsers, roles  } from '@/features/dashboard/services/dashboard.service';
import { useModal } from '@/hooks/useModal';
import { HiPlus } from 'react-icons/hi2';
import { cn } from '@/utils/cn';
import PageContainer from '@/components/layouts/PageContainer';


const TABS = ['Users', 'Roles', 'Permissions'];

export default function UsersPage() {
  const [activeTab, setActiveTab] = useState('Users');
  const [selectedRole, setSelectedRole] = useState(roles[0]);
  const addUserModal = useModal();

  const handleEditRole = (role) => {
    setSelectedRole(role);
    setActiveTab('Permissions');
  };

  return (
    <PageContainer>
      <PageHeader 
        title="Users & Roles" 
        crumbs={[{ label: 'Dashboard', path: '/' }, { label: 'Settings' }, { label: 'Users & Roles' }]}
      >
        {activeTab === 'Users' && (
          <Button className="bg-blue-600 hover:bg-blue-700 text-white shadow-sm" onClick={addUserModal.open}>
            <HiPlus className="h-4 w-4" />
            Add User
          </Button>
        )}
      </PageHeader>

      {/* Tab navigation */}
      <div className="mb-6 flex gap-1 rounded-xl border border-slate-100 bg-white p-1 w-fit">
        {TABS.map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              'rounded-lg px-4 py-2 text-sm font-medium transition-all duration-150',
              activeTab === tab
                ? 'bg-indigo-600 text-white shadow-sm'
                : 'text-slate-600 hover:bg-slate-100'
            )}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'Users' && <UsersTable users={adminUsers} />}

      {activeTab === 'Roles' && (
        <RolesTable roles={roles} onEdit={handleEditRole} />
      )}

      {activeTab === 'Permissions' && (
        <div className="card">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-slate-900">Permission Matrix</h3>
              <p className="text-sm text-slate-500">Role: <span className="font-semibold text-indigo-600">{selectedRole.name}</span></p>
            </div>
            <div className="flex gap-2">
              {roles.map(role => (
                <button
                  key={role.id}
                  onClick={() => setSelectedRole(role)}
                  className={cn(
                    'rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
                    selectedRole.id === role.id
                      ? 'bg-indigo-600 text-white'
                      : 'border border-slate-200 text-slate-600 hover:bg-slate-50'
                  )}
                >
                  {role.name}
                </button>
              ))}
            </div>
          </div>
          <PermissionMatrix role={selectedRole} />
        </div>
      )}

      {/* Add User Modal (placeholder) */}
      <Modal
        isOpen={addUserModal.isOpen}
        onClose={addUserModal.close}
        title="Add Admin User"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={addUserModal.close}>Cancel</Button>
            <Button onClick={addUserModal.close}>Create User</Button>
          </>
        }
      >
        <p className="text-sm text-slate-500">
          New user form — fields will be connected to the backend API in the next phase.
        </p>
      </Modal>
    </PageContainer>
  );
}
