package forge.animation;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;

public abstract class ForgeAnimation {
    private static final List<ForgeAnimation> activeAnimations = new ArrayList<ForgeAnimation>();
    private boolean stopped;

    public void start() {
        if (activeAnimations.contains(this)) { return; } //prevent starting the same animation multiple times

        activeAnimations.add(this);
    }

    protected boolean stopWhenScreenChanges() {
        return true;
    }

    public void stop() {
        stopped = true; //will be removed on the next iteration
    }

    public static void advanceAll() {
        synchronized (activeAnimations) {
            if (!activeAnimations.isEmpty()) {
                float dt = Gdx.graphics.getDeltaTime();
                for (int i = 0; i < activeAnimations.size(); i++) {
                    ForgeAnimation animation = activeAnimations.get(i);
                    if (animation.stopped || !animation.advance(dt)) {
                        animation.stopped = true;
                        activeAnimations.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    public static void stopAll(boolean forScreenChange) {
        synchronized (activeAnimations) {
            if (!activeAnimations.isEmpty()) {
                for (int i = 0; i < activeAnimations.size(); i++) {
                    ForgeAnimation animation = activeAnimations.get(i);
                    if (!forScreenChange || animation.stopWhenScreenChanges()) {
                        animation.stopped = true;
                        activeAnimations.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    //return true if animation should continue, false to stop the animation
    protected abstract boolean advance(float dt);
}
