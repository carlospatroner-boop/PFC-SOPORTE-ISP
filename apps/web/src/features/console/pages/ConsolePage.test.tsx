import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { ConsolePage } from './ConsolePage'
import * as ticketsApi from '../api/ticketsApi'
import * as adminApi from '../../admin/api/adminApi'
import type { TicketResponse } from '../types/ticket'
import { AuthProvider } from '../../auth/hooks/useAuth'
import { saveSession } from '../../auth/session'
import { fakeJwt } from '../../../test/fakeJwt'

const mockTickets: TicketResponse[] = [
  {
    zone: 'QUEVEDO_NORTE',
    ticketId: 'a1',
    clientId: 'c1',
    technicianId: null,
    category: null,
    priority: 'ALTO',
    status: 'NUEVO',
    description: 'Sin acceso a Internet',
    createdAt: '2026-01-01T00:00:00Z',
    slaDeadline: null,
    resolvedAt: null,
    slaBreached: true,
  },
]

function loginAs(role: 'ADMIN' | 'TECNICO' | 'CLIENTE') {
  saveSession(fakeJwt({ sub: 'u1', email: 'u@test.com', role, permissions: [] }), 'refresh', null)
}

function renderAsRole(role: 'ADMIN' | 'TECNICO' | 'CLIENTE') {
  loginAs(role)
  return render(
    <AuthProvider>
      <ConsolePage />
    </AuthProvider>,
  )
}

describe('ConsolePage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    window.sessionStorage.clear()
    // useTechnicians solo se dispara para ADMIN; se deja mockeado por defecto en todos los
    // tests para que ninguno intente un fetch real contra auth-service.
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([])
  })

  it('muestra las tarjetas KPI y la tabla con los tickets cargados', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
    renderAsRole('ADMIN')

    expect(await screen.findByText('Sin acceso a Internet')).toBeInTheDocument()
    expect(screen.getByText('Tickets totales')).toBeInTheDocument()
    expect(screen.getByText('Escalados')).toBeInTheDocument()
    expect(screen.getByText('SLA vencido')).toBeInTheDocument()
  })

  it('muestra el estado vacio cuando el filtro no encuentra nada', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
    renderAsRole('ADMIN')
    await screen.findByText('Sin acceso a Internet')

    fireEvent.change(screen.getByPlaceholderText('Buscar por descripción o ID…'), {
      target: { value: 'texto que no existe en ningun ticket' },
    })

    expect(await screen.findByText('No hay tickets con estos filtros')).toBeInTheDocument()
  })

  it('muestra un error si la carga falla', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockRejectedValue(new Error('network'))
    renderAsRole('ADMIN')

    await waitFor(() => expect(screen.getByText(/No se pudieron cargar los tickets/)).toBeInTheDocument())
  })

  it('ADMIN ve el nombre del tecnico asignado, no el id crudo', async () => {
    const assignedTicket: TicketResponse = { ...mockTickets[0], ticketId: 'a2', technicianId: 'tec-1' }
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue([assignedTicket])
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([
      { id: 'tec-1', email: 'tec@test.com', fullName: 'Tecnico Norte', role: 'TECNICO', zone: 'QUEVEDO_NORTE', active: true, createdAt: '2026-01-01T00:00:00Z' },
    ])
    renderAsRole('ADMIN')

    expect(await screen.findByText('Tecnico Norte')).toBeInTheDocument()
    expect(screen.queryByText('tec-1')).not.toBeInTheDocument()
  })

  it('ADMIN ve el filtro de zona y la columna de acciones, no el boton de nueva solicitud', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
    renderAsRole('ADMIN')
    await screen.findByText('Sin acceso a Internet')

    expect(screen.getByText(/^Zona: Todas$/)).toBeInTheDocument()
    expect(screen.getByText('Acciones')).toBeInTheDocument()
    expect(screen.queryByText('+ Nueva solicitud')).not.toBeInTheDocument()
  })

  it('TECNICO ve la columna de acciones pero no el filtro de zona ni el boton de nueva solicitud', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
    renderAsRole('TECNICO')
    await screen.findByText('Sin acceso a Internet')

    expect(screen.queryByText(/^Zona: Todas$/)).not.toBeInTheDocument()
    expect(screen.getByText('Acciones')).toBeInTheDocument()
    expect(screen.queryByText('+ Nueva solicitud')).not.toBeInTheDocument()
  })

  it('CLIENTE ve el boton de nueva solicitud, no el filtro de zona ni la columna de acciones', async () => {
    vi.spyOn(ticketsApi, 'listTickets').mockResolvedValue(mockTickets)
    renderAsRole('CLIENTE')
    await screen.findByText('Sin acceso a Internet')

    expect(screen.getByText('+ Nueva solicitud')).toBeInTheDocument()
    expect(screen.queryByText(/^Zona: Todas$/)).not.toBeInTheDocument()
    expect(screen.queryByText('Acciones')).not.toBeInTheDocument()
  })
})
