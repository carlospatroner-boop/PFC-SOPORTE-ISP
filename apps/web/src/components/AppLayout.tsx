import type { CSSProperties, ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/useAuth'
import type { Role } from '../features/auth/session'
import { ChartIcon, ConsoleIcon, GearIcon, InfoIcon, ShieldIcon } from './icons'

// Un color por rol, coherente con la paleta de estados del resto del sistema -- ADMIN en
// ambar (el mismo tono "elevado"/administrativo que ya usaba el boton, ver ADR de la
// consola), TECNICO en teal, CLIENTE en un gris neutro (no gestiona nada, no necesita
// un color de "accion").
const ROLE_BADGE_COLOR: Record<Role, string> = {
  ADMIN: 'var(--color-amber)',
  TECNICO: 'var(--color-teal)',
  CLIENTE: 'var(--color-gray)',
}

export function AppLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const { logout, role } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header
        style={{
          background: 'var(--color-navy)',
          color: '#fff',
          padding: '12px 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <strong>{t('app.title')}</strong>
          {/* Insignia de rol: visible en todas las paginas autenticadas (no solo /admin), para
              que quede claro con que cuenta se entro apenas se hace login -- pedido explicito
              tras mostrar la consola de ADMIN sin ninguna marca que la distinguiera. */}
          {role && (
            <span style={{ ...roleBadgeStyle, background: ROLE_BADGE_COLOR[role] }}>
              {t(`role.${role}`)}
            </span>
          )}
        </div>
        <nav style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <NavLink to="/main" style={navLinkStyle}>
            <ConsoleIcon /> {t('nav.console')}
          </NavLink>
          {/* Admin/Reportes: solo visibles para ADMIN, igual que en la version anterior
              (frontend/app.js, navAdmin.classList.toggle("hidden", role !== "ADMIN")).
              RoleRoute hace cumplir esto tambien si alguien fuerza la URL a mano. */}
          {role === 'ADMIN' && (
            <NavLink to="/admin" style={navLinkStyle}>
              <ShieldIcon /> {t('nav.admin')}
            </NavLink>
          )}
          {role === 'ADMIN' && (
            <NavLink to="/reports" style={navLinkStyle}>
              <ChartIcon /> {t('nav.reports')}
            </NavLink>
          )}
          <NavLink to="/settings" style={navLinkStyle}>
            <GearIcon /> {t('nav.settings')}
          </NavLink>
          <NavLink to="/about" style={navLinkStyle}>
            <InfoIcon /> {t('nav.about')}
          </NavLink>
          <button type="button" onClick={handleLogout} style={logoutStyle}>
            {t('nav.logout')}
          </button>
        </nav>
      </header>
      <main style={{ flex: 1, background: 'var(--bg)' }}>{children}</main>
    </div>
  )
}

function navLinkStyle({ isActive }: { isActive: boolean }): CSSProperties {
  return {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
    color: '#fff',
    textDecoration: isActive ? 'underline' : 'none',
    fontWeight: isActive ? 700 : 400,
  }
}

const roleBadgeStyle: CSSProperties = {
  color: '#fff',
  fontSize: '0.72rem',
  fontWeight: 700,
  letterSpacing: '0.04em',
  textTransform: 'uppercase',
  padding: '3px 10px',
  borderRadius: 999,
}

const logoutStyle: CSSProperties = {
  background: 'transparent',
  border: '1px solid rgba(255,255,255,0.6)',
  color: '#fff',
  borderRadius: 6,
  padding: '4px 12px',
  cursor: 'pointer',
}
