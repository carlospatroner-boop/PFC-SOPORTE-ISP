import { describe, it, expect, beforeEach } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { ThemeProvider, useTheme } from './ThemeContext'

describe('ThemeContext', () => {
  beforeEach(() => {
    window.localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
  })

  it('toggle alterna entre claro y oscuro y lo refleja en <html data-theme>', () => {
    const { result } = renderHook(() => useTheme(), { wrapper: ThemeProvider })

    const initial = result.current.mode
    act(() => result.current.toggle())

    expect(result.current.mode).not.toBe(initial)
    expect(document.documentElement.getAttribute('data-theme')).toBe(result.current.mode)
  })

  it('useTheme fuera de ThemeProvider lanza un error claro', () => {
    // Se prueba el hook aislado (sin wrapper) para confirmar la validacion explicita.
    expect(() => renderHook(() => useTheme())).toThrowError(/useTheme debe usarse dentro/)
  })
})
