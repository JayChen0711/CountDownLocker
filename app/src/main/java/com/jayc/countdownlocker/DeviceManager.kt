package com.jayc.countdownlocker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceManager(private val context: Context) {
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val componentName = ComponentName(context, DeviceReceiver::class.java)

    fun isActive() = devicePolicyManager.isAdminActive(componentName)

    fun enableDeviceManager() {

        if (!devicePolicyManager.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.active_device_manager)
            )
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.already_active), Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun disableDeviceManager() {
        devicePolicyManager.removeActiveAdmin(componentName)
    }

    fun lockNow() {
        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.please_active_first),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}