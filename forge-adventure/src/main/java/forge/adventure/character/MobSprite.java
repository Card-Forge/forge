package forge.adventure.character;

import forge.adventure.stage.GameStage;
import forge.adventure.util.Res;

public class MobSprite extends CharacterSprite {
    public MobSprite(GameStage stage) {
        super(Res.CurrentRes.GetFile("sprites/mob.atlas"),stage);
    }
}

