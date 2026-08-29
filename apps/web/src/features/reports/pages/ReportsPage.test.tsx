import { describe, it, expect, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { ReportsPage } from './ReportsPage'
import * as reportsApi from '../api/reportsApi'

describe('ReportsPage', () => {
  it('muestra el total y los desgloses cuando la carga tiene exito', async () => {
    vi.spyOn(reportsApi, 'getSummary').mockResolvedValue({
      totalTickets: 5,
      byStatus: { NUEVO: 3, RESUELTO: 2 },
      byZone: { QUEVEDO_NORTE: 5 },
      byCategory: { HARDWARE: 5 },
    })
    render(<ReportsPage />)

    expect(await screen.findByText('5 tickets en total')).toBeInTheDocument()
    expect(screen.getByText('Tickets por estado')).toBeInTheDocument()
    expect(screen.getByText('QUEVEDO_NORTE')).toBeInTheDocument()
  })

  it('muestra un error si la carga falla', async () => {
    vi.spyOn(reportsApi, 'getSummary').mockRejectedValue(new Error('network'))
    render(<ReportsPage />)

    await waitFor(() => expect(screen.getByText('No se pudieron cargar los reportes')).toBeInTheDocument())
  })
})
