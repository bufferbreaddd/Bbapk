package dev.colorup

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sqrt

data class AnimationSpec(
    val id: String,
    val title: String,
    val group: String,
    var enabled: Boolean = false,
    var durationMs: Int = 320,
    var curve: String = "STANDARD",
    var scale: Float = 1f,
    var fade: Float = 1f,
    var translateY: Int = 24,
    var delayMs: Int = 0,
    var springDamping: Float = 0.82f,
    var springStiffness: Float = 420f,
    var direction: String = "UP"
)

object AnimationCatalog {
    val experimentalHookIds = setOf("unlock_to_app", "unlock_to_home", "shade_open", "shade_close", "qs_expand", "qs_collapse", "notification_appear")

    val defaults = listOf(
        AnimationSpec("unlock_to_home","Unlock → home","Lock screen"),
        AnimationSpec("unlock_to_app","Unlock → app","Lock screen"),
        AnimationSpec("keyguard_show","Show lock screen","Lock screen"),
        AnimationSpec("keyguard_hide","Hide lock screen","Lock screen"),
        AnimationSpec("screen_on","Screen turns on","Power"),
        AnimationSpec("screen_off","Screen turns off","Power"),
        AnimationSpec("app_launch","App launch","Apps & tasks"),
        AnimationSpec("app_close","App close","Apps & tasks"),
        AnimationSpec("home_return","Return to home","Apps & tasks"),
        AnimationSpec("recents_open","Open recents","Apps & tasks"),
        AnimationSpec("recents_close","Close recents","Apps & tasks"),
        AnimationSpec("shade_open","Open notification shade","SystemUI"),
        AnimationSpec("shade_close","Close notification shade","SystemUI"),
        AnimationSpec("qs_expand","Expand Quick Settings","SystemUI"),
        AnimationSpec("qs_collapse","Collapse Quick Settings","SystemUI"),
        AnimationSpec("notification_appear","Notification appears","Notifications"),
        AnimationSpec("notification_dismiss","Dismiss notification","Notifications"),
        AnimationSpec("notification_expand","Expand notification","Notifications")
    )

    fun preset(name: String): MutableList<AnimationSpec> {
        val result = defaults.map { it.copy() }.toMutableList()
        result.forEach { s ->
            when (name) {
                "Fluid" -> { s.enabled = s.id in experimentalHookIds; s.durationMs = 430; s.curve = "SPRING"; s.scale = 1.04f; s.springDamping = 0.78f; s.springStiffness = 360f; s.translateY = 30 }
                "Crisp" -> { s.enabled = s.id in experimentalHookIds; s.durationMs = 220; s.curve = "EASE_OUT"; s.scale = 1f; s.fade = 1f; s.translateY = 12 }
                "Gentle" -> { s.enabled = s.id == "unlock_to_app"; s.durationMs = 520; s.curve = "EASE_IN_OUT"; s.scale = 1.01f; s.fade = 0.96f; s.translateY = 16 }
                else -> Unit
            }
        }
        return result
    }
}

object ConfigCodec {
    private const val SCHEMA = 2

    fun encode(specs: List<AnimationSpec>): String {
        val root = JSONObject().put("schemaVersion", SCHEMA)
        root.put("animations", JSONArray().apply {
            specs.forEach { s ->
                put(JSONObject()
                    .put("id", s.id).put("enabled", s.enabled && s.id in AnimationCatalog.experimentalHookIds)
                    .put("durationMs", s.durationMs.coerceIn(0, 2000))
                    .put("curve", s.curve).put("scale", s.scale.coerceIn(0f, 2f).toDouble())
                    .put("fade", s.fade.coerceIn(0f, 1f).toDouble())
                    .put("translateY", s.translateY.coerceIn(-150, 150))
                    .put("delayMs", s.delayMs.coerceIn(0, 1000))
                    .put("springDamping", s.springDamping.coerceIn(0.1f, 2f).toDouble())
                    .put("springStiffness", s.springStiffness.coerceIn(50f, 1500f).toDouble())
                    .put("direction", s.direction))
            }
        })
        return root.toString(2)
    }

    fun decode(json: String): MutableList<AnimationSpec> {
        val root = JSONObject(json)
        val array = root.optJSONArray("animations") ?: throw IllegalArgumentException("Missing animations array")
        val defaults = AnimationCatalog.defaults.associateBy { it.id }
        val imported = mutableMapOf<String, AnimationSpec>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val base = defaults[id] ?: continue
            val curve = item.optString("curve", base.curve).takeIf { it in CURVES } ?: base.curve
            val direction = item.optString("direction", base.direction).takeIf { it in DIRECTIONS } ?: base.direction
            imported[id] = base.copy(
                enabled = item.optBoolean("enabled", false) && id in AnimationCatalog.experimentalHookIds,
                durationMs = item.optInt("durationMs", 320).coerceIn(0, 2000),
                curve = curve,
                scale = item.optDouble("scale", 1.0).toFloat().coerceIn(0f, 2f),
                fade = item.optDouble("fade", 1.0).toFloat().coerceIn(0f, 1f),
                translateY = item.optInt("translateY", 24).coerceIn(-150, 150),
                delayMs = item.optInt("delayMs", 0).coerceIn(0, 1000),
                springDamping = item.optDouble("springDamping", 0.82).toFloat().coerceIn(0.1f, 2f),
                springStiffness = item.optDouble("springStiffness", 420.0).toFloat().coerceIn(50f, 1500f),
                direction = direction
            )
        }
        return AnimationCatalog.defaults.map { imported[it.id] ?: it.copy() }.toMutableList()
    }

    val CURVES = setOf("STANDARD", "LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT", "SPRING")
    val DIRECTIONS = setOf("UP", "DOWN", "LEFT", "RIGHT", "CENTER")
}


object MotionMath {
    fun ease(progress: Float, curve: String, damping: Float, stiffness: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        val value = when (curve) {
            "LINEAR" -> t
            "EASE_IN" -> t * t
            "EASE_OUT" -> 1f - (1f - t) * (1f - t)
            "EASE_IN_OUT" -> t * t * (3f - 2f * t)
            "SPRING" -> {
                val frequency = sqrt((stiffness.coerceIn(50f, 1500f) / 420f).toDouble()) * 6.0
                val decay = exp(-damping.coerceIn(0.1f, 2f).toDouble() * 6.0 * t.toDouble())
                (1.0 - decay * cos(frequency * t.toDouble())).toFloat()
            }
            else -> 1f - (1f - t) * (1f - t)
        }
        return value.coerceIn(0f, 1.15f)
    }
}

