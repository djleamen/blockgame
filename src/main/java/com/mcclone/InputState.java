package com.mcclone;

/**
 * Tracks the state of a single digital input (a key or mouse button).
 * This helps distinguish between a key being held down, just pressed, or just released.
 */
public class InputState {

    /** True between PRESS and RELEASE callbacks (live state). */
    private boolean currentlyPressed;

    /** Snapshot of the live state at the start of the previous frame. */
    private boolean previouslyPressed;

    /**
     * True if a PRESS callback arrived since the last {@link #update()}. We
     * keep this latch so a key that was pressed AND released within the same
     * frame (common on macOS when the event queue is drained in one batch)
     * still registers as "down" for exactly one game tick.
     */
    private boolean pressedSinceLastUpdate;

    /**
     * Snapshot computed during {@link #update()} that {@link #isDown()} and
     * {@link #wasJustPressed()} read from. Stable for the whole frame.
     */
    private boolean downThisFrame;

    /**
     * Promote the live state into a frame-stable snapshot. Call once per
     * game tick before reading {@link #isDown()}.
     */
    public void update() {
        previouslyPressed = downThisFrame;
        // A key counts as "down" this frame if it's physically held now OR
        // if it was pressed-and-released between the last update and this one.
        downThisFrame = currentlyPressed || pressedSinceLastUpdate;
        pressedSinceLastUpdate = false;
    }

    /**
     * Called from a GLFW callback when the key/button transitions.
     *
     * @param isPressed true on PRESS, false on RELEASE
     */
    public void setPressed(boolean isPressed) {
        if (isPressed) {
            pressedSinceLastUpdate = true;
        }
        this.currentlyPressed = isPressed;
    }

    /** @return true while the key/button is "down" for the current frame. */
    public boolean isDown() {
        return downThisFrame;
    }

    /** @return true only on the single frame the key/button was first pressed. */
    public boolean wasJustPressed() {
        return downThisFrame && !previouslyPressed;
    }

    /** @return true only on the single frame the key/button was released. */
    public boolean wasJustReleased() {
        return !downThisFrame && previouslyPressed;
    }

    /** Drop all state — used when the window loses focus. */
    public void reset() {
        currentlyPressed = false;
        previouslyPressed = false;
        pressedSinceLastUpdate = false;
        downThisFrame = false;
    }
}
