// Coincide campo a campo con TicketResponse.java del backend real
// (services/svc-principal/.../web/dto/TicketResponse.java) -- mismo contrato que consume
// la app movil (ver apps/mobile/.../data/remote/dto/ApiDtos.kt).

export type TicketStatus = 'NUEVO' | 'ASIGNADO' | 'EN_PROGRESO' | 'ESCALADO' | 'RESUELTO' | 'CERRADO'

export type Zone = 'QUEVEDO_CENTRO' | 'QUEVEDO_NORTE' | 'QUEVEDO_SUR'

export type Category = 'CONECTIVIDAD' | 'DNS' | 'HARDWARE' | 'CONFIGURACION' | 'VELOCIDAD'

export type Priority = 'CRITICO' | 'ALTO' | 'MEDIO' | 'BAJO'

export interface TicketResponse {
  zone: Zone
  ticketId: string
  clientId: string
  technicianId: string | null
  category: Category | null
  priority: Priority | null
  status: TicketStatus
  description: string
  createdAt: string
  slaDeadline: string | null
  resolvedAt: string | null
  slaBreached: boolean
}
