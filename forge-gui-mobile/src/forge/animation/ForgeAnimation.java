package forge.animation;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;

import forge.Forge;

public abstract class ForgeAnimation {
    private static final List<ForgeAnimation> activeAnimations = new ArrayList<ForgeAnimation>();

    public void start() {
        if (activeAnimations.contains(this)) { return; } //prevent starting the same animation multiple times

        activeAnimations.add(this);
        if (activeAnimations.size() == 1) { //if first animation being started, ensure continuous rendering turned on
            Forge.startContinuousRendering();
        }
    }

    public void stop() {
        if (!activeAnimations.contains(this)) { return; } //prevent stopping the same animation multiple times

        activeAnimations.remove(this);
        onEnd(false);
        if (activeAnimations.isEmpty()) { //when all animations have stopped, turn continuous rendering back off
            Forge.stopContinuousRendering();
        }
    }

    public static void advanceAll() {
        if (activeAnimations.isEmpty()) { return; }

        float dt = Gdx.graphics.getDeltaTime();
        for (int i = 0; i < activeAnimations.size(); i++) {
            if (!activeAnimations.get(i).advance(dt)) {
                activeAnimations.remove(i).onEnd(false);
                i--;
            }
        }

        if (activeAnimations.isEmpty()) { //when all animations have ended, turn continuous rendering back off
            Forge.stopContinuousRendering();
        }
    }

    public static void endAll() {
        if (activeAnimations.isEmpty()) { return; }

        for (ForgeAnimation animation : activeAnimations) {
            animation.onEnd(true);
        }
        activeAnimations.clear();
        Forge.stopContinuousRendering();
    }

    //return true if animation should continue, false to stop the animation
    protected abstract boolean advance(float dt);
    protected abstract void onEnd(boolean endingAll);
}
