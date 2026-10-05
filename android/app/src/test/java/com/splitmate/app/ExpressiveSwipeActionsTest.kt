package com.splitmate.app

import com.splitmate.app.ui.components.ExpressiveSwipeActionsDefaults
import com.splitmate.app.ui.components.ExpressiveSwipeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** v2.3.6 Step C: the snap-back swipe's threshold, rubber band and travel limits. */
class ExpressiveSwipeActionsTest {
    private val d = ExpressiveSwipeActionsDefaults

    @Test fun `threshold is capped by row width`() {
        assertEquals(96f, d.thresholdPx(96f, 0), 0f)
        assertEquals(96f, d.thresholdPx(96f, 1000), 0f)
        assertEquals(40f, d.thresholdPx(96f, 100), 0f)
    }

    @Test fun `release resolves only past the threshold`() {
        assertNull(d.resolve(50f, 96f))
        assertNull(d.resolve(-95.9f, 96f))
        assertEquals(ExpressiveSwipeSide.Start, d.resolve(96f, 96f))
        assertEquals(ExpressiveSwipeSide.End, d.resolve(-120f, 96f))
        assertNull(d.resolve(500f, 0f))
    }

    @Test fun `sides without an action never move`() {
        assertEquals(0f, d.applyDrag(0f, 30f, 96f, 600f, hasStartAction = false, hasEndAction = true), 0f)
        assertEquals(0f, d.applyDrag(0f, -30f, 96f, 600f, hasStartAction = true, hasEndAction = false), 0f)
        assertEquals(-30f, d.applyDrag(0f, -30f, 96f, 600f, hasStartAction = false, hasEndAction = true), 0f)
    }

    @Test fun `drag past the threshold is damped and capped`() {
        val damped = d.applyDrag(100f, 100f, 96f, 600f, true, true)
        assertEquals(100f + 100f * d.OverdragResistance, damped, 0.001f)
        // Moving back towards rest is never damped.
        assertEquals(50f, d.applyDrag(100f, -50f, 96f, 600f, true, true), 0f)
        assertEquals(600f, d.applyDrag(590f, 1000f, 96f, 600f, true, true), 0f)
        assertEquals(-600f, d.applyDrag(-590f, -1000f, 96f, 600f, true, true), 0f)
    }

    @Test fun `settled celebration fires only when the last payment clears`() {
        val c = com.splitmate.app.ui.components.SettledCelebrationDefaults
        org.junit.Assert.assertTrue(c.shouldCelebrate(hadTransfers = true, hasTransfersNow = false))
        org.junit.Assert.assertFalse(c.shouldCelebrate(hadTransfers = false, hasTransfersNow = false))
        org.junit.Assert.assertFalse(c.shouldCelebrate(hadTransfers = true, hasTransfersNow = true))
        org.junit.Assert.assertFalse(c.shouldCelebrate(hadTransfers = false, hasTransfersNow = true))
    }
}
