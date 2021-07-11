package forge.adventure.character;

import forge.adventure.util.Res;

public class MobSprite extends CharacterSprite {
    public MobSprite() {
        super(Res.CurrentRes.GetFile("sprites/mob.atlas"));
    }
}

