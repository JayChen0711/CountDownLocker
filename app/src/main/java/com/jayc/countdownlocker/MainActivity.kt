package com.jayc.countdownlocker

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.MutableLiveData
import com.jayc.countdownlocker.CountdownService.Companion.CHANNEL_ID
import com.jayc.countdownlocker.CountdownService.Companion.EXTRA_MINUTES
import com.jayc.countdownlocker.ui.theme.CountDownLockerTheme


class MainActivity : ComponentActivity() {

    private val permissionState = PermissionState(
        activeState = MutableLiveData(false),
        notificationState = MutableLiveData(false)
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CountDownLockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    permissionState.notificationState.value = areNotificationsEnabled()
                    if (permissionState.notificationState.value != true) {
                        RequestNotificationPermissions()
                    }
                    val launcher =
                        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                            permissionState.notificationState.value = areNotificationsEnabled()
                        }

                    MainInterface(permissionState, launcher)

                }
            }
        }
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

        channel.description = getString(R.string.notification_channel_description)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onResume() {
        super.onResume()
        permissionState.activeState.value = DeviceManager(this).isActive()
        permissionState.notificationState.value = areNotificationsEnabled()
    }

    fun startCountdownService(minutes: Int) {
        val intent = Intent(this, CountdownService::class.java)
        intent.putExtra(EXTRA_MINUTES, minutes)
        startService(intent)

        Toast.makeText(this, getString(R.string.start_to_count_down), LENGTH_SHORT).show()
        finish()
    }
}

class PermissionState(
    val activeState: MutableLiveData<Boolean>,
    val notificationState: MutableLiveData<Boolean>
)

fun Context.areNotificationsEnabled(): Boolean {
    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.areNotificationsEnabled()
}

@Preview
@Composable
fun QuickButton(minutes: Int = 1) {
    val context = LocalContext.current
    Button(onClick = {
        if (context is MainActivity) {
            context.startCountdownService(minutes)
        }
    }) {
        Text(text = stringResource(id = R.string.minutes, minutes))
    }
}

@Preview(showBackground = true)
@Composable
fun ActiveDeviceAdminPreview() {
    Column {
        ActiveDeviceAdmin(activeClick = {})
    }
}

@Composable
fun ActiveDeviceAdmin(activeClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.please_active),
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { activeClick.invoke() }) {
            Text(text = stringResource(id = R.string.active_device_manager))
        }
//        Button(onClick = { }) {
//            Text(text = stringResource(id = R.string.more_info))
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainInterface(
    permissionState: PermissionState,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    modifier: Modifier = Modifier
) {
    val deviceManager = DeviceManager(LocalContext.current)
    val context = LocalContext.current

    val isActive: Boolean by permissionState.activeState.observeAsState(deviceManager.isActive())
    val isNotificationEnabled: Boolean by permissionState.notificationState.observeAsState(context.areNotificationsEnabled())
    val openAlertDialog = remember { mutableStateOf(false) }

    Column {

        TopAppBar(
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary
            ), title = { Text(stringResource(R.string.app_name)) }, actions = {
                if (isActive)
                    IconButton(onClick = { openAlertDialog.value = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description"
                        )
                    }
            })

        if (!isNotificationEnabled) {
            Card(modifier = Modifier.padding(16.dp)) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.enable_notification_suggestion),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                launcher.launch(intent)
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = stringResource(R.string.enable_notification))
            }
        }



        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!isActive) {

                Spacer(modifier = Modifier.height(100.dp))

                ActiveDeviceAdmin(activeClick = { deviceManager.enableDeviceManager() })

            } else {
                var minutes by remember {
                    mutableStateOf("")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        modifier = Modifier.absolutePadding(right = 16.dp),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        text = "Quick"
                    )
                    QuickButton(minutes = 3)
                    QuickButton(minutes = 5)
                    QuickButton(minutes = 10)
                }


                TextField(modifier = Modifier.width(IntrinsicSize.Max),
                    value = minutes,
                    label = { Text(text = stringResource(R.string.how_many)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        minutes = it
                    })

                Button(modifier = Modifier.width(100.dp), onClick = {
                    if (minutes.isNotEmpty() && minutes.isDigitsOnly()) {

                        if (context is MainActivity) {
                            context.startCountdownService(minutes.toInt())
                        }
                    } else {
                        if (context is Activity) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.how_many),
                                LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Text(
                        text = stringResource(R.string.start)
                    )
                }
            }

        }

        when {
            openAlertDialog.value -> AlertDialog(
                icon = { Icon(Icons.Default.Delete, contentDescription = "Deactivate Icon") },
                title = { Text(text = stringResource(R.string.deactivate_device_manager)) },
                text = { Text(text = stringResource(R.string.deactivate_content)) },
                onDismissRequest = {
                    openAlertDialog.value = !openAlertDialog.value
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deviceManager.disableDeviceManager()
                            permissionState.activeState.value = false
                            openAlertDialog.value = !openAlertDialog.value
                        }
                    ) {
                        Text(stringResource(id = R.string.deactivate_device_manager))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { openAlertDialog.value = !openAlertDialog.value }
                    ) {
                        Text(stringResource(id = R.string.dismiss))
                    }
                }
            )
        }
    }
}
