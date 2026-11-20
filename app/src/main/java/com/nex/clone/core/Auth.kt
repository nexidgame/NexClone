package com.nex.clone.core

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Verifikasi sidik jari/wajah atau PIN/pola/sandi perangkat. */
object Auth {
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun available(a: FragmentActivity): Boolean =
        BiometricManager.from(a).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(a: FragmentActivity, title: String, onOk: () -> Unit, onFail: () -> Unit = {}) {
        val prompt = BiometricPrompt(
            a, ContextCompat.getMainExecutor(a),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail()
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
