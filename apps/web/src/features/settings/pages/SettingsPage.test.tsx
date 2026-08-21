import { describe, it, expect, afterEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import i18n from '../../../i18n'
import { ThemeProvider } from '../../../theme/ThemeContext'
import { SettingsPage } from './SettingsPage'

function renderSettings() {
  return render(
    <ThemeProvider>
      <SettingsPage />
    </ThemeProvider>,
  )
}

describe('SettingsPage', () => {
  afterEach(async () => {
    await i18n.changeLanguage('es')
  })

  it('cambia el idioma al hacer clic en English', async () => {
    renderSettings()
    fireEvent.click(screen.getByRole('button', { name: 'English' }))
    expect(i18n.language).toBe('en')
    expect(await screen.findByText('Settings')).toBeInTheDocument()
  })

  it('cambia el tema al hacer clic en Oscuro', () => {
    renderSettings()
    fireEvent.click(screen.getByRole('button', { name: /Oscuro/ }))
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })
})
