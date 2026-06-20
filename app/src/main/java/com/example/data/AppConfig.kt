package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfig(
    @PrimaryKey val id: Int = 1,
    val pinCode: String = "1234",
    val isCommitmentModeEnabled: Boolean = true,
    val calculationMethod: String = "UmmAlQura",
    val isFajrEnabled: Boolean = true,
    val isDhuhrEnabled: Boolean = true,
    val isAsrEnabled: Boolean = true,
    val isMaghribEnabled: Boolean = true,
    val isIshaEnabled: Boolean = true,
    val customLatitude: Double = 24.7136, // Default: Riyadh, KSA
    val customLongitude: Double = 46.6753,
    val locationName: String = "الرياض، المملكة العربية السعودية",
    val showNightsAlert: Boolean = false,
    val showDailyQuran: Boolean = true,
    val isOnboardingCompleted: Boolean = false
)
