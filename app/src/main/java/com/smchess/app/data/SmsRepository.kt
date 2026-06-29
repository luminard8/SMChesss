package com.smchess.app.data

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsRepository {

    /** Liste des conversations, triées par date du dernier message (plus récent en premier) */
    suspend fun getConversations(context: Context): List<Conversation> = withContext(Dispatchers.IO) {
        val lastMessagePerThread = mutableMapOf<Long, SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = try {
            context.contentResolver.query(uri, projection, null, null, "${Telephony.Sms.DATE} DESC")
        } catch (e: SecurityException) {
            null
        } ?: return@withContext emptyList()

        cursor.use {
            val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

            // Comme on trie par DATE DESC, le premier message rencontré pour
            // chaque thread_id est forcément le plus récent : on peut s'arrêter
            // là pour ce thread et ignorer tous les suivants.
            while (it.moveToNext()) {
                val threadId = if (threadIdx >= 0) it.getLong(threadIdx) else 0L
                if (lastMessagePerThread.containsKey(threadId)) continue // déjà trouvé, on ignore

                val type = if (typeIdx >= 0) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                lastMessagePerThread[threadId] = SmsMessage(
                    id = 0L,
                    threadId = threadId,
                    address = if (addrIdx >= 0) it.getString(addrIdx) ?: "" else "",
                    body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else "",
                    date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L,
                    isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT
                )
            }
        }

        lastMessagePerThread.values.mapNotNull { last ->
            if (last.address.isBlank()) return@mapNotNull null
            val name = ContactsHelper.resolveName(context, last.address) ?: last.address
            Conversation(
                threadId = last.threadId,
                address = last.address,
                displayName = name,
                lastMessage = if (com.smchess.app.chess.GameProtocol.isProtocolMessage(last.body)) "" else last.body,
                lastDate = last.date
            )
        }.sortedByDescending { it.lastDate }
    }

    /** Tous les messages d'une conversation (codes de jeu inclus), triés du plus ancien au plus récent */
    suspend fun getMessagesForThread(context: Context, threadId: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        queryMessages(
            context,
            selection = "${Telephony.Sms.THREAD_ID} = ?",
            selectionArgs = arrayOf(threadId.toString())
        ).sortedBy { it.date }
    }

    private fun queryMessages(
        context: Context,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): List<SmsMessage> {
        val result = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val cursor = try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} DESC")
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()
        cursor.use {
            val idIdx = it.getColumnIndex(Telephony.Sms._ID)
            val threadIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
            while (it.moveToNext()) {
                val type = if (typeIdx >= 0) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                result.add(
                    SmsMessage(
                        id = if (idIdx >= 0) it.getLong(idIdx) else 0L,
                        threadId = if (threadIdx >= 0) it.getLong(threadIdx) else 0L,
                        address = if (addrIdx >= 0) it.getString(addrIdx) ?: "" else "",
                        body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else "",
                        date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L,
                        isOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT
                    )
                )
            }
        }
        return result
    }
}
