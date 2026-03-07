import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.g.gradeapp.ui.theme.GTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "GradeForge") {
        GTheme {
            // For now, a simple placeholder until we move the full UI to commonMain
            Text("Welcome to GradeForge Desktop!")
        }
    }
}
