import { useCallback, useEffect, useMemo, useState } from 'react'
import { listTickets } from '../api/ticketsApi'
import type { TicketResponse, TicketStatus, Zone } from '../types/ticket'

interface UseTicketsResult {
  tickets: TicketResponse[]
  allCount: number
  breachedCount: number
  escaladoCount: number
  loading: boolean
  error: string | null
  search: string
  setSearch: (value: string) => void
  zoneFilter: Zone | null
  setZoneFilter: (value: Zone | null) => void
  statusFilter: TicketStatus | null
  setStatusFilter: (value: TicketStatus | null) => void
  refresh: () => void
}

export function useTickets(): UseTicketsResult {
  const [allTickets, setAllTickets] = useState<TicketResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [zoneFilter, setZoneFilter] = useState<Zone | null>(null)
  const [statusFilter, setStatusFilter] = useState<TicketStatus | null>(null)

  const refresh = useCallback(() => {
    setLoading(true)
    setError(null)
    listTickets()
      .then(setAllTickets)
      .catch(() => setError('error'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  const tickets = useMemo(() => {
    return allTickets.filter((ticket) => {
      // Busca tambien por ticketId (no solo descripcion) -- util para verificar en la
      // consola web que una accion hecha desde otro cliente (ej. la app movil) se propago
      // al mismo backend real: se copia el id mostrado alla y se pega aqui.
      const query = search.trim().toLowerCase()
      const matchesSearch =
        query.length === 0 ||
        ticket.description.toLowerCase().includes(query) ||
        ticket.ticketId.toLowerCase().includes(query)
      const matchesZone = zoneFilter === null || ticket.zone === zoneFilter
      const matchesStatus = statusFilter === null || ticket.status === statusFilter
      return matchesSearch && matchesZone && matchesStatus
    })
  }, [allTickets, search, zoneFilter, statusFilter])

  return {
    tickets,
    allCount: allTickets.length,
    breachedCount: allTickets.filter((t) => t.slaBreached).length,
    escaladoCount: allTickets.filter((t) => t.status === 'ESCALADO').length,
    loading,
    error,
    search,
    setSearch,
    zoneFilter,
    setZoneFilter,
    statusFilter,
    setStatusFilter,
    refresh,
  }
}
