package com.queatz.ailaai

import android.location.Location
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import at.bluesource.choicesdk.maps.common.LatLng
import com.queatz.ailaai.ui.screens.*
import com.queatz.ailaai.ui.theme.AiLaAiTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiLaAiTheme {
                navController = rememberNavController()
                push.navController = navController

                val showBottomBar = navController.currentBackStackEntryAsState()
                    .value
                    ?.destination
                    ?.route
                    ?.startsWith("group/") != true

                var known by remember { mutableStateOf(api.hasToken()) }

                if (!known) {
                    InitialScreen { known = true }
                } else {
                    var me by remember { mutableStateOf<Person?>(null) }
                    val snackbarHostState = remember { SnackbarHostState() }
                    val coroutineScope = rememberCoroutineScope()
                    val cantConnectString = stringResource(R.string.cant_connect)

                    LaunchedEffect(true) {
                        try {
                            me = api.me()
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            snackbarHostState.showSnackbar(cantConnectString, withDismissAction = true)
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(showBottomBar) {
                                    NavigationBar {
                                        listOf(
                                            NavButton("explore", getString(R.string.explore), Icons.Outlined.Search),
                                        NavButton("messages", getString(R.string.messages), Icons.Outlined.Email),
                                        NavButton("me", getString(R.string.me), Icons.Outlined.Person)
                                        ).forEachIndexed { index, item ->
                                            NavigationBarItem(
                                                icon = { Icon(item.icon, contentDescription = null) },
                                                label = { Text(item.text) },
                                                selected = navController.currentDestination?.route == item.route,
                                                onClick = {
                                                    navController.popBackStack()
                                                    navController.navigate(item.route)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) {
                        NavHost(navController, "explore", modifier = Modifier.padding(it).fillMaxSize()) {
                            composable("explore") { ExploreScreen(navController) { me } }
                            composable("messages") { MessagesScreen(navController) { me } }
                            composable(
                                "group/{id}",
                                deepLinks = listOf(navDeepLink { uriPattern = "${appDomain}/group/{id}" })
                            ) { GroupScreen(it, navController) { me } }
                            composable("me") { MeScreen(navController) { me } }
                            composable("settings") {
                                SettingsScreen(navController, { me }) {
                                    if (api.hasToken()) {
                                        coroutineScope.launch {
                                            try {
                                                me = api.me()
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                                snackbarHostState.showSnackbar(
                                                    getString(R.string.cant_connect),
                                                    withDismissAction = true
                                                )
                                            }
                                        }
                                    } else {
                                        known = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Location.toLatLng() = LatLng(latitude, longitude)

data class NavButton(
    val route: String,
    val text: String,
    val icon: ImageVector
)
