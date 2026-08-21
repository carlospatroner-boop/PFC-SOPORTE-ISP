import { useCallback, useEffect, useState, type CSSProperties, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { createUser, listUsers } from '../api/adminApi'
import type { Role, UserResponse } from '../types/user'

const ROLES: Role[] = ['CLIENTE', 'TECNICO', 'ADMIN']
const ZONES = ['QUEVEDO_CENTRO', 'QUEVEDO_NORTE', 'QUEVEDO_SUR']

/**
 * Alta de cuentas CLIENTE/TECNICO/ADMIN + listado -- lo unico que la version anterior
 * (frontend/index.html, vista "Administracion") mostraba solo a ADMIN y que la SPA nueva
 * todavia no tenia (ver Modulo B: nada impide agregar rutas ademas de las 5 minimas).
 */
export function AdminPage() {
  const { t } = useTranslation()
  const [users, setUsers] = useState<UserResponse[]>([])
  const [loadingUsers, setLoadingUsers] = useState(true)
  const [listError, setListError] = useState<string | null>(null)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [role, setRole] = useState<Role>('CLIENTE')
  const [zone, setZone] = useState<string>(ZONES[0])
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [formSuccess, setFormSuccess] = useState<string | null>(null)

  const refreshUsers = useCallback(() => {
    setLoadingUsers(true)
    setListError(null)
    listUsers()
      .then(setUsers)
      .catch(() => setListError(t('admin.list.error')))
      .finally(() => setLoadingUsers(false))
  }, [t])

  useEffect(() => {
    refreshUsers()
  }, [refreshUsers])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setFormError(null)
    setFormSuccess(null)
    if (!email.trim() || !password.trim() || !fullName.trim()) {
      setFormError(t('admin.form.errorEmpty'))
      return
    }
    setSubmitting(true)
    try {
      const created = await createUser({
        email: email.trim(),
        password,
        fullName: fullName.trim(),
        role,
        zone: role === 'TECNICO' ? zone : null,
      })
      setFormSuccess(t('admin.form.success', { email: created.email }))
      setEmail('')
      setPassword('')
      setFullName('')
      refreshUsers()
    } catch {
      setFormError(t('admin.form.errorGeneric'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ padding: 24, maxWidth: 1100, margin: '0 auto' }}>
      <h1 style={{ marginBottom: 4 }}>{t('admin.title')}</h1>
      <p style={{ color: 'var(--text-muted)', marginTop: 0 }}>{t('admin.subtitle')}</p>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 380px) 1fr', gap: 24, alignItems: 'start' }}>
        <form onSubmit={handleSubmit} style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>{t('admin.form.title')}</h3>

          <label style={labelStyle}>
            {t('admin.form.email')}
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            {t('admin.form.password')}
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} style={inputStyle} />
          </label>
          <label style={labelStyle}>
            {t('admin.form.fullName')}
            <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} style={inputStyle} />
          </label>

          <span style={{ ...labelStyle, marginBottom: 4 }}>{t('admin.form.role')}</span>
          <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
            {ROLES.map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => setRole(r)}
                style={{ ...roleButtonStyle, ...(role === r ? roleButtonActiveStyle : {}) }}
              >
                {t(`admin.roles.${r}`)}
              </button>
            ))}
          </div>

          {role === 'TECNICO' && (
            <label style={labelStyle}>
              {t('admin.form.zone')}
              <select value={zone} onChange={(e) => setZone(e.target.value)} style={inputStyle}>
                {ZONES.map((z) => (
                  <option key={z} value={z}>
                    {z}
                  </option>
                ))}
              </select>
            </label>
          )}

          {formError && <p style={{ color: 'var(--status-escalado)' }}>{formError}</p>}
          {formSuccess && <p style={{ color: 'var(--status-resuelto)' }}>{formSuccess}</p>}

          <button type="submit" disabled={submitting} style={submitStyle}>
            {submitting ? t('admin.form.submitting') : t('admin.form.submit')}
          </button>
        </form>

        <div style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>{t('admin.list.title')}</h3>
          {loadingUsers && <p>{t('admin.list.loading')}</p>}
          {listError && <p style={{ color: 'var(--status-escalado)' }}>{listError}</p>}
          {!loadingUsers && !listError && (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '2px solid var(--border)' }}>
                    <th style={thStyle}>{t('admin.list.columns.name')}</th>
                    <th style={thStyle}>{t('admin.list.columns.email')}</th>
                    <th style={thStyle}>{t('admin.list.columns.role')}</th>
                    <th style={thStyle}>{t('admin.list.columns.zone')}</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} style={{ borderBottom: '1px solid var(--border)' }}>
                      <td style={tdStyle}>{u.fullName}</td>
                      <td style={tdStyle}>{u.email}</td>
                      <td style={tdStyle}>{t(`admin.roles.${u.role}`)}</td>
                      <td style={tdStyle}>{u.zone ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const cardStyle: CSSProperties = {
  background: 'var(--surface)',
  border: '1px solid var(--border)',
  borderRadius: 12,
  padding: 20,
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
}

const roleButtonStyle: CSSProperties = {
  flex: 1,
  padding: '8px 0',
  borderRadius: 8,
  border: '1px solid var(--border)',
  background: 'var(--bg)',
  color: 'var(--text)',
  cursor: 'pointer',
}

const roleButtonActiveStyle: CSSProperties = {
  background: 'var(--color-navy)',
  color: '#fff',
  borderColor: 'var(--color-navy)',
  fontWeight: 700,
}

const submitStyle: CSSProperties = {
  ...inputStyle,
  background: 'var(--color-navy)',
  color: '#fff',
  fontWeight: 700,
  cursor: 'pointer',
  border: 'none',
  width: '100%',
}

const thStyle: CSSProperties = { padding: '10px 12px', fontSize: '0.85rem', color: 'var(--text-muted)' }
const tdStyle: CSSProperties = { padding: '10px 12px', fontSize: '0.9rem' }
