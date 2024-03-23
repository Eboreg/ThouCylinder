package us.huseli.thoucylinder

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.thoucylinder.compose.App
import us.huseli.thoucylinder.viewmodels.AppViewModel
import us.huseli.thoucylinder.viewmodels.LastFmViewModel
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val spotifyImportViewModel by viewModels<SpotifyImportViewModel>()
    private val lastFmViewModel by viewModels<LastFmViewModel>()
    private val appViewModel by viewModels<AppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build()
            )
        }

        installSplashScreen()
        super.onCreate(savedInstanceState)

        val startDestination: String = intent?.let { handleIntent(it) } ?: LibraryDestination.route

        appViewModel.doStartupTasks(this)

        /**
         * Recomposition of the below happens at app start for unknown reasons ... hopefully, it only happens when run
         * from the IDE, in the emulator? Does not seem to happen on device.
         * Some hints: https://stackoverflow.com/questions/72301445/why-is-setcontent-being-called-twice
         */
        setContent {
            ThouCylinderTheme {
                App(startDestination = remember { startDestination }, modifier = Modifier.safeDrawingPadding())
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.also { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent): String? {
        intent.data?.pathSegments?.also { pathSegments ->
            if (pathSegments.getOrNull(0) == "spotify" && pathSegments.getOrNull(1) == "import-albums") {
                spotifyImportViewModel.handleIntent(intent, this)
                return ImportDestination.route
            }
            if (pathSegments.getOrNull(0) == "lastfm" && pathSegments.getOrNull(1) == "auth") {
                lastFmViewModel.handleIntent(intent, this)
                return SettingsDestination.route
            }
        }
        return null
    }
}
