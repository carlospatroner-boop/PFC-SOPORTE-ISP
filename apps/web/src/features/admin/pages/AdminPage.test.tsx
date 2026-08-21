import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AdminPage } from './AdminPage'
import * as adminApi from '../api/adminApi'
import type { UserResponse } from '../types/user'

const existingUser: UserResponse = {
  id: 'u1',
  email: 'tecorte@test.com',
  fullName: 'Tecnico Norte',
  role: 'TECNICO',
  zone: 'QUEVEDO_NORTE',
  active: true,
  createdAt: '2026-01-01T00:00:00Z',
}

describe('AdminPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('carga y muestra la lista de cuentas registradas', async () => {
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([existingUser])
    render(<AdminPage />)

    expect(await screen.findByText('tecorte@test.com')).toBeInTheDocument()
  })

  it('muestra un error si la lista falla', async () => {
    vi.spyOn(adminApi, 'listUsers').mockRejectedValue(new Error('network'))
    render(<AdminPage />)

    expect(await screen.findByText('No se pudieron cargar las cuentas')).toBeInTheDocument()
  })

  it('el campo de zona solo aparece cuando el rol elegido es TECNICO', async () => {
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([])
    render(<AdminPage />)
    await waitFor(() => expect(adminApi.listUsers).toHaveBeenCalled())

    expect(screen.queryByText('Zona (técnico)')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Técnico' }))
    expect(screen.getByText('Zona (técnico)')).toBeInTheDocument()
  })

  it('crea una cuenta y refresca la lista', async () => {
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([])
    const createSpy = vi.spyOn(adminApi, 'createUser').mockResolvedValue({ ...existingUser, email: 'nuevo@test.com' })
    render(<AdminPage />)
    await waitFor(() => expect(adminApi.listUsers).toHaveBeenCalled())

    fireEvent.change(screen.getByLabelText('Correo'), { target: { value: 'nuevo@test.com' } })
    fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'Passw0rd!' } })
    fireEvent.change(screen.getByLabelText('Nombre completo'), { target: { value: 'Cliente Nuevo' } })

    fireEvent.click(screen.getByRole('button', { name: '+ Crear cuenta' }))

    await waitFor(() => expect(createSpy).toHaveBeenCalledWith({
      email: 'nuevo@test.com',
      password: 'Passw0rd!',
      fullName: 'Cliente Nuevo',
      role: 'CLIENTE',
      zone: null,
    }))
    expect(await screen.findByText('Cuenta creada: nuevo@test.com')).toBeInTheDocument()
  })

  it('muestra un error si faltan campos obligatorios', async () => {
    vi.spyOn(adminApi, 'listUsers').mockResolvedValue([])
    render(<AdminPage />)
    await waitFor(() => expect(adminApi.listUsers).toHaveBeenCalled())

    fireEvent.click(screen.getByRole('button', { name: '+ Crear cuenta' }))

    expect(await screen.findByText('Completa correo, contraseña y nombre')).toBeInTheDocument()
  })
})
