import { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useUI } from '@/contexts/UIContext';
import {
  HiOutlineBars3, HiOutlineBell, HiOutlineMagnifyingGlass,
  HiOutlineChevronDown, HiOutlineArrowRightOnRectangle,
  HiOutlineUser, HiOutlineCog6Tooth, HiChevronLeft,
} from 'react-icons/hi2';

const PAGE_TITLES = {
  '/':              { title: 'Dashboard', crumbs: ['Dashboard'] },
  '/products':      { title: 'Products', crumbs: ['Dashboard', 'Products'] },
  '/products/add':  { title: 'Add Product', crumbs: ['Dashboard', 'Products', 'Add Product'] },
  '/categories':    { title: 'Categories', crumbs: ['Dashboard', 'Products', 'Categories'] },
  '/brands':        { title: 'Brands', crumbs: ['Dashboard', 'Products', 'Brands'] },
  '/brands/add':    { title: 'Add Brand', crumbs: ['Dashboard', 'Brands', 'Add Brand'] },
  '/orders':        { title: 'Orders', crumbs: ['Dashboard', 'Orders'] },
  '/customers':     { title: 'Customers', crumbs: ['Dashboard', 'Customers'] },
  '/payments':      { title: 'Payment Details', crumbs: ['Dashboard', 'Payments'] },
  '/returns':       { title: 'Returns', crumbs: ['Dashboard', 'Orders', 'Returns'] },
  '/reports':       { title: 'Reports', crumbs: ['Dashboard', 'Reports'] },
  '/settings':      { title: 'Settings', crumbs: ['Dashboard', 'Settings'] },
  '/users':         { title: 'Users & Roles', crumbs: ['Dashboard', 'Settings', 'Users & Roles'] },
};

function getPageInfo(pathname) {
  if (PAGE_TITLES[pathname]) return PAGE_TITLES[pathname];
  if (pathname.startsWith('/products/edit/')) return { title: 'Edit Product', crumbs: ['Dashboard', 'Products', 'Edit Product'] };
  if (pathname.startsWith('/orders/'))   return { title: 'Order Detail', crumbs: ['Dashboard', 'Orders', 'Order Detail'] };
  if (pathname.startsWith('/customers/')) return { title: 'Customer Detail', crumbs: ['Dashboard', 'Customers', 'Customer Detail'] };
  return { title: 'Admin', crumbs: ['Dashboard'] };
}

export default function Topbar() {
  const { toggleSidebar, toggleMobileSidebar, sidebarCollapsed } = useUI();
  const location = useLocation();
  const navigate = useNavigate();
  const [profileOpen, setProfileOpen] = useState(false);
  const [searchVal, setSearchVal] = useState('');
  const profileRef = useRef(null);

  const { title, crumbs } = getPageInfo(location.pathname);

  // Close profile dropdown on outside click
  useEffect(() => {
    const handler = (e) => {
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <header className="flex h-16 flex-shrink-0 items-center justify-between border-b border-slate-100 bg-white px-4 shadow-sm">
      {/* Left: toggle + title */}
      <div className="flex items-center gap-3">
        {/* Desktop collapse toggle */}
        <button
          onClick={toggleSidebar}
          className="hidden md:flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700"
          title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          <HiChevronLeft
            className={`h-5 w-5 transition-transform duration-250 ${sidebarCollapsed ? 'rotate-180' : ''}`}
          />
        </button>

        {/* Mobile hamburger */}
        <button
          onClick={toggleMobileSidebar}
          className="flex md:hidden h-9 w-9 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100"
        >
          <HiOutlineBars3 className="h-5 w-5" />
        </button>

        <div>
          <h1 className="text-lg font-bold text-slate-900 leading-tight">{title}</h1>
          <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5 font-medium">
            {crumbs.map((crumb, idx) => (
              <span key={idx} className="flex items-center gap-2">
                <span className={idx === crumbs.length - 1 ? 'text-slate-400' : 'text-slate-500 hover:text-indigo-600 cursor-pointer transition-colors'}>
                  {crumb}
                </span>
                {idx < crumbs.length - 1 && <span className="text-slate-300">›</span>}
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Right: search + notifications + profile */}
      <div className="flex items-center gap-2">
        {/* Search */}
        <div className="relative hidden sm:block">
          <HiOutlineMagnifyingGlass className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search orders, customers, products..."
            value={searchVal}
            onChange={e => setSearchVal(e.target.value)}
            className="w-64 rounded-lg border border-slate-200 bg-white py-2 pl-9 pr-10 text-sm text-slate-900 placeholder-slate-400 transition-all focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 lg:w-80"
          />
          <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center justify-center rounded bg-slate-100 px-1.5 py-0.5 text-[10px] font-bold text-slate-500 border border-slate-200">
            ⌘K
          </div>
        </div>

        {/* Notifications */}
        <button className="relative flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700">
          <HiOutlineBell className="h-5 w-5" />
          <span className="absolute top-1.5 right-1.5 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-red-500 text-[8px] font-bold text-white ring-2 ring-white">
            3
          </span>
        </button>

        {/* Divider */}
        <div className="h-6 w-px bg-slate-200" />

        {/* Profile dropdown */}
        <div className="relative" ref={profileRef}>
          <button
            onClick={() => setProfileOpen(prev => !prev)}
            className="flex items-center gap-2.5 rounded-lg px-2 py-1.5 transition-colors hover:bg-slate-100"
          >
            <img src="https://randomuser.me/api/portraits/men/32.jpg" alt="Profile" className="h-8 w-8 rounded-full object-cover border border-slate-200" />
            <div className="hidden text-left sm:block">
              <p className="text-sm font-semibold text-slate-800 leading-none">John Admin</p>
              <p className="text-[11px] text-slate-500 mt-0.5">Super Admin</p>
            </div>
            <HiOutlineChevronDown
              className={`hidden sm:block h-4 w-4 text-slate-400 transition-transform duration-150 ${profileOpen ? 'rotate-180' : ''}`}
            />
          </button>

          {profileOpen && (
            <div className="absolute right-0 top-full mt-2 w-52 rounded-xl border border-slate-100 bg-white py-1 shadow-lg">
              <div className="px-4 py-2 border-b border-slate-100">
                <p className="text-sm font-semibold text-slate-800">John Admin</p>
                <p className="text-xs text-slate-500">john@shopadmin.com</p>
              </div>
              <button
                onClick={() => { setProfileOpen(false); navigate('/dashboard/settings'); }}
                className="flex w-full items-center gap-2.5 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50"
              >
                <HiOutlineUser className="h-4 w-4 text-slate-400" />
                Profile
              </button>
              <button
                onClick={() => { setProfileOpen(false); navigate('/dashboard/settings'); }}
                className="flex w-full items-center gap-2.5 px-4 py-2.5 text-sm text-slate-700 hover:bg-slate-50"
              >
                <HiOutlineCog6Tooth className="h-4 w-4 text-slate-400" />
                Settings
              </button>
              <div className="my-1 border-t border-slate-100" />
              <button
                className="flex w-full items-center gap-2.5 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50"
              >
                <HiOutlineArrowRightOnRectangle className="h-4 w-4" />
                Log Out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
