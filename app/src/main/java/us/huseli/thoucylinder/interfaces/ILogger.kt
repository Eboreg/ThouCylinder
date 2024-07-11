package us.huseli.thoucylinder.interfaces

import android.util.Log
import us.huseli.retaintheme.snackbar.SnackbarEngine
import us.huseli.thoucylinder.BuildConfig

interface ILogger {
    private fun formatMessage(message: String): String = "[${getThreadSignature()}] $message"

    private fun getThreadSignature(): String {
        val thread = Thread.currentThread()
        val ret = "${thread.name}:${thread.id}:${thread.priority}"

        return thread.threadGroup?.name?.let { "$ret:$it" } ?: ret
    }

    fun log(priority: Int, tag: String, message: String, force: Boolean = false) {
        if (BuildConfig.DEBUG || force) Log.println(priority, tag, formatMessage(message))
    }

    fun log(priority: Int, message: String, force: Boolean = false) =
        log(priority = priority, tag = javaClass.simpleName, message = message, force = force)

    fun log(tag: String, message: String, force: Boolean = false) =
        log(priority = Log.INFO, tag = tag, message = message, force = force)

    fun log(message: String, force: Boolean = false) =
        log(priority = Log.INFO, tag = javaClass.simpleName, message = message, force = force)

    fun logError(tag: String, message: String, exception: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, formatMessage(message), exception)
    }

    fun logError(message: String, exception: Throwable? = null) =
        logError(tag = javaClass.simpleName, message = message, exception = exception)

    fun logError(exception: Throwable) =
        logError(message = exception.toString(), exception = exception, tag = javaClass.simpleName)

    fun logWarning(tag: String, message: String, exception: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, formatMessage(message), exception)
    }

    fun logWarning(message: String, exception: Throwable? = null) =
        logWarning(tag = javaClass.simpleName, message = message, exception = exception)

    fun showErrorSnackbar(message: String) {
        logError(message)
        SnackbarEngine.addError(message = message)
    }

    fun showErrorSnackbar(exception: Throwable) {
        logError(exception)
        SnackbarEngine.addError(message = exception.message ?: exception.toString())
    }

    fun showInfoSnackbar(message: String) = SnackbarEngine.addInfo(message = message)
}
