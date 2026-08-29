import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { AuthProvider } from '../features/auth/hooks/useAuth'
import { saveSession, isLoggedIn } from '../features/auth/session'
import { fakeJwt } from '../test/fakeJwt'

describe('AppLayout', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('muestra los enlaces de navegacion y el contenido', () => {
    render(
      <AuthProvider>
        <MemoryRouter>
          <AppLayout>
            <p>contenido de la pagina</p>
          </AppLayout>
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(screen.getByText('Consola')).toBeInTheDocument()
    expect(screen.getByText('Ajustes')).toBeInTheDocument()
    expect(screen.getByText('Acerca de')).toBeInTheDocument()
    expect(screen.getByText('contenido de la pagina')).toBeInTheDocument()
    expect(screen.queryByText('Administración')).not.toBeInTheDocument()
    expect(screen.queryByText('Reportes')).not.toBeInTheDocument()
  })

  it('los enlaces Administracion y Reportes solo aparecen para el rol ADMIN', () => {
    saveSession(fakeJwt({ sub: 'u1', email: 'a@test.com', role: 'ADMIN', permissions: [] }), 'refresh', null)
    render(
      <AuthProvider>
        <MemoryRouter>
          <AppLayout>
            <p>contenido</p>
          </AppLayout>
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(screen.getByText('Administración')).toBeInTheDocument()
    expect(screen.getByText('Reportes')).toBeInTheDocument()
    expect(screen.getByText('Administrador')).toBeInTheDocument()
  })

  it('muestra una insignia con el rol de la cuenta activa', () => {
    saveSession(fakeJwt({ sub: 'u1', email: 'tec@test.com', role: 'TECNICO', permissions: [] }), 'refresh', null)
    render(
      <AuthProvider>
        <MemoryRouter>
          <AppLayout>
            <p>contenido</p>
          </AppLayout>
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(screen.getByText('Técnico')).toBeInTheDocument()
  })

  it('el boton Salir cierra la sesion', () => {
    saveSession('token', 'refresh', null)
    render(
      <AuthProvider>
        <MemoryRouter>
          <AppLayout>
            <p>contenido</p>
          </AppLayout>
        </MemoryRouter>
      </AuthProvider>,
    )
    fireEvent.click(screen.getByText('Salir'))
    expect(isLoggedIn()).toBe(false)
  })
})
