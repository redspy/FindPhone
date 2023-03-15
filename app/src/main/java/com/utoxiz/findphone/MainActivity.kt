package com.utoxiz.findphone

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.utoxiz.findphone.ui.theme.FindPhoneTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val SERVER_KEY = "AAAAt3OXB1A:APA91bEdwTCp2-1bKfqJAp6W-Vncrn8Jlvkz4yg9e-3h6olmpFYRd08Qcv-Cy4g6Ab5enfGv6xVCF6Y-WY-DisXGX13LEvesRGBqEOiFfKhf97xUmea9ml6vPPJACZ6ceQECYPNmD0Jb"
const val MINSU_PHONE = "druk5fN-QMyrvRwOSH1urK:APA91bF1SkrYb4JvK9sXJtAa3lGAmc-H8rTjq1KVNOM6ioFVzyS_oq7cO_w9fDd9zVBWH39tmftkdlFS7k_0t4gymmxRcaDqXMp76qQMg4q9NP3Yx5hcDxApepCOcZ0RFnKzFoYVWV5h"
const val INSOOK_PHONE = "egsJhxMfSNutAuuVBO5y-7:APA91bHFb9zQZm2vUdRXLMiU6kJwBCy1SdydTNSMI_S7JvFnSfMytaP-30PvZzcJ5cLAVxIyO6Mpnm07ESmQpQC_XpOTlbn7UC2UN_8jDANjadU3NzGkge0IEOlC4mvgfFPyYHVqK27i"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitializeFirebase()

        setContent {
            FindPhoneTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column () {
                        Greeting("Android")
                        Button(onClick = {
                            SendNotification("Let's Go!", "Together", MINSU_PHONE)
                        }, modifier = Modifier.wrapContentSize()) {
                            Text(text = "민수폰을 찾아라!")
                        }
                        Button(onClick = {
                            SendNotification("Let's Go!", "Together", INSOOK_PHONE)
                        }, modifier = Modifier.wrapContentSize()) {
                            Text(text = "인숙이폰을 찾아라!")
                        }

                    }

                }
            }
        }
    }
    private fun SendNotification(title: String, message: String, recipientToken: String,) {
        if (title.isNotEmpty() && message.isNotEmpty() && recipientToken.isNotEmpty()) {
            PushNotification(
                NotificationData(title, message, "TAG"),
                recipientToken
            ).also {
                sendNotification(it)
            }
        }
    }
    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                 Log.d(ContentValues.TAG, response.isSuccessful.toString())
            } else {
                Log.e(ContentValues.TAG, response.errorBody().toString())
                //makeToast(response.errorBody().toString())
                Toast.makeText(this@MainActivity, response.errorBody().toString(), Toast.LENGTH_SHORT).show()
            }
        } catch(e: Exception) {
            Log.e(ContentValues.TAG, e.toString())
        }
    }
    private fun initDynamicLink() {
        val dynamicLinkData = intent.extras
        if (dynamicLinkData != null) {
            var dataStr = "DynamicLink 수신받은 값\n"
            for (key in dynamicLinkData.keySet()) {
                dataStr += "key: $key / value: ${dynamicLinkData.getString(key)}\n"
            }
            Toast.makeText(this@MainActivity, dataStr, Toast.LENGTH_SHORT).show()
            // binding.tvToken.text = dataStr
        }
    }
    private fun InitializeFirebase() {
        /** FCM설정, Token값 가져오기 */
        MyFirebaseMessagingService().getFirebaseToken()
        /** DynamicLink 수신확인 */
        initDynamicLink()
        val TOPIC = "minsuTopic"
        MyFirebaseMessagingService.sharedPref = getSharedPreferences("sharedPref", MODE_PRIVATE)
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FindPhoneTheme {
        Greeting("Android")
    }
}