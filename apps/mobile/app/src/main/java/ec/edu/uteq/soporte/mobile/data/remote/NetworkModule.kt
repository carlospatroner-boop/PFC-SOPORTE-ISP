package ec.edu.uteq.soporte.mobile.data.remote

import ec.edu.uteq.soporte.mobile.BuildConfig
import ec.edu.uteq.soporte.mobile.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye los clientes Retrofit contra auth-service y ticket-service. El interceptor agrega
 * "Authorization: Bearer <token>" a toda request contra ticket-service cuando hay sesion activa
 * -- el mismo esquema que usa AuthGatewayFilter en el backend (ver TicketController.java).
 */
class NetworkModule(private val sessionManager: SessionManager) {

    private fun authInterceptor() = okhttp3.Interceptor { chain ->
        val token = sessionManager.accessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor())
        .addInterceptor(loggingInterceptor())
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy {
        retrofit(BuildConfig.AUTH_BASE_URL).create(AuthApi::class.java)
    }

    val ticketApi: TicketApi by lazy {
        retrofit(BuildConfig.TICKETS_BASE_URL).create(TicketApi::class.java)
    }
}
