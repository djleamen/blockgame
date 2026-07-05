/**
 * Day/night cycle: a game-time clock driving sky colour and world brightness.
 */

package com.mcclone;

/**
 * Tracks time of day on a fixed-length cycle and derives the current sky
 * colour and a global brightness factor from it.
 *
 * <p>The cycle is 10 real minutes long (roughly matching Minecraft's 20-minute
 * day scaled down for a small world). Time is expressed as a fraction in
 * {@code [0, 1)}:</p>
 * <ul>
 *   <li>0.00 — midnight</li>
 *   <li>0.25 — dawn</li>
 *   <li>0.50 — noon</li>
 *   <li>0.75 — dusk</li>
 * </ul>
 */
public class DayNightCycle {

    /** Length of a full day/night cycle in seconds (10 minutes). */
    public static final float CYCLE_SECONDS = 600f;

    /**
     * Keyframes: {time fraction, r, g, b, brightness}. Linearly interpolated;
     * the list wraps around from the last entry back to the first.
     */
    private static final float[][] KEYFRAMES = {
            {0.00f, 0.02f, 0.03f, 0.08f, 0.25f}, // midnight
            {0.20f, 0.02f, 0.03f, 0.08f, 0.25f}, // late night
            {0.25f, 0.85f, 0.55f, 0.35f, 0.65f}, // dawn
            {0.32f, 0.55f, 0.80f, 1.00f, 1.00f}, // morning
            {0.50f, 0.55f, 0.80f, 1.00f, 1.00f}, // noon
            {0.68f, 0.55f, 0.80f, 1.00f, 1.00f}, // afternoon
            {0.75f, 0.95f, 0.50f, 0.25f, 0.60f}, // dusk
            {0.82f, 0.02f, 0.03f, 0.08f, 0.25f}, // nightfall
    };

    /** Elapsed cycle time in seconds. Starts at morning so new worlds are lit. */
    private float time = 0.30f * CYCLE_SECONDS;

    /**
     * Advance the clock.
     *
     * @param dt elapsed real time in seconds
     */
    public void advance(float dt) {
        time = (time + dt) % CYCLE_SECONDS;
        if (time < 0) time += CYCLE_SECONDS;
    }

    /** @return current time as a fraction of the cycle in {@code [0, 1)}. */
    public float timeFraction() {
        return time / CYCLE_SECONDS;
    }

    /** @return elapsed cycle time in seconds (persisted by {@link WorldSave}). */
    public float timeSeconds() {
        return time;
    }

    /**
     * Restore a saved clock value.
     *
     * @param seconds elapsed cycle time in seconds
     */
    public void setTimeSeconds(float seconds) {
        time = ((seconds % CYCLE_SECONDS) + CYCLE_SECONDS) % CYCLE_SECONDS;
    }

    /** @return the current sky colour as {r, g, b}. */
    public float[] skyColor() {
        float[] s = sample();
        return new float[]{s[0], s[1], s[2]};
    }

    /**
     * @return global light multiplier in {@code [0.25, 1]} applied to world
     *         geometry so nights actually look dark
     */
    public float brightness() {
        return sample()[3];
    }

    /** Interpolate {r, g, b, brightness} for the current time. */
    private float[] sample() {
        float t = timeFraction();
        int n = KEYFRAMES.length;
        for (int i = 0; i < n; i++) {
            float[] a = KEYFRAMES[i];
            float[] b = KEYFRAMES[(i + 1) % n];
            float ta = a[0];
            float tb = (i + 1 == n) ? b[0] + 1f : b[0];
            float tt = (i + 1 == n && t < ta) ? t + 1f : t;
            if (tt >= ta && tt <= tb) {
                float f = (tb - ta) < 1e-6f ? 0f : (tt - ta) / (tb - ta);
                return new float[]{
                        a[1] + (b[1] - a[1]) * f,
                        a[2] + (b[2] - a[2]) * f,
                        a[3] + (b[3] - a[3]) * f,
                        a[4] + (b[4] - a[4]) * f,
                };
            }
        }
        // Unreachable, but keep the compiler happy.
        float[] last = KEYFRAMES[n - 1];
        return new float[]{last[1], last[2], last[3], last[4]};
    }
}
