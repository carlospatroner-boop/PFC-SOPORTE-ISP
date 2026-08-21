package ec.edu.uteq.soporte.mobile

import android.app.Application
import ec.edu.uteq.soporte.mobile.di.ServiceLocator

class MobileApp : Application() {
    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(this)
    }
}
