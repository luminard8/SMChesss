package com.smchess.app.permissions

import android.util.Log
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

fun isDefaultSmsApp(context: Context): Boolean {
    val actual = Telephony.Sms.getDefaultSmsPackage(context)
    Log.d("SMChess", "Default SMS package = $actual, ours = ${context.packageName}")
    return actual == context.packageName
}

/** Construit l'Intent qui ouvre la demande "Définir comme appli SMS par défaut" */
fun buildRequestDefaultSmsAppIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
    } else {
        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        }
    }
}
