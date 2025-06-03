package forge.animation;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.badlogic.gdx.math.Rectangle;

import forge.Graphics;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

public class ForgeTransition extends ForgeAnimation {
    private static final FOverlay overlay = new FOverlay(null) {
        @Override protected void doLayout(final float width, final float height) {
        }
    };
    private static final Map<FDisplayObject, TransitionObject> transitionLookup = new LinkedHashMap<>();

    public static void queue(final FDisplayObject obj, final Rectangle destBounds, final float duration, final Runnable onFinished) {
        queue(obj, destBounds, duration, 0, false, onFinished);
    }
    public static void queue(final FDisplayObject obj, final Rectangle destBounds, final float duration, final float arcAmount, final boolean arcOriginBelow, final Runnable onFinished) {
        TransitionObject transitionObj = transitionLookup.get(obj);
        if (transitionObj == null) {
            transitionObj = new TransitionObject(obj);
            transitionLookup.put(obj, transitionObj);
            overlay.add(transitionObj);
            obj.setVisible(false); //hide original object while transition in progress
        }
        final ForgeTransition transition = new ForgeTransition(transitionObj, destBounds, duration, arcAmount, arcOriginBelow, onFinished);
        transitionObj.transitions.add(transition);
        if (transitionObj.transitions.size() == 1) {
            transition.start(); //start transition right away if first transition added
            overlay.setVisible(true);
        }
    }

    private final TransitionObject obj;
    /*private final Rectangle destBounds;
    private final float duration;
    private final float arcAmount;
    private final boolean arcOriginBelow;*/
    private final Runnable onFinished;

    private ForgeTransition(final TransitionObject obj0, final Rectangle destBounds0, final float duration0, final float arcAmount0, final boolean arcOriginBelow0, final Runnable onFinished0) {
        obj = obj0;
        /*destBounds = destBounds0;
        duration = duration0;
        arcAmount = arcAmount0;
        arcOriginBelow = arcOriginBelow0;*/
        onFinished = onFinished0;
    }

    @Override
    protected boolean advance(final float dt) {
        return false;
    }

    @Override
    protected void onEnd(final boolean endingAll) {
        if (onFinished != null) {
            onFinished.run();
        }

        if (endingAll) {
            transitionLookup.clear();
            return;
        }

        final int index = obj.transitions.indexOf(this);
        obj.transitions.remove(index);
        if (index == 0) {
            if (obj.transitions.isEmpty()) {
                transitionLookup.remove(obj.originalObj);
                overlay.remove(obj);
                obj.originalObj.setVisible(true);
                if (transitionLookup.isEmpty()) {
                    overlay.setVisible(false);
                }
            }
            else {
                obj.transitions.getFirst().start(); //start next transition if needed
            }
        }
    }

    private static class TransitionObject extends FDisplayObject {
        private final FDisplayObject originalObj;
        private final LinkedList<ForgeTransition> transitions = new LinkedList<>();

        private TransitionObject(final FDisplayObject originalObj0) {
            originalObj = originalObj0;
            setBounds(originalObj.screenPos.x, originalObj.screenPos.y, originalObj.getWidth(), originalObj.getHeight());
        }

        @Override
        public void draw(final Graphics g) {
            originalObj.draw(g);
        }
    }
}
