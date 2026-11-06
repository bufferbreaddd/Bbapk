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
        val allowed = setOf("com.android.systemui", "com.sec.android.app.launcher", "dev.colorup")
        val packages = context?.packageManager?.getPackagesForUid(Binder.getCallingUid())?.toSet().orEmpty()
        val caller = packages.firstOrNull { it in allowed }
            ?: throw SecurityException("Caller is outside ColorUp's allowlist")
        if (uri.path != "/config") throw IllegalArgumentException("Unknown config endpoint")
        val preferences = context?.getSharedPreferences("colorup", android.content.Context.MODE_PRIVATE)
        if (caller != "dev.colorup") {
            preferences?.edit()?.putString("last_runtime_seen_pkg", caller)?.putLong("last_runtime_seen_at", System.currentTimeMillis())?.apply()
        }
        val json = preferences?.getString("config_json", null) ?: ConfigCodec.encode(AnimationCatalog.defaults)
        return MatrixCursor(arrayOf("json")).apply { addRow(arrayOf(json)) }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/vnd.dev.colorup.config"
    override fun insert(uri: Uri, values: ContentValues?) = throw UnsupportedOperationException("Read only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException("Read only")
}
