package com.nightlynexus.diskcrashreporter

internal class NotificationIdProvider {
  var notificationId = 0
    get() = ++field
    private set
}
