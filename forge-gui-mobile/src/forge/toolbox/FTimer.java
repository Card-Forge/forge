package forge.toolbox;

import forge.animation.ForgeAnimation;

public abstract class FTimer extends ForgeAnimation {
    private final float interval;
    private float elapsed;

    public FTimer(float interval0) {
        interval = interval0;
    }

    @Override
    protected boolean advance(float dt) {
        elapsed += dt;
        while (elapsed >= interval) {
            tick();
            elapsed -= interval;
        }
        return true;
    }

    public void restart() {
        elapsed = 0; //assume this only called if already started
    }

    protected abstract void tick();

    @Override
    protected void onEnd(boolean endingAll) {
        elapsed = 0;
    }
}
