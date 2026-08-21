import { ticketsClient } from '../../../lib/apiClient'
import type { TicketResponse, TicketStatus, Zone } from '../types/ticket'

interface ApiEnvelope<T> {
  data: T
  message: string
  timestamp: string
}

// Sin filtro de query: el backend ya resuelve el alcance por rol/zona a partir del JWT
// (ver TicketController.java / TicketService.listTickets) -- ADMIN ve todo, que es el caso
// de uso de esta consola de operadores.
export async function listTickets(): Promise<TicketResponse[]> {
  const response = await ticketsClient.get<ApiEnvelope<TicketResponse[]>>('api/v1/tickets')
  return response.data.data
}

export interface CreateTicketPayload {
  zone: Zone
  title: string
  description: string
  contactPhone?: string
  address?: string
}

// Solo CLIENTE (y ADMIN de alta administrativa) puede llamar esto -- TECNICO recibe 403
// del backend (ver TicketAuthorization.assertCanCreate). El clientId nunca se manda: lo
// toma el backend del token.
export async function createTicket(payload: CreateTicketPayload): Promise<TicketResponse> {
  const response = await ticketsClient.post<ApiEnvelope<TicketResponse>>('api/v1/tickets', payload)
  return response.data.data
}

// TECNICO (solo dentro de su zona) o ADMIN -- ver TicketAuthorization.assertCanManage.
export async function updateTicketStatus(ticketId: string, status: TicketStatus): Promise<TicketResponse> {
  const response = await ticketsClient.patch<ApiEnvelope<TicketResponse>>(`api/v1/tickets/${ticketId}/status`, {
    status,
  })
  return response.data.data
}

// Autoasignacion: el TECNICO se asigna el ticket a si mismo (technicianId = su propio
// userId, extraido del JWT) -- mas simple que armar un selector de "elige un tecnico"
// cuando el unico tecnico que puede resolverlo dentro de su zona es quien esta mirando la
// pantalla. ADMIN puede asignar a cualquier id de tecnico si lo necesita.
export async function assignTechnician(ticketId: string, technicianId: string): Promise<TicketResponse> {
  const response = await ticketsClient.post<ApiEnvelope<TicketResponse>>(
    `api/v1/tickets/${ticketId}/assign`,
    null,
    { params: { technicianId } },
  )
  return response.data.data
}
