package com.jayc.countdownlocker

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jayc.countdownlocker.ui.theme.CountDownLockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CountDownLockerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val deviceManager = DeviceManager(LocalContext.current)
    var remain by remember {
        mutableStateOf(0)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello $name! Please active the device manager first!", modifier = modifier
        )
        Text(
            text = "Remain $remain!", modifier = modifier
        )
        Button(onClick = { deviceManager.enableDeviceManager() }) {
            Text(text = "Active Device Manager")
        }
        Button(onClick = { deviceManager.lockNow() }) {
            Text(text = "Lock Now")
        }
        Button(onClick = {
            object : CountDownTimer(180 * 1000, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    remain = (millisUntilFinished / 1000).toInt()
                }

                override fun onFinish() {
                    deviceManager.lockNow()
                }

            }.start()
        }) {
            Text(text = "Lock in 3 minutes")
        }

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CountDownLockerTheme {
        Greeting("Android")
    }
}

class DeviceManager(private val context: Context) {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val componentName = ComponentName(context, DeviceReceiver::class.java)

    fun enableDeviceManager() {

        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Active Device Manager")
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Already active.", Toast.LENGTH_SHORT).show()
        }
    }

    fun lockNow() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        } else {
            Toast.makeText(context, "Please active first!", Toast.LENGTH_SHORT).show()
        }
    }
}

class DeviceReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Active!", Toast.LENGTH_SHORT).show()
    }
}