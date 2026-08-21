import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { PriorityBadge, SlaBadge, StatusBadge, UnassignedChip } from './Badges'

describe('Badges', () => {
  it('StatusBadge muestra la etiqueta traducida del estado', () => {
    render(<StatusBadge status="ESCALADO" />)
    expect(screen.getByText('Escalado')).toBeInTheDocument()
  })

  it('PriorityBadge muestra la etiqueta traducida de la prioridad', () => {
    render(<PriorityBadge priority="CRITICO" />)
    expect(screen.getByText('Crítico')).toBeInTheDocument()
  })

  it('SlaBadge muestra OK cuando no esta vencido', () => {
    render(<SlaBadge breached={false} />)
    expect(screen.getByText('OK')).toBeInTheDocument()
  })

  it('SlaBadge muestra el aviso cuando esta vencido', () => {
    render(<SlaBadge breached={true} />)
    expect(screen.getByText(/Vencido/)).toBeInTheDocument()
  })

  it('UnassignedChip muestra la etiqueta recibida', () => {
    render(<UnassignedChip label="Sin asignar" />)
    expect(screen.getByText('Sin asignar')).toBeInTheDocument()
  })
})
