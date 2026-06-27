import React, { useState } from 'react';
import { Ripple } from 'primereact/ripple';
import './Layout.css';
import { Page } from '../App';

interface LayoutProps {
  currentPage: Page;
  onNavigate: (page: Page) => void;
  children: React.ReactNode;
}

const navItems: { page: Page; icon: string; label: string; desc: string }[] = [
  { page: 'clients',  icon: 'pi-users',       label: 'Clients',  desc: 'Manage clients'  },
  { page: 'accounts', icon: 'pi-credit-card',  label: 'Accounts', desc: 'Bank accounts'   },
  { page: 'loans',    icon: 'pi-dollar',       label: 'Loans',    desc: 'Loan products'   },
];

const Layout: React.FC<LayoutProps> = ({ currentPage, onNavigate, children }) => {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="bs-shell">
      {/* ── Sidebar ─────────────────────────────────────────────────── */}
      <aside className={`bs-sidebar${mobileOpen ? ' bs-sidebar--open' : ''}`}>
        {/* Brand */}
        <div className="bs-brand">
          <div className="bs-brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M3 21h18M3 10h18M5 6l7-3 7 3M4 10v11M20 10v11M8 10v11M12 10v11M16 10v11"
                stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <div>
            <div className="bs-brand-name">BankSystem</div>
            <div className="bs-brand-sub">Management Portal</div>
          </div>
        </div>

        <div className="bs-sidebar-sep" />

        {/* Nav label */}
        <div className="bs-nav-label">NAVIGATION</div>

        {/* Nav items */}
        <nav className="bs-nav">
          {navItems.map(({ page, icon, label, desc }) => (
            <button
              key={page}
              className={`bs-nav-item p-ripple${currentPage === page ? ' bs-nav-item--active' : ''}`}
              onClick={() => { onNavigate(page); setMobileOpen(false); }}
            >
              <span className="bs-nav-item-icon">
                <i className={`pi ${icon}`} />
              </span>
              <span className="bs-nav-item-text">
                <span className="bs-nav-item-label">{label}</span>
                <span className="bs-nav-item-desc">{desc}</span>
              </span>
              {currentPage === page && <span className="bs-nav-item-dot" />}
              <Ripple />
            </button>
          ))}
        </nav>

        {/* Footer */}
        <div className="bs-sidebar-footer">
          <div className="bs-sidebar-footer-avatar">
            <i className="pi pi-shield" />
          </div>
          <div>
            <div className="bs-sidebar-footer-name">Secure Connection</div>
            <div className="bs-sidebar-footer-role">v1.0.0 · Production</div>
          </div>
        </div>
      </aside>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="bs-overlay" onClick={() => setMobileOpen(false)} />
      )}

      {/* ── Main area ───────────────────────────────────────────────── */}
      <div className="bs-main">
        {/* Top bar */}
        <header className="bs-topbar">
          <button className="bs-hamburger" onClick={() => setMobileOpen(v => !v)}>
            <i className={`pi ${mobileOpen ? 'pi-times' : 'pi-bars'}`} />
          </button>
          <div className="bs-topbar-breadcrumb">
            <span className="bs-topbar-page">
              {navItems.find(n => n.page === currentPage)?.label}
            </span>
          </div>
          <div className="bs-topbar-right">
            <div className="bs-topbar-badge">
              <i className="pi pi-bell" />
            </div>
            <div className="bs-topbar-user">
              <div className="bs-topbar-avatar">B</div>
              <span>Admin</span>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="bs-content">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
