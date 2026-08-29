package ec.edu.uteq.soporte.mobile.data.repository

import ec.edu.uteq.soporte.mobile.data.remote.AuthApi
import ec.edu.uteq.soporte.mobile.data.remote.dto.LoginRequest
import ec.edu.uteq.soporte.mobile.data.session.SessionManager

class AuthRepository(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager,
) {
    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val response = authApi.login(LoginRequest(email, password))
        val auth = requireNotNull(response.data) { "Respuesta de login sin datos" }
        sessionManager.saveTokens(auth.accessToken, auth.refreshToken)
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun logout() = sessionManager.clear()
}
