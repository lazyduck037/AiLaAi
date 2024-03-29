package com.queatz.ailaai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.concurrent.Flow

val push = Push()

class Push {

    private lateinit var context: Context
    var navController: NavController? = null

    private val latestMessageFlow = MutableStateFlow<String?>(null)
    val latestMessage: StateFlow<String?> = latestMessageFlow

    fun receive(data: Map<String, String>) {
        if (!data.containsKey("action")) {
            Log.w("PUSH", "Push notification does not contain 'action'")
            return
        }

        Log.w("PUSH", "Got push: ${data["action"]}")

        try {
            when (PushAction.valueOf(data["action"]!!)) {
                PushAction.Message -> receive(gson.fromJson(data["data"]!!, MessagePushData::class.java)!!)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun receive(data: MessagePushData) {
        if (
            navController?.currentBackStackEntry?.destination?.route == "group/{id}" &&
            navController?.currentBackStackEntry?.arguments?.getString("id") == data.group.id
        ) {
            latestMessageFlow.tryEmit(data.group.id)
            return
        }

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "${appDomain}/group/${data.group.id}".toUri(),
            context,
            MainActivity::class.java
        )

        val builder = NotificationCompat.Builder(context, "messages")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(data.person.name)
            .setContentText(data.message.text)
            .setGroup(data.group.id)
            .setAutoCancel(true)
            .setContentIntent(TaskStackBuilder.create(context).run {
                    addNextIntentWithParentStack(deepLinkIntent)
                    getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                }
            )

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        notificationManager.notify("group/${data.group.id}", 1, builder.build())
    }

    fun init(context: Context) {
        this.context = context

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "messages",
                context.getString(R.string.messages),
                // Change importance
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationChannel.description = context.getString(R.string.notification_channel_description)

            val notificationManager = context.getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

enum class PushAction {
    Message
}

data class MessagePushData(
    val group: Group,
    val person: Person,
    val message: Message
)
