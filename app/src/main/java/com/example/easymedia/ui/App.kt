package com.example.easymedia.ui

import android.app.Application
import android.util.Log
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val appID = "YOUR_APP_ID"
        val region = "YOUR_REGION"
        val authKey = "YOUR_AUTH_KEY"

        val appSettings = CometChat.AppSettingsBuilder()
            .subscribePresenceForAllUsers()
            .setRegion(region)
            .build()

        CometChat.init(
            this,
            appID,
            appSettings,
            object : CometChat.CallbackListener<String>() {
                override fun onSuccess(p0: String?) {
                    Log.d("CometChat", "Initialization successful")
                }

                override fun onError(e: CometChatException?) {
                    Log.e("CometChat", "Initialization failed: ${e?.message}")
                }
            }
        )
    }
}
