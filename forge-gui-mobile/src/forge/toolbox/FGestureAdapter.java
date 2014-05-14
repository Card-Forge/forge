package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.util.Utils;

public abstract class FGestureAdapter extends InputAdapter {
    public static final float DOUBLE_TAP_INTERVAL = 0.25f;

    public abstract boolean press(float x, float y);
    public abstract boolean longPress(float x, float y);
    public abstract boolean release(float x, float y);
    public abstract boolean tap(float x, float y, int count);
    public abstract boolean fling(float velocityX, float velocityY);
    public abstract boolean pan(float x, float y, float deltaX, float deltaY);
    public abstract boolean panStop(float x, float y);
    public abstract boolean zoom(float x, float y, float amount);

    private float tapSquareSize, pressDelay, longPressDelay, quickTapDelay, lastTapX, lastTapY, tapSquareCenterX, tapSquareCenterY;
    private long tapCountInterval, flingDelay, lastTapTime, gestureStartTime;
    private int tapCount, lastTapButton, lastTapPointer;
    private boolean inTapSquare, pressed, longPressed, longPressHandled, quickTapped, pinching, panning;

    private final VelocityTracker tracker = new VelocityTracker();
    private final Vector2 pointer1 = new Vector2();
    private final Vector2 pointer2 = new Vector2();
    private final Vector2 prevPointer1 = new Vector2();
    private final Vector2 prevPointer2 = new Vector2();
    private final Vector2 initialPointer1 = new Vector2();
    private final Vector2 initialPointer2 = new Vector2();

    private final Task pressTask = new Task() {
        @Override
        public void run () {
            if (!pressed) {
                pressed = true;
                press(pointer1.x, pointer1.y);
            }
        }
    };
    private final Task longPressTask = new Task() {
        @Override
        public void run () {
            if (!longPressed) {
                longPressed = true;
                if (longPress(pointer1.x, pointer1.y)) {
                    Gdx.input.vibrate(25); //perform a quick vibrate to signify a successful long press
                    endPress(pointer1.x, pointer1.y); //end press immediately if long press handled
                    longPressHandled = true;
                }
            }
        }
    };
    private final Task quickTapTask = new Task() {
        @Override
        public void run () {
            if (quickTapped) {
                quickTapped = false;
                endPress(lastTapX, lastTapY);
                tap(lastTapX, lastTapY, tapCount);
            }
        }
    };

    public FGestureAdapter() {
        this(Utils.AVG_FINGER_WIDTH / 2f, DOUBLE_TAP_INTERVAL, 0.05f, 0.5f, 0.025f, 0.15f);
    }

    /** @param tapSquareSize0 half width in pixels of the square around an initial touch event
     * @param tapCountInterval0 time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
     * @param pressDelay0 time in seconds that must pass for a press event to be fired.
     * @param longPressDelay0 time in seconds that must pass for a long press event to be fired.
     * @param flingDelay0 time in seconds the finger must have been dragged for a fling event to be fired. */
    public FGestureAdapter(float tapSquareSize0, float tapCountInterval0, float pressDelay0, float longPressDelay0, float quickTapDelay0, float flingDelay0) {
        tapSquareSize = tapSquareSize0;
        tapCountInterval = Utils.secondsToTimeSpan(tapCountInterval0);
        pressDelay = pressDelay0;
        longPressDelay = longPressDelay0;
        quickTapDelay = quickTapDelay0;
        flingDelay = Utils.secondsToTimeSpan(flingDelay0);
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
                prevPointer1.set(pointer1);
                prevPointer2.set(pointer2);
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
            prevPointer1.set(pointer1);
            prevPointer2.set(pointer2);
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
            prevPointer1.set(pointer1);
            pointer1.set(x, y);
        }
        else {
            prevPointer2.set(pointer2);
            pointer2.set(x, y);
        }

        // handle pinch zoom
        if (pinching) {
            Vector2 focalPoint = Utils.getIntersection(pointer1, pointer2, initialPointer1, initialPointer2);
            return zoom(focalPoint.x, focalPoint.y, pointer1.dst(pointer2) - prevPointer1.dst(prevPointer2));
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

        if (longPressHandled) { //do nothing more if long press handled
            longPressHandled = false; //reset for next touch event
            return false;
        }

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
            Timer.schedule(quickTapTask, quickTapDelay);
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

        boolean handled = false;
        if (wasPanning) { // handle no longer panning
            handled = panStop(x, y);

            gestureStartTime = 0;
            long time = Gdx.input.getCurrentEventTime();
            if (time - tracker.lastTime < flingDelay) { // handle fling if needed
                tracker.update(x, y, time);
                handled = fling(tracker.getVelocityX(), tracker.getVelocityY()) || handled;
            }
        }
        return handled;
    }

    private void startPress() {
        pressed = false; //ensure these fields reset
        longPressed = false;
        longPressHandled = false;

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
            if (meanTime == 0) { return 0; }
            return meanX / meanTime;
        }

        public float getVelocityY() {
            float meanY = getAverage(this.meanY, numSamples);
            float meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f;
            if (meanTime == 0) { return 0; }
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
