package us.huseli.thoucylinder

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.spotify.sdk.android.auth.AuthorizationResponse
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.thoucylinder.compose.App
import us.huseli.thoucylinder.viewmodels.SpotifyImportViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val spotifyImportViewModel by viewModels<SpotifyImportViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        setContent {
            ThouCylinderTheme {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.also { uri ->
            AuthorizationResponse.fromUri(uri)?.also { spotifyImportViewModel.setAuthorizationResponse(it) }
        }
    }
}
