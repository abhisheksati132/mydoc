package com.abhisheksati.mydoc.security

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import java.util.concurrent.Executor

object BiometricLockHelper {

    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return keyguardManager?.isDeviceSecure == true
    }

    fun promptBiometricAuthentication(
        activity: ComponentActivity,
        confirmCredentialLauncher: ActivityResultLauncher<Intent>?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
            // If device has no screen lock, bypass
            onSuccess()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cancellationSignal = CancellationSignal()
                val executor: Executor = activity.mainExecutor

                val builder = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle("Unlock mydoc")
                    .setSubtitle("Authenticate to access your documents")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true)
                } else {
                    builder.setNegativeButton("Use PIN", executor) { _, _ ->
                        launchKeyguardFallback(activity, keyguardManager, confirmCredentialLauncher, onError)
                    }
                }

                val prompt = builder.build()
                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            // If biometric is not enrolled or hardware unavailable, fallback to Keyguard PIN
                            if (errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                                errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                                errorCode == android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                            ) {
                                launchKeyguardFallback(activity, keyguardManager, confirmCredentialLauncher, onError)
                            } else {
                                onError(errString?.toString() ?: "Authentication cancelled")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                        }
                    }
                )
                return
            } catch (e: Exception) {
                // Fall back to keyguard intent
                launchKeyguardFallback(activity, keyguardManager, confirmCredentialLauncher, onError)
            }
        } else {
            launchKeyguardFallback(activity, keyguardManager, confirmCredentialLauncher, onError)
        }
    }

    private fun launchKeyguardFallback(
        activity: Activity,
        keyguardManager: KeyguardManager,
        launcher: ActivityResultLauncher<Intent>?,
        onError: (String) -> Unit
    ) {
        @Suppress("DEPRECATION")
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
            "Unlock mydoc",
            "Enter your device PIN, pattern, or password"
        )
        if (intent != null && launcher != null) {
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                onError("Failed to launch security prompt: ${e.message}")
            }
        } else {
            onError("Authentication not supported on this device")
        }
    }
}
