package ec.edu.uteq.soporte.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ec.edu.uteq.soporte.mobile.MobileApp
import ec.edu.uteq.soporte.mobile.ui.auth.LoginScreen
import ec.edu.uteq.soporte.mobile.ui.tickets.detail.TicketDetailScreen
import ec.edu.uteq.soporte.mobile.ui.tickets.list.TicketListScreen

private object Routes {
    const val LOGIN = "login"
    const val TICKET_LIST = "tickets"
    const val TICKET_DETAIL = "tickets/{ticketId}"
    fun ticketDetail(ticketId: String) = "tickets/$ticketId"
}

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as MobileApp
    val navController = rememberNavController()
    val startDestination = if (app.serviceLocator.sessionManager.isLoggedIn()) {
        Routes.TICKET_LIST
    } else {
        Routes.LOGIN
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.TICKET_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TICKET_LIST) {
            TicketListScreen(
                onTicketClick = { ticketId -> navController.navigate(Routes.ticketDetail(ticketId)) },
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.TICKET_LIST) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.TICKET_DETAIL,
            arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId").orEmpty()
            TicketDetailScreen(
                ticketId = ticketId,
                onBack = { navController.popBackStack() },
                onClosed = { navController.popBackStack() },
            )
        }
    }
}
