package com.nightlynexus.diskcrashreporter

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

internal class DiskCrashReporterContentProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    val notificationIdProvider = NotificationIdProvider()
    val diskCrashReporter = DiskCrashReporter(
      context!!.applicationContext as Application,
      notificationIdProvider
    )
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()!!
    Thread.setDefaultUncaughtExceptionHandler { thread, e ->
      var cause: Throwable = e
      var forward = cause.cause
      while (forward != null) {
        cause = forward
        forward = forward.cause
      }
      diskCrashReporter.report(cause)
      defaultHandler.uncaughtException(thread, e)
    }
    return true
  }

  override fun query(
    uri: Uri,
    projectionArg: Array<String>?,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?
  ): Cursor? = null

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, contentValues: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

  override fun update(
    uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
  ): Int = 0
}
