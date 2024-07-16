package us.huseli.thoucylinder

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import us.huseli.thoucylinder.Constants.PREF_WIDGET_BUTTONS
import us.huseli.thoucylinder.managers.WidgetManager
import us.huseli.thoucylinder.widget.WidgetConfig
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject
    lateinit var manager: WidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        setResult(RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            WidgetConfig(
                manager = manager,
                onSave = { buttons ->
                    val resultValue = Intent()

                    preferences.edit().putStringSet(PREF_WIDGET_BUTTONS, buttons.map { it.name }.toSet()).apply()
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    setResult(RESULT_OK, resultValue)
                    finish()
                }
            )
        }
    }
}
