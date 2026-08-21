import { useMemo, useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../auth/hooks/useAuth'
import { getUserId } from '../../auth/session'
import { useTickets } from '../hooks/useTickets'
import { useTechnicians } from '../hooks/useTechnicians'
import { PriorityBadge, SlaBadge, StatusBadge, UnassignedChip } from '../components/Badges'
import { CreateTicketModal } from '../components/CreateTicketModal'
import { TicketRowActions } from '../components/TicketRowActions'
import type { TicketStatus, Zone } from '../types/ticket'

const ZONES: Zone[] = ['QUEVEDO_CENTRO', 'QUEVEDO_NORTE', 'QUEVEDO_SUR']
const STATUSES: TicketStatus[] = ['NUEVO', 'ASIGNADO', 'EN_PROGRESO', 'ESCALADO', 'RESUELTO', 'CERRADO']
const SKELETON_ROWS = 6

// El backend ya limita QUE tickets llegan por rol (ADMIN todos, TECNICO su zona, CLIENTE
// los propios -- ver TicketQueryService.listTickets); esto solo decide que CONTROLES se
// muestran para cada rol, que es la diferencia que antes no se notaba entre entrar como
// ADMIN/TECNICO/CLIENTE con la SPA nueva (la version anterior en frontend/app.js si la tenia).
export function ConsolePage() {
  const { t } = useTranslation()
  const { role } = useAuth()
  const userId = getUserId()
  const isAdmin = role === 'ADMIN'
  const isTecnico = role === 'TECNICO'
  const isCliente = role === 'CLIENTE'
  const canManage = isAdmin || isTecnico
  const technicians = useTechnicians(isAdmin)
  // Solo se llena para ADMIN (el unico rol que puede pedir la lista completa de tecnicos,
  // ver useTechnicians) -- TECNICO/CLIENTE siguen viendo el id recortado como respaldo, que
  // es lo que ya se mostraba para todos antes de este cambio.
  const technicianNameById = useMemo(
    () => Object.fromEntries(technicians.map((tech) => [tech.id, tech.fullName])),
    [technicians],
  )

  const [showCreateModal, setShowCreateModal] = useState(false)

  const {
    tickets,
    allCount,
    breachedCount,
    escaladoCount,
    loading,
    error,
    search,
    setSearch,
    zoneFilter,
    setZoneFilter,
    statusFilter,
    setStatusFilter,
    refresh,
  } = useTickets()

  const columnCount = canManage ? 7 : 6

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
        <div>
          <h1 style={{ marginBottom: 4 }}>{t('console.title')}</h1>
          <p style={{ color: 'var(--text-muted)', marginTop: 0 }}>{t('console.subtitle')}</p>
        </div>
        {isCliente && (
          <button type="button" onClick={() => setShowCreateModal(true)} style={createButtonStyle}>
            + {t('console.newTicket')}
          </button>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 12, margin: '8px 0 20px' }}>
        <StatCard label={t('console.kpi.total')} value={allCount} color="var(--color-navy)" />
        <StatCard label={t('console.kpi.escalated')} value={escaladoCount} color="var(--status-escalado)" />
        <StatCard label={t('console.kpi.breached')} value={breachedCount} color="var(--color-amber)" />
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
        <input
          type="text"
          placeholder={t('console.search') ?? ''}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="field-input"
          style={inputStyle}
        />
        {/* Filtro de zona: solo tiene sentido para ADMIN. Un TECNICO ya solo recibe
            tickets de su propia zona (el backend ignora cualquier filtro que mande) y un
            CLIENTE solo ve los suyos -- mostrarles un selector de 3 zonas era enganoso. */}
        {isAdmin && (
          <select
            value={zoneFilter ?? ''}
            onChange={(e) => setZoneFilter((e.target.value || null) as Zone | null)}
            className="field-input"
            style={inputStyle}
          >
            <option value="">{`${t('console.filterZone')}: ${t('console.filterAll')}`}</option>
            {ZONES.map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </select>
        )}
        <select
          value={statusFilter ?? ''}
          onChange={(e) => setStatusFilter((e.target.value || null) as TicketStatus | null)}
          className="field-input"
          style={inputStyle}
        >
          <option value="">{`${t('console.filterStatus')}: ${t('console.filterAll')}`}</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {t(`status.${status}`)}
            </option>
          ))}
        </select>
        <button type="button" onClick={refresh} style={buttonStyle}>
          ⟳
        </button>
      </div>

      {error && <p style={{ color: 'var(--status-escalado)' }}>📡 {t('console.error')}</p>}

      {!error && (
        <div className="data-table" style={{ overflow: 'auto', maxHeight: 'calc(100vh - 340px)', border: '1px solid var(--border)', borderRadius: 8 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', background: 'var(--surface)' }}>
            <thead>
              <tr style={{ textAlign: 'left', borderBottom: '2px solid var(--border)' }}>
                <th style={thStyle}>{t('console.table.zone')}</th>
                <th style={thStyle}>{t('console.table.technician')}</th>
                <th style={thStyle}>{t('console.table.status')}</th>
                <th style={thStyle}>{t('console.table.priority')}</th>
                <th style={thStyle}>{t('console.table.sla')}</th>
                <th style={thStyle}>{t('console.table.description')}</th>
                {canManage && <th style={thStyle}>{t('console.table.actions')}</th>}
              </tr>
            </thead>
            <tbody>
              {loading &&
                Array.from({ length: SKELETON_ROWS }).map((_, i) => (
                  <tr key={`skeleton-${i}`}>
                    {Array.from({ length: columnCount }).map((__, col) => (
                      <td key={col} style={tdStyle}>
                        <div className="skeleton-row" style={{ height: 16, width: col === 5 ? '90%' : '60%' }} />
                      </td>
                    ))}
                  </tr>
                ))}
              {!loading && tickets.length === 0 && (
                <tr>
                  <td colSpan={columnCount} style={{ padding: 24, textAlign: 'center', color: 'var(--text-muted)' }}>
                    {t('console.empty')}
                  </td>
                </tr>
              )}
              {!loading &&
                tickets.map((ticket) => (
                  <tr key={ticket.ticketId}>
                    <td style={tdStyle}>{ticket.zone}</td>
                    <td style={tdStyle}>
                      {ticket.technicianId ? (
                        (technicianNameById[ticket.technicianId] ?? ticket.technicianId.slice(0, 8))
                      ) : (
                        <UnassignedChip label={t('console.unassigned')} />
                      )}
                    </td>
                    <td style={tdStyle}>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td style={tdStyle}>{ticket.priority && <PriorityBadge priority={ticket.priority} />}</td>
                    <td style={tdStyle}>
                      <SlaBadge breached={ticket.slaBreached} />
                    </td>
                    <td style={{ ...tdStyle, maxWidth: 360 }}>{ticket.description}</td>
                    {canManage && (
                      <td style={tdStyle}>
                        <TicketRowActions
                          ticket={ticket}
                          currentUserId={userId}
                          role={role}
                          technicians={technicians}
                          onChanged={refresh}
                        />
                      </td>
                    )}
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <CreateTicketModal
          onClose={() => setShowCreateModal(false)}
          onCreated={() => {
            setShowCreateModal(false)
            refresh()
          }}
        />
      )}
    </div>
  )
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: '12px 16px' }}>
      <div style={{ fontSize: '1.7rem', fontWeight: 700, color, lineHeight: 1.1 }}>{value}</div>
      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: 2 }}>{label}</div>
    </div>
  )
}

const inputStyle: CSSProperties = {
  padding: '8px 12px',
  borderRadius: 8,
  border: '1px solid var(--border)',
  background: 'var(--surface)',
  color: 'var(--text)',
  fontSize: '0.95rem',
}

const buttonStyle: CSSProperties = {
  ...inputStyle,
  cursor: 'pointer',
  fontWeight: 700,
}

const createButtonStyle: CSSProperties = {
  ...inputStyle,
  cursor: 'pointer',
  fontWeight: 700,
  background: 'var(--color-navy)',
  color: '#fff',
  border: 'none',
  whiteSpace: 'nowrap',
}

const thStyle: CSSProperties = { padding: '10px 12px', fontSize: '0.85rem', color: 'var(--text-muted)' }
const tdStyle: CSSProperties = { padding: '10px 12px', fontSize: '0.9rem' }
