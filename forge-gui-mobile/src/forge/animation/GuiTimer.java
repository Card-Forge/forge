package forge.animation;

import forge.interfaces.IGuiTimer;

//implement a GuiTimer as an animation since it can utilize the same logic for advancing
public class GuiTimer extends ForgeAnimation implements IGuiTimer {
    private final Runnable proc;
    private float elapsed;
    private int interval;

    public GuiTimer(Runnable proc0, int interval0) {
        proc = proc0;
        interval = interval0;
    }

    @Override
    public void setInterval(int interval0) {
        interval = interval0;
    }

    @Override
    protected boolean advance(float dt) {
        elapsed += dt * 1000; //convert seconds to milliseconds
        if (elapsed >= interval) {
            elapsed = 0;
            proc.run();
        }
        return true;
    }

    @Override
    protected boolean stopWhenScreenChanges() {
        return false; //don't stop timers just because screen changed
    }
}
