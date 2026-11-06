package dev.colorup

import org.json.JSONArray
import org.json.JSONObject

data class AnimationSpec(val id:String,val title:String,val group:String,var enabled:Boolean=false,var durationMs:Int=320,var curve:String="STANDARD",var scale:Float=1f,var fade:Float=1f,var translateY:Int=24)

object AnimationCatalog {
    val defaults = listOf(
        AnimationSpec("unlock_to_home","Unlock → home","Lock screen"), AnimationSpec("unlock_to_app","Unlock → app","Lock screen"), AnimationSpec("keyguard_show","Show lock screen","Lock screen"), AnimationSpec("keyguard_hide","Hide lock screen","Lock screen"),
        AnimationSpec("screen_on","Screen turns on","Power"), AnimationSpec("screen_off","Screen turns off","Power"),
        AnimationSpec("app_launch","App launch","Apps & tasks"), AnimationSpec("app_close","App close","Apps & tasks"), AnimationSpec("home_return","Return to home","Apps & tasks"), AnimationSpec("recents_open","Open recents","Apps & tasks"), AnimationSpec("recents_close","Close recents","Apps & tasks"),
        AnimationSpec("shade_open","Open notification shade","SystemUI"), AnimationSpec("shade_close","Close notification shade","SystemUI"), AnimationSpec("qs_expand","Expand Quick Settings","SystemUI"), AnimationSpec("qs_collapse","Collapse Quick Settings","SystemUI"),
        AnimationSpec("notification_appear","Notification appears","Notifications"), AnimationSpec("notification_dismiss","Dismiss notification","Notifications"), AnimationSpec("notification_expand","Expand notification","Notifications")
    )
}
object ConfigCodec {
    fun encode(specs:List<AnimationSpec>):String {
        val root=JSONObject().put("schemaVersion",1)
        root.put("animations",JSONArray().apply { specs.forEach { s -> put(JSONObject().put("id",s.id).put("enabled",s.enabled).put("durationMs",s.durationMs).put("curve",s.curve).put("scale",s.scale.toDouble()).put("fade",s.fade.toDouble()).put("translateY",s.translateY)) } })
        return root.toString(2)
    }
}
