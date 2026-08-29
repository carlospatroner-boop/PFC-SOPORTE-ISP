import { useState, type CSSProperties, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { MouseParticles } from '../../../components/MouseParticles'

// Iconos inline (sin dependencia nueva) -- solo los 4 que usa esta pantalla.
function HeadsetIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 14v-2a9 9 0 0 1 18 0v2" />
      <path d="M21 15v2a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3z" />
      <path d="M3 15v2a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z" />
    </svg>
  )
}
function MailIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <path d="m22 6-10 7L2 6" />
    </svg>
  )
}
function LockIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="4" y="10" width="16" height="11" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </svg>
  )
}
function EyeIcon({ off }: { off: boolean }) {
  return off ? (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 10 8 10 8a13.16 13.16 0 0 1-3.17 4.6M6.61 6.61A13.16 13.16 0 0 0 2 12s3 8 10 8a9.15 9.15 0 0 0 5.39-1.61M2 2l20 20" />
      <path d="M12 9a3 3 0 0 1 3 3" />
    </svg>
  ) : (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { login, loading, error: authError } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [validationError, setValidationError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!email.trim() || !password) {
      setValidationError(t('login.errorEmpty'))
      return
    }
    setValidationError(null)
    const ok = await login(email.trim(), password)
    if (ok) navigate('/main', { replace: true })
  }

  const error = validationError ?? authError

  return (
    <div style={outerStyle}>
      <MouseParticles />
      {/* Motivo decorativo: mismos circulos concentricos que el resto del material del PFC
          (mazo de diapositivas) -- muy sutiles, solo para que el fondo no quede vacio. */}
      <div style={{ ...blob, top: -140, right: -120, width: 420, height: 420, background: 'var(--color-teal)' }} />
      <div style={{ ...blob, bottom: -160, left: -140, width: 380, height: 380, background: '#2C4A70' }} />

      <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <form onSubmit={handleSubmit} style={cardStyle}>
          <div style={badgeStyle}>
            <HeadsetIcon />
          </div>

          <p style={kickerStyle}>{t('login.brand')}</p>
          <h1 style={titleStyle}>{t('login.title')}</h1>

          <label style={labelStyle}>
            <span style={visuallyHiddenStyle}>{t('login.email')}</span>
            <div style={fieldWrapStyle}>
              <span style={fieldIconStyle}>
                <MailIcon />
              </span>
              <input
                type="email"
                placeholder={t('login.email') ?? ''}
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  setValidationError(null)
                }}
                className="field-input"
                style={fieldStyle}
              />
            </div>
          </label>

          <label style={labelStyle}>
            <span style={visuallyHiddenStyle}>{t('login.password')}</span>
            <div style={fieldWrapStyle}>
              <span style={fieldIconStyle}>
                <LockIcon />
              </span>
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder={t('login.password') ?? ''}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value)
                  setValidationError(null)
                }}
                className="field-input"
                style={{ ...fieldStyle, paddingRight: 42 }}
              />
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="icon-toggle"
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                style={eyeButtonStyle}
              >
                <EyeIcon off={showPassword} />
              </button>
            </div>
          </label>

          {error && <p style={errorStyle}>{error}</p>}

          <button type="submit" disabled={loading} className="btn-accent" style={submitStyle}>
            {loading ? t('login.loading') : t('login.submit')}
          </button>
        </form>

        <p style={footerStyle}>Aplicaciones Distribuidas (ISR-701) · UTEQ · Entrega 4</p>
      </div>
    </div>
  )
}

const outerStyle: CSSProperties = {
  minHeight: '100vh',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'var(--color-navy)',
  position: 'relative',
  overflow: 'hidden',
  padding: 24,
}

const blob: CSSProperties = {
  position: 'absolute',
  borderRadius: '50%',
  opacity: 0.18,
  filter: 'blur(2px)',
  pointerEvents: 'none',
}

const cardStyle: CSSProperties = {
  background: 'var(--surface)',
  padding: '40px 36px 32px',
  borderRadius: 16,
  width: 380,
  display: 'flex',
  flexDirection: 'column',
  gap: 14,
  boxShadow: '0 20px 50px rgba(0,0,0,0.35)',
}

const badgeStyle: CSSProperties = {
  width: 56,
  height: 56,
  borderRadius: '50%',
  background: 'var(--color-navy)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  alignSelf: 'center',
  marginBottom: 4,
}

const kickerStyle: CSSProperties = {
  margin: 0,
  textAlign: 'center',
  fontSize: '0.72rem',
  fontWeight: 700,
  letterSpacing: '0.14em',
  color: 'var(--color-teal)',
}

const titleStyle: CSSProperties = {
  fontSize: '1.35rem',
  fontWeight: 700,
  textAlign: 'center',
  margin: '0 0 10px',
  color: 'var(--text)',
}

const labelStyle: CSSProperties = {
  display: 'block',
}

const visuallyHiddenStyle: CSSProperties = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
}

const fieldWrapStyle: CSSProperties = {
  position: 'relative',
  display: 'flex',
  alignItems: 'center',
}

const fieldIconStyle: CSSProperties = {
  position: 'absolute',
  left: 12,
  display: 'flex',
  color: 'var(--text-muted)',
  pointerEvents: 'none',
}

const fieldStyle: CSSProperties = {
  width: '100%',
  padding: '11px 12px 11px 40px',
  borderRadius: 8,
  border: '1px solid var(--border)',
  fontSize: '1rem',
  background: 'var(--bg)',
  color: 'var(--text)',
}

const eyeButtonStyle: CSSProperties = {
  position: 'absolute',
  right: 10,
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  color: 'var(--text-muted)',
  display: 'flex',
  padding: 4,
}

const errorStyle: CSSProperties = {
  color: 'var(--status-escalado)',
  margin: 0,
  fontSize: '0.88rem',
  background: 'rgba(179, 38, 30, 0.08)',
  padding: '8px 10px',
  borderRadius: 6,
}

const submitStyle: CSSProperties = {
  padding: '12px 12px',
  borderRadius: 8,
  border: 'none',
  background: 'var(--color-teal)',
  color: '#fff',
  fontWeight: 700,
  fontSize: '1rem',
  cursor: 'pointer',
  marginTop: 4,
}

const footerStyle: CSSProperties = {
  marginTop: 20,
  fontSize: '0.8rem',
  color: 'rgba(255,255,255,0.6)',
  textAlign: 'center',
}
