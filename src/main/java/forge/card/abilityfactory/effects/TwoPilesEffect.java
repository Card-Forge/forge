package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class TwoPilesEffect extends SpellEffect {

    // *************************************************************************
    // ***************************** TwoPiles **********************************
    // *************************************************************************

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        sb.append("Separate all ").append(valid).append(" cards ");

        for (final Player p : tgtPlayers) {
            sb.append(p).append(" ");
        }
        sb.append("controls into two piles.");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();
        ZoneType zone = null;
        boolean pile1WasChosen = true;

        if (sa.hasParam("Zone")) {
            zone = ZoneType.smartValueOf(sa.getParam("Zone"));
        }

        String valid = "";
        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        Player separator = card.getController();
        if (sa.hasParam("Separator")) {
            separator = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Separator"), sa).get(0);
        }

        Player chooser = tgtPlayers.get(0);
        if (sa.hasParam("Chooser")) {
            chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Chooser"), sa).get(0);
        }

        for (final Player p : tgtPlayers) {
            if ((tgt == null) || p.canBeTargetedBy(sa)) {
                final ArrayList<Card> pile1 = new ArrayList<Card>();
                final ArrayList<Card> pile2 = new ArrayList<Card>();
                List<Card> pool = new ArrayList<Card>();
                if (sa.hasParam("DefinedCards")) {
                    pool = new ArrayList<Card>(AbilityFactory.getDefinedCards(sa.getSourceCard(), sa.getParam("DefinedCards"), sa));
                } else {
                    pool = p.getCardsIn(zone);
                }
                pool = CardLists.getValidCards(pool, valid, card.getController(), card);
                int size = pool.size();

                // first, separate the cards into piles
                if (separator.isHuman()) {
                    final List<Card> firstPile = GuiChoose.getOrderChoices("Place into two piles", "Pile 1", -1, pool, null, card);
                    for (final Object o : firstPile) {
                        pile1.add((Card) o);
                    }

                    for (final Card c : pool) {
                        if (!pile1.contains(c)) {
                            pile2.add(c);
                        }
                    }
                } else if (size > 0) {
                    //computer separates
                    Card biggest = null;
                    Card smallest = null;
                    biggest = pool.get(0);
                    smallest = pool.get(0);

                    for (Card c : pool) {
                        if (c.getCMC() >= biggest.getCMC()) {
                            biggest = c;
                        }
                        if (c.getCMC() <= smallest.getCMC()) {
                            smallest = c;
                        }
                    }
                    pile1.add(biggest);

                    if (size > 3) {
                        pile1.add(smallest);
                    }
                    for (Card c : pool) {
                        if (!pile1.contains(c)) {
                            pile2.add(c);
                        }
                    }
                }

                System.out.println("Pile 1:" + pile1);
                System.out.println("Pile 2:" + pile2);
                card.clearRemembered();

                pile1WasChosen = selectPiles(sa, pile1, pile2, chooser, card, pool);

                // take action on the chosen pile
                if (sa.hasParam("ChosenPile")) {
                    final AbilityFactory afPile = new AbilityFactory();
                    final SpellAbility action = afPile.getAbility(card.getSVar(sa.getParam("ChosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);

                    AbilityFactory.resolve(action, false);
                }

                // take action on the chosen pile
                if (sa.hasParam("UnchosenPile")) {
                    //switch the remembered cards
                    card.clearRemembered();
                    if (pile1WasChosen) {
                        for (final Card c : pile2) {
                            card.addRemembered(c);
                        }
                    } else {
                        for (final Card c : pile1) {
                            card.addRemembered(c);
                        }
                    }
                    final AbilityFactory afPile = new AbilityFactory();
                    final SpellAbility action = afPile.getAbility(card.getSVar(sa.getParam("UnchosenPile")), card);
                    action.setActivatingPlayer(sa.getActivatingPlayer());
                    ((AbilitySub) action).setParent(sa);

                    AbilityFactory.resolve(action, false);
                }
            }
        }
    } // end twoPiles resolve

    private boolean selectPiles(final SpellAbility sa, ArrayList<Card> pile1, ArrayList<Card> pile2,
            Player chooser, Card card, List<Card> pool) {
        boolean pile1WasChosen = true;
        // then, the chooser picks a pile

        if (sa.hasParam("FaceDown")) {
            // Used for Phyrexian Portal, FaceDown Pile choosing
            if (chooser.isHuman()) {
                final String p1Str = String.format("Pile 1 (%s cards)", pile1.size());
                final String p2Str = String.format("Pile 2 (%s cards)", pile2.size());

                final String message = String.format("Choose a pile\n%s or %s", p1Str, p2Str);

                final Object[] possibleValues = { p1Str , p2Str };

                final Object playDraw = JOptionPane.showOptionDialog(null, message, "Choose a Pile",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                        possibleValues, possibleValues[0]);

                pile1WasChosen = playDraw.equals(0);
            }
            else {
                // AI will choose the first pile if it is larger or the same
                // TODO Improve this to be slightly more random to not be so predictable
                pile1WasChosen = pile1.size() >= pile2.size();
            }
        } else {
            if (chooser.isHuman()) {
                final Card[] disp = new Card[pile1.size() + pile2.size() + 2];
                disp[0] = new Card();
                disp[0].setName("Pile 1");
                for (int i = 0; i < pile1.size(); i++) {
                    disp[1 + i] = pile1.get(i);
                }
                disp[pile1.size() + 1] = new Card();
                disp[pile1.size() + 1].setName("Pile 2");
                for (int i = 0; i < pile2.size(); i++) {
                    disp[pile1.size() + i + 2] = pile2.get(i);
                }

                // make sure Pile 1 or Pile 2 is clicked on
                while (true) {
                    final Object o = GuiChoose.one("Choose a pile", disp);
                    final Card c = (Card) o;
                    String name = c.getName();

                    if (!(name.equals("Pile 1") || name.equals("Pile 2"))) {
                        continue;
                    }

                    pile1WasChosen = name.equals("Pile 1");
                    break;
                }
            } else {
                int cmc1 = CardFactoryUtil.evaluatePermanentList(new ArrayList<Card>(pile1));
                int cmc2 = CardFactoryUtil.evaluatePermanentList(new ArrayList<Card>(pile2));
                if (CardLists.getNotType(pool, "Creature").isEmpty()) {
                    cmc1 = CardFactoryUtil.evaluateCreatureList(new ArrayList<Card>(pile1));
                    cmc2 = CardFactoryUtil.evaluateCreatureList(new ArrayList<Card>(pile2));
                    System.out.println("value:" + cmc1 + " " + cmc2);
                }

                // for now, this assumes that the outcome will be bad
                // TODO: This should really have a ChooseLogic param to
                // figure this out
                pile1WasChosen = cmc1 >= cmc2;
                if (pile1WasChosen) {
                    JOptionPane.showMessageDialog(null, "Computer chooses the Pile 1", "",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Computer chooses the Pile 2", "",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        if (pile1WasChosen) {
            for (final Card z : pile1) {
                card.addRemembered(z);
            }
        } else {
            for (final Card z : pile2) {
                card.addRemembered(z);
            }
        }

        return pile1WasChosen;
    }
}
