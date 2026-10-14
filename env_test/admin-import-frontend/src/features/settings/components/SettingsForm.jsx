import { useState } from 'react';
import Input, { Toggle, Select } from '@/components/ui/Input';
import Button from '@/components/ui/Button';


export default function SettingsForm() {
  const [form, setForm] = useState({
    storeName: 'ShopAdmin Store',
    email: 'admin@shopadmin.com',
    phone: '+1 555-0100',
    address: '123 Commerce Ave, New York, NY 10001',
    currency: 'USD',
    taxRate: '8.0',
    language: 'English',
    timezone: 'America/New_York',
    // Toggles
    emailNotifications: true,
    orderNotifications: true,
    inventoryAlerts: true,
    maintenanceMode: false,
    twoFactorAuth: true,
  });
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const set = (key, val) => setForm(prev => ({ ...prev, [key]: val }));

  const handleSave = async () => {
    setSaving(true);
    await new Promise(r => setTimeout(r, 800));
    setSaving(false);
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Store info */}
      <div className="card">
        <h3 className="mb-4 text-base font-semibold text-slate-900">Store Information</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="Store Name" value={form.storeName} onChange={e => set('storeName', e.target.value)} />
          <Input label="Email" type="email" value={form.email} onChange={e => set('email', e.target.value)} />
          <Input label="Phone" value={form.phone} onChange={e => set('phone', e.target.value)} />
          <Select label="Currency" value={form.currency} onChange={e => set('currency', e.target.value)}>
            {['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD'].map(c => <option key={c} value={c}>{c}</option>)}
          </Select>
          <Input
            label="Tax Rate (%)"
            type="number"
            value={form.taxRate}
            onChange={e => set('taxRate', e.target.value)}
            rightIcon={<span className="text-sm text-slate-400">%</span>}
          />
          <Select label="Timezone" value={form.timezone} onChange={e => set('timezone', e.target.value)}>
            <option value="America/New_York">America/New York (UTC-5)</option>
            <option value="America/Los_Angeles">America/Los Angeles (UTC-8)</option>
            <option value="Europe/London">Europe/London (UTC+0)</option>
            <option value="Asia/Tokyo">Asia/Tokyo (UTC+9)</option>
          </Select>
          <div className="sm:col-span-2">
            <Input
              label="Address"
              value={form.address}
              onChange={e => set('address', e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Notification settings */}
      <div className="card">
        <h3 className="mb-4 text-base font-semibold text-slate-900">Notifications</h3>
        <div className="space-y-4">
          {[
            { key: 'emailNotifications', label: 'Email Notifications', desc: 'Receive email updates for store activity' },
            { key: 'orderNotifications', label: 'Order Notifications', desc: 'Get notified for new orders and updates' },
            { key: 'inventoryAlerts',    label: 'Inventory Alerts',    desc: 'Low stock and out-of-stock alerts' },
          ].map(item => (
            <div key={item.key} className="flex items-center justify-between rounded-xl border border-slate-100 p-4">
              <div>
                <p className="text-sm font-medium text-slate-800">{item.label}</p>
                <p className="text-xs text-slate-500">{item.desc}</p>
              </div>
              <Toggle
                id={item.key}
                checked={form[item.key]}
                onChange={val => set(item.key, val)}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Security settings */}
      <div className="card">
        <h3 className="mb-4 text-base font-semibold text-slate-900">Security</h3>
        <div className="space-y-4">
          {[
            { key: 'twoFactorAuth',   label: 'Two-Factor Authentication', desc: 'Add an extra layer of security', danger: false },
            { key: 'maintenanceMode', label: 'Maintenance Mode',           desc: 'Show maintenance page to visitors', danger: true },
          ].map(item => (
            <div key={item.key} className={`flex items-center justify-between rounded-xl border p-4 ${item.danger ? 'border-red-100 bg-red-50/40' : 'border-slate-100'}`}>
              <div>
                <p className={`text-sm font-medium ${item.danger ? 'text-red-700' : 'text-slate-800'}`}>{item.label}</p>
                <p className="text-xs text-slate-500">{item.desc}</p>
              </div>
              <Toggle
                id={item.key}
                checked={form[item.key]}
                onChange={val => set(item.key, val)}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Save */}
      <div className="flex items-center justify-end gap-3">
        {saved && (
          <span className="flex items-center gap-1.5 text-sm text-emerald-600 font-medium">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            Settings saved
          </span>
        )}
        <Button variant="secondary" onClick={() => {}}>Reset</Button>
        <Button loading={saving} onClick={handleSave}>Save Changes</Button>
      </div>
    </div>
  );
}
