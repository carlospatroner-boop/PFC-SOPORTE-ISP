import { useTranslation } from 'react-i18next'
import type { Priority, TicketStatus } from '../types/ticket'

const STATUS_VAR: Record<TicketStatus, string> = {
  NUEVO: 'var(--status-nuevo)',
  ASIGNADO: 'var(--status-asignado)',
  EN_PROGRESO: 'var(--status-en-progreso)',
  ESCALADO: 'var(--status-escalado)',
  RESUELTO: 'var(--status-resuelto)',
  CERRADO: 'var(--status-cerrado)',
}

export function StatusBadge({ status }: { status: TicketStatus }) {
  const { t } = useTranslation()
  return (
    <span
      style={{
        background: STATUS_VAR[status],
        color: '#fff',
        borderRadius: 999,
        padding: '2px 10px',
        fontSize: '0.8rem',
        fontWeight: 700,
        whiteSpace: 'nowrap',
      }}
    >
      {t(`status.${status}`)}
    </span>
  )
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const { t } = useTranslation()
  const color = STATUS_VAR[priority === 'CRITICO' ? 'ESCALADO' : priority === 'ALTO' ? 'ASIGNADO' : priority === 'MEDIO' ? 'EN_PROGRESO' : 'CERRADO']
  return (
    <span
      style={{
        border: `1.5px solid ${color}`,
        color,
        borderRadius: 999,
        padding: '2px 10px',
        fontSize: '0.8rem',
        fontWeight: 700,
        whiteSpace: 'nowrap',
      }}
    >
      {t(`priority.${priority}`)}
    </span>
  )
}

// Chip discreto para "sin asignar" -- antes era texto gris plano, indistinguible de una
// celda vacia a primer vistazo; ahora tiene la misma forma de pastilla que el resto de
// columnas, pero sin color de fondo (solo borde punteado) para no competir visualmente con
// los estados reales (StatusBadge) mientras sigue senalando "esto necesita atencion".
export function UnassignedChip({ label }: { label: string }) {
  return (
    <span
      style={{
        display: 'inline-block',
        border: '1px dashed var(--text-muted)',
        color: 'var(--text-muted)',
        borderRadius: 999,
        padding: '2px 10px',
        fontSize: '0.8rem',
        fontStyle: 'italic',
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </span>
  )
}

export function SlaBadge({ breached }: { breached: boolean }) {
  const { t } = useTranslation()
  if (!breached) {
    return <span style={{ color: 'var(--status-resuelto)', fontWeight: 600 }}>{t('console.slaOk')}</span>
  }
  return (
    <span style={{ color: 'var(--status-escalado)', fontWeight: 700 }}>⚠ {t('console.slaBreached')}</span>
  )
}
