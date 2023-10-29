package forge.adventure.character;

import com.badlogic.gdx.math.Vector2;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.Scene;
import forge.adventure.stage.GameStage;
import forge.adventure.util.Config;
import forge.adventure.util.Current;

/**
 * Class that will represent the player sprite on the map
 */
public class PlayerSprite extends CharacterSprite {
    private final float playerSpeed;
    private final Vector2 direction = Vector2.Zero.cpy();
    private float playerSpeedModifier = 1f;
    private float playerSpeedEquipmentModifier = 1f;
    GameStage gameStage;

    public PlayerSprite(GameStage gameStage) {
        super(AdventurePlayer.current().spriteName());
        this.gameStage = gameStage;
        setOriginX(getWidth() / 2);
        Current.player().onPlayerChanged(PlayerSprite.this::updatePlayer);

        playerSpeed = Config.instance().getConfigData().playerBaseSpeed;

        //Attach signals here.
        Current.player().onBlessing(() -> playerSpeedEquipmentModifier = Current.player().equipmentSpeed());
        Current.player().onEquipmentChanged(() -> playerSpeedEquipmentModifier = Current.player().equipmentSpeed());
    }

    private void updatePlayer() {
        load(AdventurePlayer.current().spriteName());
    }

    public void LoadPos() {
        setPosition(AdventurePlayer.current().getWorldPosX(), AdventurePlayer.current().getWorldPosY());
    }

    public void storePos() {
        AdventurePlayer.current().setWorldPosX(getX());
        AdventurePlayer.current().setWorldPosY(getY());
    }

    public Vector2 getMovementDirection() {
        return direction;
    }

    public void setMovementDirection(final Vector2 dir) {
        direction.set(dir);
    }

    public void setMoveModifier(float speed) {
        playerSpeedModifier = speed;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        direction.setLength(playerSpeed * delta * playerSpeedModifier*playerSpeedEquipmentModifier);
        Vector2 previousDirection = getMovementDirection().cpy();
        Scene previousScene = forge.Forge.getCurrentScene();

        if(!direction.isZero()) {
            gameStage.prepareCollision(pos(),direction,boundingRect);
            direction.set(gameStage.adjustMovement(direction,boundingRect));
            moveBy(direction.x, direction.y);

            // If the player is blocked by an obstacle, and they haven't changed scenes,
            // they will keep trying to move in that direction
            if (previousScene == forge.Forge.getCurrentScene()) {
                direction.set(previousDirection.cpy());
            }
        }
    }

    public boolean isMoving() {
        return !direction.isZero();
    }

    public void stop() {
        direction.setZero();
        setAnimation(AnimationTypes.Idle);
    }

    public void setPosition(Vector2 oldPosition) {
        setPosition(oldPosition.x, oldPosition.y);
    }
}
