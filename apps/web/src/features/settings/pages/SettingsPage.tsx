import type { CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { useTheme } from '../../../theme/ThemeContext'

export function SettingsPage() {
  const { t, i18n } = useTranslation()
  const { mode, toggle } = useTheme()

  return (
    <div style={{ padding: 24, maxWidth: 480 }}>
      <h1>{t('settings.title')}</h1>

      <section style={{ marginTop: 24 }}>
        <h2 style={{ fontSize: '1rem' }}>{t('settings.language')}</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button
            type="button"
            onClick={() => void i18n.changeLanguage('es')}
            style={pillStyle(i18n.language === 'es')}
          >
            Español
          </button>
          <button
            type="button"
            onClick={() => void i18n.changeLanguage('en')}
            style={pillStyle(i18n.language === 'en')}
          >
            English
          </button>
        </div>
      </section>

      <section style={{ marginTop: 24 }}>
        <h2 style={{ fontSize: '1rem' }}>{t('settings.theme')}</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="button" onClick={() => mode !== 'light' && toggle()} style={pillStyle(mode === 'light')}>
            ☀ {t('settings.themeLight')}
          </button>
          <button type="button" onClick={() => mode !== 'dark' && toggle()} style={pillStyle(mode === 'dark')}>
            🌙 {t('settings.themeDark')}
          </button>
        </div>
      </section>
    </div>
  )
}

function pillStyle(active: boolean): CSSProperties {
  return {
    padding: '8px 16px',
    borderRadius: 999,
    border: active ? '2px solid var(--color-navy)' : '1px solid var(--border)',
    background: active ? 'var(--color-navy)' : 'var(--surface)',
    color: active ? '#fff' : 'var(--text)',
    fontWeight: 600,
    cursor: 'pointer',
  }
}
