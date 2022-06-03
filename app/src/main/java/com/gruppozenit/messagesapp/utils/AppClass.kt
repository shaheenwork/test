package com.gruppozenit.messagesapp.utils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.multidex.MultiDexApplication
import com.gruppozenit.messagesapp.utils.Consts.Companion.ADMIN_APPROVAL_NOTIF_CHANNEL_ID
import com.gruppozenit.messagesapp.utils.Consts.Companion.ADMIN_APPROVAL_NOTIF_CHANNEL_NAME
import com.gruppozenit.messagesapp.utils.Consts.Companion.NEW_MSG_NOTIF_CHANNEL_ID
import com.gruppozenit.messagesapp.utils.Consts.Companion.NEW_MSG_NOTIF_CHANNEL_NAME


@Suppress("DEPRECATION")
class AppClass : MultiDexApplication() {
    private var manager: NotificationManager? = null
    private var alarmManager: AlarmManager? = null

     val manger: NotificationManager
        get() {
            if(manager==null) {
                manager = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    applicationContext.getSystemService(NotificationManager::class.java)
                } else {
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                }
            }
            return manager!!
        }

    override fun onCreate() {
        super.onCreate()
      //  Stetho.initializeWithDefaults(this)
        instance = this
        createNotificationChannel()
       // setLocale()


    }




   /* private fun setLocale() {
        val resources = resources
        val configuration = resources.configuration
        val locale = Locale.ENGLISH
        if(configuration.locale!=locale) {
            configuration.setLocale(locale)
            resources.updateConfiguration(configuration, null)
        }
    }*/


    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val newMsgChannel = NotificationChannel(
                    NEW_MSG_NOTIF_CHANNEL_ID, NEW_MSG_NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)

            val adminApprovalChannel = NotificationChannel(ADMIN_APPROVAL_NOTIF_CHANNEL_ID,
                    ADMIN_APPROVAL_NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)



            adminApprovalChannel.setShowBadge(true)
            newMsgChannel.setShowBadge(true)
            adminApprovalChannel.enableVibration(true)
            newMsgChannel.enableVibration(true)

            adminApprovalChannel.lockscreenVisibility=Notification.VISIBILITY_PUBLIC
            newMsgChannel.lockscreenVisibility=Notification.VISIBILITY_PUBLIC
            manger.createNotificationChannel(newMsgChannel)
            manger.createNotificationChannel(adminApprovalChannel)

        }


    }


    fun setConnectivityListener(listener: ConnectivityReceiver.ConnectivityReceiverListener) {
        ConnectivityReceiver.connectivityReceiverListener = listener
    }

    companion object {
        @get:Synchronized
        var instance: AppClass? = null
            private set

    }
}
