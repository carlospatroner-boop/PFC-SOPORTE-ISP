/**
 * Arma un JWT con forma valida (header.payload.firma) pero sin firmar de verdad -- solo
 * sirve para que session.ts pueda decodificar el payload en las pruebas de componentes que
 * dependen del rol (ver getClaims/getRole/getUserId). Nunca se valida la firma en el
 * cliente, asi que esto es seguro para tests.
 */
export function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.sig`
}
