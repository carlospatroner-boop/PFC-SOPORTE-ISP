import { describe, it, expect, vi } from 'vitest'
import { ticketsClient } from '../../../lib/apiClient'
import { assignTechnician, createTicket, listTickets, updateTicketStatus } from './ticketsApi'
import type { TicketResponse } from '../types/ticket'

describe('ticketsApi.listTickets', () => {
  it('llama a GET api/v1/tickets y devuelve el data desenvuelto', async () => {
    const tickets: TicketResponse[] = []
    const spy = vi.spyOn(ticketsClient, 'get').mockResolvedValue({
      data: { data: tickets, message: 'OK', timestamp: '2026-01-01T00:00:00Z' },
    })

    const result = await listTickets()

    expect(spy).toHaveBeenCalledWith('api/v1/tickets')
    expect(result).toBe(tickets)
  })
})

describe('ticketsApi.createTicket', () => {
  it('llama a POST api/v1/tickets con el payload y devuelve el ticket creado', async () => {
    const ticket = { ticketId: 't1' } as TicketResponse
    const spy = vi.spyOn(ticketsClient, 'post').mockResolvedValue({
      data: { data: ticket, message: 'Ticket creado exitosamente', timestamp: '2026-01-01T00:00:00Z' },
    })
    const payload = { zone: 'QUEVEDO_NORTE' as const, title: 'Sin internet', description: 'Router apagado' }

    const result = await createTicket(payload)

    expect(spy).toHaveBeenCalledWith('api/v1/tickets', payload)
    expect(result).toBe(ticket)
  })
})

describe('ticketsApi.updateTicketStatus', () => {
  it('llama a PATCH api/v1/tickets/:id/status con el nuevo estado', async () => {
    const ticket = { ticketId: 't1', status: 'EN_PROGRESO' } as TicketResponse
    const spy = vi.spyOn(ticketsClient, 'patch').mockResolvedValue({
      data: { data: ticket, message: 'Estado actualizado', timestamp: '2026-01-01T00:00:00Z' },
    })

    const result = await updateTicketStatus('t1', 'EN_PROGRESO')

    expect(spy).toHaveBeenCalledWith('api/v1/tickets/t1/status', { status: 'EN_PROGRESO' })
    expect(result).toBe(ticket)
  })
})

describe('ticketsApi.assignTechnician', () => {
  it('llama a POST api/v1/tickets/:id/assign con technicianId como query param', async () => {
    const ticket = { ticketId: 't1', technicianId: 'tec-1' } as TicketResponse
    const spy = vi.spyOn(ticketsClient, 'post').mockResolvedValue({
      data: { data: ticket, message: 'Tecnico asignado', timestamp: '2026-01-01T00:00:00Z' },
    })

    const result = await assignTechnician('t1', 'tec-1')

    expect(spy).toHaveBeenCalledWith('api/v1/tickets/t1/assign', null, { params: { technicianId: 'tec-1' } })
    expect(result).toBe(ticket)
  })
})
