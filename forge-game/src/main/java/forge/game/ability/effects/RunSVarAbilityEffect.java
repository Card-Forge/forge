package forge.game.ability.effects;

import forge.game.ability.AbilityFactory;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;

public class RunSVarAbilityEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        String sVars = sa.getParam("SVars");
        List<Card> cards = getTargetCards(sa);
        if (sVars == null || cards.isEmpty()) {
        	return;
        }
        List<SpellAbility> validSA = new ArrayList<SpellAbility>();
        final boolean isTrigger = sa.hasParam("IsTrigger");
        for (final Card tgtC : cards) {
            if (!tgtC.hasSVar(sVars)) {
        	    continue;
        	}
            final SpellAbility actualSA = AbilityFactory.getAbility(tgtC.getSVar(sVars), tgtC);
            actualSA.setTrigger(isTrigger);
            actualSA.setActivatingPlayer(sa.getActivatingPlayer());
            actualSA.setDescription(tgtC.getName() + "'s ability");
            validSA.add(actualSA);
        }
        sa.getActivatingPlayer().getController().orderAndPlaySimultaneousSa(validSA);

    }
}
