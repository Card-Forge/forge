package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class DigUntilEffect extends SpellEffect {
    

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
            untilAmount = AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa);
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

        String type = "Card";
        if (sa.hasParam("Valid")) {
            type = sa.getParam("Valid");
        }

        int untilAmount = 1;
        if (sa.hasParam("Amount")) {
            untilAmount = AbilityFactory.calculateAmount(host, sa.getParam("Amount"), sa);
        }

        Integer maxRevealed = null;
        if (sa.hasParam("MaxRevealed")) {
            maxRevealed = AbilityFactory.calculateAmount(host, sa.getParam("MaxRevealed"), sa);
        }

        final boolean remember = sa.hasParam("RememberFound");

        final Target tgt = sa.getTarget();

        final ZoneType foundDest = ZoneType.smartValueOf(sa.getParam("FoundDestination"));
        final int foundLibPos = AbilityFactory.calculateAmount(host, sa.getParam("FoundLibraryPosition"), sa);
        final ZoneType revealedDest = ZoneType.smartValueOf(sa.getParam("RevealedDestination"));
        final int revealedLibPos = AbilityFactory.calculateAmount(host, sa.getParam("RevealedLibraryPosition"), sa);

        for (final Player p : getTargetPlayers(sa)) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final List<Card> found = new ArrayList<Card>();
                final List<Card> revealed = new ArrayList<Card>();

                final PlayerZone library = p.getZone(ZoneType.Library);

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

                if (revealed.size() > 0) {
                    GuiChoose.one(p + " revealed: ", revealed);
                }

                // TODO Allow Human to choose the order
                if (foundDest != null) {
                    final Iterator<Card> itr = found.iterator();
                    while (itr.hasNext()) {
                        final Card c = itr.next();
                        if (sa.hasParam("GainControl") && foundDest.equals(ZoneType.Battlefield)) {
                            c.addController(sa.getSourceCard());
                            Singletons.getModel().getGame().getAction().moveTo(c.getController().getZone(foundDest), c);
                        } else {
                            Singletons.getModel().getGame().getAction().moveTo(foundDest, c, foundLibPos);
                        }
                        revealed.remove(c);
                    }
                }

                if (sa.hasParam("RememberRevealed")) {
                    for (final Card c : revealed) {
                        host.addRemembered(c);
                    }
                }

                final Iterator<Card> itr = revealed.iterator();
                while (itr.hasNext()) {
                    final Card c = itr.next();
                    Singletons.getModel().getGame().getAction().moveTo(revealedDest, c, revealedLibPos);
                }

                if (sa.hasParam("Shuffle")) {
                    p.shuffle();
                }
            } // end foreach player
        }
    } // end resolve

}