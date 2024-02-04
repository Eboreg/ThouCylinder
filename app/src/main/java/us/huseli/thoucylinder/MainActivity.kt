package us.huseli.thoucylinder

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.compose.App
import us.huseli.thoucylinder.viewmodels.LastFmViewModel
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val spotifyImportViewModel by viewModels<SpotifyImportViewModel>()
    private val lastFmViewModel by viewModels<LastFmViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val startDestination: String = intent?.let { handleIntent(it) } ?: LibraryDestination.route

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        setContent {
            ThouCylinderTheme {
                App(startDestination = startDestination)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == spotifyImportViewModel.requestCode) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            spotifyImportViewModel.setAuthorizationResponse(response)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.also { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent): String? {
        intent.data?.pathSegments?.also { pathSegments ->
            if (pathSegments.getOrNull(0) == "spotify" && pathSegments.getOrNull(1) == "import-albums") {
                AuthorizationResponse.fromUri(intent.data)?.also {
                    spotifyImportViewModel.setAuthorizationResponse(it)
                }
                return ImportDestination.route
            }
            if (pathSegments.getOrNull(0) == "lastfm" && pathSegments.getOrNull(1) == "auth") {
                intent.data?.getQueryParameter("token")?.also { authToken ->
                    lastFmViewModel.getSessionKey(
                        authToken = authToken,
                        onError = { exception ->
                            SnackbarEngine.addError(getString(R.string.last_fm_authorization_failed, exception))
                        },
                    )
                }
                return SettingsDestination.route
            }
        }
        return null
    }
}
