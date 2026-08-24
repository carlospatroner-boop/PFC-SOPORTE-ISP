// Pruebas de contrato consumer-driven (Modulo D de la Guia de Entrega 4): apps/web como
// CONSUMIDOR de ticket-service, verificando el contrato exacto que espera (forma del JSON,
// codigos de estado) sin depender de que el backend real este corriendo. El archivo de
// contrato generado (pacts/web-ticketservice.json) es lo que despues verifica el PROVEEDOR
// (ver services/svc-principal/src/test/.../contract/TicketServiceProviderPactTest.java) --
// si el backend cambia la forma de la respuesta sin avisar, esa verificacion falla en rojo.
//
// Se prueba directo contra el ticket-service (no contra el API Gateway): el contrato real
// es entre el cliente y quien produce el JSON, el gateway solo reenvia sin transformar el
// cuerpo (ver services/api-gateway/application.yml, enrutamiento por prefijo sin reescritura).
import { PactV3, MatchersV3 } from '@pact-foundation/pact'
import axios from 'axios'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

const { like, arrayContaining, nullValue } = MatchersV3

const provider = new PactV3({
  consumer: 'soporte-web',
  provider: 'ticket-service',
  dir: path.resolve(process.cwd(), '..', '..', 'pacts'),
})

describe('Contrato Pact: soporte-web -> ticket-service', () => {
  it('GET /api/v1/tickets devuelve una lista de tickets con la forma esperada', async () => {
    // technicianId y resolvedAt son legitimamente nullable (un ticket NUEVO no tiene
    // tecnico ni fecha de resolucion todavia) y con datos reales el endpoint devuelve
    // una mezcla de ambos casos en el mismo arreglo -- ver resultados/rebalance_demo_tickets.sql,
    // que a proposito deja tickets en los dos estados. Un solo eachLike() no puede exigir
    // "string o null" para el mismo campo, asi que se usan dos variantes representativas
    // con arrayContaining (patron oficial de Pact para colecciones con formas mixtas,
    // ver https://docs.pact.io/recipes/optional): exige que exista al menos un ticket de
    // cada variante en la respuesta real, sin mentir sobre la nulabilidad del campo.
    const ticketAsignadoYResuelto = {
      zone: like('QUEVEDO_NORTE'),
      ticketId: like('c8e57689-c021-40df-86d3-01da9615f56c'),
      clientId: like('648954f1-c33c-4e88-8492-62ec39f90f0f'),
      technicianId: like('9510f44d-e785-4091-9454-6cf3e546a0cb'),
      category: like('CONECTIVIDAD'),
      priority: like('MEDIO'),
      status: like('RESUELTO'),
      description: like('Sin acceso a Internet'),
      createdAt: like('2026-08-20T12:00:00Z'),
      slaDeadline: like('2026-08-27T12:00:00Z'),
      resolvedAt: like('2026-08-21T09:00:00Z'),
      slaBreached: like(false),
    }

    const ticketNuevoSinAsignar = {
      zone: like('QUEVEDO_NORTE'),
      ticketId: like('a1e57689-c021-40df-86d3-01da9615f000'),
      clientId: like('648954f1-c33c-4e88-8492-62ec39f90f0f'),
      technicianId: nullValue(),
      category: like('CONECTIVIDAD'),
      priority: like('MEDIO'),
      status: like('NUEVO'),
      description: like('Sin acceso a Internet'),
      createdAt: like('2026-08-20T12:00:00Z'),
      slaDeadline: like('2026-08-27T12:00:00Z'),
      resolvedAt: nullValue(),
      slaBreached: like(false),
    }

    provider
      .given('el rol ADMIN tiene al menos un ticket visible')
      .uponReceiving('una peticion GET de la lista de tickets, sin filtros, con un JWT valido de ADMIN')
      .withRequest({
        method: 'GET',
        path: '/api/v1/tickets',
        headers: { Authorization: like('Bearer token-valido') },
      })
      .willRespondWith({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: {
          data: arrayContaining(ticketAsignadoYResuelto, ticketNuevoSinAsignar),
          message: like('OK'),
          timestamp: like('2026-08-24T00:00:00Z'),
        },
      })

    await provider.executeTest(async (mockServer) => {
      const response = await axios.get(`${mockServer.url}/api/v1/tickets`, {
        headers: { Authorization: 'Bearer token-valido' },
      })
      expect(response.status).toBe(200)
      expect(Array.isArray(response.data.data)).toBe(true)
      expect(response.data.data[0]).toHaveProperty('ticketId')
      expect(response.data.data[0]).toHaveProperty('status')
    })
  })

  it('GET /api/v1/tickets sin token responde 401', async () => {
    provider
      .given('no importa el estado')
      .uponReceiving('una peticion GET de la lista de tickets sin header Authorization')
      .withRequest({ method: 'GET', path: '/api/v1/tickets' })
      .willRespondWith({ status: 401 })

    await provider.executeTest(async (mockServer) => {
      await expect(axios.get(`${mockServer.url}/api/v1/tickets`)).rejects.toMatchObject({
        response: { status: 401 },
      })
    })
  })
})
