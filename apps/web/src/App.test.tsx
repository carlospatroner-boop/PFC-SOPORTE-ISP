import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './features/auth/hooks/useAuth'
import { saveSession } from './features/auth/session'

function renderApp(initialPath: string) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialPath]}>
        <App />
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('App routing', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('sin sesion, "/" redirige a login', () => {
    renderApp('/')
    expect(screen.getByRole('button', { name: /ingresar/i })).toBeInTheDocument()
  })

  it('con sesion activa, "/" redirige a la consola', async () => {
    saveSession('token', 'refresh', null)
    renderApp('/')
    expect(await screen.findByText('Consola de operadores')).toBeInTheDocument()
  })

  it('una ruta desconocida no rompe la app', () => {
    renderApp('/ruta-que-no-existe')
    // termina en login (sin sesion) o consola (con sesion); aqui sin sesion.
    expect(screen.getByRole('button', { name: /ingresar/i })).toBeInTheDocument()
  })
})
