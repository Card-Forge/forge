package forge.screens.rogue;

import forge.animation.ForgeAnimation;
import java.util.function.Consumer;

/** A short animation on the GUI thread: progress runs from 0 to 1 over the duration. Ending early jumps to 1. */
final class RogueMotion extends ForgeAnimation {
    private final float duration;
    private final Consumer<Float> step;
    private final Runnable done;
    private float progress;

    private RogueMotion(float duration, Consumer<Float> step, Runnable done) {
        this.duration = duration;
        this.step = step;
        this.done = done;
    }

    /** done may be null. */
    static RogueMotion play(float seconds, Consumer<Float> step, Runnable done) {
        RogueMotion motion = new RogueMotion(seconds, step, done);
        step.accept(0f);
        motion.start();
        return motion;
    }

    @Override
    protected boolean advance(float dt) {
        progress = Math.min(1, progress + dt / duration);
        step.accept(progress);
        return progress < 1;
    }

    @Override
    protected void onEnd(boolean endingAll) {
        step.accept(1f);
        if (done != null) {
            done.run();
        }
    }
}
