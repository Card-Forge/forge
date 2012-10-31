package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactoryReveal;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class RevealEffect extends SpellEffect {
    /**
     * <p>
     * revealResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();
        final boolean anyNumber = params.containsKey("AnyNumber");

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> handChoices = p.getCardsIn(ZoneType.Hand);
                if (handChoices.size() > 0) {
                    final List<Card> revealed = new ArrayList<Card>();
                    if (params.containsKey("Random")) {
                        revealed.add(CardUtil.getRandom(handChoices));
                        GuiChoose.oneOrNone("Revealed card(s)", revealed);
                    } else {
                        List<Card> valid = new ArrayList<Card>(handChoices);
                        int max = 1;
                        if (params.containsKey("RevealValid")) {
                            valid = CardLists.getValidCards(valid, params.get("RevealValid"), p, host);
                        }
                        if (params.containsKey("AnyNumber")) {
                            max = valid.size();
                        }
                        revealed.addAll(AbilityFactoryReveal.getRevealedList(sa.getActivatingPlayer(), valid, max, anyNumber));
                        if (sa.getActivatingPlayer().isComputer()) {
                            GuiChoose.oneOrNone("Revealed card(s)", revealed);
                        }
                    }

                    if (params.containsKey("RememberRevealed")) {
                        for (final Card rem : revealed) {
                            host.addRemembered(rem);
                        }
                    }

                }
            }
        }
    }

    /**
     * <p>
     * revealStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        if (sa instanceof AbilitySub) {
            sb.append(" ");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if (tgtPlayers.size() > 0) {
            sb.append(tgtPlayers.get(0)).append(" reveals ");
            if (params.containsKey("AnyNumber")) {
                sb.append("any number of cards ");
            } else {
                sb.append("a card ");
            }
            if (params.containsKey("Random")) {
                sb.append("at random ");
            }
            sb.append("from his or her hand.");
        } else {
            sb.append("Error - no target players for RevealHand. ");
        }
    
        return sb.toString();
    }

} // end class AbilityFactory_Reveal