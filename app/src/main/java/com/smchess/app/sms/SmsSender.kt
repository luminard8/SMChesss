package com.smchess.app.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony
import android.telephony.SmsManager

object SmsSender {
    fun send(context: Context, address: String, body: String) {
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(body)
        if (parts.size > 1) {
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)
        } else {
            smsManager.sendTextMessage(address, null, body, null, null)
        }
        // Important : une appli qui n'est pas l'appli SMS par défaut du téléphone ne voit pas
        // ses messages envoyés automatiquement enregistrés dans l'historique. On les insère
        // nous-mêmes pour que la reconstitution de la partie depuis l'historique reste fiable.
        insertSentMessage(context, address, body)
    }

    private fun insertSentMessage(context: Context, address: String, body: String) {
        try {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, address)
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                put(Telephony.Sms.THREAD_ID, threadId)
                put(Telephony.Sms.READ, 1)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
        } catch (e: SecurityException) {
            // Si l'insertion échoue (restriction selon le fabricant du téléphone), le SMS part
            // tout de même : seul l'historique local de l'appli pourrait être incomplet.
        }
        SmsEvents.notifyChanged()
    }
}
