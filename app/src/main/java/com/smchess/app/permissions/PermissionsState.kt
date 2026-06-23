package com.smchess.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.READ_SMS,
    Manifest.permission.SEND_SMS,
    Manifest.permission.RECEIVE_SMS,
    Manifest.permission.READ_CONTACTS
)

fun hasAllPermissions(context: Context): Boolean =
    REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

class PermissionsState(initialGranted: Boolean) {
    var allGranted by mutableStateOf(initialGranted)
    var requestedAtLeastOnce by mutableStateOf(false)
}

/**
 * Garde l'état des permissions à jour en permanence :
 * - se met à jour quand l'utilisateur répond à la demande de permission
 * - se RE-vérifie automatiquement quand l'app revient au premier plan (ex: retour des Réglages système),
 *   ce qui évite le bug classique où l'app ne détecte pas qu'une permission a été accordée
 *   tant qu'elle n'est pas relancée manuellement.
 */
@Composable
fun rememberPermissionsState(): Pair<PermissionsState, () -> Unit> {
    val context = LocalContext.current
    val state = remember { PermissionsState(hasAllPermissions(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        state.requestedAtLeastOnce = true
        state.allGranted = result.values.all { it }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.allGranted = hasAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestPermissions = {
        state.requestedAtLeastOnce = true
        launcher.launch(REQUIRED_PERMISSIONS)
    }

    return Pair(state, requestPermissions)
}

fun shouldShowRationale(activity: Activity): Boolean =
    REQUIRED_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED &&
            activity.shouldShowRequestPermissionRationale(it)
    }
