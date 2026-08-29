import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { CreateTicketModal } from './CreateTicketModal'
import * as ticketsApi from '../api/ticketsApi'
import type { TicketResponse } from '../types/ticket'

describe('CreateTicketModal', () => {
  it('llama a onClose al hacer click en cancelar', () => {
    const onClose = vi.fn()
    render(<CreateTicketModal onClose={onClose} onCreated={vi.fn()} />)

    fireEvent.click(screen.getByText('Cancelar'))
    expect(onClose).toHaveBeenCalled()
  })

  it('muestra un error si falta el titulo o la descripcion', async () => {
    render(<CreateTicketModal onClose={vi.fn()} onCreated={vi.fn()} />)

    fireEvent.click(screen.getByText('Crear solicitud'))

    expect(await screen.findByText('Completa el título y la descripción')).toBeInTheDocument()
  })

  it('crea el ticket y llama a onCreated', async () => {
    const created = { ticketId: 't1' } as TicketResponse
    const spy = vi.spyOn(ticketsApi, 'createTicket').mockResolvedValue(created)
    const onCreated = vi.fn()
    render(<CreateTicketModal onClose={vi.fn()} onCreated={onCreated} />)

    fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'Sin internet' } })
    fireEvent.change(screen.getByLabelText('Descripción del problema'), { target: { value: 'Router apagado' } })
    fireEvent.click(screen.getByText('Crear solicitud'))

    await waitFor(() =>
      expect(spy).toHaveBeenCalledWith({
        zone: 'QUEVEDO_CENTRO',
        title: 'Sin internet',
        description: 'Router apagado',
        contactPhone: undefined,
        address: undefined,
      }),
    )
    expect(onCreated).toHaveBeenCalled()
  })

  it('muestra un error generico si la creacion falla', async () => {
    vi.spyOn(ticketsApi, 'createTicket').mockRejectedValue(new Error('network'))
    render(<CreateTicketModal onClose={vi.fn()} onCreated={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Título'), { target: { value: 'Sin internet' } })
    fireEvent.change(screen.getByLabelText('Descripción del problema'), { target: { value: 'Router apagado' } })
    fireEvent.click(screen.getByText('Crear solicitud'))

    expect(await screen.findByText('No se pudo crear la solicitud')).toBeInTheDocument()
  })
})
