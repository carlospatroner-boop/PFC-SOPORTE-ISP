import { useEffect, useRef } from 'react'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  life: number
  maxLife: number
  size: number
  color: string
}

const COLORS = ['#3FA88F', '#9FD6C6', '#FFFFFF', '#C97F00']

/**
 * Estela de particulas que sigue el cursor -- puramente decorativo, pensado para el fondo
 * navy del login (LoginPage.tsx). Canvas + requestAnimationFrame en vez de una libreria
 * nueva: es una unica pantalla, no vale la pena la dependencia extra. pointerEvents: 'none'
 * para que nunca intercepte clics sobre el formulario que esta encima.
 */
export function MouseParticles() {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    let width = window.innerWidth
    let height = window.innerHeight
    canvas.width = width
    canvas.height = height

    const particles: Particle[] = []
    let mouseX = width / 2
    let mouseY = height / 2
    let lastSpawn = 0

    const handleResize = () => {
      width = window.innerWidth
      height = window.innerHeight
      canvas.width = width
      canvas.height = height
    }

    const handleMouseMove = (e: MouseEvent) => {
      mouseX = e.clientX
      mouseY = e.clientY
      const now = performance.now()
      // 1 particula cada ~16ms de movimiento real, no una por evento (los eventos de
      // mousemove disparan mucho mas seguido que eso y saturarian el lienzo).
      if (now - lastSpawn > 16) {
        lastSpawn = now
        for (let i = 0; i < 2; i++) {
          particles.push({
            x: mouseX,
            y: mouseY,
            vx: (Math.random() - 0.5) * 1.4,
            vy: (Math.random() - 0.5) * 1.4 - 0.3,
            life: 0,
            maxLife: 40 + Math.random() * 25,
            size: 1.5 + Math.random() * 2.5,
            color: COLORS[Math.floor(Math.random() * COLORS.length)],
          })
        }
      }
    }

    window.addEventListener('resize', handleResize)
    window.addEventListener('mousemove', handleMouseMove)

    let rafId: number
    const tick = () => {
      ctx.clearRect(0, 0, width, height)
      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i]
        p.life += 1
        p.x += p.vx
        p.y += p.vy
        if (p.life >= p.maxLife) {
          particles.splice(i, 1)
          continue
        }
        const t = p.life / p.maxLife
        const alpha = 1 - t
        ctx.beginPath()
        ctx.arc(p.x, p.y, p.size * (1 - t * 0.4), 0, Math.PI * 2)
        ctx.fillStyle = p.color
        ctx.globalAlpha = alpha * 0.75
        ctx.fill()
      }
      ctx.globalAlpha = 1
      rafId = requestAnimationFrame(tick)
    }
    rafId = requestAnimationFrame(tick)

    return () => {
      cancelAnimationFrame(rafId)
      window.removeEventListener('resize', handleResize)
      window.removeEventListener('mousemove', handleMouseMove)
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      style={{ position: 'fixed', inset: 0, pointerEvents: 'none', zIndex: 0 }}
      aria-hidden="true"
    />
  )
}
