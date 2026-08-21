import { reportsClient } from '../../../lib/apiClient'

interface ApiEnvelope<T> {
  data: T
  message: string
  timestamp: string
}

// Coincide con SummaryResponse.java (report-service, lado de lectura del CQRS: lee de
// ticket_summary/report_db, nunca de ticket_db). Solo ADMIN -- AuthGatewayFilter en
// report-service rechaza cualquier otro rol antes de llegar al controller.
export interface ReportSummary {
  totalTickets: number
  byStatus: Record<string, number>
  byZone: Record<string, number>
  byCategory: Record<string, number>
}

export async function getSummary(): Promise<ReportSummary> {
  const response = await reportsClient.get<ApiEnvelope<ReportSummary>>('api/v1/reports/summary')
  return response.data.data
}
