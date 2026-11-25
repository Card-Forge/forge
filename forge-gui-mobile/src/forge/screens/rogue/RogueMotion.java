package forge.screens.rogue;

import forge.animation.ForgeAnimation;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * An animation on the GUI thread. A short one runs progress from 0 to 1 over the duration, and ending early jumps to 1.
 * A loop repeats progress from 0 to 1 every period for as long as it is wanted.
 */
final class RogueMotion extends ForgeAnimation {
    private final float duration;
    private final Consumer<Float> step;
    private final Runnable done;
    /** Null for a short animation. */
    private final BooleanSupplier wanted;
    private float progress;

    private RogueMotion(float duration, Consumer<Float> step, Runnable done, BooleanSupplier wanted) {
        this.duration = duration;
        this.step = step;
        this.done = done;
        this.wanted = wanted;
    }

    /** done may be null. */
    static RogueMotion play(float seconds, Consumer<Float> step, Runnable done) {
        RogueMotion motion = new RogueMotion(seconds, step, done, null);
        step.accept(0f);
        motion.start();
        return motion;
    }

    /**
     * Repeats while wanted holds. It also ends when the screen changes, as every animation does, and then done runs, which
     * may be null. A loop must not restart itself from done: the screen change is still ending animations then.
     */
    static RogueMotion loop(float period, Consumer<Float> step, BooleanSupplier wanted, Runnable done) {
        RogueMotion motion = new RogueMotion(period, step, done, wanted);
        step.accept(0f);
        motion.start();
        return motion;
    }

    @Override
    protected boolean advance(float dt) {
        if (wanted != null) {
            progress = (progress + dt / duration) % 1f;
            step.accept(progress);
            return wanted.getAsBoolean();
        }
        progress = Math.min(1, progress + dt / duration);
        step.accept(progress);
        return progress < 1;
    }

    @Override
    protected void onEnd(boolean endingAll) {
        if (wanted == null) {
            step.accept(1f);
        }
        if (done != null) {
            done.run();
        }
    }
}
