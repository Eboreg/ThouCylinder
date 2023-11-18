package us.huseli.thoucylinder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

        // TODO: What does this do?
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val requestPermissionLauncher = registerForActivityResult(contract) { }

        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                )
            else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        requestPermissionLauncher.launch(permissions)

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        try {
            setContent {
                ThouCylinderTheme {
                    App()
                }
            }
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, e.toString(), e)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.also { uri ->
            AuthorizationResponse.fromUri(uri)?.also { spotifyImportViewModel.setAuthorizationResponse(it) }
        }
    }
}
