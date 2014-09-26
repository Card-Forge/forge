package forge.animation;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;

public abstract class ForgeAnimation {
    private static final List<ForgeAnimation> activeAnimations = new ArrayList<ForgeAnimation>();

    public void start() {
        if (activeAnimations.contains(this)) { return; } //prevent starting the same animation multiple times

        activeAnimations.add(this);
    }

    public static void advanceAll() {
        if (activeAnimations.isEmpty()) { return; }

        float dt = Gdx.graphics.getDeltaTime();
        for (int i = 0; i < activeAnimations.size(); i++) {
            if (!activeAnimations.get(i).advance(dt)) {
                activeAnimations.remove(i);
                i--;
            }
        }
    }

    public static void endAll() {
        if (activeAnimations.isEmpty()) { return; }

        activeAnimations.clear();
    }

    //return true if animation should continue, false to stop the animation
    protected abstract boolean advance(float dt);
}
