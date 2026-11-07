package dev.colorup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors

/** Owns the transfer/result across recreation; the current Activity owns its dialog. */
class ProfileFilesModel(application: Application) : AndroidViewModel(application) {
    data class Result(val imported: MutableList<AnimationSpec>? = null, val error: String? = null)
    private val executor = Executors.newSingleThreadExecutor()
    private val mutableResult = MutableLiveData<Result?>()
    val result: LiveData<Result?> = mutableResult
    @Volatile var busy = false
        private set

    fun transfer(export: Boolean, uri: Uri, json: String) {
        if (busy) return
        busy = true
        val resolver = getApplication<Application>().contentResolver
        executor.execute {
            val outcome = runCatching {
                if (export) {
                    (resolver.openOutputStream(uri, "wt") ?: error("Cannot open output file")).use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Result()
                } else {
                    val bytes = (resolver.openInputStream(uri) ?: error("Cannot open input file")).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= ConfigCodec.MAX_IMPORT_BYTES) {
                            val count = input.read(buffer, 0, minOf(buffer.size, ConfigCodec.MAX_IMPORT_BYTES + 1 - output.size()))
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    require(bytes.size <= ConfigCodec.MAX_IMPORT_BYTES) { "Profile file is too large (maximum 256 KB)" }
                    Result(imported = ConfigCodec.decode(bytes.toString(Charsets.UTF_8)))
                }
            }.getOrElse { Result(error = it.message ?: "Unable to read profiles") }
            busy = false
            mutableResult.postValue(outcome)
        }
    }
    fun consumeResult() { mutableResult.value = null }
    override fun onCleared() { executor.shutdownNow() }
}
