package com.smchess.app.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactsHelper {

    fun resolveName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
        } catch (e: SecurityException) {
            null
        } ?: return null

        cursor.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }
}
