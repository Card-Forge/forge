package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;

public class ChangeZoneResolveEffect extends SpellAbilityEffect {

    public ChangeZoneResolveEffect() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Game game = sa.getHostCard().getGame();
        CardZoneTable table = sa.getChangeZoneTable();
        if (table != null) {

            table.triggerChangesZoneAll(game, sa);
            table.clear();
        }
    }

    /* (non-Javadoc)
     * @see forge.game.ability.SpellAbilityEffect#getStackDescription(forge.game.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "";
    }
}
