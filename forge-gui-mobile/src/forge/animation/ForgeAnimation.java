package forge.animation;

import com.badlogic.gdx.Gdx;
import forge.Forge;

import java.util.ArrayList;
import java.util.List;

public abstract class ForgeAnimation {
    private static final List<ForgeAnimation> activeAnimations = new ArrayList<>();
    // A guard against inspecting activeAnimations while it's in the process of being edited
    private static boolean changingActiveAnimations = false;

    public void start() {
        if (activeAnimations.contains(this)) { return; } //prevent starting the same animation multiple times

        activeAnimations.add(this);
        if (activeAnimations.size() == 1 && !changingActiveAnimations) { //if first animation being started, ensure continuous rendering turned on
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
                // Without this guard, there is leaky behavior when a new animation is started
                // via the onEnd callback of a finishing animation; this is because the length
                // of the list is in the process of changing from 1 to 0 to 1 again, so
                // stopContinuousRendering() won't be called in this function (so it's
                // important to not allow startContinuousRendering() to be called either).
                changingActiveAnimations = true;
                activeAnimations.remove(i).onEnd(false);
                changingActiveAnimations = false;
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
