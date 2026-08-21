import { useEffect, useState } from 'react'
import { listUsers } from '../../admin/api/adminApi'
import type { UserResponse } from '../../admin/types/user'

/**
 * Lista de tecnicos para el selector "asignar a" de ADMIN (ver TicketRowActions.tsx) --
 * reutiliza GET /api/v1/auth/admin/users (el mismo que ya usa AdminPage), filtrado a
 * role === 'TECNICO'. No hace falta ningun endpoint nuevo en el backend.
 *
 * "enabled" evita la llamada para TECNICO/CLIENTE: ese endpoint exige rol ADMIN
 * (@PreAuthorize en AdminUserController), asi que para cualquier otro rol seria un 403
 * inevitable -- mejor no dispararlo.
 */
export function useTechnicians(enabled: boolean): UserResponse[] {
  const [technicians, setTechnicians] = useState<UserResponse[]>([])

  useEffect(() => {
    if (!enabled) {
      setTechnicians([])
      return
    }
    listUsers()
      .then((users) => setTechnicians(users.filter((u) => u.role === 'TECNICO')))
      .catch(() => setTechnicians([]))
  }, [enabled])

  return technicians
}
