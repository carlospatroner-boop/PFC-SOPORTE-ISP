package ec.edu.uteq.soporte.mobile.di

import android.content.Context
import ec.edu.uteq.soporte.mobile.data.local.AppDatabase
import ec.edu.uteq.soporte.mobile.data.remote.NetworkModule
import ec.edu.uteq.soporte.mobile.data.repository.AuthRepository
import ec.edu.uteq.soporte.mobile.data.repository.TicketRepository
import ec.edu.uteq.soporte.mobile.data.session.SessionManager

/**
 * Contenedor de dependencias manual, deliberadamente simple (sin Hilt/Dagger): el equipo no
 * tiene experiencia previa en Android y un framework de inyeccion con anotaciones suma otro
 * punto de falla de build a un modulo que ya es el de mayor riesgo del semestre. Cada
 * repositorio se construye una sola vez (patron singleton manual) y se expone por caracteristica,
 * igual que si fuera un modulo de Hilt.
 */
class ServiceLocator(context: Context) {
    val sessionManager = SessionManager(context)
    private val networkModule = NetworkModule(sessionManager)
    private val database = AppDatabase.get(context)

    val authRepository: AuthRepository by lazy {
        AuthRepository(networkModule.authApi, sessionManager)
    }

    val ticketRepository: TicketRepository by lazy {
        TicketRepository(networkModule.ticketApi, database.ticketDao())
    }
}
