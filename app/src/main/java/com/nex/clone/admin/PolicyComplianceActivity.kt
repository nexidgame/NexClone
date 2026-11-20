package com.nex.clone.admin

import android.app.Activity
import android.os.Bundle
import com.nex.clone.core.ProfileSetup

/** Android 10+: dipanggil di dalam profil baru untuk menyelesaikan penyiapan. */
class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfileSetup.run(this)
        setResult(RESULT_OK)
        finish()
    }
}
