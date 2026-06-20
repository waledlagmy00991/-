package com.example.util

import java.util.*
import kotlin.math.*

object PrayerTimesCalculator {
    data class PrayerTimes(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String
    )

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezone: Double = 3.0,
        method: String = "UmmAlQura",
        calendar: Calendar = Calendar.getInstance()
    ): PrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Julian date offset calculations
        val d = (367 * year - (7 * (year + (month + 9) / 12)) / 4 + (275 * month) / 9 + day - 730531.5)
        val l = (280.461 + 0.9856474 * d) % 360.0
        val g = (357.528 + 0.9856003 * d) % 360.0
        val lambda = (l + 1.915 * sin(Math.toRadians(g)) + 0.02 * sin(Math.toRadians(2 * g))) % 360.0
        val obliq = 23.439 - 0.0000004 * d
        val declination = Math.toDegrees(asin(sin(Math.toRadians(obliq)) * sin(Math.toRadians(lambda))))

        // Solar transit calculation (approx EqT in clock minutes)
        val e = 0.016708
        val varY = tan(Math.toRadians(obliq) / 2.0).pow(2.0)
        val eq = varY * sin(2 * Math.toRadians(l)) - 2 * e * sin(Math.toRadians(g)) + 4 * e * varY * sin(Math.toRadians(g)) * cos(2 * Math.toRadians(l)) - 0.5 * varY.pow(2.0) * sin(4 * Math.toRadians(l)) - 1.25 * e.pow(2.0) * sin(2 * Math.toRadians(g))
        val eqOfTime = Math.toDegrees(eq) * 4.0

        // Base solar noon
        val dhuhrLocal = 12.0 - longitude / 15.0 + timezone - eqOfTime / 60.0

        // Fajr angle by method
        val fajrAngle = when (method) {
            "Egyptian" -> 19.5
            "MWL" -> 18.0
            "ISNA" -> 15.0
            else -> 18.5 // Umm Al-Qura
        }

        // loc calculations
        val locFajr = computeHourAngle(fajrAngle, latitude, declination)
        val locSunrise = computeHourAngle(0.833, latitude, declination)

        // Asr Shadow Calculation (Multipler = 1)
        val delta = abs(latitude - declination)
        val asrAltitude = Math.toDegrees(atan(1.0 / (1.0 + tan(Math.toRadians(delta)))))
        val locAsr = computeHourAngle(180.0 - asrAltitude, latitude, declination)

        val fajrTime = if (locFajr != null) formatTime(dhuhrLocal - locFajr / 15.0) else "04:52"
        val sunriseTime = if (locSunrise != null) formatTime(dhuhrLocal - locSunrise / 15.0) else "06:14"
        val dhuhrTime = formatTime(dhuhrLocal)
        val asrTime = if (locAsr != null) formatTime(dhuhrLocal + locAsr / 15.0) else "15:22"
        val maghribTime = if (locSunrise != null) formatTime(dhuhrLocal + locSunrise / 15.0) else "18:41"

        val ishaTime = when (method) {
            "UmmAlQura" -> {
                val mTime = if (locSunrise != null) (dhuhrLocal + locSunrise / 15.0) else 18.68
                formatTime(mTime + 1.5) // Adds exactly 90 mins (1.5 hours) after Maghrib
            }
            "Egyptian" -> {
                val locIsha = computeHourAngle(17.5, latitude, declination)
                if (locIsha != null) formatTime(dhuhrLocal + locIsha / 15.0) else "19:11"
            }
            else -> {
                val locIsha = computeHourAngle(15.0, latitude, declination)
                if (locIsha != null) formatTime(dhuhrLocal + locIsha / 15.0) else "19:11"
            }
        }

        return PrayerTimes(
            fajr = fajrTime,
            sunrise = sunriseTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime
        )
    }

    private fun computeHourAngle(angle: Double, latitude: Double, declination: Double): Double? {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val cosH = (cos(Math.toRadians(180 - angle)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        return if (cosH in -1.0..1.0) Math.toDegrees(acos(cosH)) else null
    }

    private fun formatTime(hoursDecimal: Double): String {
        var h = hoursDecimal.toInt()
        var m = round((hoursDecimal - h) * 60).toInt()
        if (m >= 60) {
            h += 1
            m -= 60
        }
        h = (h % 24 + 24) % 24
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
