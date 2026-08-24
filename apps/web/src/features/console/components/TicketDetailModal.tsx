import { useEffect, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { getTicket } from '../api/ticketsApi'
import { PriorityBadge, SlaBadge, StatusBadge, UnassignedChip } from './Badges'
import type { TicketResponse } from '../types/ticket'

/**
 * Vista de detalle completo de un ticket, abierta al hacer clic en una fila de la consola
 * (ver ConsolePage.tsx). Se pide GET /api/v1/tickets/{id} en vez de reusar el objeto de la
 * fila para que muestre siempre el estado mas reciente (relevante porque el
 * EscalationScheduler puede haber cambiado el estado del ticket entre que se cargo la lista
 * y que alguien hizo clic en la fila) y para tener un caso real de estado de carga.
 */
export function TicketDetailModal({
  ticketId,
  technicianName,
  onClose,
}: {
  ticketId: string
  technicianName: string | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [ticket, setTicket] = useState<TicketResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    getTicket(ticketId)
      .then((data) => {
        if (!cancelled) setTicket(data)
      })
      .catch(() => {
        if (!cancelled) setError(t('console.detail.error'))
      })
    return () => {
      cancelled = true
    }
  }, [ticketId, t])

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  return (
    <div style={overlayStyle} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div style={modalStyle} role="dialog" aria-modal="true" aria-label={t('console.detail.title') ?? ''}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <div>
            <h2 style={{ margin: 0 }}>{t('console.detail.title')}</h2>
            <p style={{ color: 'var(--text-muted)', margin: '4px 0 0', fontSize: '0.85rem', fontFamily: 'monospace' }}>
              {ticketId}
            </p>
          </div>
          <button type="button" onClick={onClose} aria-label={t('console.detail.close') ?? ''} style={closeButtonStyle}>
            ✕
          </button>
        </div>

        {error && <p style={{ color: 'var(--status-escalado)' }}>📡 {error}</p>}

        {!ticket && !error && (
          <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="skeleton-row" style={{ height: 18, width: `${70 - i * 8}%` }} />
            ))}
          </div>
        )}

        {ticket && (
          <>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '14px 0 18px' }}>
              <StatusBadge status={ticket.status} />
              {ticket.priority && <PriorityBadge priority={ticket.priority} />}
              <SlaBadge breached={ticket.slaBreached} />
            </div>

            <div style={sectionGridStyle}>
              <Field label={t('console.detail.zone')} value={ticket.zone} />
              <Field
                label={t('console.detail.technician')}
                value={ticket.technicianId ? (technicianName ?? ticket.technicianId) : undefined}
                fallback={<UnassignedChip label={t('console.unassigned')} />}
              />
              <Field label={t('console.detail.category')} value={ticket.category ?? undefined} fallback="—" />
              <Field label={t('console.detail.clientId')} value={ticket.clientId} mono />
            </div>

            <h3 style={sectionTitleStyle}>{t('console.detail.description')}</h3>
            <p style={{ margin: '4px 0 18px', lineHeight: 1.5 }}>{ticket.description}</p>

            <h3 style={sectionTitleStyle}>{t('console.detail.timeline')}</h3>
            <div style={sectionGridStyle}>
              <Field label={t('console.detail.createdAt')} value={formatDate(ticket.createdAt)} />
              <Field label={t('console.detail.slaDeadline')} value={formatDate(ticket.slaDeadline)} fallback="—" />
              <Field label={t('console.detail.resolvedAt')} value={formatDate(ticket.resolvedAt)} fallback="—" />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 20 }}>
              <button type="button" onClick={onClose} style={closeFooterButtonStyle}>
                {t('console.detail.close')}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

function Field({
  label,
  value,
  fallback = '—',
  mono = false,
}: {
  label: string
  value?: string
  fallback?: React.ReactNode
  mono?: boolean
}) {
  return (
    <div>
      <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginBottom: 2 }}>{label}</div>
      <div style={{ fontSize: '0.95rem', fontFamily: mono ? 'monospace' : 'inherit' }}>
        {value ? value : fallback}
      </div>
    </div>
  )
}

function formatDate(value: string | null): string | undefined {
  if (!value) return undefined
  return new Date(value).toLocaleString()
}

const overlayStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  background: 'rgba(0,0,0,0.5)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 50,
  padding: 16,
}

const modalStyle: CSSProperties = {
  background: 'var(--surface)',
  border: '1px solid var(--border)',
  borderRadius: 12,
  padding: 24,
  width: '100%',
  maxWidth: 560,
  maxHeight: '85vh',
  overflowY: 'auto',
}

const sectionGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
  gap: 14,
  marginBottom: 18,
}

const sectionTitleStyle: CSSProperties = {
  fontSize: '0.9rem',
  color: 'var(--text-muted)',
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  margin: '0 0 6px',
  borderTop: '1px solid var(--border)',
  paddingTop: 14,
}

const closeButtonStyle: CSSProperties = {
  background: 'transparent',
  border: 'none',
  fontSize: '1.1rem',
  cursor: 'pointer',
  color: 'var(--text-muted)',
  lineHeight: 1,
  padding: 4,
}

const closeFooterButtonStyle: CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: '1px solid var(--border)',
  background: 'transparent',
  color: 'var(--text)',
  cursor: 'pointer',
}
