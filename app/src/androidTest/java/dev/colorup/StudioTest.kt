package dev.colorup

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import com.google.android.material.slider.Slider
import org.hamcrest.Matchers.`is`

@RunWith(AndroidJUnit4::class)
class StudioTest {
    @Before fun resetConfiguration() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("colorup", 0).edit().clear().commit()
    }

    @Test fun editingSurvivesRecreationAndSaveReachesStoredConfig() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.nav_motion)).perform(click())
            onView(withText("Unlock → app")).perform(click())
            onView(withTagValue(`is`("duration_slider" as Any))).perform(scrollTo(), swipeRight())
            scenario.recreate()
            onView(withText("Unlock → app")).check(matches(isDisplayed()))
            onView(withId(R.id.save_config)).perform(click())
            scenario.onActivity { activity ->
                val json = activity.getSharedPreferences("colorup", 0).getString("config_json", null)
                assertNotNull(json)
                assertTrue(ConfigCodec.decode(json!!)[1].durationMs > 320)
            }
        }
    }

    @Test fun setupExplainsActualScopeAndPendingLauncherSupport() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_setup)).perform(click())
            onView(withText("System UI")).check(matches(isDisplayed()))
            onView(withText("com.android.systemui")).check(matches(isDisplayed()))
        }
    }

    @Test fun pendingTransitionsCannotEnableSystemHooks() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_motion)).perform(click())
            onView(withText("Unlock → home")).perform(click())
            onView(withTagValue(`is`("enable_transition" as Any))).check(matches(isNotEnabled()))
            onView(withText("Preview only")).check(matches(isDisplayed()))
        }
    }
    @Test fun slowImportSurvivesActivityRecreation() {
        androidx.test.espresso.intent.Intents.init()
        try {
            androidx.test.espresso.intent.Intents.intending(
                androidx.test.espresso.intent.matcher.IntentMatchers.hasAction(android.content.Intent.ACTION_OPEN_DOCUMENT)
            ).respondWith(android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK,
                android.content.Intent().setData(android.net.Uri.parse("content://dev.colorup.test.slowprofile/config"))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                onView(withId(R.id.nav_presets)).perform(click())
                onView(withText("Import profiles")).perform(scrollTo(), click())
                scenario.recreate()
                val deadline = android.os.SystemClock.uptimeMillis() + 8000
                var visible = false
                while (android.os.SystemClock.uptimeMillis() < deadline && !visible) {
                    try { onView(withText("Import 18 profiles?")).check(matches(isDisplayed())); visible = true }
                    catch (_: androidx.test.espresso.NoMatchingViewException) { android.os.SystemClock.sleep(100) }
                }
                assertTrue("Completed import must be offered after rotation", visible)
                onView(withText("Import")).perform(click())
                onView(withId(R.id.save_config)).perform(click())
                scenario.onActivity { activity ->
                    val json = activity.getSharedPreferences("colorup", 0).getString("config_json", null)!!
                    assertEquals(730, ConfigCodec.decode(json)[1].durationMs)
                }
            }
        } finally { androidx.test.espresso.intent.Intents.release() }
    }
    @Test fun disableAllKeepsTuningAndPublishesOffConfiguration() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.nav_presets)).perform(click())
            onView(withText("Load Fluid")).perform(click())
            onView(withText("Load preset")).perform(click())
            onView(withId(R.id.save_config)).perform(click())
            onView(withId(R.id.nav_overview)).perform(click())
            onView(withText("Disable all hooks")).perform(scrollTo(), click())
            scenario.onActivity { activity ->
                activity.contentResolver.query(android.net.Uri.parse("content://dev.colorup.config/config"), null, null, null, null)!!.use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    val actual = ConfigCodec.decode(cursor.getString(0))
                    assertTrue(actual.none { it.enabled })
                    assertEquals(430, actual[1].durationMs)
                }
            }
        }
    }
    @Test fun legacyLauncherHeartbeatIsNotReportedAsSystemUiConnection() {
        InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("colorup", 0).edit()
            .putString("last_runtime_seen_pkg", "com.sec.android.app.launcher")
            .putLong("last_runtime_seen_at", System.currentTimeMillis()).commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.nav_setup)).perform(click())
            onView(withText("No SystemUI connection recorded. Complete the setup steps to connect SystemUI.")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }
}
