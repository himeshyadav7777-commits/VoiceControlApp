package com.voicecontrol.app

import android.content.Context
import android.provider.ContactsContract

object ContactHelper {

    fun findNumberByName(context: Context, spokenName: String): String? {
        val cleanedName = spokenName.trim()
        if (cleanedName.isEmpty()) return null

        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val contactName = it.getString(nameIndex) ?: continue
                if (contactName.contains(cleanedName, ignoreCase = true) ||
                    cleanedName.contains(contactName, ignoreCase = true)
                ) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }
}