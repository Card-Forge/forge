package forge.screens.planarconquest;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinImage;
import forge.assets.FSkinTexture;
import forge.planarconquest.ConquestEvent.ConquestEventReward;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.util.Aggregates;
import forge.util.Callback;
import forge.util.PhysicsObject;
import forge.util.ThreadUtil;

public class ConquestChaosWheel extends FOverlay {
    public static void spin(Callback<ConquestEventReward> callback0) {
        ConquestChaosWheel wheel = new ConquestChaosWheel(callback0);
        wheel.show();
    }

    private final WheelSpinAnimation animation = new WheelSpinAnimation();
    private final Callback<ConquestEventReward> callback;

    private ConquestChaosWheel(Callback<ConquestEventReward> callback0) {
        callback = callback0;
    }

    @Override
    public void setVisible(boolean visible0) {
        if (this.isVisible() == visible0) { return; }

        super.setVisible(visible0);

        if (visible0) {
            animation.start();
        }
    }

    @Override
    public void drawOverlay(Graphics g) {
        //draw wheel
        float x = FOptionPane.PADDING;
        float wheelSize = getWidth() - 2 * x;
        float y = (getHeight() - wheelSize) / 2;
        FSkinTexture.BG_CHAOS_WHEEL.drawRotated(g, x, y, wheelSize, wheelSize, animation.getWheelRotation());

        //draw spoke at top using Planeswalker icon
        float spokeSize = wheelSize * 0.15f;
        FSkinImage.PW_BADGE_UNCOMMON.draw(g, x + (wheelSize - spokeSize) / 2, y - spokeSize * 0.75f, spokeSize, spokeSize);
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    private class WheelSpinAnimation extends ForgeAnimation {
        private final PhysicsObject rotationManager;

        private WheelSpinAnimation() {
            float initialPosition = Aggregates.randomInt(1, 8) * 45f - 22.5f; //-22.5f because wheel image slightly rotated initially
            float initialVelocity = Aggregates.randomInt(360, 720);
            float acceleration = Aggregates.randomInt(50, 100) * -1f;
            rotationManager = new PhysicsObject(new Vector2(initialPosition, 0), new Vector2(initialVelocity, 0), new Vector2(acceleration, 0), false);
        }

        private float getWheelRotation() {
            return -rotationManager.getPosition().x; //use negative so wheel rotates clockwise
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
                    callback.run(ConquestEventReward.getReward(getWheelRotation()));
                }
            });
        }
    }

    @Override
    public boolean keyDown(int keyCode) {
        return true; //suppress key pressing while this overlay is open
    }
}
