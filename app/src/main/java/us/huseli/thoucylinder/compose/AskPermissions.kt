package us.huseli.thoucylinder.compose

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.viewmodels.AppViewModel

@Composable
fun AskMusicDownloadPermissions() {
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    val permissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
            )
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    LaunchedEffect(Unit) {
        requestPermissionLauncher.launch(permissions)
    }
}

@Composable
fun AskMusicImportPermissions(viewModel: AppViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val autoImportLocalMusic by viewModel.autoImportLocalMusic.collectAsStateWithLifecycle()
    val musicImportRelativePath by viewModel.musicImportRelativePath.collectAsStateWithLifecycle()
    var isDialogShown by rememberSaveable { mutableStateOf(autoImportLocalMusic == null) }
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.READ_EXTERNAL_STORAGE] == true || it[Manifest.permission.READ_MEDIA_AUDIO] == true) {
                musicImportRelativePath?.also { relativePath ->
                    viewModel.importNewMediaStoreAlbums(context, relativePath)
                }
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
    val selectImportDirlauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val relativePath = DocumentsContract.getTreeDocumentId(uri).substringAfterLast(':')

            viewModel.setAutoImportLocalMusic(true)
            viewModel.setMusicImportRelativePath(relativePath)
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            requestPermissionLauncher.launch(permissions)
        }
    }

    if (isDialogShown) {
        AlertDialog(
            onDismissRequest = { isDialogShown = false },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setAutoImportLocalMusic(false)
                        isDialogShown = false
                    },
                    content = { Text(text = stringResource(R.string.don_t_import)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = Environment.getExternalStorageDirectory()
                            .toUri()
                            .buildUpon()
                            .appendPath(Environment.DIRECTORY_MUSIC)
                            .build()
                        selectImportDirlauncher.launch(uri)
                        isDialogShown = false
                    },
                    content = { Text(text = stringResource(R.string.select_directory)) },
                )
            },
            title = { Text(text = stringResource(R.string.import_local_music)) },
            text = { Text(text = stringResource(R.string.select_root_directory_to_import_from)) },
        )
    }
}
