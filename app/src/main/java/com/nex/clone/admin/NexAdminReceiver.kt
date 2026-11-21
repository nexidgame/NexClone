package com.nex.clone.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.nex.clone.core.ProfileSetup

class NexAdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        ProfileSetup.run(context)
    }
}
