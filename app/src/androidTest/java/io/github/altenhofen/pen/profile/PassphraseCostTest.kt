package io.github.altenhofen.pen.profile

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.altenhofen.pen.recognition.ClusterId
import io.github.altenhofen.pen.recognition.FeatureVector
import io.github.altenhofen.pen.recognition.PrototypeCluster
import io.github.altenhofen.pen.settings.MotorSettings
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * What the passphrase costs on a real ART runtime. [ProfileVault.ITERATIONS] is chosen from this
 * number, so the assertion is a loose ceiling and the logged median is the measurement.
 */
@RunWith(AndroidJUnit4::class)
class PassphraseCostTest {
    @Test
    fun derivingTheKeyStaysUnderThreeSeconds() {
        val profile = PenProfile.create(
            MotorSettings.Default,
            listOf(PrototypeCluster(ClusterId("train:a:cost:0"), 'a', FeatureVector.from(FloatArray(96) { it * 0.001f }))),
        )
        encrypt(profile)
        val runs = List(5) {
            val started = System.nanoTime()
            encrypt(profile)
            (System.nanoTime() - started) / 1_000_000
        }.sorted()
        Log.i(TAG, "PBKDF2 ${ProfileVault.ITERATIONS} iterations, export millis $runs, median ${runs[2]}")
        assertTrue("export took ${runs[2]} ms", runs[2] < 3_000)
    }

    private fun encrypt(profile: PenProfile) {
        ByteArrayOutputStream().use { ProfileVault.encrypt(it, profile, "a strong enough passphrase".toCharArray()) }
    }

    private companion object {
        const val TAG = "PassphraseCost"
    }
}
