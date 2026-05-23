package pl.dekrate.kofeino.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun formatDate(millis: Long): String =
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(millis))

    fun formatFullDate(millis: Long): String =
        SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDayOfWeek(millis: Long): String =
        SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(millis))
}
