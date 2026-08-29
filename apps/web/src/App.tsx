import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { RoleRoute } from './components/RoleRoute'
import { AppLayout } from './components/AppLayout'
import { LoginPage } from './features/auth/pages/LoginPage'
import { ConsolePage } from './features/console/pages/ConsolePage'
import { SettingsPage } from './features/settings/pages/SettingsPage'
import { AboutPage } from './features/about/pages/AboutPage'
import { AdminPage } from './features/admin/pages/AdminPage'
import { ReportsPage } from './features/reports/pages/ReportsPage'
import { isLoggedIn } from './features/auth/session'

// Las 5 rutas minimas exigidas por el Modulo B item 4 de la guia de E4: "/", "/login",
// "/main" (el dominio del PFC -- la consola de operadores), "/settings" y "/about" -- la
// guia pide "al menos cinco", asi que /admin y /reports (solo ADMIN, RoleRoute) se agregan
// sin romper ese minimo. Portadas de la version anterior (frontend/index.html, vistas
// "Administracion" y "Reportes"), que la SPA nueva todavia no tenia.
function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to={isLoggedIn() ? '/main' : '/login'} replace />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/main"
        element={
          <ProtectedRoute>
            <AppLayout>
              <ConsolePage />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin"
        element={
          <RoleRoute allow={['ADMIN']}>
            <AppLayout>
              <AdminPage />
            </AppLayout>
          </RoleRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <RoleRoute allow={['ADMIN']}>
            <AppLayout>
              <ReportsPage />
            </AppLayout>
          </RoleRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <AppLayout>
              <SettingsPage />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/about"
        element={
          <ProtectedRoute>
            <AppLayout>
              <AboutPage />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
