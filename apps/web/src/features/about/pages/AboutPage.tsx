import { useTranslation } from 'react-i18next'

export function AboutPage() {
  const { t } = useTranslation()
  return (
    <div style={{ padding: 24, maxWidth: 640 }}>
      <h1>{t('about.title')}</h1>
      <p>{t('about.body')}</p>
      <p style={{ color: 'var(--text-muted)' }}>{t('about.version')} · PFC-E4 · ISR-701</p>
    </div>
  )
}
