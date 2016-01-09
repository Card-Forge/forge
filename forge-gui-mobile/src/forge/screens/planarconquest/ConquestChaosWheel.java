package forge.screens.planarconquest;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinTexture;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.util.Aggregates;
import forge.util.PhysicsObject;
import forge.util.ThreadUtil;

public class ConquestChaosWheel extends FOverlay {
    private final WheelSpinAnimation animation = new WheelSpinAnimation();

    public ConquestChaosWheel() {
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        super.setVisible(visible0);

        if (visible0) { //delay spin animation briefly
            ThreadUtil.delay(250, new Runnable() {
                @Override
                public void run() {
                    animation.start();
                }
            });
        }
    }

    @Override
    public void drawOverlay(Graphics g) {
        float padding = FOptionPane.PADDING;
        float wheelSize = getWidth() - 2 * padding;
        FSkinTexture.BG_CHAOS_WHEEL.drawRotated(g, padding, (getHeight() - wheelSize) / 2, wheelSize, wheelSize, animation.getWheelRotation());
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    private class WheelSpinAnimation extends ForgeAnimation {
        private final PhysicsObject rotationManager;

        private WheelSpinAnimation() {
            float initialPosition = Aggregates.randomInt(1, 8) * 45f - 22.5f; //-22.5f because wheel image slightly rotated initially
            float initialVelocity = Aggregates.randomInt(50, 100);
            float acceleration = Aggregates.randomInt(50, 100) * -1f;
            rotationManager = new PhysicsObject(new Vector2(initialPosition, 0), new Vector2(initialVelocity, 0), new Vector2(acceleration, 0), false);
        }

        private float getWheelRotation() {
            return rotationManager.getPosition().x;
        }

        @Override
        protected boolean advance(float dt) {
            rotationManager.advance(dt);
            Vector2 pos = rotationManager.getPosition();
            while (pos.x > 360f) { //loop back around
                pos.x -= 360f;
            }
            return rotationManager.isMoving();
        }

        @Override
        protected void onEnd(boolean endingAll) {
            ThreadUtil.delay(1000, new Runnable() {
                @Override
                public void run() {
                    hide();
                }
            });
        }
    }
}
