package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class MillEffect extends SpellEffect {

    /**
     * <p>
     * millResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        final boolean bottom = params.containsKey("FromBottom");

        if (params.containsKey("ForgetOtherRemembered")) {
            source.clearRemembered();
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        ZoneType destination = ZoneType.smartValueOf(params.get("Destination"));
        if (destination == null) {
            destination = ZoneType.Graveyard;
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> milled = p.mill(numCards, destination, bottom);
                if (params.containsKey("RememberMilled")) {
                    for (final Card c : milled) {
                        source.addRemembered(c);
                    }
                }
                if (params.containsKey("Imprint")) {
                    for (final Card c : milled) {
                        source.addImprinted(c);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * millStackDescription.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final int numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard().getName()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }
    
        if (params.containsKey("StackDescription")) {
            if (params.get("StackDescription").equals("None")) {
                sb.append("");
            } else {
            sb.append(params.get("StackDescription"));
            }
        } else {
            for (final Player p : tgtPlayers) {
                sb.append(p.toString()).append(" ");
            }
    
            final ZoneType dest = ZoneType.smartValueOf(params.get("Destination"));
            if ((dest == null) || dest.equals(ZoneType.Graveyard)) {
                sb.append("mills ");
            } else if (dest.equals(ZoneType.Exile)) {
                sb.append("exiles ");
            } else if (dest.equals(ZoneType.Ante)) {
                sb.append("antes ");
            }
            sb.append(numCards);
            sb.append(" card");
            if (numCards != 1) {
                sb.append("s");
            }
            final String millPosition = params.containsKey("FromBottom") ? "bottom" : "top";
            sb.append(" from the " + millPosition + " of his or her library.");
        }
    
        return sb.toString();
    }
}