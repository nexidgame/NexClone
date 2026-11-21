package com.nex.clone.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nex.clone.core.ProfileSetup
import com.nex.clone.core.Profiles

/** Setelah APK diperbarui, perbaiki penyiapan di profil kerja yang sudah ada (izin lintas profil, komponen). */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Profiles.inManagedProfile(context)) ProfileSetup.run(context)
    }
}
