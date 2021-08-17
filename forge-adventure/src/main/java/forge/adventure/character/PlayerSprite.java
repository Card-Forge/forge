package forge.adventure.character;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.world.WorldSave;

public class PlayerSprite extends CharacterSprite {
    private final float playerSpeed = 100f;
    private Vector2 direction = Vector2.Zero.cpy();
    private float playerSpeedModifier = 1f;

    public PlayerSprite() {
        super(WorldSave.getCurrentSave().player.spriteName());

        setOriginX(getWidth() / 2);
        avatar = WorldSave.getCurrentSave().player.avatar();
    }

    public void LoadPos() {
        setPosition(WorldSave.getCurrentSave().player.getWorldPosX(), WorldSave.getCurrentSave().player.getWorldPosY());
    }

    public void storePos() {
        WorldSave.getCurrentSave().player.setWorldPosX(getX());
        WorldSave.getCurrentSave().player.setWorldPosY(getY());
    }

    public Vector2 getMovementDirection() {
        return direction;
    }

    public void setMovementDirection(Vector2 dir) {
        direction = dir;
    }

    public void setMoveModifier(float speed) {
        playerSpeedModifier = speed;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        direction.setLength(playerSpeed * delta * playerSpeedModifier);
        moveBy(direction.x, direction.y);
    }

    public boolean isMoving() {
        return !direction.isZero();
    }
}
