import { describe, it, expect, beforeEach } from 'vitest'
import { saveSession, getAccessToken, getRefreshToken, isLoggedIn, clearSession, getRole, getUserId } from './session'
import { fakeJwt } from '../../test/fakeJwt'

describe('session', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('guarda y recupera el access token y el refresh token', () => {
    saveSession('token123', 'refresh456', null)
    expect(getAccessToken()).toBe('token123')
    expect(getRefreshToken()).toBe('refresh456')
    expect(isLoggedIn()).toBe(true)
  })

  it('no hay sesion antes de guardar nada', () => {
    expect(getAccessToken()).toBeNull()
    expect(isLoggedIn()).toBe(false)
  })

  it('un token con fecha de expiracion pasada se considera expirado', () => {
    const past = new Date(Date.now() - 1000).toISOString()
    saveSession('token123', 'refresh456', past)
    expect(getAccessToken()).toBeNull()
    expect(isLoggedIn()).toBe(false)
  })

  it('un token con fecha de expiracion futura sigue siendo valido', () => {
    const future = new Date(Date.now() + 60_000).toISOString()
    saveSession('token123', 'refresh456', future)
    expect(getAccessToken()).toBe('token123')
    expect(isLoggedIn()).toBe(true)
  })

  it('clearSession borra todo', () => {
    saveSession('token123', 'refresh456', null)
    clearSession()
    expect(isLoggedIn()).toBe(false)
    expect(getRefreshToken()).toBeNull()
  })

  it('getRole/getUserId decodifican el payload del JWT', () => {
    const token = fakeJwt({ sub: 'user-42', email: 'a@b.com', role: 'TECNICO', permissions: [] })
    saveSession(token, 'refresh456', null)
    expect(getRole()).toBe('TECNICO')
    expect(getUserId()).toBe('user-42')
  })

  it('getRole/getUserId devuelven null sin sesion activa', () => {
    expect(getRole()).toBeNull()
    expect(getUserId()).toBeNull()
  })

  it('getRole devuelve null si el token no es un JWT valido', () => {
    saveSession('esto-no-es-un-jwt', 'refresh456', null)
    expect(getRole()).toBeNull()
  })
})
