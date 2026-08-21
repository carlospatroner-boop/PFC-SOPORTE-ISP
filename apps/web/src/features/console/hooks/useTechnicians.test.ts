import { describe, it, expect, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { useTechnicians } from './useTechnicians'
import * as adminApi from '../../admin/api/adminApi'
import type { UserResponse } from '../../admin/types/user'

const users: UserResponse[] = [
  { id: 't1', email: 'tec1@test.com', fullName: 'Tecnico Uno', role: 'TECNICO', zone: 'QUEVEDO_NORTE', active: true, createdAt: '2026-01-01T00:00:00Z' },
  { id: 'c1', email: 'cliente@test.com', fullName: 'Cliente Uno', role: 'CLIENTE', zone: null, active: true, createdAt: '2026-01-01T00:00:00Z' },
  { id: 't2', email: 'tec2@test.com', fullName: 'Tecnico Dos', role: 'TECNICO', zone: 'QUEVEDO_SUR', active: true, createdAt: '2026-01-01T00:00:00Z' },
]

describe('useTechnicians', () => {
  it('no llama a listUsers si enabled es false', () => {
    const spy = vi.spyOn(adminApi, 'listUsers')
    const { result } = renderHook(() => useTechnicians(false))
    expect(spy).not.toHaveBeenCalled()
    expect(result.current).toEqual([])
  })

  it('carga y filtra solo los usuarios con rol TECNICO cuando enabled es true', async () => {
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue(users)
    const { result } = renderHook(() => useTechnicians(true))

    await waitFor(() => expect(result.current).toHaveLength(2))
    expect(result.current.every((u) => u.role === 'TECNICO')).toBe(true)
  })

  it('si la carga falla, devuelve una lista vacia sin lanzar', async () => {
    vi.spyOn(adminApi, 'listUsers').mockRejectedValue(new Error('network'))
    const { result } = renderHook(() => useTechnicians(true))

    await waitFor(() => expect(result.current).toEqual([]))
  })
})
