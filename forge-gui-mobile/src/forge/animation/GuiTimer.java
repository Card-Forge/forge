package forge.animation;

import com.badlogic.gdx.utils.Timer;

import forge.interfaces.IGuiTimer;

public class GuiTimer implements IGuiTimer {
    private float interval;
    private Timer.Task task;

    public GuiTimer(final Runnable proc0, int interval0) {
        interval = (float)interval0 / 1000f; //convert to seconds
        task = new Timer.Task() {
            @Override
            public void run() {
                proc0.run();
            }
        };
    }

    @Override
    public void start() {
        Timer.schedule(task, interval, interval);
    }

    @Override
    public void stop() {
        task.cancel();
    }

    @Override
    public void setInterval(int interval0) {
        //ignore this function since there's not a reliable way to change the interval on the fly
    }
}
