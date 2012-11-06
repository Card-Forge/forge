package forge.card.abilityfactory.effects;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.GameActionUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class DrawEffect extends SpellEffect {
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
    
        final String conditionDesc = params.get("ConditionDescription");
        if (conditionDesc != null) {
            sb.append(conditionDesc).append(" ");
        }
    
        final List<Player> tgtPlayers = getDefinedPlayersBeforeTargetOnes(sa, params);
    
    
        if (!tgtPlayers.isEmpty()) {
            
            sb.append(StringUtils.join(tgtPlayers, " and "));
            
            int numCards = 1;
            if (params.containsKey("NumCards")) {
                numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
            }
    
            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(" draw");
            if (tgtPlayers.size() == 1) {
                sb.append("s");
            }
            sb.append(" (").append(numCards).append(")");
    
            if (params.containsKey("NextUpkeep")) {
                sb.append(" at the beginning of the next upkeep");
            }
    
            sb.append(".");
        }
    
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int numCards = 1;
        if (params.containsKey("NumCards")) {
            numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("NumCards"), sa);
        }

        final Target tgt = sa.getTarget();

        final boolean optional = params.containsKey("OptionalDecider");
        final boolean slowDraw = params.containsKey("NextUpkeep");

        for (final Player p : getDefinedPlayersBeforeTargetOnes(sa, params)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                if (optional) {
                    if (p.isComputer()) {
                        if (numCards >= p.getCardsIn(ZoneType.Library).size()) {
                            // AI shouldn't itself
                            continue;
                        }
                    } else {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("Do you want to draw ").append(numCards).append(" cards(s)");

                        if (slowDraw) {
                            sb.append(" next upkeep");
                        }

                        sb.append("?");

                        if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                            continue;
                        }
                    }
                }

                if (slowDraw) {
                    for (int i = 0; i < numCards; i++) {
                        p.addSlowtripList(source);
                    }
                } else {
                    final List<Card> drawn = p.drawCards(numCards);
                    if (params.containsKey("Reveal")) {
                        GuiChoose.one("Revealing drawn cards", drawn);
                    }
                    if (params.containsKey("RememberDrawn")) {
                        for (final Card c : drawn) {
                            source.addRemembered(c);
                        }
                    }

                }

            }
        }
    } // drawResolve()
}