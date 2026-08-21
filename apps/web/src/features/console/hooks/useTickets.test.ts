import { describe, it, expect, vi, beforeEach } from 'vitest'
import { act, renderHook, waitFor } from '@testing-library/react'
import { useTickets } from './useTickets'
import * as ticketsApi from '../api/ticketsApi'
import type { TicketResponse } from '../types/ticket'

const mockTickets: TicketResponse[] = [
  {
    zone: 'QUEVEDO_NORTE',
    ticketId: '1',
    clientId: 'c1',
    technicianId: null,
    category: null,
    priority: 'ALTO',
    status: 'NUEVO',
    description: 'Sin acceso a Internet',
    createdAt: '2026-01-01T00:00:00Z',
    slaDeadline: null,
    slaBreached: false,
  },
  {
    zone: 'QUEVEDO_SUR',
    ticketId: '2',
    clientId: 'c2',
    technicianId: null,
    category: null,
    priority: 'BAJO',
    status: 'RESUELTO',
    description: 'Router lento',
    createdAt: '2026-01-02T00:00:00Z',
    slaDeadline: null,
    slaBreached: true,
  },
]

describe('useTickets', () => {
  beforeEach(() => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
  })

  it('carga los tickets y calcula los conteos totales', async () => {
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.allCount).toBe(2)
    expect(result.current.breachedCount).toBe(1)
    expect(result.current.escaladoCount).toBe(0)
    expect(result.current.tickets).toHaveLength(2)
  })

  it('filtra por texto de busqueda en la descripcion', async () => {
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))

    act(() => result.current.setSearch('router'))

    expect(result.current.tickets).toHaveLength(1)
    expect(result.current.tickets[0].ticketId).toBe('2')
    // el conteo del resumen sigue siendo sobre TODOS los tickets, no solo los filtrados
    expect(result.current.allCount).toBe(2)
  })

  it('filtra por zona', async () => {
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))

    act(() => result.current.setZoneFilter('QUEVEDO_SUR'))

    expect(result.current.tickets).toHaveLength(1)
    expect(result.current.tickets[0].zone).toBe('QUEVEDO_SUR')
  })

  it('filtra por estado', async () => {
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))

    act(() => result.current.setStatusFilter('RESUELTO'))

    expect(result.current.tickets).toHaveLength(1)
    expect(result.current.tickets[0].status).toBe('RESUELTO')
  })

  it('reporta error si la carga falla, sin romper el hook', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockRejectedValueOnce(new Error('network'))
    const { result } = renderHook(() => useTickets())
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).not.toBeNull()
  })
})
