import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/useAuth'
import type { Role } from '../features/auth/session'

/**
 * Igual que ProtectedRoute (exige sesion activa) pero ademas restringe por rol -- usado
 * para /admin y /reports, que en la version anterior (frontend/app.js) solo se mostraban
 * en el menu cuando state.role === "ADMIN". Un CLIENTE/TECNICO que entre a la URL a mano
 * rebota a /main en vez de ver una pantalla vacia o un 403 crudo.
 */
export function RoleRoute({ allow, children }: { allow: Role[]; children: ReactNode }) {
  const { authenticated, role } = useAuth()
  if (!authenticated) {
    return <Navigate to="/login" replace />
  }
  if (!role || !allow.includes(role)) {
    return <Navigate to="/main" replace />
  }
  return <>{children}</>
}
