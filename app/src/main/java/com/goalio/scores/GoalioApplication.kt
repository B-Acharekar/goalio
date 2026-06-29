package com.goalio.scores

import android.app.Application
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.onesignal.OneSignal

class GoalioApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                    .build()
            )
            setDefaultsAsync(
                mapOf(
                    "profile_setup_enabled" to true,
                    "profile_teams_limit" to 6L,
                    "profile_players_limit" to 6L,
                    "backend_base_url" to "http://10.0.2.2:8000"
                )
            )
            fetchAndActivate()
        }

        val oneSignalAppId = getString(R.string.onesignal_app_id)
        if (oneSignalAppId.matches(Regex("[0-9a-fA-F-]{36}"))) {
            OneSignal.initWithContext(this, oneSignalAppId)
        }
    }
}
