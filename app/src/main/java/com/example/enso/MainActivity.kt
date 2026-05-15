package com.example.enso

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.enso.di.AppModule
import com.example.enso.ui.navigation.AppNavigation
import com.example.enso.ui.theme.EnsoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val missingSmsPermissions = mutableStateOf<Set<String>>(emptySet())

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val missing = REQUIRED_SMS_PERMISSIONS
            .filter { results[it] != true && !isPermissionGranted(it) }
            .toSet()

        missingSmsPermissions.value = missing
        if (missing.isEmpty()) {
            triggerInitialSmsImport()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasSmsPermissions()) {
            triggerInitialSmsImport()
        } else {
            smsPermissionLauncher.launch(REQUIRED_SMS_PERMISSIONS)
        }

        setContent {
            val missingPermissions by missingSmsPermissions
            EnsoTheme {
                if (missingPermissions.isEmpty()) {
                    AppNavigation()
                } else {
                    SmsPermissionRequiredScreen(
                        missingPermissions = missingPermissions,
                        onOpenSettings = ::openAppSettings
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasSmsPermissions() && missingSmsPermissions.value.isNotEmpty()) {
            missingSmsPermissions.value = emptySet()
            triggerInitialSmsImport()
        }
    }

    private fun hasSmsPermissions(): Boolean {
        return REQUIRED_SMS_PERMISSIONS.all(::isPermissionGranted)
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun triggerInitialSmsImport() {
        val service = AppModule.provideSmsImportService(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            service.maybeRunInitialImport()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    companion object {
        private val REQUIRED_SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }
}

@Composable
private fun SmsPermissionRequiredScreen(
    missingPermissions: Set<String>,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "SMS access is required",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Enso needs SMS read and receive permissions to import mobile money confirmations.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Missing: ${missingPermissions.joinToString { it.toPermissionLabel() }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open App Settings")
            }
        }
    }
}

private fun String.toPermissionLabel(): String {
    return when (this) {
        Manifest.permission.READ_SMS -> "Read SMS"
        Manifest.permission.RECEIVE_SMS -> "Receive SMS"
        else -> this
    }
}
