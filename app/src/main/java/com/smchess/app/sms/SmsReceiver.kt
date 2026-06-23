package com.smchess.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Compteur observable : incrémenté à chaque SMS reçu, observé par l'UI pour se rafraîchir automatiquement */
object SmsEvents {
    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick

    fun notifyChanged() {
        _tick.value = _tick.value + 1
    }
}

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Le SMS est déjà inséré dans le fournisseur de contenu par l'appli SMS par défaut du téléphone.
        // On se contente de signaler à l'UI qu'il faut recharger les conversations.
        SmsEvents.notifyChanged()
    }
}
