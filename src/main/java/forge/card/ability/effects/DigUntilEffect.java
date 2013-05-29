package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.MyRandom;

public class DigUntilEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = "Card";
        if (sa.hasParam("ValidDescription")) {
            desc = sa.getParam("ValidDescription");
        }

        int untilAmount = 1;
        if (sa.hasParam("Amount")) {
            untilAmount = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa);
        }

        for (final Player pl : getTargetPlayers(sa)) {
            sb.append(pl).append(" ");
        }

        sb.append("reveals cards from his or her library until revealing ");
        sb.append(untilAmount).append(" ").append(desc).append(" card");
        if (untilAmount != 1) {
            sb.append("s");
        }
        sb.append(". Put ");

        final ZoneType found = ZoneType.smartValueOf(sa.getParam("FoundDestination"));
        final ZoneType revealed = ZoneType.smartValueOf(sa.getParam("RevealedDestination"));
        if (found != null) {

            sb.append(untilAmount > 1 ? "those cards" : "that card");
            sb.append(" ");

            if (found.equals(ZoneType.Hand)) {
                sb.append("into his or her hand ");
            }

            if (revealed.equals(ZoneType.Graveyard)) {
                sb.append("and all other cards into his or her graveyard.");
            }
            if (revealed.equals(ZoneType.Exile)) {
                sb.append("and exile all other cards revealed this way.");
            }
        } else {
            if (revealed.equals(ZoneType.Hand)) {
                sb.append("all cards revealed this way into his or her hand");
            }
        }
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();

        String[] type = new String[]{"Card"};
        if (sa.hasParam("Valid")) {
            type = sa.getParam("Valid").split(",");
        }

        int untilAmount = 1;
        if (sa.hasParam("Amount")) {
            untilAmount = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);
        }

        Integer maxRevealed = null;
        if (sa.hasParam("MaxRevealed")) {
            maxRevealed = AbilityUtils.calculateAmount(host, sa.getParam("MaxRevealed"), sa);
        }

        final boolean remember = sa.hasParam("RememberFound");

        final Target tgt = sa.getTarget();

        final ZoneType foundDest = ZoneType.smartValueOf(sa.getParam("FoundDestination"));
        final int foundLibPos = AbilityUtils.calculateAmount(host, sa.getParam("FoundLibraryPosition"), sa);
        final ZoneType revealedDest = ZoneType.smartValueOf(sa.getParam("RevealedDestination"));
        final int revealedLibPos = AbilityUtils.calculateAmount(host, sa.getParam("RevealedLibraryPosition"), sa);
        final ZoneType noneFoundDest = ZoneType.smartValueOf(sa.getParam("NoneFoundDestination"));
        final int noneFoundLibPos = AbilityUtils.calculateAmount(host, sa.getParam("NoneFoundLibraryPosition"), sa);
        final ZoneType digSite = sa.hasParam("DigZone") ? ZoneType.smartValueOf(sa.getParam("DigZone")) : ZoneType.Library;
        boolean shuffle = sa.hasParam("Shuffle");

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                List<Card> found = new ArrayList<Card>();
                List<Card> revealed = new ArrayList<Card>();

                final PlayerZone library = p.getZone(digSite);

                final int maxToDig = maxRevealed != null ? maxRevealed : library.size();

                for (int i = 0; i < maxToDig; i++) {
                    final Card c = library.get(i);
                    revealed.add(c);
                    if (c.isValid(type, sa.getActivatingPlayer(), host)) {
                        found.add(c);
                        if (remember) {
                            host.addRemembered(c);
                        }
                        if (found.size() == untilAmount) {
                            break;
                        }
                    }
                }

                if (shuffle && sa.hasParam("ShuffleCondition")) {
                    if (sa.getParam("ShuffleCondition").equals("NoneFound")) {
                        shuffle = found.isEmpty();
                    }
                }

                if (revealed.size() > 0) {
                    GuiChoose.one(p + " revealed: ", revealed);
                }
                final GameState game = p.getGame();

                if (foundDest != null) {
                    // Allow ordering of found cards
                    if ((foundDest.isKnown()) && found.size() >= 2) {
                        found = p.getController().orderMoveToZoneList(found, foundDest);
                    }

                    final Iterator<Card> itr = found.iterator();
                    while (itr.hasNext()) {
                        final Card c = itr.next();
                        if (sa.hasParam("GainControl") && foundDest.equals(ZoneType.Battlefield)) {
                            c.setController(sa.getActivatingPlayer(), game.getNextTimestamp());
                            game.getAction().moveTo(c.getController().getZone(foundDest), c);
                        } else if (sa.hasParam("NoMoveFound") && foundDest.equals(ZoneType.Library)) {
                            //Don't do anything
                        } else {
                            game.getAction().moveTo(foundDest, c, foundLibPos);
                        }
                        revealed.remove(c);
                    }
                }

                if (sa.hasParam("RememberRevealed")) {
                    for (final Card c : revealed) {
                        host.addRemembered(c);
                    }
                }
                if (sa.hasParam("ImprintRevealed")) {
                    for (final Card c : revealed) {
                        host.addImprinted(c);
                    }
                }
                if (sa.hasParam("RevealRandomOrder")) {
                    final Random random = MyRandom.getRandom();
                    Collections.shuffle(revealed, random);
                }

                if (sa.hasParam("NoneFoundDestination") && found.size() < untilAmount) {
                 // Allow ordering the revealed cards
                    if ((noneFoundDest.isKnown()) && revealed.size() >= 2) {
                        revealed = p.getController().orderMoveToZoneList(revealed, noneFoundDest);
                    }
                    if (noneFoundDest == ZoneType.Library && !shuffle
                            && !sa.hasParam("RevealRandomOrder") && revealed.size() >= 2) {
                        revealed = p.getController().orderMoveToZoneList(revealed, noneFoundDest);
                    }

                    final Iterator<Card> itr = revealed.iterator();
                    while (itr.hasNext()) {
                        final Card c = itr.next();
                        game.getAction().moveTo(noneFoundDest, c, noneFoundLibPos);
                    }
                } else {
                 // Allow ordering the rest of the revealed cards
                    if ((revealedDest.isKnown()) && revealed.size() >= 2) {
                        revealed = p.getController().orderMoveToZoneList(revealed, revealedDest);
                    }
                    if (revealedDest == ZoneType.Library && !shuffle
                            && !sa.hasParam("RevealRandomOrder") && revealed.size() >= 2) {
                        revealed = p.getController().orderMoveToZoneList(revealed, revealedDest);
                    }

                    final Iterator<Card> itr = revealed.iterator();
                    while (itr.hasNext()) {
                        final Card c = itr.next();
                        game.getAction().moveTo(revealedDest, c, revealedLibPos);
                    }
                }

                if (shuffle) {
                    p.shuffle();
                }
            } // end foreach player
        }
    } // end resolve

}
