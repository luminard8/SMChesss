package com.smchess.app.data

import android.content.Context
import android.provider.Telephony

object SmsRepository {

    /** Liste des conversations, triées par date du dernier message (plus récent en premier) */
    fun getConversations(context: Context): List<Conversation> {
        val messages = queryMessages(context)
        val byThread = messages.groupBy { it.threadId }
        val conversations = mutableListOf<Conversation>()
        for ((threadId, msgs) in byThread) {
            val last = msgs.maxByOrNull { it.date } ?: continue
            val address = msgs.firstOrNull { it.address.isNotBlank() }?.address ?: continue
            val name = ContactsHelper.resolveName(context, address) ?: address
            conversations.add(
                Conversation(
                    threadId = threadId,
                    address = address,
                    displayName = name,
                    lastMessage = if (com.smchess.app.chess.GameProtocol.isProtocolMessage(last.body)) "" else last.body,
                    lastDate = last.date
                )
            )
        }
        return conversations.sortedByDescending { it.lastDate }
    }

    /** Tous les messages d'une conversation (codes de jeu inclus), triés du plus ancien au plus récent */
    fun getMessagesForThread(context: Context, threadId: Long): List<SmsMessage> {
        return queryMessages(
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
