package us.huseli.thoucylinder

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.thoucylinder.compose.App

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val contract = ActivityResultContracts.RequestMultiplePermissions()
        val requestPermissionLauncher = registerForActivityResult(contract) { results ->
            results.forEach { (permission, granted) ->
                Log.i("MainActivity", "requestPermissionLauncher: permission=$permission, granted=$granted")
            }
        }
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                )
            else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        requestPermissionLauncher.launch(permissions)

        setContent {
            ThouCylinderTheme {
                App()
            }
        }
    }
}
