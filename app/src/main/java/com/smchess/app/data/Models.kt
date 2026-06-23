package com.smchess.app.data

data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val isOutgoing: Boolean
)

data class Conversation(
    val threadId: Long,
    val address: String,
    val displayName: String,
    val lastMessage: String,
    val lastDate: Long
)
