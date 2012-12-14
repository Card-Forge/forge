package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.GameActionUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class DrawEffect extends SpellEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getDefinedPlayersBeforeTargetOnes(sa);

        if (!tgtPlayers.isEmpty()) {

            sb.append(StringUtils.join(tgtPlayers, " and "));

            int numCards = 1;
            if (sa.hasParam("NumCards")) {
                numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
            }

            if (tgtPlayers.size() > 1) {
                sb.append(" each");
            }
            sb.append(" draw");
            if (tgtPlayers.size() == 1) {
                sb.append("s");
            }
            sb.append(" (").append(numCards).append(")");

            if (sa.hasParam("NextUpkeep")) {
                sb.append(" at the beginning of the next upkeep");
            }

            sb.append(".");
        }

        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card source = sa.getSourceCard();
        int numCards = 1;
        if (sa.hasParam("NumCards")) {
            numCards = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("NumCards"), sa);
        }

        final Target tgt = sa.getTarget();

        final boolean optional = sa.hasParam("OptionalDecider");
        final boolean slowDraw = sa.hasParam("NextUpkeep");

        for (final Player p : getDefinedPlayersBeforeTargetOnes(sa)) {
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
                // Some multiplayer compatibility so that calculated draw amounts
                // can be localised to the player
                if (sa.hasParam("LocalCount")) {
                    // playerXCount needs a player array
                    final ArrayList<Player> players = new ArrayList<Player>();
                    players.add(p);
                    numCards = sa.getParam("NumCards").matches("[0-9][0-9]?") ? Integer.parseInt(sa.getParam("NumCards"))
                                    : CardFactoryUtil.playerXCount(players,
                                            sa.getSourceCard().getSVar(sa.getParam("NumCards")), sa.getSourceCard());
                }

                if (slowDraw) {
                    for (int i = 0; i < numCards; i++) {
                        p.addSlowtripList(source);
                    }
                } else {
                    final List<Card> drawn = p.drawCards(numCards);
                    if (sa.hasParam("Reveal")) {
                        GuiChoose.one("Revealing drawn cards", drawn);
                    }
                    if (sa.hasParam("RememberDrawn")) {
                        for (final Card c : drawn) {
                            source.addRemembered(c);
                        }
                    }

                }

            }
        }
    } // drawResolve()
}
