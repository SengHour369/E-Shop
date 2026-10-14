import { NavLink, useLocation } from 'react-router-dom';
import { useUI } from '@/contexts/UIContext';
import { NAV_ITEMS, NAV_GROUPS } from '@/config/navigation';
import { cn } from '@/utils/cn';
import {
  HiOutlineSquares2X2, HiOutlineShoppingBag, HiOutlineTag,
  HiOutlineBuildingStorefront, HiOutlineClipboardDocumentList,
  HiOutlineUsers, HiOutlineCreditCard, HiOutlineChartBar,
  HiOutlineCog6Tooth, HiOutlineUserGroup, HiOutlineArrowPath,
  HiOutlineXMark, HiOutlineArchiveBox, HiOutlineQrCode,
} from 'react-icons/hi2';

const ICONS = {
  dashboard:  HiOutlineSquares2X2,
  orders:     HiOutlineClipboardDocumentList,
  returns:    HiOutlineArrowPath,
  cancellations: HiOutlineXMark,
  payments:   HiOutlineCreditCard,
  refunds:    HiOutlineArrowPath,
  transactions: HiOutlineClipboardDocumentList,
  customers:  HiOutlineUsers,
  'customer-groups': HiOutlineUserGroup,
  products:   HiOutlineShoppingBag,
  scanner:    HiOutlineQrCode,
  categories: HiOutlineTag,
  brands:     HiOutlineBuildingStorefront,
  inventory:  HiOutlineArchiveBox,
  reports:    HiOutlineChartBar,
  settings:   HiOutlineCog6Tooth,
  users:      HiOutlineUserGroup,
};

export default function Sidebar() {
  const { sidebarCollapsed, sidebarMobileOpen, closeMobileSidebar } = useUI();
  const location = useLocation();

  const isActive = (path) =>
    path === '/dashboard' ? location.pathname === '/dashboard' : location.pathname.startsWith(path);

  return (
    <>
      {/* Mobile overlay */}
      {sidebarMobileOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 backdrop-blur-sm md:hidden"
          onClick={closeMobileSidebar}
        />
      )}

      {/* Sidebar panel */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex flex-col bg-slate-50 border-r border-slate-200 transition-sidebar',
          'md:relative md:translate-x-0',
          sidebarCollapsed ? 'sidebar-collapsed' : 'sidebar-width',
          sidebarMobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        )}
      >
        {/* Logo */}
        <div className="flex h-16 flex-shrink-0 items-center justify-between px-4 border-b border-transparent">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 flex-shrink-0 items-center justify-center rounded-lg bg-blue-600">
              <svg className="h-4 w-4 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
              </svg>
            </div>
            {!sidebarCollapsed && (
              <span className="text-lg font-bold text-slate-900 tracking-tight">
                ShopAdmin
              </span>
            )}
          </div>
          {/* Mobile close button */}
          <button
            onClick={closeMobileSidebar}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 hover:bg-slate-800 md:hidden"
          >
            <HiOutlineXMark className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="sidebar-scroll flex-1 overflow-y-auto px-3 py-2">
          {NAV_GROUPS.map(group => {
            const groupItems = NAV_ITEMS.filter(i => i.group === group.key);
            if (!groupItems.length) return null;

            return (
              <div key={group.key} className="mb-4">
                {group.label && !sidebarCollapsed && (
                  <p className="mb-1 px-2 text-[10px] font-semibold uppercase tracking-widest text-slate-500">
                    {group.label}
                  </p>
                )}
                {group.label && sidebarCollapsed && (
                  <div className="mb-1 h-px bg-slate-200 mx-2" />
                )}
                <ul className="space-y-0.5">
                  {groupItems.map(item => {
                    const Icon = ICONS[item.icon] || HiOutlineClipboardDocumentList;
                    const active = isActive(item.path);
                    return (
                      <li key={item.path}>
                        <NavLink
                          to={item.path}
                          onClick={closeMobileSidebar}
                          title={sidebarCollapsed ? item.label : undefined}
                          className={cn(
                            'flex items-center justify-between rounded-lg px-2 py-2 text-sm font-medium transition-all duration-150',
                            active
                              ? 'bg-blue-50 text-blue-600'
                              : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900',
                            sidebarCollapsed && 'justify-center px-0'
                          )}
                        >
                          <div className="flex items-center gap-3">
                            {Icon && <Icon className="h-5 w-5 flex-shrink-0" />}
                            {!sidebarCollapsed && (
                              <span className="truncate">{item.label}</span>
                            )}
                          </div>
                          {!sidebarCollapsed && item.badge && (
                            <span className="text-xs font-semibold text-blue-600">{item.badge}</span>
                          )}
                        </NavLink>
                      </li>
                    );
                  })}
                </ul>
              </div>
            );
          })}
        </nav>

        {/* Bottom user info removed to match image design */}
      </aside>
    </>
  );
}
