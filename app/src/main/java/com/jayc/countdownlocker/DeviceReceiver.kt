package com.jayc.countdownlocker

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)

        Toast.makeText(context, context.getString(R.string.active), Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)

        Toast.makeText(context, context.getString(R.string.deactivate), Toast.LENGTH_SHORT).show()
    }

}