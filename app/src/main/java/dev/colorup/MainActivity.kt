package dev.colorup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("colorup", MODE_PRIVATE) }
    private var specs = AnimationCatalog.defaults.map { it.copy() }.toMutableList()
    private var savedJson = ""
    private var tab = R.id.nav_overview
    private var editingId: String? = null
    private var category = "All"
    private var search = ""
    private lateinit var root: LinearLayout
    private lateinit var page: LinearLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var navigation: BottomNavigationView
    private lateinit var save: MaterialButton
    private lateinit var saveState: TextView
    private val files by lazy { ViewModelProvider(this)[ProfileFilesModel::class.java] }
    private val previews = mutableListOf<View>()
    private val exportDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) handleFile(REQUEST_EXPORT, uri)
    }
    private val importDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) handleFile(REQUEST_IMPORT, uri)
    }
    private val dirty get() = ConfigCodec.encode(specs) != savedJson

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
        super.onCreate(savedInstanceState)
        if (prefs.getBoolean("dynamic_color", true)) DynamicColors.applyToActivityIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val stored = prefs.getString("config_json", null)
        specs = stored?.let { runCatching { ConfigCodec.decode(it) }.getOrNull() }
            ?: AnimationCatalog.defaults.map { it.copy() }.toMutableList()
        savedJson = ConfigCodec.encode(specs)
        val draft = savedInstanceState?.getString("draft") ?: prefs.getString("draft_json", null)
        if (draft != null) runCatching { specs = ConfigCodec.decode(draft) }
        tab = savedInstanceState?.getInt("tab", tab) ?: tab
        editingId = savedInstanceState?.getString("editing")
        category = savedInstanceState?.getString("category") ?: "All"
        search = savedInstanceState?.getString("search") ?: ""
        buildShell()
        renderPage()
        files.result.observe(this) { result ->
            if (result != null) showFileResult(result)
        }
        if (stored != null && runCatching { ConfigCodec.decode(stored) }.isFailure) {
            message("Stored settings could not be read. Hooks remain off until you save a valid profile.")
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editingId != null) { editingId = null; renderPage() }
                else if (tab != R.id.nav_overview) { navigation.selectedItemId = R.id.nav_overview }
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
    }

    private fun buildShell() {
        root = column().apply { setBackgroundColor(tone(com.google.android.material.R.attr.colorSurface)) }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            WindowInsetsCompat.CONSUMED
        }
        toolbar = MaterialToolbar(this).apply {
            title = "ColorUp"
            setTitleTextColor(tone(com.google.android.material.R.attr.colorOnSurface))
            setNavigationIconTint(tone(com.google.android.material.R.attr.colorOnSurface))
            setNavigationOnClickListener { editingId = null; renderPage() }
        }
        root.addView(toolbar)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        page = column().apply { setPadding(dp(20), dp(8), dp(20), dp(24)) }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val saveBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(8), dp(20), dp(8))
        }
        saveState = textLabel("", 13, secondary = true)
        saveBar.addView(saveState, LinearLayout.LayoutParams(0, -2, 1f))
        save = button("Save changes") { saveConfig() }.apply { id = R.id.save_config }
        saveBar.addView(save)
        root.addView(saveBar)
        navigation = BottomNavigationView(this).apply {
            labelVisibilityMode = com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED
            menu.add(0, R.id.nav_overview, 0, "Overview").setIcon(R.drawable.ic_overview)
            menu.add(0, R.id.nav_motion, 1, "Motion").setIcon(R.drawable.ic_motion)
            menu.add(0, R.id.nav_presets, 2, "Presets").setIcon(R.drawable.ic_presets)
            menu.add(0, R.id.nav_setup, 3, "Setup").setIcon(R.drawable.ic_setup)
            selectedItemId = tab
            setOnItemSelectedListener { item ->
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0)
                tab = item.itemId; editingId = null; renderPage(); true
            }
        }
        root.addView(navigation)
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        val light = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, root).apply { isAppearanceLightStatusBars = light; isAppearanceLightNavigationBars = light }
    }

    private fun renderPage() {
        previews.clear()
        page.removeAllViews()
        (page.parent as? ScrollView)?.scrollTo(0, 0)
        val editing = specs.firstOrNull { it.id == editingId }
        toolbar.title = editing?.title ?: "ColorUp"
        toolbar.subtitle = if (editing == null) "ANIMATION STUDIO · 2.0" else editing.group
        toolbar.navigationIcon = if (editing == null) null else androidx.appcompat.content.res.AppCompatResources.getDrawable(this, R.drawable.ic_back)
        toolbar.navigationContentDescription = "Back to transitions"
        if (editing != null) showEditor(editing)
        else when (tab) {
            R.id.nav_motion -> showMotion()
            R.id.nav_presets -> showPresets()
            R.id.nav_setup -> showSetup()
            else -> showOverview()
        }
        updateSaveState()
    }

    private fun heading(title: String, subtitle: String) {
        page.addView(textLabel(title, 32, true))
        page.addView(textLabel(subtitle, 14, secondary = true).apply { setPadding(0, dp(4), 0, dp(20)) })
    }

    private fun showOverview() {
        heading("Make every\nmove yours.", "Your motion, at your pace. Built for One UI.")
        page.addView(surface {
            addView(textLabel("YOUR MOTION", 11, true, true))
            addView(textLabel("${specs.count { it.enabled }} transitions enabled", 24, true))
            addView(textLabel("18 profiles · 5 categories · 11 experimental hooks", secondary = true))
            addView(button("Explore transitions") { navigation.selectedItemId = R.id.nav_motion })
        }.apply { setCardBackgroundColor(tone(com.google.android.material.R.attr.colorPrimaryContainer)) })
        page.addView(surface {
            addView(textLabel("Module connection", 18, true))
            addView(textLabel(connectionSummary(), secondary = true))
            addView(button("Setup & diagnostics", outlined = true) { navigation.selectedItemId = R.id.nav_setup })
        })
        page.addView(textLabel("Jump into a category", 19, true))
        specs.groupBy { it.group }.forEach { (name, items) ->
            page.addView(surface {
                addView(textLabel(name, 17, true))
                addView(textLabel("${items.size} transitions · ${items.count { it.enabled }} enabled", secondary = true))
                addView(button("Edit $name", outlined = true) {
                    category = name; search = ""; navigation.selectedItemId = R.id.nav_motion
                })
            })
        }
        page.addView(surface {
            addView(textLabel("Return to stock motion", 18, true))
            addView(textLabel("Turn every hook off immediately. Your custom values stay saved.", secondary = true))
            addView(button("Disable all hooks", outlined = true) { disableAll() })
        })
    }

    private fun showMotion() {
        heading("Motion library", "Choose a transition, then shape its feel.")
        val searchBox = TextInputLayout(this).apply { hint = "Search transitions" }
        val input = TextInputEditText(searchBox.context).apply {
            setSingleLine(true)
            setText(search)
            contentDescription = "Search transitions"
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        }
        searchBox.addView(input)
        page.addView(searchBox)
        val chips = ChipGroup(this).apply { isSingleSelection = true; isSelectionRequired = true; isSingleLine = true }
        (listOf("All") + specs.map { it.group }.distinct()).forEach { name ->
            chips.addView(Chip(this).apply {
                text = name; isCheckable = true; isChecked = category == name
                setOnClickListener { category = name; renderTransitionList() }
            })
        }
        page.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        val list = column().apply { tag = "transition_list" }
        page.addView(list)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { search = s.toString(); renderTransitionList() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        renderTransitionList()
    }

    private fun renderTransitionList() {
        val list = page.findViewWithTag<LinearLayout>("transition_list") ?: return
        list.removeAllViews()
        val visible = specs.filter { (category == "All" || it.group == category) && "${it.title} ${it.group}".contains(search, true) }
        if (visible.isEmpty()) list.addView(textLabel("No matching transitions. Try another search or category.", secondary = true))
        visible.groupBy { it.group }.forEach { (group, items) ->
            list.addView(textLabel(group, 18, true))
            items.forEach { spec ->
                list.addView(surface {
                    addView(textLabel(spec.title, 18, true))
                    addView(textLabel(HookCapabilities.description(spec.id), 12, secondary = true))
                    addView(textLabel(if (spec.enabled) "Enabled · ${spec.durationMs} ms" else "${spec.durationMs} ms · ${friendly(spec.curve)}", secondary = true))
                    contentDescription = "${spec.title}. ${HookCapabilities.description(spec.id)}. Tap to edit."
                }.apply {
                    isClickable = true; isFocusable = true
                    setOnClickListener { editingId = spec.id; renderPage() }
                })
            }
        }
    }

    private fun showEditor(spec: AnimationSpec) {
        page.addView(textLabel(HookCapabilities.description(spec.id), 14, true))
        page.addView(textLabel(HookCapabilities.explanation(spec.id), secondary = true))
        page.addView(surface {
            addView(MaterialSwitch(this@MainActivity).apply {
                tag = "enable_transition"
                text = "Enable transition"
                minHeight = dp(56)
                isChecked = spec.enabled
                isEnabled = spec.id in AnimationCatalog.experimentalHookIds
                setOnCheckedChangeListener { _, checked -> spec.enabled = checked; changed() }
            })
        })
        val previewCard = surface {
            addView(textLabel("Try the feel", 18, true))
            addView(textLabel("Preview illustrates the full profile. See the support note above for system effects.", 12, secondary = true))
            val preview = MotionPreview(this@MainActivity) { spec }
            addView(preview, LinearLayout.LayoutParams(-1, dp(180)))
            previews.add(preview)
            addView(button("Play preview", outlined = true) { preview.play() })
        }
        val durationOnly = spec.id in HookCapabilities.durationOnly
        val panel = spec.id in HookCapabilities.panels
        val pending = spec.id !in AnimationCatalog.experimentalHookIds
        if (!panel) {
            page.addView(surface {
                addView(textLabel(if (pending) "Preview timing" else "Timing", 19, true))
                addView(slider("Duration", "duration_slider", spec.durationMs.toFloat(), 0f, 2000f, 10f, { "${it.toInt()} ms" }) { spec.durationMs = it.toInt() })
                if (!durationOnly) addView(slider("Start delay", "delay_slider", spec.delayMs.toFloat(), 0f, 1000f, 10f, { "${it.toInt()} ms" }) { spec.delayMs = it.toInt() })
            })
        }
        page.addView(previewCard)
        val motionControls = column().apply {
            addView(textLabel(if (durationOnly) "Preview motion" else "Motion shape", 19, true))
            if (durationOnly) {
                addView(textLabel("These controls affect the preview only.", 12, secondary = true))
                addView(slider("Preview start delay", "delay_slider", spec.delayMs.toFloat(), 0f, 1000f, 10f, { "${it.toInt()} ms" }) { spec.delayMs = it.toInt() })
            }
            addView(textLabel("Motion curve", 14, true))
            addView(choice(ConfigCodec.CURVES.toList(), spec.curve) { spec.curve = it; changed() })
            val graph = CurvePreview(this@MainActivity) { spec }
            addView(graph, LinearLayout.LayoutParams(-1, dp(100))); previews.add(graph)
            addView(slider("Start scale", "scale_slider", spec.scale, 0f, 2f, 0.01f, { "%.2f×".format(it) }) { spec.scale = it })
            if (!panel) addView(slider("Fade amount", "fade_slider", spec.fade, 0f, 1f, 0.01f, { "${(it * 100).toInt()}%" }) { spec.fade = it })
            addView(slider("Vertical offset", "offset_slider", spec.translateY.toFloat(), -150f, 150f, 1f, { "${it.toInt()} dp" }) { spec.translateY = it.toInt() })
            addView(textLabel("Scale anchor", 14, true))
            addView(choice(ConfigCodec.DIRECTIONS.toList(), spec.direction) { spec.direction = it; changed() })
            addView(textLabel("Spring response", 17, true))
            addView(textLabel("Damping and stiffness apply when Spring is selected.", 12, secondary = true))
            addView(slider("Damping", "damping_slider", spec.springDamping, 0.1f, 2f, 0.01f, { "%.2f".format(it) }) { spec.springDamping = it })
            addView(slider("Stiffness", "stiffness_slider", spec.springStiffness, 50f, 1500f, 10f, { "${it.toInt()}" }) { spec.springStiffness = it })
        }
        page.addView(surface {
            if (durationOnly) {
                motionControls.visibility = View.GONE
                addView(button("Show preview controls", outlined = true) {
                    motionControls.visibility = if (motionControls.visibility == View.GONE) View.VISIBLE else View.GONE
                })
            }
            addView(motionControls)
        })
        page.addView(button("Reset this transition", outlined = true) {
            MaterialAlertDialogBuilder(this).setTitle("Reset ${spec.title}?")
                .setMessage("Restore its default values and turn it off. Save changes to apply.")
                .setNegativeButton("Cancel", null).setPositiveButton("Reset") { _, _ ->
                    val index = specs.indexOfFirst { it.id == spec.id }
                    specs[index] = AnimationCatalog.defaults.first { it.id == spec.id }.copy()
                    renderPage()
                }.show()
        })
    }

    private fun showPresets() {
        heading("Find your rhythm", "A starting point for your own motion style.")
        listOf("Fluid" to "Longer, expressive spring motion.", "Crisp" to "Quick, direct and responsive.", "Gentle" to "A softer app-unlock duration.", "Stock" to "Disable every hook and restore default values.").forEach { (name, description) ->
            page.addView(surface {
                addView(textLabel(name, 24, true))
                addView(textLabel(description, secondary = true))
                val preset = AnimationCatalog.preset(name)
                addView(textLabel("${preset.count { it.enabled }} experimental transitions · ${preset[1].durationMs} ms unlock", 12, secondary = true))
                addView(button("Load $name", outlined = name != "Fluid") {
                    MaterialAlertDialogBuilder(this@MainActivity).setTitle("Load $name preset?")
                        .setMessage("This replaces all 18 profiles in your draft. Save changes to apply it to the module.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Load preset") { _, _ ->
                            specs = AnimationCatalog.preset(name); updateSaveState(); message("$name loaded. Save changes to apply.")
                        }.show()
                })
            })
        }
        page.addView(textLabel("Your profiles", 20, true))
        page.addView(button("Export profiles", outlined = true) { exportConfig() })
        page.addView(button("Import profiles", outlined = true) { importConfig() })
        page.addView(textLabel("Imports are validated before replacing your draft. Existing ColorUp backups are supported.", 13, secondary = true))
    }

    private fun showSetup() {
        heading("Ready to move", "Connect the module, then tune one transition at a time.")
        page.addView(surface {
            addView(textLabel("1   Enable ColorUp", 19, true))
            addView(textLabel("In your LSPosed or Vector manager, enable ColorUp in Modules. Installing the APK or granting root alone does not enable hooks.", secondary = true))
            addView(button("Open framework manager", outlined = true) { openManager() })
        })
        page.addView(surface {
            addView(textLabel("2   Select the scope", 19, true))
            addView(textLabel("System UI", 18, true))
            addView(textLabel("com.android.systemui"))
            addView(textLabel("Required for the implemented lock screen, shade, Quick Settings and notification hooks. The module recommends this scope automatically; confirm it in your manager.", secondary = true))
            addView(textLabel("Samsung One UI Home", 17, true))
            addView(textLabel("com.sec.android.app.launcher", 12, secondary = true))
            addView(textLabel("Pending: Launcher, home, app launch and recents hooks are not implemented. Launcher and Android System Framework are not required scopes in this version.", secondary = true))
            addView(button("Copy required scope", outlined = true) { copy("Required ColorUp scope", "com.android.systemui") })
        })
        page.addView(surface {
            addView(textLabel("3   Restart & connect", 19, true))
            addView(textLabel("Reboot after enabling or updating the module. Open ColorUp once after installation, enable a transition and save. Later saves refresh the running hooks automatically.", secondary = true))
            addView(textLabel(connectionSummary(), 14, true))
            addView(button("Refresh connection", outlined = true) { renderPage() })
        })
        page.addView(surface {
            addView(textLabel("Device & diagnostics", 19, true))
            addView(textLabel("${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}\nBuild ${Build.DISPLAY}", secondary = true))
            addView(textLabel("Target: Galaxy A35 5G · One UI 8.5. A connection confirms the bridge works; it does not prove a Samsung animation matches its hook.", 13, secondary = true))
            val installed = prefs.getString("runtime_hooks", "") ?: ""
            val reportedVersion = prefs.getString("runtime_version", "unknown")
            addView(textLabel("Last runtime: $reportedVersion\nInstalled candidates: ${installed.split(',').count { it.isNotBlank() }} of 11", secondary = true))
            addView(button("Copy diagnostic report", outlined = true) { copy("ColorUp diagnostics", diagnosticReport()) })
        })
        page.addView(surface {
            addView(textLabel("Appearance", 19, true))
            addView(MaterialSwitch(this@MainActivity).apply {
                text = "Wallpaper colours"; minHeight = dp(56)
                isChecked = prefs.getBoolean("dynamic_color", true)
                isEnabled = Build.VERSION.SDK_INT >= 31
                setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("dynamic_color", checked).apply(); preserveDraft(); recreate() }
            })
            val modes = listOf("System", "Light", "Dark")
            val values = listOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)
            addView(choice(modes, modes[values.indexOf(prefs.getInt("theme_mode", values[0])).coerceAtLeast(0)]) { name ->
                val mode = values[modes.indexOf(name)]
                if (prefs.getInt("theme_mode", values[0]) != mode) {
                    preserveDraft(); prefs.edit().putInt("theme_mode", mode).apply(); AppCompatDelegate.setDefaultNightMode(mode)
                }
            })
        })
        page.addView(surface {
            addView(textLabel("Recovery", 19, true))
            addView(textLabel("Disable hooks below to keep your tuning but return to stock. If SystemUI cannot open, disable ColorUp in your framework manager and reboot.", secondary = true))
            addView(button("Disable all hooks", outlined = true) { disableAll() })
        })
    }

    private fun connectionSummary(): String {
        val at = prefs.getLong("last_runtime_seen_at", 0)
        val source = prefs.getString("last_runtime_seen_pkg", null)
        return if (at == 0L || source != "com.android.systemui") "No SystemUI connection recorded. Complete the setup steps to connect SystemUI."
        else "Last SystemUI connection: ${DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)}. Reboot after module updates."
    }

    private fun diagnosticReport() = """
        ColorUp ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        Build: ${Build.DISPLAY}
        Fingerprint: ${Build.FINGERPRINT}
        Target: Galaxy A35 5G / One UI 8.5 (user reported)
        Required scope: com.android.systemui
        ${connectionSummary()}
        Runtime version: ${prefs.getString("runtime_version", "none")}
        Installed candidates: ${prefs.getString("runtime_hooks", "none")}
        Enabled draft IDs: ${specs.filter { it.enabled }.joinToString { it.id }}
        Enabled saved IDs: ${ConfigCodec.decode(savedJson).filter { it.enabled }.joinToString { it.id }}
        Installed candidates are not proof of animation invocation on this firmware.
    """.trimIndent()

    private fun openManager() {
        val launch = packageManager.getLaunchIntentForPackage("org.lsposed.manager")
        if (launch != null && runCatching { startActivity(launch) }.isSuccess) return
        // Many frameworks hide their manager: open the documented manager shortcut.
        val shortcut = Intent("android.intent.action.MAIN").addCategory("org.lsposed.manager.LAUNCH_MANAGER").setPackage("com.android.shell")
        if (runCatching { startActivity(shortcut) }.isFailure) message("Open your LSPosed or Vector manager manually, then choose Modules → ColorUp.")
    }

    private fun disableAll() {
        specs.forEach { it.enabled = false }
        saveConfig()
        renderPage()
        message("All hooks disabled. Your motion values have been kept.")
    }

    private fun changed() { previews.forEach { it.invalidate() }; updateSaveState() }
    private fun updateSaveState() {
        save.isEnabled = dirty
        saveState.text = if (dirty) "Unsaved changes" else "All changes saved"
    }
    private fun preserveDraft() {
        prefs.edit().apply { if (dirty) putString("draft_json", ConfigCodec.encode(specs)) else remove("draft_json") }.apply()
    }
    private fun saveConfig() {
        val json = ConfigCodec.encode(specs)
        prefs.edit().putString("config_json", json).putLong("updated_at", System.currentTimeMillis()).remove("draft_json").apply()
        savedJson = json
        runCatching { contentResolver.notifyChange(Uri.parse("content://dev.colorup.config/config"), null) }
            .onFailure { message("Saved. Runtime notification failed; reboot to reload.") }
        updateSaveState()
    }
    override fun onPause() { preserveDraft(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("draft", ConfigCodec.encode(specs)); outState.putInt("tab", tab)
        outState.putString("editing", editingId); outState.putString("category", category); outState.putString("search", search)
        super.onSaveInstanceState(outState)
    }

    private fun slider(title: String, key: String, initial: Float, from: Float, to: Float, step: Float, format: (Float) -> String, change: (Float) -> Unit): View = column().apply {
        val label = textLabel("$title  ·  ${format(initial)}", 14, true)
        addView(label)
        addView(Slider(this@MainActivity).apply {
            tag = key; contentDescription = title
            valueFrom = from; valueTo = to; stepSize = step
            value = (from + kotlin.math.round((initial.coerceIn(from, to) - from) / step) * step).coerceIn(from, to)
            setLabelFormatter { format(it) }
            addOnChangeListener { _, next, fromUser ->
                if (fromUser) { change(next); label.text = "$title  ·  ${format(next)}"; changed() }
            }
        }, LinearLayout.LayoutParams(-1, dp(52)))
    }
    private fun choice(values: List<String>, selected: String, change: (String) -> Unit): View = ChipGroup(this).apply {
        isSingleSelection = true; isSelectionRequired = true
        values.forEach { option -> addView(Chip(this@MainActivity).apply {
            text = friendly(option); isCheckable = true; isChecked = option == selected; minHeight = dp(48)
            setOnClickListener { change(option) }
        }) }
    }
    private fun button(title: String, outlined: Boolean = false, action: () -> Unit): MaterialButton = MaterialButton(this, null,
        if (outlined) com.google.android.material.R.attr.materialButtonOutlinedStyle else com.google.android.material.R.attr.materialButtonStyle).apply {
        text = title; isAllCaps = false; minHeight = dp(48)
        setOnClickListener { action() }
    }
    private fun friendly(value: String) = value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    private fun message(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }
    private fun copy(label: String, value: String) { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(label, value)); message("Copied to clipboard") }

    private fun exportConfig() { exportDocument.launch("colorup-v2-profiles.json") }
    private fun importConfig() { importDocument.launch(arrayOf("application/json", "text/plain")) }
    private fun handleFile(requestCode: Int, uri: Uri) {
        if (files.busy) { message("Another file operation is still running."); return }
        files.transfer(requestCode == REQUEST_EXPORT, uri, ConfigCodec.encode(specs))
    }
    private fun showFileResult(result: ProfileFilesModel.Result) {
        when {
            result.error != null -> { message("File error: ${result.error}"); files.consumeResult() }
            result.imported == null -> { message("Profiles exported"); files.consumeResult() }
            else -> MaterialAlertDialogBuilder(this).setTitle("Import ${result.imported.size} profiles?")
                .setMessage("This replaces your current draft. Save changes to apply it.")
                .setNegativeButton("Cancel") { _, _ -> files.consumeResult() }
                .setOnCancelListener { files.consumeResult() }
                .setPositiveButton("Import") { _, _ ->
                    specs = result.imported.map { it.copy() }.toMutableList()
                    files.consumeResult(); renderPage(); message("Profiles imported. Save changes to apply.")
                }.show()
        }
    }
    companion object { private const val REQUEST_EXPORT = 501; private const val REQUEST_IMPORT = 502 }
}
