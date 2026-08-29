import '@testing-library/jest-dom/vitest'
import '../i18n'

// jsdom no implementa matchMedia -- lo necesita ThemeContext para detectar el tema del
// sistema operativo. Se mockea con un valor fijo (sin preferencia oscura) solo para pruebas.
if (!window.matchMedia) {
  window.matchMedia = (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  })
}
