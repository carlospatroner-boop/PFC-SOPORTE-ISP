import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import { AuthProvider } from '../features/auth/hooks/useAuth'
import { saveSession } from '../features/auth/session'

function renderWithRoute(initialPath: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<p>pantalla de login</p>} />
          <Route
            path="/main"
            element={
              <ProtectedRoute>
                <p>contenido protegido</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('redirige a /login si no hay sesion activa', () => {
    renderWithRoute('/main')
    expect(screen.getByText('pantalla de login')).toBeInTheDocument()
  })

  it('muestra el contenido si hay sesion activa', () => {
    saveSession('token', 'refresh', null)
    renderWithRoute('/main')
    expect(screen.getByText('contenido protegido')).toBeInTheDocument()
  })
})
