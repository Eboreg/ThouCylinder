package us.huseli.thoucylinder.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.material3.ColorProviders
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import us.huseli.retaintheme.ui.theme.DarkColors
import us.huseli.retaintheme.ui.theme.LightColors
import us.huseli.thoucylinder.managers.WidgetManager

class AppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppWidgetEntryPoint {
        fun widgetManager(): WidgetManager
    }

    private fun getWidgetManager(context: Context): WidgetManager =
        EntryPointAccessors.fromApplication(context, AppWidgetEntryPoint::class.java).widgetManager()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val colors = ColorProviders(
            light = LightColors,
            dark = DarkColors.copy(onSurfaceVariant = Color(0xFF899294)),
        )
        val manager = getWidgetManager(context.applicationContext)

        provideContent {
            GlanceTheme(colors = colors) {
                Widget(manager = manager)
            }
        }
    }
}
