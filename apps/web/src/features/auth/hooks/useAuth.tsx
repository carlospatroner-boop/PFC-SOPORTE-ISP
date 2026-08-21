/* eslint-disable react-refresh/only-export-components -- patron Context+Provider+hook en un
   solo archivo, deliberado y estandar; solo afecta granularidad de fast-refresh en dev. */
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { login as loginRequest } from '../api/authApi'
import { clearSession, getRole, isLoggedIn, saveSession, type Role } from '../session'

interface AuthContextValue {
  authenticated: boolean
  role: Role | null
  loading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<boolean>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authenticated, setAuthenticated] = useState(isLoggedIn())
  const [role, setRole] = useState<Role | null>(getRole())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const login = useCallback(async (email: string, password: string) => {
    setLoading(true)
    setError(null)
    try {
      const auth = await loginRequest(email, password)
      saveSession(auth.accessToken, auth.refreshToken, auth.accessTokenExpiresAt)
      setAuthenticated(true)
      setRole(getRole())
      return true
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión')
      return false
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setAuthenticated(false)
    setRole(null)
  }, [])

  return (
    <AuthContext.Provider value={{ authenticated, role, loading, error, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
