package igrek.songbook

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class InstrumentationTest {

    @Test
    fun test_navigationMenuShows() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        assertThat(appContext.packageName).isEqualTo("igrek.songbook")
    }
}
