import { useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import './AdminLayout.css'

function AdminLayout() {
  const location = useLocation()
  const [openMenus, setOpenMenus] = useState<string[]>(['settlement', 'analysis'])

  const toggleMenu = (menuKey: string) => {
    setOpenMenus(prev =>
      prev.includes(menuKey)
        ? prev.filter(key => key !== menuKey)
        : [...prev, menuKey]
    )
  }

  const isMenuOpen = (menuKey: string) => openMenus.includes(menuKey)
  const isActive = (path: string) => location.pathname === path
  const isActiveParent = (paths: string[]) => paths.some(path => location.pathname.startsWith(path))

  return (
    <div className="admin-layout">
      <header className="admin-header">
        <Link to="/admin" className="admin-logo">
          박신사 데이터 플랫폼
        </Link>
      </header>
      <div className="admin-content-wrapper">
        <aside className="admin-sidebar">
          <nav className="admin-nav">
            {/* 대시보드 */}
            <div className="nav-menu-item">
              <Link 
                to="/admin/dashboard" 
                className={`nav-link ${isActive('/admin/dashboard') ? 'active' : ''}`}
              >
                대시보드
              </Link>
            </div>

            {/* 통계/분석 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/analysis']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('analysis')}
              >
                <span>통계/분석</span>
                <span className={`nav-arrow ${isMenuOpen('analysis') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('analysis') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/analysis/statistics" 
                    className={`nav-link submenu-link ${isActive('/admin/analysis/statistics') ? 'active' : ''}`}
                  >
                    매출 통계
                  </Link>
                </div>
              )}
            </div>

            {/* 정산 관리 */}
            <div className="nav-menu-item">
              <div 
                className={`nav-parent ${isActiveParent(['/admin/settlement']) ? 'active-parent' : ''}`}
                onClick={() => toggleMenu('settlement')}
              >
                <span>정산 관리</span>
                <span className={`nav-arrow ${isMenuOpen('settlement') ? 'open' : ''}`}>▼</span>
              </div>
              {isMenuOpen('settlement') && (
                <div className="nav-submenu">
                  <Link 
                    to="/admin/settlement/manage" 
                    className={`nav-link submenu-link ${isActive('/admin/settlement/manage') ? 'active' : ''}`}
                  >
                    정산 내역 조회
                  </Link>
                </div>
              )}
            </div>
          </nav>
        </aside>
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
