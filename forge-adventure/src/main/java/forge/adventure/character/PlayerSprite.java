package forge.adventure.character;

import forge.adventure.stage.GameStage;
import forge.adventure.util.Res;
import forge.adventure.world.WorldSave;

public class PlayerSprite extends CharacterSprite {
    public PlayerSprite(GameStage stage) {
        super(Res.CurrentRes.GetFile("sprites/player.atlas"),stage);

        setOriginX(getWidth()/2);
    }

    public void LoadPos() {
        setPosition(WorldSave.getCurrentSave().player.getWorldPosX(),WorldSave.getCurrentSave().player.getWorldPosY());
    }
}
