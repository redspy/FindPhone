package com.utoxiz.findphone

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
                        Button(onClick = { /*TODO*/ }) {

                        }
                    }

                }
            }
        }
    }
    private fun SendNotification() {
        val title = "Let's Go!"
        val message = "Together"
        val recipientToken =
            "eVh8cmA6TKyluftyVWOJ_j:APA91bFm2sUTNggIkmNw8ONpiQUbIic9F2EliKP9xa-4bjdHNLe0Ud4dxQxQXwZvxR9tZsynR_bW6JE8UfuSu00DTciGP1UtVjkGphUC3x1-jL9RRWKCcwLTrTxpUwdMdjEXFIRBR8Jd"
        if (title.isNotEmpty() && message.isNotEmpty() && recipientToken.isNotEmpty()) {
            PushNotification(
                NotificationData(title, message),
                recipientToken
            ).also {
                sendNotification(it)
            }
        }
    }
    fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d(ContentValues.TAG, "Response: ${Gson().toJson(response)}")
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
            Toast.makeText(this@MainActivity, dataStr, Toast.LENGTH_LONG).show()
            // binding.tvToken.text = dataStr
        }
    }
    private fun InitializeFirebase() {
        /** FCM설정, Token값 가져오기 */
        MyFirebaseMessagingService().getFirebaseToken()
        /** DynamicLink 수신확인 */
        initDynamicLink()
        val TOPIC = "/topics/myTopic2"
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