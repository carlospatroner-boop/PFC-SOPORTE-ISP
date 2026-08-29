import { describe, it, expect, vi } from 'vitest'
import { authClient } from '../../../lib/apiClient'
import { login } from './authApi'

describe('authApi.login', () => {
  it('llama a POST api/v1/auth/login con las credenciales y devuelve el data desenvuelto', async () => {
    const spy = vi.spyOn(authClient, 'post').mockResolvedValue({
      data: {
        data: { accessToken: 'a', refreshToken: 'b', accessTokenExpiresAt: null },
        message: 'Sesion iniciada',
        timestamp: '2026-01-01T00:00:00Z',
      },
    })

    const result = await login('user@test.com', 'secret')

    expect(spy).toHaveBeenCalledWith('api/v1/auth/login', { email: 'user@test.com', password: 'secret' })
    expect(result).toEqual({ accessToken: 'a', refreshToken: 'b', accessTokenExpiresAt: null })
  })
})
