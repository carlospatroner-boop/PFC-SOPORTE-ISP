import { configDefaults, defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/test/setup.ts'],
      // e2e/**: son pruebas de Playwright (navegador real, ver playwright.config.ts), no de
      // Vitest -- ambos usan el patron *.spec.ts por defecto y Playwright registra su propio
      // test.describe() global, que choca con el runner de Vitest si este intenta cargarlo.
      exclude: [...configDefaults.exclude, 'e2e/**'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/**/*.{ts,tsx}'],
        exclude: [
          'src/main.tsx',
          'src/vite-env.d.ts',
          'src/**/*.d.ts',
          'src/assets/**',
          'src/**/*.test.{ts,tsx}',
          'src/test/**',
          'src/features/console/types/**', // solo tipos, sin codigo ejecutable
        ],
      },
    },
  }),
)
