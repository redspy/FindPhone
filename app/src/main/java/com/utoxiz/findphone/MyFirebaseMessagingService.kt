package com.utoxiz.findphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.utoxiz.findphone.Constants.Companion.BASE_URL
import com.utoxiz.findphone.Constants.Companion.CONTENT_TYPE
import com.utoxiz.findphone.Constants.Companion.SERVER_KEY
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


class Constants {
    companion object {
        const val BASE_URL = "https://fcm.googleapis.com/"
        const val SERVER_KEY = "AAAAt3OXB1A:APA91bEdwTCp2-1bKfqJAp6W-Vncrn8Jlvkz4yg9e-3h6olmpFYRd08Qcv-Cy4g6Ab5enfGv6xVCF6Y-WY-DisXGX13LEvesRGBqEOiFfKhf97xUmea9ml6vPPJACZ6ceQECYPNmD0Jb"
        const val CONTENT_TYPE = "application/json"
    }
}

private const val CHANNEL_ID = "my_channel"

interface NotificationAPI {

    @Headers("Authorization: key=$SERVER_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}

data class PushNotification(
    val data: NotificationData,
    var to: String
)

data class NotificationData(
    val title: String,
    val message: String,
    val tag: String
)

class RetrofitInstance {
    companion object {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        val api by lazy {
            retrofit.create(NotificationAPI::class.java)
        }
    }
}

class MyFirebaseMessagingService : FirebaseMessagingService() {
    /** 푸시 알림으로 보낼 수 있는 메세지는 2가지
     * 1. Notification: 앱이 실행중(포그라운드)일 떄만 푸시 알림이 옴
     * 2. Data: 실행중이거나 백그라운드(앱이 실행중이지 않을때) 알림이 옴 -> TODO: 대부분 사용하는 방식 */

    private val TAG = "FirebaseService"
    companion object {
        var sharedPref: SharedPreferences? = null

        var token: String?
            get() {
                return sharedPref?.getString("token", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("token", value)?.apply()
            }
    }
    /** Token 생성 메서드(FirebaseInstanceIdService 사라짐) */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    /** 메시지 수신 메서드(포그라운드) */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: " + remoteMessage!!.from)

        //받은 remoteMessage의 값 출력해보기. 데이터메세지 / 알림메세지
        Log.d(TAG, "Message data : ${remoteMessage.data}")
        Log.d(TAG, "Message noti : ${remoteMessage.notification}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "data is not Empty")
            Log.d(TAG, remoteMessage.data["title"].toString())
            Log.d(TAG, remoteMessage.data["body"].toString())
            sendNotificationData(remoteMessage)// 요기로 오고있음
        }
        else if (remoteMessage.notification != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.notification!!.body)
            sendNotification(remoteMessage)//.notification!!.body)
        }
        //onDeletedMessages()
    }
    private fun sendNotificationData(remoteMessage: RemoteMessage) {
        val uniId: Int = (System.currentTimeMillis() / 7).toInt()
        // 알림 채널 이름
        val channelId = "my_channel"
        // 알림 소리
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // 일회용 PendingIntent : Intent 의 실행 권한을 외부의 어플리케이션에게 위임
        val intent = Intent(this, MainActivity::class.java)
        //각 key, value 추가
        for(key in remoteMessage.data.keys){
            intent.putExtra(key, remoteMessage.data.getValue(key))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Activity Stack 을 경로만 남김(A-B-C-D-B => A-B)

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */,
            intent, PendingIntent.FLAG_IMMUTABLE
        )

        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume: Int = audio.getStreamVolume(AudioManager.STREAM_SYSTEM)

        val ringerMode = audio.ringerMode

        audio.setStreamVolume(AudioManager.STREAM_SYSTEM, 15, 1)
        audio.setStreamVolume(AudioManager.STREAM_RING, 15, 1)

        audio.playSoundEffect(AudioManager.FX_KEYPRESS_INVALID, 100.0f)

        val uriRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(this, uriRingtone)
        //ringtone.play()

        val zenModeValue = Settings.Global.getInt(contentResolver, "zen_mode")

        when (zenModeValue) {
            0 -> Log.d("TAG", "DnD:Off")
            1 -> Log.d("TAG", "DnD:ON-Priority Only")
            2 -> Log.d("TAG", "DnD:ON-Total Silence")
            3 -> Log.d("TAG", "DnD:ON-Alarms Only")
        }

        try {
            audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        catch(e: Exception) {
            callActivityForPolicyAccessSettings()
        }

        var messageType = remoteMessage.data["tag"]

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 아이콘 설정
            .setContentTitle(remoteMessage.data["title"]) // 제목
            .setContentText(remoteMessage.data["message"]) // 메시지 내용
            .setAutoCancel(true) // 알람클릭시 삭제여부
            .setSound(soundUri)  // 알림 소리
            .setContentIntent(pendingIntent) // 알림 실행 시 Intent

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 오레오 버전 이후에는 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notice", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성
        notificationManager.notify(uniId, notificationBuilder.build())
    }

    private fun callActivityForPolicyAccessSettings() {
        // Ask the user to grant access
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            //휴대폰이 방해금지 모드일때 오류 발생
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (notificationManager != null) && !notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (notificationManager != null) && notificationManager.isNotificationPolicyAccessGranted) {
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    if (audioManager != null) {
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /** 알림 생성 메서드 */
    private fun sendNotification(remoteMessage: RemoteMessage) {
        // RequestCode, Id를 고유값으로 지정하여 알림이 개별 표시
        val uniId: Int = (System.currentTimeMillis() / 7).toInt()

        // 일회용 PendingIntent : Intent 의 실행 권한을 외부의 어플리케이션에게 위임
        val intent = Intent(this, MainActivity::class.java)
        //각 key, value 추가
        for(key in remoteMessage.data.keys){
            intent.putExtra(key, remoteMessage.data.getValue(key))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Activity Stack 을 경로만 남김(A-B-C-D-B => A-B)

        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */,
            intent, PendingIntent.FLAG_IMMUTABLE
        )
        // 알림 채널 이름
        val channelId = "my_channel"
        // 알림 소리
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // 알림에 대한 UI 정보, 작업
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            //.setSmallIcon(androidx.core.R.drawable.notification_icon_background) // 아이콘 설정
            .setContentTitle(remoteMessage.notification?.title) // 제목
            .setContentText(remoteMessage.notification?.body) // 메시지 내용
            .setAutoCancel(true) // 알람클릭시 삭제여부
            .setSound(soundUri)  // 알림 소리
            .setContentIntent(pendingIntent) // 알림 실행 시 Intent

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 오레오 버전 이후에는 채널이 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Notice", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성
        notificationManager.notify(uniId, notificationBuilder.build())
    }

    /** Token 가져오기 */
    fun getFirebaseToken() {
        //비동기 방식
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d(TAG, "token=${it}")
        }
    }
}

/**
"notificationBuilder" 알림 생성시 여러가지 옵션을 이용해 커스텀 가능.
setSmallIcon : 작은 아이콘 (필수)
setContentTitle : 제목 (필수)
setContentText : 내용 (필수)
setColor : 알림내 앱 이름 색
setWhen : 받은 시간 커스텀 ( 기본 시스템에서 제공합니다 )
setShowWhen : 알림 수신 시간 ( default 값은 true, false시 숨길 수 있습니다 )
setOnlyAlertOnce : 알림 1회 수신 ( 동일 아이디의 알림을 처음 받았을때만 알린다, 상태바에 알림이 잔존하면 무음 )
setContentTitle : 제목
setContentText : 내용
setFullScreenIntent : 긴급 알림 ( 자세한 설명은 아래에서 설명합니다 )
setTimeoutAfter : 알림 자동 사라지기 ( 지정한 시간 후 수신된 알림이 사라집니다 )
setContentIntent : 알림 클릭시 이벤트 ( 지정하지 않으면 클릭했을때 아무 반응이 없고 setAutoCancel 또한 작동하지 않는다 )
setLargeIcon : 큰 아이콘 ( mipmap 에 있는 아이콘이 아닌 drawable 폴더에 있는 아이콘을 사용해야 합니다. )
setAutoCancel : 알림 클릭시 삭제 여부 ( true = 클릭시 삭제 , false = 클릭시 미삭제 )
setPriority : 알림의 중요도를 설정 ( 중요도에 따라 head up 알림으로 설정할 수 있는데 자세한 내용은 밑에서 설명하겠습니다. )
setVisibility : 잠금 화면내 알림 노출 여부
Notification.VISIBILITY_PRIVATE : 알림의 기본 정보만 노출 (제목, 타이틀 등등)
Notification.VISIBILITY_PUBLIC : 알림의 모든 정보 노출
Notification.VISIBILITY_SECRET : 알림의 모든 정보 비노출
 */