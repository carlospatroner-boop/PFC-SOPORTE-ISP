import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/useAuth'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { authenticated } = useAuth()
  if (!authenticated) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}
