/**
 * El backend no emite cookies httpOnly (no hay soporte de sesion de servidor), asi que la
 * alternativa exigida por el Modulo B item 6 es sessionStorage con expiracion -- nunca
 * localStorage plano, y nunca persistente entre pestañas/reinicios del navegador.
 */
const ACCESS_TOKEN_KEY = 'soporte_access_token'
const REFRESH_TOKEN_KEY = 'soporte_refresh_token'
const EXPIRES_AT_KEY = 'soporte_expires_at'

export function saveSession(accessToken: string, refreshToken: string, expiresAt: string | null) {
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  if (expiresAt) {
    window.sessionStorage.setItem(EXPIRES_AT_KEY, expiresAt)
  }
}

function isExpired(): boolean {
  const expiresAt = window.sessionStorage.getItem(EXPIRES_AT_KEY)
  if (!expiresAt) return false
  return new Date(expiresAt).getTime() <= Date.now()
}

export function getAccessToken(): string | null {
  if (isExpired()) {
    clearSession()
    return null
  }
  return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return window.sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

export function isLoggedIn(): boolean {
  return getAccessToken() !== null
}

export function clearSession() {
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  window.sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  window.sessionStorage.removeItem(EXPIRES_AT_KEY)
}

export type Role = 'CLIENTE' | 'TECNICO' | 'ADMIN'

interface JwtClaims {
  sub: string
  email: string
  role: Role
  permissions: string[]
  zone?: string
  exp: number
}

// El login (AuthResponseDto) solo devuelve accessToken/refreshToken -- nunca el rol por
// separado -- asi que se decodifica el propio JWT (payload en base64url, sin verificar
// firma: eso ya lo hizo el backend) para saber que UI mostrar. Nunca se usa esto para
// autorizar nada; la autorizacion real vive en el backend (ver TicketAuthorization.java) y
// se vuelve a validar ahi en cada request pase lo que pase en el cliente.
function decodeClaims(token: string): JwtClaims | null {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json) as JwtClaims
  } catch {
    return null
  }
}

export function getClaims(): JwtClaims | null {
  const token = getAccessToken()
  return token ? decodeClaims(token) : null
}

export function getRole(): Role | null {
  return getClaims()?.role ?? null
}

export function getUserId(): string | null {
  return getClaims()?.sub ?? null
}
