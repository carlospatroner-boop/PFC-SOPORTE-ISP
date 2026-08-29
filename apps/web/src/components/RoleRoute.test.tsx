import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { RoleRoute } from './RoleRoute'
import { AuthProvider } from '../features/auth/hooks/useAuth'
import { saveSession } from '../features/auth/session'
import { fakeJwt } from '../test/fakeJwt'

function renderWithRoute(initialPath: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<p>pantalla de login</p>} />
          <Route path="/main" element={<p>consola</p>} />
          <Route
            path="/admin"
            element={
              <RoleRoute allow={['ADMIN']}>
                <p>panel de administracion</p>
              </RoleRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

function loginAs(role: 'ADMIN' | 'TECNICO' | 'CLIENTE') {
  saveSession(fakeJwt({ sub: 'u1', email: 'u@test.com', role, permissions: [] }), 'refresh', null)
}

describe('RoleRoute', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('redirige a /login si no hay sesion activa', () => {
    renderWithRoute('/admin')
    expect(screen.getByText('pantalla de login')).toBeInTheDocument()
  })

  it('redirige a /main si el rol no esta permitido', () => {
    loginAs('CLIENTE')
    renderWithRoute('/admin')
    expect(screen.getByText('consola')).toBeInTheDocument()
  })

  it('muestra el contenido si el rol esta permitido', () => {
    loginAs('ADMIN')
    renderWithRoute('/admin')
    expect(screen.getByText('panel de administracion')).toBeInTheDocument()
  })
})
