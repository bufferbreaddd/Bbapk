package dev.colorup

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.*
import android.widget.*
import java.io.DataOutputStream

class MainActivity : Activity() {
    private var specs = AnimationCatalog.defaults.map { it.copy() }.toMutableList()
    private lateinit var status: TextView
    private lateinit var content: LinearLayout
    private lateinit var editorContainer: LinearLayout
    private val prefs by lazy { getSharedPreferences("colorup", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val saved = prefs.getString("config_json", null)
        if (saved != null) runCatching { specs = ConfigCodec.decode(saved) }
        buildUi()
        val runtimePackage = prefs.getString("last_runtime_seen_pkg", null)
        status.text = if (runtimePackage == null) "No hook runtime heartbeat yet · preview and config editor are ready" else "Hook runtime last connected in " + runtimePackage
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 18)
            setBackgroundColor(0xFF10131A.toInt())
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("ColorUp", 30, true))
        content.addView(label("One UI 8.5 · animation studio", 14))
        status = label("18 transition profiles · settings are independent", 13)
        content.addView(status)
        content.addView(label("App-unlock duration and Quick Settings panel motion have experimental reference-derived hooks. Other controls are saved and previewable but do not change One UI yet.", 13))
        content.addView(label("PRESETS", 12, true))
        val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Stock", "Fluid", "Crisp", "Gentle").forEach { name ->
            presetRow.addView(Button(this).apply {
                text = name
                setOnClickListener {
                    specs = AnimationCatalog.preset(name)
                    renderEditors()
                    status.text = if (name == "Stock") "Stock settings restored" else "$name preset loaded · save to apply configuration"
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        content.addView(presetRow)
        val fileRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fileRow.addView(Button(this).apply { text = "Export"; setOnClickListener { exportConfig() } }, LinearLayout.LayoutParams(0, -2, 1f))
        fileRow.addView(Button(this).apply { text = "Import"; setOnClickListener { importConfig() } }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(fileRow)
        editorContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(editorContainer)
        renderEditors()
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun renderEditors() {
        editorContainer.removeAllViews()
        specs.groupBy { it.group }.forEach { (group, items) ->
            editorContainer.addView(label(group.uppercase(), 12, true))
            items.forEach { editorContainer.addView(editor(it)) }
        }
        editorContainer.addView(Button(this).apply { text = "Save configuration"; setOnClickListener { saveConfig() } })
    }

    private fun editor(spec: AnimationSpec): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xFF202633.toInt())
        }
        panel.addView(label(spec.title, 17, true))
        panel.addView(CheckBox(this).apply {
            text = when (spec.id) {
                "unlock_to_app" -> "Enable experimental app-unlock duration hook"
                "qs_expand" -> "Enable experimental Quick Settings transform"
                else -> "Not hooked yet · preview/config only"
            }
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = spec.enabled
            isEnabled = spec.id == "unlock_to_app" || spec.id == "qs_expand"
            setOnCheckedChangeListener { _, value -> spec.enabled = value }
        })
        panel.addView(seekControl("Duration", 2000, spec.durationMs, { v -> v.toString() + " ms" }) { spec.durationMs = it })
        panel.addView(label("Motion curve", 13))
        panel.addView(spinner(ConfigCodec.CURVES.toTypedArray(), spec.curve) { spec.curve = it })
        panel.addView(seekControl("Scale", 200, (spec.scale * 100).toInt(), { v -> "%.2f".format(v / 100f) + "×" }) { spec.scale = it / 100f })
        panel.addView(seekControl("Fade", 100, (spec.fade * 100).toInt(), { v -> "%.2f".format(v / 100f) }) { spec.fade = it / 100f })
        panel.addView(seekControl("Vertical offset", 300, spec.translateY + 150, { v -> (v - 150).toString() + " dp" }) { spec.translateY = it - 150 })
        panel.addView(seekControl("Start delay", 1000, spec.delayMs, { v -> v.toString() + " ms" }) { spec.delayMs = it })
        panel.addView(seekControl("Spring damping", 200, (spec.springDamping * 100).toInt(), { v -> "%.2f".format(v / 100f) }) { spec.springDamping = (it / 100f).coerceAtLeast(0.1f) })
        panel.addView(seekControl("Spring stiffness", 1500, spec.springStiffness.toInt(), { v -> v.toString() }) { spec.springStiffness = it.coerceAtLeast(50).toFloat() })
        panel.addView(label("Direction / anchor", 13))
        panel.addView(spinner(ConfigCodec.DIRECTIONS.toTypedArray(), spec.direction) { spec.direction = it })
        val preview = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xFF78A9FF.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)).apply { topMargin = dp(12); bottomMargin = dp(8) }
        }
        panel.addView(preview)
        panel.addView(Button(this).apply {
            text = "Preview motion"
            setOnClickListener { runPreview(preview, spec) }
        })
        return panel.apply { layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = dp(12) } }
    }

    private fun seekControl(title: String, maxValue: Int, initial: Int, format: (Int) -> String, changed: (Int) -> Unit): View {
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val valueLabel = label(title + ": " + format(initial), 13)
        wrapper.addView(valueLabel)
        wrapper.addView(SeekBar(this).apply {
            max = maxValue
            progress = initial.coerceIn(0, maxValue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    valueLabel.text = title + ": " + format(value)
                    if (fromUser) changed(value)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        })
        return wrapper
    }

    private fun spinner(items: Array<String>, selected: String, changed: (String) -> Unit) =
        Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(items.indexOf(selected).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { changed(items[position]) }
            }
        }

    private fun runPreview(view: View, spec: AnimationSpec) {
        view.animate().cancel()
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
        val distance = dp(spec.translateY).toFloat()
        val (x, y) = when (spec.direction) {
            "DOWN" -> 0f to -distance
            "LEFT" -> distance to 0f
            "RIGHT" -> -distance to 0f
            "CENTER" -> 0f to 0f
            else -> 0f to distance
        }
        val interpolator: android.view.animation.Interpolator = when (spec.curve) {
            "LINEAR" -> LinearInterpolator()
            "EASE_IN" -> AccelerateInterpolator()
            "EASE_OUT" -> DecelerateInterpolator()
            "EASE_IN_OUT", "STANDARD" -> AccelerateDecelerateInterpolator()
            "SPRING" -> OvershootInterpolator((1.0f - spec.springDamping).coerceIn(0.1f, 1.2f))
            else -> AccelerateDecelerateInterpolator()
        }
        view.translationX = x
        view.translationY = y
        view.scaleX = 0.75f
        view.scaleY = 0.75f
        view.alpha = 0f
        view.animate().translationX(0f).translationY(0f).scaleX(spec.scale).scaleY(spec.scale)
            .alpha(spec.fade).setStartDelay(spec.delayMs.toLong())
            .setDuration(spec.durationMs.toLong()).setInterpolator(interpolator)
            .withEndAction { view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start() }
            .start()
    }

    private fun saveConfig() {
        val json = ConfigCodec.encode(specs)
        prefs.edit().putString("config_json", json).putLong("updated_at", System.currentTimeMillis()).apply()
        contentResolver.notifyChange(Uri.parse("content://dev.colorup.config/config"), null)
        val encoded = android.util.Base64.encodeToString(json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { out ->
                out.writeBytes("mkdir -p /data/adb/colorup\n")
                out.writeBytes("echo '$encoded' | base64 -d > /data/adb/colorup/config.json\n")
                out.writeBytes("chmod 0644 /data/adb/colorup/config.json\nexit\n")
                out.flush()
            }
            val result = process.waitFor()
            status.text = if (result == 0) "Saved locally and exported to /data/adb/colorup/config.json" else "Saved locally · root export failed ($result)"
        } catch (e: Exception) {
            status.text = "Saved locally · root unavailable, config export skipped"
        }
    }

    private fun exportConfig() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "colorup-config.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private fun importConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT)
    }

    @Deprecated("Activity result API retained for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            when (requestCode) {
                REQUEST_EXPORT -> contentResolver.openOutputStream(uri)?.use { it.write(ConfigCodec.encode(specs).toByteArray(Charsets.UTF_8)) }
                REQUEST_IMPORT -> {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Unable to read file")
                    specs = ConfigCodec.decode(json)
                    renderEditors()
                    status.text = "Imported settings · save to keep them"
                }
            }
        } catch (e: Exception) {
            status.text = "File error: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun label(value: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(0xFFE7ECF4.toInt())
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 7, 0, 7)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_EXPORT = 501
        private const val REQUEST_IMPORT = 502
    }
}
