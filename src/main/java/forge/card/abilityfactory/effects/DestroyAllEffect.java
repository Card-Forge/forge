package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DestroyAllEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final boolean noRegen = sa.hasParam("NoRegen");

        ArrayList<Card> tgtCards;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtCards = tgt.getTargetCards();
        } else {
            tgtCards = new ArrayList<Card>();
            tgtCards.add(sa.getSourceCard());
        }

        sb.append(sa.getSourceCard().getName()).append(" - Destroy permanents.");

        if (noRegen) {
            sb.append(" They can't be regenerated");
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {

        final boolean noRegen = sa.hasParam("NoRegen");
        final Card card = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        Player targetPlayer = null;
        if (tgt != null) {
            for (final Object o : tgt.getTargets()) {
                if (o instanceof Player) {
                    targetPlayer = (Player) o;
                }
            }
        }

        String valid = "";

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        // Ugh. If calculateAmount needs to be called with DestroyAll it _needs_
        // to use the X variable
        // We really need a better solution to this
        if (valid.contains("X")) {
            valid = valid.replace("X", Integer.toString(AbilityFactory.calculateAmount(card, "X", sa)));
        }

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

        if (targetPlayer != null) {
            list = CardLists.filterControlledBy(list, targetPlayer);
        }

        list = AbilityFactory.filterListByType(list, valid, sa);

        final boolean remDestroyed = sa.hasParam("RememberDestroyed");
        if (remDestroyed) {
            card.clearRemembered();
        }

        if (noRegen) {
            for (int i = 0; i < list.size(); i++) {
                if (Singletons.getModel().getGame().getAction().destroyNoRegeneration(list.get(i)) && remDestroyed) {
                    card.addRemembered(list.get(i));
                }
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (Singletons.getModel().getGame().getAction().destroy(list.get(i)) && remDestroyed) {
                    card.addRemembered(list.get(i));
                }
            }
        }
    }

}
