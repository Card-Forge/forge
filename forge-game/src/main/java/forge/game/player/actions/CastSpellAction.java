package forge.game.player.actions;

import forge.game.GameEntityView;

public class CastSpellAction extends PlayerAction {
    public CastSpellAction(GameEntityView cardView) {
        super(cardView, "Cast spell");
    }

    @Override
    public String describe() {
        return localize("lblMacroActionCastSpell", describeEntity());
    }
}
