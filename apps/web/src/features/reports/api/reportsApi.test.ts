import { describe, it, expect, vi } from 'vitest'
import { reportsClient } from '../../../lib/apiClient'
import { getSummary } from './reportsApi'

describe('reportsApi.getSummary', () => {
  it('llama a GET api/v1/reports/summary y devuelve el data desenvuelto', async () => {
    const summary = { totalTickets: 3, byStatus: { NUEVO: 3 }, byZone: {}, byCategory: {} }
    const spy = vi.spyOn(reportsClient, 'get').mockResolvedValue({
      data: { data: summary, message: 'OK', timestamp: '2026-01-01T00:00:00Z' },
    })

    const result = await getSummary()

    expect(spy).toHaveBeenCalledWith('api/v1/reports/summary')
    expect(result).toBe(summary)
  })
})
