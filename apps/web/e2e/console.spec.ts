import { test, expect } from '@playwright/test'

// E2E real (Modulo D, Guia de Entrega 4): navegador real -> apps/web (nginx, puerto 5173)
// -> API Gateway -> auth-service/ticket-service reales. Usa la cuenta ADMIN de demo que
// crea db-init (ver docker-compose.yml, servicio db-init) -- requiere "docker compose up -d".
const ADMIN_EMAIL = 'admin@soporte.local'
const ADMIN_PASSWORD = 'Admin123!'

test.describe('Flujo de consola (login real -> lista de tickets -> detalle)', () => {
  test('un ADMIN inicia sesion y ve la consola con tickets reales', async ({ page }) => {
    await page.goto('/login')

    await page.locator('input[type="email"]').fill(ADMIN_EMAIL)
    await page.locator('input[type="password"]').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Ingresar' }).click()

    // Login real contra auth-service: redirige a /main solo si el backend acepto el token.
    await expect(page).toHaveURL(/\/main$/)
    await expect(page.getByRole('heading', { name: 'Consola de operadores' })).toBeVisible()

    // Datos reales (ver resultados/rebalance_demo_tickets.sql, 504 tickets de demo), no un mock.
    const rows = page.locator('.data-table tbody tr[data-clickable="true"]')
    await expect(rows.first()).toBeVisible({ timeout: 30000 })
    expect(await rows.count()).toBeGreaterThan(0)
  })

  test('abrir un ticket muestra su detalle completo y se puede volver a encontrar por ID', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input[type="email"]').fill(ADMIN_EMAIL)
    await page.locator('input[type="password"]').fill(ADMIN_PASSWORD)
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page).toHaveURL(/\/main$/)

    const rows = page.locator('.data-table tbody tr[data-clickable="true"]')
    await expect(rows.first()).toBeVisible({ timeout: 30000 })
    await rows.first().click()

    const dialog = page.getByRole('dialog', { name: 'Detalle del ticket' })
    await expect(dialog).toBeVisible()
    const ticketId = (await dialog.locator('p').first().textContent())?.trim()
    expect(ticketId).toBeTruthy()

    await page.keyboard.press('Escape')
    await expect(dialog).not.toBeVisible()

    // El buscador de la consola tambien filtra por ID (no solo por descripcion, ver
    // useTickets.ts) -- se agrego justamente para poder ubicar un ticket especifico
    // despues de operarlo desde otro cliente (ver la app movil, TicketDetailScreen.kt).
    await page.getByPlaceholder('Buscar por descripción o ID…').fill(ticketId!)
    await expect(rows).toHaveCount(1)
    await rows.first().click()
    await expect(dialog).toBeVisible()
    await expect(dialog.locator('p').first()).toHaveText(ticketId!)
  })

  test('credenciales invalidas muestran un error real del backend', async ({ page }) => {
    await page.goto('/login')
    await page.locator('input[type="email"]').fill('no-existe@soporte.local')
    await page.locator('input[type="password"]').fill('claveIncorrecta123')
    await page.getByRole('button', { name: 'Ingresar' }).click()

    // No hay interceptor que reescriba el mensaje de axios (ver src/lib/apiClient.ts), asi
    // que el texto exacto es el de AxiosError ("Request failed with status code 401") -- lo
    // que importa para el contrato de este test es que el backend rechazo el login de
    // verdad y la UI se quedo en /login mostrando algun error, no el texto literal.
    await expect(page).toHaveURL(/\/login$/)
    await expect(page.locator('form p').last()).toBeVisible({ timeout: 10000 })
    await expect(page.locator('form p').last()).not.toBeEmpty()
  })
})
