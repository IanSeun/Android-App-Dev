// app/src/main/java/com/g/gradeapp/ui/navigation/NavGraph.kt
package com.g.gradeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.g.gradeapp.ui.screens.ConfigScreen
import com.g.gradeapp.ui.screens.GradesScreen
import com.g.gradeapp.ui.screens.StatsScreen

sealed class Dest(val route: String) {
    object Grades  : Dest("grades")
    object Config  : Dest("config")
    object Stats   : Dest("stats")
}

@Composable
fun GNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Dest.Grades.route,
    ) {
        composable(Dest.Grades.route) { GradesScreen() }
        composable(Dest.Config.route) { ConfigScreen() }
        composable(Dest.Stats.route)  { StatsScreen() }
    }
}
