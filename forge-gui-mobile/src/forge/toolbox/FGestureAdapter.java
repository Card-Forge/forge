package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

import forge.Forge;
import forge.assets.FSkin;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Utils;

public abstract class FGestureAdapter extends InputAdapter {
    public abstract boolean press(float x, float y);
    public abstract boolean longPress(float x, float y);
    public abstract boolean release(float x, float y);
    public abstract boolean tap(float x, float y, int count);
    public abstract boolean flick(float x, float y);
    public abstract boolean fling(float velocityX, float velocityY);
    public abstract boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical);
    public abstract boolean panStop(float x, float y);
    public abstract boolean zoom(float x, float y, float amount);
    public abstract boolean scrolled(float amountX, float amountY);

    private float tapSquareSize, longPressDelay, lastTapX, lastTapY, tapSquareCenterX, tapSquareCenterY;
    private long tapCountInterval, flingDelay, lastTapTime;
    private int tapCount, lastTapButton, lastTapPointer;
    private boolean inTapSquare, pressed, longPressed, longPressHandled, pinching, panning, disablePanning;

    private final VelocityTracker tracker = new VelocityTracker();
    private final Vector2 pointer1 = new Vector2();
    private final Vector2 pointer2 = new Vector2();
    private final Vector2 prevPointer1 = new Vector2();
    private final Vector2 prevPointer2 = new Vector2();
    private final Vector2 focalPoint = new Vector2();

    private final Task longPressTask = new Task() {
        @Override
        public void run() {
            if (pressed) {
                if (Gdx.input.isTouched(0)) {
                    if (!longPressed) {
                        longPressed = true;
                        if (longPress(pointer1.x, pointer1.y)) {
                            if (FModel.getPreferences().getPrefBoolean(FPref.UI_VIBRATE_ON_LONG_PRESS)) {
                                Gdx.input.vibrate(25); //perform a quick vibrate to signify a successful long press
                            }
                            endPress(pointer1.x, pointer1.y); //end press immediately if long press handled
                            longPressHandled = true;
                        }
                    }
                }
                else { //end press immediately if finger no longer down
                    endPress(pointer1.x, pointer1.y);
                }
            }
        }
    };

    public FGestureAdapter() {
        this(Utils.AVG_FINGER_WIDTH / 2f, 0.25f, 0.5f, 0.15f);
    }

    /** @param tapSquareSize0 half width in pixels of the square around an initial touch event
     * @param tapCountInterval0 time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
     * @param longPressDelay0 time in seconds that must pass for a long press event to be fired.
     * @param flingDelay0 time in seconds the finger must have been dragged for a fling event to be fired. */
    public FGestureAdapter(float tapSquareSize0, float tapCountInterval0, float longPressDelay0, float flingDelay0) {
        tapSquareSize = tapSquareSize0;
        tapCountInterval = Utils.secondsToTimeSpan(tapCountInterval0);
        longPressDelay = longPressDelay0;
        flingDelay = Utils.secondsToTimeSpan(flingDelay0);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        Forge.magnify = true;
        return super.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        return touchDown((float)x, (float)y, pointer, button);
    }
    private boolean touchDown(float x, float y, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            //catch right click
            return true;
        }
        if (pointer > 1) { return false; }

        if (pointer == 0) {
            pointer1.set(x, y);
            if (!Gdx.input.isTouched(1)) {
                // handle single finger press
                tracker.start(x, y, Gdx.input.getCurrentEventTime());
                inTapSquare = true;
                panning = false;
                pinching = false;
                tapSquareCenterX = x;
                tapSquareCenterY = y;
                startPress();
                return true;
            }
        }
        else {
            pointer2.set(x, y);
            if (!Gdx.input.isTouched(0)) {
                return true;
            }
        }

        // start pinch if two fingers down
        inTapSquare = false;
        panning = false;
        pinching = true;
        prevPointer1.set(pointer1);
        prevPointer2.set(pointer2);
        focalPoint.set(Utils.getMidpoint(pointer1, pointer2));
        endPress(pointer1.x, pointer1.y);
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
            return zoom(focalPoint.x, focalPoint.y, pointer1.dst(pointer2) - prevPointer1.dst(prevPointer2));
        }

        if (disablePanning) {
            return false; //avoid updating tracker or panning if second finger just came up but first hasn't yet
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
            boolean moreVertical = Math.abs(tracker.startY - y) > Math.abs(tracker.startX - x);
            return pan(x, y, tracker.deltaX, tracker.deltaY, moreVertical);
        }
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return touchUp((float)x, (float)y, pointer, button);
    }
    private boolean touchUp(float x, float y, int pointer, int button) {
        if (button == Input.Buttons.RIGHT) {
            //catch right click and set toggle magnify
            if (inTapSquare) {
                // handle taps
                long time = Gdx.input.getCurrentEventTime();
                if (tapCount == 2 //treat 3rd tap as a first tap, and 4th as a double tap
                        || lastTapButton != button
                        || lastTapPointer != pointer
                        || time - lastTapTime > tapCountInterval
                        || !isWithinTapSquare(x, y, lastTapX, lastTapY)) {
                    Forge.magnifyShowDetails = !Forge.magnifyShowDetails;
                    tapCount = 0;
                }
                tapCount++;
                lastTapTime = time;
                lastTapX = x;
                lastTapY = y;
                lastTapButton = button;
                lastTapPointer = pointer;
                Forge.magnifyToggle = !Forge.magnifyToggle;
                if (Forge.magnifyToggle) {
                    Forge.setCursor(FSkin.getCursor().get(1), "1");
                } else {
                    Forge.setCursor(FSkin.getCursor().get(2), "2");
                }
            }
            return false;
        }
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
            long time = Gdx.input.getCurrentEventTime();
            if (tapCount == 2 //treat 3rd tap as a first tap, and 4th as a double tap
                    || lastTapButton != button
                    || lastTapPointer != pointer
                    || time - lastTapTime > tapCountInterval
                    || !isWithinTapSquare(x, y, lastTapX, lastTapY)) {
                tapCount = 0;
            }
            tapCount++;
            lastTapTime = time;
            lastTapX = x;
            lastTapY = y;
            lastTapButton = button;
            lastTapPointer = pointer;
            if (wasPressed) {
                Forge.magnify = false;
                return tap(x, y, tapCount);
            }
            return false;
        }

        if (pinching) { //don't pan after finishing a pinch
            pinching = false;
            disablePanning = true; //disable panning until after you release both fingers, otherwise unintentional fling can result
            return false;
        }

        disablePanning = false; //once both fingers come off, allow panning again

        boolean handled = false;
        if (wasPanning) { // handle no longer panning
            handled = panStop(x, y);

            long time = Gdx.input.getCurrentEventTime();
            if (time - tracker.lastTime < flingDelay) { // handle flick/fling if needed
                tracker.update(x, y, time);
                float velocityX = tracker.getVelocityX();
                float velocityY = tracker.getVelocityY();
                if (velocityY < 0 && Math.abs(velocityY) > Math.abs(velocityX)) {
                    handled = flick(tracker.startX, tracker.startY) || handled; //flick is a special case for flinging towards the top of the screen
                }
                handled = fling(velocityX, velocityY) || handled;
            }
        }
        return handled;
    }

    private void startPress() {
        longPressed = false; //ensure these fields reset
        longPressHandled = false;

        if (!pressed) {
            pressed = true;
            press(pointer1.x, pointer1.y);
        }
        if (!longPressTask.isScheduled()) {
            Timer.schedule(longPressTask, longPressDelay);
        }
    }

    private void endPress(float x, float y) {
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
        float startX, startY;
        float lastX, lastY;
        float deltaX, deltaY;
        long lastTime;
        int numSamples;
        float[] meanX = new float[sampleSize];
        float[] meanY = new float[sampleSize];
        long[] meanTime = new long[sampleSize];

        public void start(float x, float y, long timeStamp) {
            startX = x;
            startY = y;
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
