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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
            NotificationChannel(CHANNEL_ID, "Countdown", NotificationManager.IMPORTANCE_DEFAULT)

        channel.description = "Used to show the countdown."
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)


        Toast.makeText(this, "Notification enable: ${areNotificationsEnabled()}", LENGTH_SHORT)
            .show()
    }

    override fun onResume() {
        super.onResume()
        permissionState.activeState.value = DeviceManager(this).isActive()
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
                    text = "Please enable notification to make sure the app working properly."
                )
            }

            Button(onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                launcher.launch(intent)
            }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(text = "Enable Notification")
            }
        }


        Spacer(modifier = Modifier.height(100.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (!isActive) {

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
                Button(onClick = { deviceManager.enableDeviceManager() }) {
                    Text(text = stringResource(id = R.string.active_device_manager))
                }
            }
            var minutes by remember {
                mutableStateOf("")
            }
            if (isActive) {
                TextField(modifier = Modifier.width(IntrinsicSize.Max),
                    value = minutes,
                    label = { Text(text = stringResource(R.string.how_many)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = {
                        minutes = it
                    })

                Button(modifier = Modifier.width(100.dp), onClick = {
                    if (minutes.isNotEmpty() && minutes.isDigitsOnly()) {

                        val intent = Intent(context, CountdownService::class.java)
                        intent.putExtra(EXTRA_MINUTES, minutes.toInt())
                        context.startService(intent)

                        if (context is Activity) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.start_to_count_down),
                                LENGTH_SHORT
                            ).show()
                            context.finish()
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
