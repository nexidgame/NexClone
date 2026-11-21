package com.nex.clone

import android.app.Application
import com.nex.clone.core.CloneStore
import com.nex.clone.core.Profiles
import com.nex.clone.core.ProfileSetup

class NexCloneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CloneStore.init(this)
        // Proses yang berjalan di dalam profil klon: aktifkan penerima perintah, sembunyikan layar utama.
        if (Profiles.inManagedProfile(this)) ProfileSetup.run(this)
    }
}
