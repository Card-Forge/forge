package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public abstract class FGestureAdapter extends InputAdapter {
    public abstract boolean press(float x, float y);
    public abstract boolean longPress(float x, float y);
    public abstract boolean release(float x, float y);
    public abstract boolean tap(float x, float y, int count);
    public abstract boolean fling(float velocityX, float velocityY);
    public abstract boolean pan(float x, float y, float deltaX, float deltaY);
    public abstract boolean panStop(float x, float y);
    public abstract boolean zoom(float initialDistance, float distance);
    public abstract boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2);

    private float tapSquareSize, pressDelay, longPressDelay, lastTapX, lastTapY, tapSquareCenterX, tapSquareCenterY;
    private long tapCountInterval, flingDelay, lastTapTime, gestureStartTime;
    private int tapCount, lastTapButton, lastTapPointer;
    private boolean inTapSquare, pressed, longPressed, quickTapped, pinching, panning;

    private final VelocityTracker tracker = new VelocityTracker();
    Vector2 pointer1 = new Vector2();
    private final Vector2 pointer2 = new Vector2();
    private final Vector2 initialPointer1 = new Vector2();
    private final Vector2 initialPointer2 = new Vector2();

    private final Task pressTask = new Task() {
        @Override
        public void run () {
            if (!pressed) {
                press(pointer1.x, pointer1.y);
                pressed = true;
            }
        }
    };
    private final Task longPressTask = new Task() {
        @Override
        public void run () {
            if (!longPressed) {
                longPress(pointer1.x, pointer1.y);
                longPressed = true;
            }
        }
    };
    private final Task quickTapTask = new Task() {
        @Override
        public void run () {
            if (quickTapped) {
                endPress(lastTapX, lastTapY);
                tap(lastTapX, lastTapY, tapCount);
                quickTapped = false;
            }
        }
    };

    public FGestureAdapter() {
        this(20f, 0.4f, 0.1f, 1.1f, 0.15f);
    }

    /** @param tapSquareSize0 half width in pixels of the square around an initial touch event
     * @param tapCountInterval0 time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
     * @param pressDelay0 time in seconds that must pass for a press event to be fired.
     * @param longPressDelay0 time in seconds that must pass for a long press event to be fired.
     * @param flingDelay0 time in seconds the finger must have been dragged for a fling event to be fired. */
    public FGestureAdapter(float tapSquareSize0, float tapCountInterval0, float pressDelay0, float longPressDelay0, float flingDelay0) {
        tapSquareSize = tapSquareSize0;
        tapCountInterval = (long)(tapCountInterval0 * 1000000000l);
        pressDelay = pressDelay0;
        longPressDelay = longPressDelay0;
        flingDelay = (long)(flingDelay0 * 1000000000l);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        return touchDown((float)x, (float)y, pointer, button);
    }
    private boolean touchDown(float x, float y, int pointer, int button) {
        if (pointer > 1) { return false; }

        if (quickTapped) { //finish quick tap immediately if another touchDown event is received
            quickTapTask.cancel();
            quickTapTask.run();
        }

        if (pointer == 0) {
            pointer1.set(x, y);
            gestureStartTime = Gdx.input.getCurrentEventTime();
            tracker.start(x, y, gestureStartTime);
            if (Gdx.input.isTouched(1)) {
                // Start pinch.
                inTapSquare = false;
                pinching = true;
                initialPointer1.set(pointer1);
                initialPointer2.set(pointer2);
                endPress(x, y);
            }
            else {
                // Normal touch down.
                inTapSquare = true;
                pinching = false;
                tapSquareCenterX = x;
                tapSquareCenterY = y;
                startPress();
            }
        }
        else {
            // Start pinch.
            pointer2.set(x, y);
            inTapSquare = false;
            pinching = true;
            initialPointer1.set(pointer1);
            initialPointer2.set(pointer2);
            endPress(pointer1.x, pointer1.y);
        }
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        return touchDragged((float)x, (float)y, pointer);
    }
    private boolean touchDragged(float x, float y, int pointer) {
        if (pointer > 1) { return false; }

        if (pointer == 0) {
            pointer1.set(x, y);
        }
        else {
            pointer2.set(x, y);
        }

        // handle pinch zoom
        if (pinching) {
            boolean result = pinch(initialPointer1, initialPointer2, pointer1, pointer2);
            return zoom(initialPointer1.dst(initialPointer2), pointer1.dst(pointer2)) || result;
        }

        // update tracker
        tracker.update(x, y, Gdx.input.getCurrentEventTime());

        // check if we are still tapping.
        if (inTapSquare && !isWithinTapSquare(x, y, tapSquareCenterX, tapSquareCenterY)) {
            endPress(x, y);
            inTapSquare = false;
        }

        // if we have left the tap square, we are panning
        if (!inTapSquare) {
            panning = true;
            return pan(x, y, tracker.deltaX, tracker.deltaY);
        }

        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return touchUp((float)x, (float)y, pointer, button);
    }
    private boolean touchUp(float x, float y, int pointer, int button) {
        if (pointer > 1) { return false; }

        // check if we are still tapping.
        if (inTapSquare && !isWithinTapSquare(x, y, tapSquareCenterX, tapSquareCenterY)) {
            inTapSquare = false;
        }

        boolean wasPanning = panning;
        panning = false;

        boolean wasPressed = pressed;
        endPress(x, y);

        if (inTapSquare) {
            // handle taps
            if (lastTapButton != button || lastTapPointer != pointer
                    || TimeUtils.nanoTime() - lastTapTime > tapCountInterval
                    || !isWithinTapSquare(x, y, lastTapX, lastTapY)) {
                tapCount = 0;
            }
            tapCount++;
            lastTapTime = TimeUtils.nanoTime();
            lastTapX = x;
            lastTapY = y;
            lastTapButton = button;
            lastTapPointer = pointer;
            gestureStartTime = 0;
            if (wasPressed) {
                return tap(x, y, tapCount);
            }

            pressTask.run(); //allow pressed and released to fire if quick tapping
            quickTapped = true;
            Timer.schedule(quickTapTask, pressDelay);
            return false;
        }

        if (pinching) {
            // handle pinch end
            pinching = false;
            panning = true;
            // we are in pan mode again, reset velocity tracker
            if (pointer == 0) {
                // first pointer has lifted off, set up panning to use the second pointer...
                tracker.start(pointer2.x, pointer2.y, Gdx.input.getCurrentEventTime());
            }
            else {
                // second pointer has lifted off, set up panning to use the first pointer...
                tracker.start(pointer1.x, pointer1.y, Gdx.input.getCurrentEventTime());
            }
            return false;
        }

        // handle no longer panning
        boolean handled = false;
        if (wasPanning && !panning) {
            handled = panStop(x, y);
        }

        // handle fling
        gestureStartTime = 0;
        long time = Gdx.input.getCurrentEventTime();
        if (time - tracker.lastTime < flingDelay) {
            tracker.update(x, y, time);
            handled = fling(tracker.getVelocityX(), tracker.getVelocityY()) || handled;
        }
        return handled;
    }

    private void startPress() {
        pressed = false;
        if (!pressTask.isScheduled()) {
            Timer.schedule(pressTask, pressDelay);
        }
        if (!longPressTask.isScheduled()) {
            Timer.schedule(longPressTask, longPressDelay);
        }
    }

    private void endPress(float x, float y) {
        pressTask.cancel();
        longPressTask.cancel();
        longPressed = false;
        if (pressed) {
            pressed = false;
            release(x, y);
        }
    }

    private boolean isWithinTapSquare(float x, float y, float centerX, float centerY) {
        return Math.abs(x - centerX) < tapSquareSize && Math.abs(y - centerY) < tapSquareSize;
    }

    private static class VelocityTracker {
        int sampleSize = 10;
        float lastX, lastY;
        float deltaX, deltaY;
        long lastTime;
        int numSamples;
        float[] meanX = new float[sampleSize];
        float[] meanY = new float[sampleSize];
        long[] meanTime = new long[sampleSize];

        public void start(float x, float y, long timeStamp) {
            lastX = x;
            lastY = y;
            deltaX = 0;
            deltaY = 0;
            numSamples = 0;
            for (int i = 0; i < sampleSize; i++) {
                meanX[i] = 0;
                meanY[i] = 0;
                meanTime[i] = 0;
            }
            lastTime = timeStamp;
        }

        public void update(float x, float y, long timeStamp) {
            long currTime = timeStamp;
            deltaX = x - lastX;
            deltaY = y - lastY;
            lastX = x;
            lastY = y;
            long deltaTime = currTime - lastTime;
            lastTime = currTime;
            int index = numSamples % sampleSize;
            meanX[index] = deltaX;
            meanY[index] = deltaY;
            meanTime[index] = deltaTime;
            numSamples++;
        }

        public float getVelocityX() {
            float meanX = getAverage(this.meanX, numSamples);
            float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
            if (meanTime == 0) return 0;
            return meanX / meanTime;
        }

        public float getVelocityY() {
            float meanY = getAverage(this.meanY, numSamples);
            float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
            if (meanTime == 0) return 0;
            return meanY / meanTime;
        }

        private float getAverage(float[] values, int numSamples) {
            numSamples = Math.min(sampleSize, numSamples);
            float sum = 0;
            for (int i = 0; i < numSamples; i++) {
                sum += values[i];
            }
            return sum / numSamples;
        }

        private long getAverage(long[] values, int numSamples) {
            numSamples = Math.min(sampleSize, numSamples);
            long sum = 0;
            for (int i = 0; i < numSamples; i++) {
                sum += values[i];
            }
            if (numSamples == 0) return 0;
            return sum / numSamples;
        }
    }
}
