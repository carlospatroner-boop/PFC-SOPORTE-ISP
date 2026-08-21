import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from './LoginPage'
import { AuthProvider } from '../hooks/useAuth'

function renderLoginPage() {
  return render(
    <AuthProvider>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthProvider>,
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('muestra un error de validacion si se envia el formulario vacio', () => {
    renderLoginPage()

    fireEvent.click(screen.getByRole('button', { name: /ingresar/i }))

    expect(screen.getByText('Ingresa correo y contraseña')).toBeInTheDocument()
  })

  it('limpia el error de validacion al escribir en los campos', () => {
    renderLoginPage()
    fireEvent.click(screen.getByRole('button', { name: /ingresar/i }))
    expect(screen.getByText('Ingresa correo y contraseña')).toBeInTheDocument()

    fireEvent.change(screen.getByPlaceholderText('Correo'), { target: { value: 'a@b.com' } })
    fireEvent.change(screen.getByPlaceholderText('Contraseña'), { target: { value: 'x' } })

    // El mensaje de validacion local se limpia; el submit real fallara por red (no hay
    // backend en el entorno de pruebas), lo cual es un caso aparte y no se afirma aqui.
    expect(screen.queryByText('Ingresa correo y contraseña')).not.toBeInTheDocument()
  })
})
