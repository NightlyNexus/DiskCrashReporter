package com.nightlynexus.diskcrashreporter

import kotlin.random.Random

internal class NotificationIdProvider {
  val notificationId
    get() = Random.nextInt(1, Int.MAX_VALUE)
}
