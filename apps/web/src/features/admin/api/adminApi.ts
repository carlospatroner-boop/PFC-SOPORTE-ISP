import { authClient } from '../../../lib/apiClient'
import type { Role, UserResponse } from '../types/user'

interface ApiEnvelope<T> {
  data: T
  message: string
  timestamp: string
}

export interface CreateUserPayload {
  email: string
  password: string
  fullName: string
  role: Role
  zone?: string | null
}

// Solo ADMIN (ver AdminUserController.java, @PreAuthorize("hasRole('ADMIN')")). A
// diferencia del registro publico (siempre CLIENTE), aqui si se puede elegir el rol.
export async function createUser(payload: CreateUserPayload): Promise<UserResponse> {
  const response = await authClient.post<ApiEnvelope<UserResponse>>('api/v1/auth/admin/users', payload)
  return response.data.data
}

export async function listUsers(): Promise<UserResponse[]> {
  const response = await authClient.get<ApiEnvelope<UserResponse[]>>('api/v1/auth/admin/users')
  return response.data.data
}
