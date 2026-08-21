// Coincide con UserResponse.java de auth-service.
export type Role = 'CLIENTE' | 'TECNICO' | 'ADMIN'

export interface UserResponse {
  id: string
  email: string
  fullName: string
  role: Role
  zone: string | null
  active: boolean
  createdAt: string
}
