package dev.colorup

/** Describes actual runtime fields, not the larger preview profile schema. */
object HookCapabilities {
    val durationOnly = setOf("unlock_to_app", "keyguard_show", "keyguard_hide", "screen_on", "screen_off", "notification_dismiss")
    val panels = setOf("shade_open", "shade_close", "qs_expand", "qs_collapse")
    fun description(id: String): String = when (id) {
        in durationOnly -> "Experimental · duration only"
        in panels -> "Experimental · gesture-driven motion"
        "notification_appear" -> "Experimental · full motion profile"
        else -> "Preview only"
    }
    fun explanation(id: String): String = when (id) {
        in durationOnly -> "Duration changes system motion. Other settings are for preview only. Availability depends on your One UI build."
        in panels -> "Follows your finger. Scale, vertical offset, curve, spring response and anchor affect the panel. Duration, delay and fade do not control this gesture."
        "notification_appear" -> "Animates newly inserted notification rows from the configured scale, fade and offset back to their original appearance. All motion controls apply."
        else -> "No system hook is implemented for this transition yet. You can design and save a preview, but it will not change One UI."
    }
}
