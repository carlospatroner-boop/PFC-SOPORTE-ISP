import { describe, it, expect, vi } from 'vitest'
import { authClient } from '../../../lib/apiClient'
import { createUser, listUsers } from './adminApi'
import type { UserResponse } from '../types/user'

describe('adminApi.createUser', () => {
  it('llama a POST api/v1/auth/admin/users con el payload y devuelve el usuario creado', async () => {
    const user = { id: 'u1', email: 'tec@test.com', fullName: 'Tec', role: 'TECNICO', zone: 'QUEVEDO_NORTE', active: true, createdAt: '2026-01-01T00:00:00Z' } as UserResponse
    const spy = vi.spyOn(authClient, 'post').mockResolvedValue({
      data: { data: user, message: 'Usuario creado', timestamp: '2026-01-01T00:00:00Z' },
    })
    const payload = { email: 'tec@test.com', password: 'Passw0rd!', fullName: 'Tec', role: 'TECNICO' as const, zone: 'QUEVEDO_NORTE' }

    const result = await createUser(payload)

    expect(spy).toHaveBeenCalledWith('api/v1/auth/admin/users', payload)
    expect(result).toBe(user)
  })
})

describe('adminApi.listUsers', () => {
  it('llama a GET api/v1/auth/admin/users y devuelve el data desenvuelto', async () => {
    const users: UserResponse[] = []
    const spy = vi.spyOn(authClient, 'get').mockResolvedValue({
      data: { data: users, message: 'OK', timestamp: '2026-01-01T00:00:00Z' },
    })

    const result = await listUsers()

    expect(spy).toHaveBeenCalledWith('api/v1/auth/admin/users')
    expect(result).toBe(users)
  })
})
