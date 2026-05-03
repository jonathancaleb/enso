package com.example.enso

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.enso.di.AppModule
import com.example.enso.ui.navigation.AppNavigation
import com.example.enso.ui.theme.EnsoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.READ_SMS] == true) {
            triggerInitialSmsImport()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasSmsPermission()) {
            triggerInitialSmsImport()
        } else {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }

        setContent {
            EnsoTheme {
                AppNavigation()
            }
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun triggerInitialSmsImport() {
        val service = AppModule.provideSmsImportService(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            service.maybeRunInitialImport()
        }
    }
}
