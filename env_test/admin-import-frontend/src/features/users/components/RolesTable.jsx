import { HiOutlineUserGroup, HiOutlinePencilSquare } from 'react-icons/hi2';

export default function RolesTable({ roles, onEdit }) {
  return (
    <div className="space-y-3">
      {roles.map(role => (
        <div key={role.id} className="card flex items-start justify-between gap-4">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-xl bg-indigo-100">
              <HiOutlineUserGroup className="h-5 w-5 text-indigo-600" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="font-semibold text-slate-900">{role.name}</h4>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                  {role.userCount} user{role.userCount !== 1 ? 's' : ''}
                </span>
              </div>
              <p className="mt-0.5 text-sm text-slate-500">{role.description}</p>
            </div>
          </div>
          <button
            onClick={() => onEdit?.(role)}
            className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg text-slate-400 hover:bg-indigo-50 hover:text-indigo-600 transition-colors"
          >
            <HiOutlinePencilSquare className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  );
}
