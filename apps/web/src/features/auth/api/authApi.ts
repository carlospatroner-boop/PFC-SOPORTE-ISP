import { authClient } from '../../../lib/apiClient'

// Coincide con AuthResponse.java / ApiResponse.java del backend real (auth-service).
interface AuthResponseDto {
  accessToken: string
  refreshToken: string
  accessTokenExpiresAt: string | null
}

interface ApiEnvelope<T> {
  data: T
  message: string
  timestamp: string
}

export async function login(email: string, password: string): Promise<AuthResponseDto> {
  const response = await authClient.post<ApiEnvelope<AuthResponseDto>>('api/v1/auth/login', {
    email,
    password,
  })
  return response.data.data
}
