package com.nightlynexus.diskcrashreporter

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build.VERSION.SDK_INT
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okio.buffer
import okio.sink

internal class DiskCrashReporter constructor(
  private val application: Application,
  private val notificationIdProvider: NotificationIdProvider
) {
  private val channelId = "crash_reporter"
  private val shortcutId = "crash_reports"
  private val directoryName = "crash_reports"
  private val fileNameFormat = object : ThreadLocal<Format>() {
    override fun initialValue(): Format {
      return SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
        timeZone = TimeZone.getDefault()
      }
    }
  }

  fun report(cause: Throwable) {
    val message = cause.message

    val notificationBuilder = Notification.Builder(application, channelId)
      .setSmallIcon(R.drawable.disk_crash_reporter_icon)
      .setContentTitle(application.getText(R.string.crash_report_notification_title))
      .setContentText(message)

    var bigTextMessage: String
    val externalFilesDirectory = application.getExternalFilesDir(null)
    if (externalFilesDirectory == null) {
      bigTextMessage = application.getString(
        R.string.crash_report_notification_big_text_message_error_shared_storage_not_available,
        message
      )
    } else {
      val parentDirectory = File(externalFilesDirectory, directoryName)
      if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
        bigTextMessage = application.getString(
          R.string.crash_report_notification_big_text_message_error_failed_to_create_directory,
          message
        )
      } else {
        val file = file(parentDirectory)
        try {
          file.sink()
            .buffer()
            .use { sink ->
              cause.printStackTrace(PrintStream(sink.outputStream()))
            }

          bigTextMessage = application.getString(
            R.string.crash_report_notification_big_text_message_written_to_disk,
            message
          )

          val textFileViewer = Intent(ACTION_VIEW).apply {
            val authority = "${application.packageName}.fileprovider"
            val data = FileProvider.getUriForFile(application, authority, file)
            setDataAndType(data, "text/plain")
            addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
          }
          if (application.packageManager.queryIntentActivities(textFileViewer).isNotEmpty()) {
            notificationBuilder.setContentIntent(
              PendingIntent.getActivity(
                application,
                0,
                textFileViewer,
                FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
              )
            )

            val shortcutManager = application.getSystemService(ShortcutManager::class.java)

            /*val mainIntent = Intent(ACTION_MAIN)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setPackage(application.packageName)
            val activities = application.packageManager.queryIntentActivities(mainIntent)
            val firstMainActivity = activities.first().activityInfo
            val componentName =
              ComponentName(firstMainActivity.packageName, firstMainActivity.name)*/
            val mainIntent =
              application.packageManager.getLaunchIntentForPackage(application.packageName)
            if (mainIntent != null) {
              // If the component name is not guaranteed to be in this returned Intent, we will have
              // to use the above old code.
              val componentName = mainIntent.component!!
              val shortcutCount = shortcutManager.dynamicShortcuts.count { shortcutInfo ->
                shortcutInfo.activity == componentName
              } + shortcutManager.manifestShortcuts.count { shortcutInfo ->
                shortcutInfo.activity == componentName
              }

              if (shortcutCount < shortcutManager.maxShortcutCountPerActivity) {
                val shortcutInfo = ShortcutInfo.Builder(application, shortcutId)
                  .setActivity(componentName)
                  .setShortLabel(application.getText(R.string.crash_report_shortcut_label_short))
                  .setLongLabel(application.getText(R.string.crash_report_shortcut_label_long))
                  .setIcon(
                    Icon.createWithResource(
                      application,
                      R.drawable.disk_crash_reporter_icon
                    )
                  )
                  .setIntent(textFileViewer)
                  .build()
                shortcutManager.addDynamicShortcuts(listOf(shortcutInfo))
              }
            }
          }
        } catch (e: IOException) {
          bigTextMessage = application.getString(
            R.string.crash_report_notification_big_text_message_error_io_exception,
            e.message,
            message
          )
        }
      }
    }

    notificationBuilder.style = Notification.BigTextStyle().bigText(bigTextMessage)
    val notificationManager =
      application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(
      NotificationChannel(
        channelId,
        application.getText(R.string.crash_report_notifications_channel_name),
        IMPORTANCE_HIGH
      ).apply {
        enableVibration(true)
        enableLights(true)
        setSound(
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        )
        setShowBadge(true)
      }
    )
    notificationManager.notify(
      notificationIdProvider.notificationId,
      notificationBuilder.build()
    )
  }

  private fun file(parent: File): File {
    val dateName = fileNameFormat.get()!!.format(Date())
    val fileName = "$dateName.txt"
    var file = File(parent, fileName)
    if (!file.exists()) {
      return file
    }
    var count = 2
    while (true) {
      file = File(parent, "$dateName ($count).txt")
      if (!file.exists()) {
        return file
      }
      count++
    }
  }

  private fun PackageManager.queryIntentActivities(intent: Intent): List<ResolveInfo> {
    return if (SDK_INT >= 33) {
      queryIntentActivities(
        intent,
        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
      )
    } else {
      @Suppress("Deprecation") queryIntentActivities(
        intent,
        PackageManager.MATCH_ALL
      )
    }
  }
}
