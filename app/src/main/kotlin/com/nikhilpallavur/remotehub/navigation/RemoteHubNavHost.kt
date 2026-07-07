package com.nikhilpallavur.remotehub.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nikhilpallavur.remotehub.feature.remote.RemoteRoute

/** Top-level navigation. Currently a single destination; new feature screens plug in here. */
@Composable
fun RemoteHubNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = REMOTE_ROUTE) {
        composable(REMOTE_ROUTE) { RemoteRoute() }
    }
}

private const val REMOTE_ROUTE = "remote"
