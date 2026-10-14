import { useState } from 'react';
import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import Input, { Select } from '@/components/ui/Input';
import { useModal } from '@/hooks/useModal';
import { formatDate } from '@/utils/formatDate';
import { HiOutlinePencilSquare, HiOutlineTrash } from 'react-icons/hi2';

export default function UsersTable({ users }) {
  const editModal = useModal();
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);

  const openEdit = (user) => {
    setForm({ ...user });
    editModal.open(user);
  };

  const handleSave = async () => {
    setSaving(true);
    await new Promise(r => setTimeout(r, 600));
    setSaving(false);
    editModal.close();
  };

  const columns = [
    {
      key: 'name', label: 'User',
      render: (val, row) => (
        <div className="flex items-center gap-3">
          <img src={row.avatar} alt={val} className="h-9 w-9 rounded-full" />
          <div>
            <p className="font-medium text-slate-900">{val}</p>
            <p className="text-xs text-slate-400">{row.email}</p>
          </div>
        </div>
      ),
    },
    { key: 'role', label: 'Role',
      render: val => (
        <span className="rounded-md bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700">{val}</span>
      ),
    },
    { key: 'status', label: 'Status',
      render: val => (
        <Badge variant={val === 'active' ? 'success' : 'default'} dot>
          {val === 'active' ? 'Active' : 'Inactive'}
        </Badge>
      ),
    },
    { key: 'lastLogin', label: 'Last Login',
      render: val => <span className="text-sm text-slate-500">{formatDate(val)}</span> },
    { key: 'actions', label: 'Actions', align: 'right',
      render: (_, row) => (
        <div className="flex items-center justify-end gap-1">
          <button
            onClick={() => openEdit(row)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 transition-colors"
          >
            <HiOutlinePencilSquare className="h-4 w-4" />
          </button>
          <button className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-red-50 hover:text-red-600 transition-colors">
            <HiOutlineTrash className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <>
      <Table columns={columns} data={users} />
      <Modal
        isOpen={editModal.isOpen}
        onClose={editModal.close}
        title="Edit User"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={editModal.close}>Cancel</Button>
            <Button loading={saving} onClick={handleSave}>Save Changes</Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input label="Name" value={form.name || ''} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} />
          <Input label="Email" type="email" value={form.email || ''} onChange={e => setForm(p => ({ ...p, email: e.target.value }))} />
          <Select label="Role" value={form.role || ''} onChange={e => setForm(p => ({ ...p, role: e.target.value }))}>
            <option value="Super Admin">Super Admin</option>
            <option value="Manager">Manager</option>
            <option value="Editor">Editor</option>
            <option value="Viewer">Viewer</option>
          </Select>
          <Select label="Status" value={form.status || ''} onChange={e => setForm(p => ({ ...p, status: e.target.value }))}>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </Select>
        </div>
      </Modal>
    </>
  );
}
