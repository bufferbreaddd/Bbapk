package dev.colorup

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder

/** Read-only bridge for the explicitly scoped system processes. */
class ConfigProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val allowed = setOf("com.android.systemui", "dev.colorup")
        val caller = callingPackage
            ?: throw SecurityException("Caller identity was not supplied")
        if (caller !in allowed) throw SecurityException("Caller is outside ColorUp's allowlist")
        val callerUid = context?.packageManager?.getPackageUid(caller, 0)
            ?: throw SecurityException("Caller package is unavailable")
        if (callerUid != Binder.getCallingUid()) throw SecurityException("Caller UID mismatch")
        if (uri.path != "/config") throw IllegalArgumentException("Unknown config endpoint")
        val preferences = context?.getSharedPreferences("colorup", android.content.Context.MODE_PRIVATE)
        if (caller != "dev.colorup") {
            preferences?.edit()?.apply {
                putString("last_runtime_seen_pkg", caller)
                putLong("last_runtime_seen_at", System.currentTimeMillis())
                uri.getQueryParameter("installed")?.let { report ->
                    putString("runtime_hooks", report.split(',').filter { it in AnimationCatalog.experimentalHookIds }.distinct().joinToString(","))
                    putString("runtime_version", uri.getQueryParameter("version")?.take(32) ?: "unknown")
                }
            }?.apply()
        }
        val json = preferences?.getString("config_json", null) ?: ConfigCodec.encode(AnimationCatalog.defaults)
        return MatrixCursor(arrayOf("json")).apply { addRow(arrayOf(json)) }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/vnd.dev.colorup.config"
    override fun insert(uri: Uri, values: ContentValues?) = throw UnsupportedOperationException("Read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")
}
