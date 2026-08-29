import { defineConfig, devices } from '@playwright/test'

// E2E real de extremo a extremo (Modulo D, Guia de Entrega 4): a diferencia de los
// tests de Vitest (que mockean fetch/axios, ver src/**/*.test.tsx), estas pruebas abren
// un navegador real contra la app YA CONSTRUIDA y servida por nginx (ver docker-compose.yml,
// servicio "web" en el puerto 5173) que a su vez habla con el API Gateway real -- prueban
// la integracion completa navegador -> web -> gateway -> ticket-service/auth-service.
//
// Requiere el stack levantado (docker compose up -d) antes de correr "npx playwright test".
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  // Serializado a proposito: los 3 proyectos (chromium/firefox/webkit, ver abajo) compiten
  // por CPU con los 17 contenedores del stack en la misma maquina de desarrollo -- con
  // workers>1 se vieron timeouts intermitentes de carga de la tabla en Firefox/WebKit que
  // desaparecen al no correr motores de navegador en paralelo entre si.
  workers: 1,
  retries: 0,
  reporter: [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  // 3 navegadores (Modulo G / Tabla 2 de la rubrica: "Compatibilidad" exige que la web
  // funcione en Chrome, Firefox y Safari -- Playwright usa WebKit como equivalente real de
  // Safari, es el mismo motor de renderizado).
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
})
