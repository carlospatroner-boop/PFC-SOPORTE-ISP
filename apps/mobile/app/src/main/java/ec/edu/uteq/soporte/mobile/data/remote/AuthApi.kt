package ec.edu.uteq.soporte.mobile.data.remote

import ec.edu.uteq.soporte.mobile.data.remote.dto.ApiResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.AuthResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

/** Contrato real de auth-service (puerto 8001) -- ver AuthController.java. */
interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>
}
