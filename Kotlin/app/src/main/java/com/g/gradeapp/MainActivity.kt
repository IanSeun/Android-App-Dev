// app/src/main/java/com/g/gradeapp/MainActivity.kt
package com.g.gradeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.g.gradeapp.ui.navigation.Dest
import com.g.gradeapp.ui.navigation.GNavGraph
import com.g.gradeapp.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GTheme { GApp() }
        }
    }
}

private data class NavItem(
    val label: String,
    val dest: Dest,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val navItems = listOf(
    NavItem("Grades", Dest.Grades, Icons.Filled.BarChart,  Icons.Outlined.BarChart),
    NavItem("Config", Dest.Config, Icons.Filled.Tune,      Icons.Outlined.Tune),
    NavItem("Stats",  Dest.Stats,  Icons.Filled.Analytics, Icons.Outlined.Analytics),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GApp() {
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    Scaffold(
        containerColor = Black,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape    = androidx.compose.foundation.shape.RoundedCornerShape(9.dp),
                            color    = Sapphire,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "G",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color      = Color.White,
                                    ),
                                )
                            }
                        }
                        Column {
                            Text(
                                "GradeCalc",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color      = TextPrimary,
                                ),
                            )
                            Text(
                                when (currentRoute) {
                                    Dest.Config.route -> "Weights & Scale"
                                    Dest.Stats.route  -> "Statistics"
                                    else              -> "Student Grades"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color       = NavyDivider,
                        start       = Offset(0f, size.height),
                        end         = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Navy900,
                tonalElevation = 0.dp,
                modifier       = Modifier.drawBehind {
                    drawLine(
                        color       = NavyDivider,
                        start       = Offset(0f, 0f),
                        end         = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                },
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            navController.navigate(item.dest.route) {
                                popUpTo(Dest.Grades.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label  = {
                            Text(item.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Sapphire,
                            selectedTextColor   = Sapphire,
                            unselectedIconColor = TextDisabled,
                            unselectedTextColor = TextDisabled,
                            indicatorColor      = Sapphire.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { innerPad ->
        Box(modifier = Modifier.padding(innerPad)) {
            GNavGraph(navController = navController)
        }
    }
}
