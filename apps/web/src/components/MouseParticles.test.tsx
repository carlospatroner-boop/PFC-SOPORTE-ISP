import { describe, it, expect } from 'vitest'
import { render, cleanup } from '@testing-library/react'
import { MouseParticles } from './MouseParticles'

describe('MouseParticles', () => {
  it('se monta y desmonta sin lanzar, y no intercepta clics (pointer-events: none)', () => {
    const { container, unmount } = render(<MouseParticles />)

    const canvas = container.querySelector('canvas')
    expect(canvas).toBeInTheDocument()
    expect(canvas).toHaveStyle({ pointerEvents: 'none' })

    // jsdom no implementa un contexto 2d real -- getContext('2d') devuelve null, y el
    // componente debe tolerarlo (early return) en vez de lanzar.
    expect(() => window.dispatchEvent(new MouseEvent('mousemove', { clientX: 10, clientY: 10 }))).not.toThrow()
    expect(() => unmount()).not.toThrow()
    cleanup()
  })
})
