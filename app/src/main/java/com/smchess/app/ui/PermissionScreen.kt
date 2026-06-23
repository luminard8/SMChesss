package com.smchess.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.smchess.app.permissions.openAppSettings
import androidx.activity.ComponentActivity
import com.smchess.app.permissions.shouldShowRationale

@Composable
fun PermissionScreen(
    requestedAtLeastOnce: Boolean,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val permanentlyDenied = requestedAtLeastOnce && activity != null && !shouldShowRationale(activity)

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SMChess a besoin d'accéder à vos SMS et vos contacts",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "L'application affiche vos conversations et envoie les coups d'échecs par SMS, " +
                "sans connexion internet ni serveur. Vos contacts permettent d'afficher des noms " +
                "plutôt que des numéros.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        if (permanentlyDenied) {
            Text(
                "Les permissions ont été refusées définitivement. Ouvrez les réglages de l'application pour les activer manuellement.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { openAppSettings(context) }) {
                Text("Ouvrir les réglages")
            }
        } else {
            Button(onClick = onRequestPermissions) {
                Text("Autoriser")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRequestPermissions) {
            Text("Réessayer")
        }
    }
}
