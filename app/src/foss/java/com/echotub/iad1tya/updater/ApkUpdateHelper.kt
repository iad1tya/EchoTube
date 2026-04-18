package com.echotube.iad1tya.updater

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Stub for FOSS builds — APK self-update is not available.
 * F-Droid / IzzyOnDroid handle updates through their own clients.
 */
object ApkUpdateHelper {
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit
    ): Result<File> {
        return Result.failure(UnsupportedOperationException("In-app updater is disabled in this build"))
    }

    fun canInstallPackages(context: Context): Boolean = false

    fun createUnknownSourcesIntent(context: Context): Intent {
        return Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
    }

    fun installDownloadedApk(activity: Activity, apkFile: File): Result<Unit> {
        return Result.failure(UnsupportedOperationException("In-app updater is disabled in this build"))
    }
}
