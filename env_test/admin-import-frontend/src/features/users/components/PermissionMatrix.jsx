import { PERMISSIONS, PERMISSION_MODULES } from '@/constants/roles';
import { HiCheck } from 'react-icons/hi2';

export default function PermissionMatrix({ role }) {
  const perms = role.permissions || {};

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[600px] border-collapse">
        <thead>
          <tr>
            <th className="w-40 pb-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Module
            </th>
            {PERMISSIONS.map(perm => (
              <th
                key={perm}
                className="pb-3 text-center text-xs font-semibold uppercase tracking-wider text-slate-500 capitalize"
              >
                {perm}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-50">
          {PERMISSION_MODULES.map(module => {
            const modulePerm = perms[module] || {};
            return (
              <tr key={module} className="hover:bg-indigo-50/30 transition-colors">
                <td className="py-3 pr-6 text-sm font-medium text-slate-700">{module}</td>
                {PERMISSIONS.map(perm => {
                  const granted = modulePerm[perm];
                  return (
                    <td key={perm} className="py-3 text-center">
                      <div className="flex items-center justify-center">
                        <div
                          className={`flex h-6 w-6 items-center justify-center rounded ${
                            granted
                              ? 'bg-indigo-600 text-white'
                              : 'border border-slate-200 bg-white text-transparent'
                          }`}
                        >
                          <HiCheck className="h-3.5 w-3.5" />
                        </div>
                      </div>
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
