package com.applebooks.android.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

object UriPermissionManager {

    fun takePersistablePermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Permission may not be grantable for all URIs
        }
    }

    fun releasePersistablePermission(contentResolver: ContentResolver, uri: Uri) {
        try {
            contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Already released or not held
        }
    }

    fun hasPermission(contentResolver: ContentResolver, uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }
}
