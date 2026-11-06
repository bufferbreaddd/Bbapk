package dev.colorup

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.widget.*
import java.io.DataOutputStream
import java.util.Base64

class MainActivity : Activity() {
    private val specs = AnimationCatalog.defaults.map { it.copy() }.toMutableList()
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 20)
            setBackgroundColor(0xFF10131A.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(label("ColorUp", 30, true))
        content.addView(label("One UI 8.5 · independent animation studio", 14))
        status = label("Editor ready · transition hooks are not active in this build", 13)
        content.addView(status)
        specs.groupBy { it.group }.forEach { (group, items) ->
            content.addView(label(group.uppercase(), 13, true))
            items.forEach { content.addView(editor(it)) }
        }
        content.addView(Button(this).apply {
            text = "Save configuration"
            setOnClickListener { saveConfig() }
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun editor(spec: AnimationSpec): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xFF202633.toInt())
        }
        panel.addView(label(spec.title, 17, true))
        panel.addView(CheckBox(this).apply {
            text = "Enable (experimental)"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = spec.enabled
            setOnCheckedChangeListener { _, value -> spec.enabled = value }
        })
        panel.addView(label("Duration: ${spec.durationMs} ms", 13))
        panel.addView(SeekBar(this).apply {
            max = 1000
            progress = spec.durationMs
            setOnSeekBarChangeListener(listener { spec.durationMs = it })
        })
        panel.addView(label("Curve", 13))
        val curves = arrayOf("STANDARD", "LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT", "SPRING")
        panel.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, curves)
            setSelection(curves.indexOf(spec.curve).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    spec.curve = curves[position]
                }
            }
        })
        panel.addView(label("Scale: ${"%.2f".format(spec.scale)}", 13))
        panel.addView(SeekBar(this).apply {
            max = 200
            progress = (spec.scale * 100).toInt()
            setOnSeekBarChangeListener(listener { spec.scale = it / 100f })
        })
        panel.addView(label("Fade: ${"%.2f".format(spec.fade)}", 13))
        panel.addView(SeekBar(this).apply {
            max = 100
            progress = (spec.fade * 100).toInt()
            setOnSeekBarChangeListener(listener { spec.fade = it / 100f })
        })
        panel.addView(label("Vertical offset: ${spec.translateY} dp", 13))
        panel.addView(SeekBar(this).apply {
            max = 300
            progress = spec.translateY + 150
            setOnSeekBarChangeListener(listener { spec.translateY = it - 150 })
        })
        return panel.apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).also { it.bottomMargin = 12 }
        }
    }

    private fun listener(change: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
            if (fromUser) change(value)
        }
        override fun onStartTrackingTouch(bar: SeekBar?) = Unit
        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
    }

    private fun label(value: String, size: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(0xFFE7ECF4.toInt())
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 8, 0, 8)
    }

    private fun saveConfig() {
        val json = ConfigCodec.encode(specs)
        getPreferences(MODE_PRIVATE).edit().putString("config", json).apply()
        val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { out ->
                out.writeBytes("mkdir -p /data/adb/colorup\n")
                out.writeBytes("echo '$encoded' | base64 -d > /data/adb/colorup/config.json\n")
                out.writeBytes("chmod 0644 /data/adb/colorup/config.json\nexit\n")
                out.flush()
            }
            val result = process.waitFor()
            status.text = if (result == 0) "Config saved to /data/adb/colorup/config.json · hooks still inactive" else "Saved locally; root write failed (exit $result)"
        } catch (e: Exception) {
            status.text = "Saved locally; root access unavailable: ${e.javaClass.simpleName}"
        }
    }
}
