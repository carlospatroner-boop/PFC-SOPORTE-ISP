import { useState, type CSSProperties, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { createTicket } from '../api/ticketsApi'
import type { Zone } from '../types/ticket'

const ZONES: Zone[] = ['QUEVEDO_CENTRO', 'QUEVEDO_NORTE', 'QUEVEDO_SUR']

/**
 * Solo CLIENTE la ve (ver ConsolePage.tsx) -- en la version anterior
 * (frontend/index.html) era el formulario "Nueva solicitud de soporte" del modal de
 * tickets, oculto para TECNICO (que no puede crear, ver TicketAuthorization.assertCanCreate).
 */
export function CreateTicketModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation()
  const [zone, setZone] = useState<Zone>(ZONES[0])
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [contactPhone, setContactPhone] = useState('')
  const [address, setAddress] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!title.trim() || !description.trim()) {
      setError(t('console.createModal.errorEmpty'))
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      await createTicket({
        zone,
        title: title.trim(),
        description: description.trim(),
        contactPhone: contactPhone.trim() || undefined,
        address: address.trim() || undefined,
      })
      onCreated()
    } catch {
      setError(t('console.createModal.errorGeneric'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={overlayStyle} onClick={(e) => e.target === e.currentTarget && onClose()}>
      <form onSubmit={handleSubmit} style={modalStyle}>
        <h2 style={{ marginTop: 0 }}>{t('console.createModal.title')}</h2>

        <label style={labelStyle}>
          {t('console.createModal.zone')}
          <select value={zone} onChange={(e) => setZone(e.target.value as Zone)} style={inputStyle}>
            {ZONES.map((z) => (
              <option key={z} value={z}>
                {z}
              </option>
            ))}
          </select>
        </label>
        <label style={labelStyle}>
          {t('console.createModal.ticketTitle')}
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)} style={inputStyle} />
        </label>
        <label style={labelStyle}>
          {t('console.createModal.description')}
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            style={{ ...inputStyle, minHeight: 80, resize: 'vertical' }}
          />
        </label>
        <label style={labelStyle}>
          {t('console.createModal.contactPhone')}
          <input type="text" value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} style={inputStyle} />
        </label>
        <label style={labelStyle}>
          {t('console.createModal.address')}
          <input type="text" value={address} onChange={(e) => setAddress(e.target.value)} style={inputStyle} />
        </label>

        {error && <p style={{ color: 'var(--status-escalado)' }}>{error}</p>}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
          <button type="button" onClick={onClose} style={cancelStyle}>
            {t('console.createModal.cancel')}
          </button>
          <button type="submit" disabled={submitting} style={submitStyle}>
            {submitting ? t('console.createModal.submitting') : t('console.createModal.submit')}
          </button>
        </div>
      </form>
    </div>
  )
}

const overlayStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  background: 'rgba(0,0,0,0.5)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 50,
}

const modalStyle: CSSProperties = {
  background: 'var(--surface)',
  border: '1px solid var(--border)',
  borderRadius: 12,
  padding: 24,
  width: '100%',
  maxWidth: 440,
  maxHeight: '90vh',
  overflowY: 'auto',
}

const labelStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 4,
  fontSize: '0.85rem',
  color: 'var(--text-muted)',
  marginBottom: 12,
}

const inputStyle: CSSProperties = {
  padding: '8px 12px',
  borderRadius: 8,
  border: '1px solid var(--border)',
  background: 'var(--bg)',
  color: 'var(--text)',
  fontSize: '0.95rem',
  fontFamily: 'inherit',
}

const submitStyle: CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: 'none',
  background: 'var(--color-navy)',
  color: '#fff',
  fontWeight: 700,
  cursor: 'pointer',
}

const cancelStyle: CSSProperties = {
  padding: '8px 16px',
  borderRadius: 8,
  border: '1px solid var(--border)',
  background: 'transparent',
  color: 'var(--text)',
  cursor: 'pointer',
}
