package com.smchess.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Ne fait rien : présent uniquement parce qu'Android l'exige pour qu'une appli soit éligible comme appli SMS par défaut. SMChess ne gère pas les MMS. */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Pas de gestion MMS dans SMChess.
    }
}
