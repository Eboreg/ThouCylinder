package us.huseli.thoucylinder.interfaces

import android.util.Log
import us.huseli.thoucylinder.BuildConfig

interface ILogger {
    fun log(priority: Int, tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.println(priority, tag, message)
    }

    fun log(priority: Int, message: String) = log(priority = priority, tag = javaClass.simpleName, message = message)

    fun log(tag: String, message: String) = log(priority = Log.INFO, tag = tag, message = message)

    fun log(message: String) = log(priority = Log.INFO, message = message)

    fun logError(tag: String, message: String, exception: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, message, exception)
    }

    fun logError(message: String, exception: Throwable? = null) =
        logError(tag = javaClass.simpleName, message = message, exception = exception)

    fun logError(exception: Throwable) = logError(message = exception.toString(), exception = exception)

    fun logWarning(tag: String, message: String, exception: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, message, exception)
    }

    fun logWarning(message: String, exception: Throwable? = null) =
        logWarning(tag = javaClass.simpleName, message = message, exception = exception)
}
