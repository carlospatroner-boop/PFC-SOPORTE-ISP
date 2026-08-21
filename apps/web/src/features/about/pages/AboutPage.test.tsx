import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AboutPage } from './AboutPage'

describe('AboutPage', () => {
  it('muestra el titulo y la descripcion del proyecto', () => {
    render(<AboutPage />)
    expect(screen.getByText('Acerca de')).toBeInTheDocument()
    expect(screen.getByText(/equipo ACC/)).toBeInTheDocument()
  })
})
