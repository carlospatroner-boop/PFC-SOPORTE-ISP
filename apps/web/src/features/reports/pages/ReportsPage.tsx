import { useEffect, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { getSummary, type ReportSummary } from '../api/reportsApi'

const STATUS_VAR: Record<string, string> = {
  NUEVO: 'var(--status-nuevo)',
  ASIGNADO: 'var(--status-asignado)',
  EN_PROGRESO: 'var(--status-en-progreso)',
  ESCALADO: 'var(--status-escalado)',
  RESUELTO: 'var(--status-resuelto)',
  CERRADO: 'var(--status-cerrado)',
}

/**
 * Lado de lectura del CQRS (report-service) expuesto en la SPA -- en la version anterior
 * (frontend/index.html, vista "Reportes") era la unica forma de ver los agregados sin
 * pegarle directo a la API con curl/Postman. Solo visible/accesible para ADMIN (ver
 * RoleRoute en App.tsx); el backend lo hace cumplir igual aunque alguien fuerce la URL.
 */
export function ReportsPage() {
  const { t } = useTranslation()
  const [summary, setSummary] = useState<ReportSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getSummary()
      .then(setSummary)
      .catch(() => setError(t('reports.error')))
      .finally(() => setLoading(false))
  }, [t])

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <h1 style={{ marginBottom: 4 }}>{t('reports.title')}</h1>
      <p style={{ color: 'var(--text-muted)', marginTop: 0 }}>{t('reports.subtitle')}</p>

      {loading && <p>{t('reports.loading')}</p>}
      {error && <p style={{ color: 'var(--status-escalado)' }}>{error}</p>}

      {summary && (
        <>
          <p style={{ fontWeight: 700, fontSize: '1.4rem' }}>
            {t('reports.total', { count: summary.totalTickets })}
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 20 }}>
            <BreakdownCard title={t('reports.byStatus')} data={summary.byStatus} colorFor={(k) => STATUS_VAR[k]} />
            <BreakdownCard title={t('reports.byZone')} data={summary.byZone} colorFor={() => 'var(--color-navy)'} />
            <BreakdownCard title={t('reports.byCategory')} data={summary.byCategory} colorFor={() => 'var(--color-teal)'} />
          </div>
        </>
      )}
    </div>
  )
}

function BreakdownCard({
  title,
  data,
  colorFor,
}: {
  title: string
  data: Record<string, number>
  colorFor: (key: string) => string | undefined
}) {
  const entries = Object.entries(data)
  const max = Math.max(1, ...entries.map(([, v]) => v))
  return (
    <div style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      {entries.length === 0 && <p style={{ color: 'var(--text-muted)' }}>—</p>}
      {entries.map(([key, value]) => (
        <div key={key} style={{ marginBottom: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: 2 }}>
            <span>{key}</span>
            <span style={{ fontWeight: 700 }}>{value}</span>
          </div>
          <div style={{ background: 'var(--bg)', borderRadius: 6, height: 8, overflow: 'hidden' }}>
            <div
              style={{
                width: `${(value / max) * 100}%`,
                background: colorFor(key) ?? 'var(--color-navy)',
                height: '100%',
              }}
            />
          </div>
        </div>
      ))}
    </div>
  )
}

const cardStyle: CSSProperties = {
  background: 'var(--surface)',
  border: '1px solid var(--border)',
  borderRadius: 12,
  padding: 20,
}
