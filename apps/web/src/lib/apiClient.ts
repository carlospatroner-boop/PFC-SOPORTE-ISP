import axios from 'axios'
import { getAccessToken } from '../features/auth/session'

/**
 * Desde la Entrega 4 (Modulo B, item 6 de la guia), el navegador ya NO llama a cada
 * microservicio por su puerto: pasa por el API Gateway unico (services/api-gateway, Spring
 * Cloud Gateway), que enruta por prefijo de path a auth-service/ticket-service/etc. sin
 * reescribirlo -- por eso ambos clientes pueden compartir la misma URL base. Las rutas
 * VITE_AUTH_BASE_URL/VITE_TICKETS_BASE_URL de la E3 (llamada directa a cada servicio) siguen
 * disponibles como override individual solo para depuracion local sin levantar el gateway.
 */
const GATEWAY_BASE_URL = import.meta.env.VITE_GATEWAY_BASE_URL ?? 'http://localhost:8000/'
const AUTH_BASE_URL = import.meta.env.VITE_AUTH_BASE_URL ?? GATEWAY_BASE_URL
const TICKETS_BASE_URL = import.meta.env.VITE_TICKETS_BASE_URL ?? GATEWAY_BASE_URL

const REPORTS_BASE_URL = import.meta.env.VITE_REPORTS_BASE_URL ?? GATEWAY_BASE_URL

function attachBearerToken(client: ReturnType<typeof axios.create>) {
  client.interceptors.request.use((config) => {
    const token = getAccessToken()
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
    return config
  })
  return client
}

// authClient tambien necesita el token: /api/v1/auth/admin/** (alta/listado de usuarios,
// solo ADMIN) esta protegido, a diferencia de login/register/refresh que son publicos (ver
// SecurityConfig.PUBLIC_PATHS en auth-service) -- adjuntar el header ahi no afecta a esos
// endpoints publicos, que simplemente lo ignoran.
export const authClient = attachBearerToken(axios.create({ baseURL: AUTH_BASE_URL }))

export const ticketsClient = attachBearerToken(axios.create({ baseURL: TICKETS_BASE_URL }))

// report-service exige rol ADMIN (ver AuthGatewayFilter alla) -- el envoltorio es igual al
// resto de clientes, la autorizacion real vive en el backend.
export const reportsClient = attachBearerToken(axios.create({ baseURL: REPORTS_BASE_URL }))
