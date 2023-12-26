package us.huseli.thoucylinder

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        var startDestination = LibraryDestination.route

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        intent?.data?.pathSegments?.also { pathSegments ->
            if (pathSegments.getOrNull(0) == "spotify" && pathSegments.getOrNull(1) == "import-albums") {
                AuthorizationResponse.fromUri(intent.data)?.also {
                    spotifyImportViewModel.setAuthorizationResponse(it)
                    startDestination = ImportDestination.route
                }
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
            }
        }

        setContent {
            ThouCylinderTheme {
                App(startDestination = startDestination)
            }
        }
    }
}
