package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.util.MyRandom;

public class DigEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final StringBuilder sb = new StringBuilder();
        final int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(host.getController()).append(" looks at the top ").append(numToDig);
        sb.append(" card");
        if (numToDig != 1) {
            sb.append("s");
        }
        sb.append(" of ");
        if (tgtPlayers.contains(host.getController())) {
            sb.append("his or her ");
        } else {
            for (final Player p : tgtPlayers) {
                sb.append(p).append("'s ");
            }
        }
        sb.append("library.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = sa.getActivatingPlayer();
        Player choser = player;
        int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);
        final ZoneType destZone1 = sa.hasParam("DestinationZone") ? ZoneType.smartValueOf(sa.getParam("DestinationZone"))
                : ZoneType.Hand;
        final ZoneType destZone2 = sa.hasParam("DestinationZone2") ? ZoneType.smartValueOf(sa.getParam("DestinationZone2")) : ZoneType.Library;

        final int libraryPosition = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : -1;
        int destZone1ChangeNum = 1;
        final boolean mitosis = sa.hasParam("Mitosis");
        String changeValid = sa.hasParam("ChangeValid") ? sa.getParam("ChangeValid") : "";
        //andOrValid is for cards with "creature card and/or a land card"
        String andOrValid = sa.hasParam("AndOrValid") ? sa.getParam("AndOrValid") : "";
        final boolean anyNumber = sa.hasParam("AnyNumber");

        final int libraryPosition2 = sa.hasParam("LibraryPosition2") ? Integer.parseInt(sa.getParam("LibraryPosition2")) : -1;
        final boolean optional = sa.hasParam("Optional");
        final boolean noMove = sa.hasParam("NoMove");
        final boolean skipReorder = sa.hasParam("SkipReorder");
        boolean changeAll = false;
        boolean allButOne = false;
        final ArrayList<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        if (sa.hasParam("ChangeNum")) {
            if (sa.getParam("ChangeNum").equalsIgnoreCase("All")) {
                changeAll = true;
            } else if (sa.getParam("ChangeNum").equalsIgnoreCase("AllButOne")) {
                allButOne = true;
            } else {
                destZone1ChangeNum = AbilityUtils.calculateAmount(host, sa.getParam("ChangeNum"), sa);
            }
        }

        final Target tgt = sa.getTarget();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("Choser")) {
            final List<Player> chosers = AbilityUtils.getDefinedPlayers(sa.getSourceCard(), sa.getParam("Choser"), sa);
            if (!chosers.isEmpty()) {
                choser = chosers.get(0);
            }
        }

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            final List<Card> top = new ArrayList<Card>();
            List<Card> valid = new ArrayList<Card>();
            final List<Card> rest = new ArrayList<Card>();
            final PlayerZone library = p.getZone(ZoneType.Library);

            numToDig = Math.min(numToDig, library.size());
            for (int i = 0; i < numToDig; i++) {
                top.add(library.get(i));
            }

            if (top.size() > 0) {
                final Card dummy = new Card();
                dummy.setName("[No valid cards]");

                if (sa.hasParam("Reveal")) {
                    GuiChoose.one("Revealing cards from library", top);
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (sa.hasParam("RevealOptional")) {
                    String question = "Reveal: ";
                    for (final Card c : top) {
                        question += c + " ";
                    }
                    if (p.isHuman() && GuiDialog.confirm(host, question)) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                        // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    } else if (p.isComputer() && (top.get(0).isInstant() || top.get(0).isSorcery())) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                    }
                } else if (sa.hasParam("RevealValid")) {
                    final String revealValid = sa.getParam("RevealValid");
                    final List<Card> toReveal = CardLists.getValidCards(top, revealValid, host.getController(), host);
                    if (!toReveal.isEmpty()) {
                        GuiChoose.one("Revealing cards from library", toReveal);
                        if (sa.hasParam("RememberRevealed")) {
                            for (final Card one : toReveal) {
                                host.addRemembered(one);
                            }
                        }
                    }
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (choser.isHuman()) {
                    // show the user the revealed cards
                    GuiChoose.one("Looking at cards from library", top);
                }

                if ((sa.hasParam("RememberRevealed")) && !sa.hasParam("RevealValid")) {
                    for (final Card one : top) {
                        host.addRemembered(one);
                    }
                }

                if (!noMove) {
                    List<Card> movedCards = new ArrayList<Card>();
                    List<Card> andOrCards = new ArrayList<Card>();
                    for (final Card c : top) {
                        rest.add(c);
                    }
                    if (mitosis) {
                        valid = sharesNameWithCardOnBattlefield(top);
                    } else if (!changeValid.equals("")) {
                        if (changeValid.contains("ChosenType")) {
                            changeValid = changeValid.replace("ChosenType", host.getChosenType());
                        }
                        valid = CardLists.getValidCards(top, changeValid.split(","), host.getController(), host);
                        if (!andOrValid.equals("")) {
                            andOrCards = CardLists.getValidCards(top, andOrValid.split(","), host.getController(), host);
                            andOrCards.removeAll(valid);
                            valid.addAll(andOrCards);
                        }
                        if (valid.isEmpty() && choser.isHuman()) {
                            valid.add(dummy);
                        }
                    } else {
                        valid = top;
                    }

                    if (changeAll) {
                        movedCards.addAll(valid);
                    } else if (sa.hasParam("RandomChange")) {
                        int numChanging = Math.min(destZone1ChangeNum, valid.size());
                        movedCards = CardLists.getRandomSubList(valid, numChanging);
                    } else if (allButOne) {
                        movedCards.addAll(valid);
                        if (choser.isHuman()) {
                            Card chosen = null;
                            String prompt = "Choose a card to leave in ";
                            if (destZone2.equals(ZoneType.Library) && (libraryPosition2 == 0)) {
                                prompt = "Leave which card on top of the ";
                            }
                            chosen = GuiChoose.one(prompt + destZone2, valid);
                            movedCards.remove(chosen);
                        } else { // Computer
                            Card chosen = CardFactoryUtil.getBestAI(valid);
                            if (sa.getActivatingPlayer().isHuman() && p.isHuman()) {
                                chosen = CardFactoryUtil.getWorstAI(valid);
                            }
                            movedCards.remove(chosen);
                        }
                        if (sa.hasParam("RandomOrder")) {
                            final Random random = MyRandom.getRandom();
                            Collections.shuffle(movedCards, random);
                        }
                    } else {
                        int j = 0;
                        if (choser.isHuman()) {
                            while ((j < destZone1ChangeNum) || (anyNumber && (j < numToDig))) {
                                // let user get choice
                                if (valid.isEmpty()) {
                                    break;
                                }
                                Card chosen = null;
                                String prompt = "Choose a card to put into the ";
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == -1)) {
                                    prompt = "Chose a card to put on the bottom of the ";
                                }
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == 0)) {
                                    prompt = "Chose a card to put on top of the ";
                                }
                                if (anyNumber || optional) {
                                    chosen = GuiChoose.oneOrNone(prompt + destZone1, valid);
                                } else {
                                    chosen = GuiChoose.one(prompt + destZone1, valid);
                                }
                                if ((chosen == null) || chosen.getName().equals("[No valid cards]")) {
                                    break;
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = new ArrayList<Card>(andOrCards);
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                                // Singletons.getModel().getGameAction().revealToComputer()
                                // - for when this exists
                                j++;
                            }
                        } // human
                        else { // computer
                            int changeNum = Math.min(destZone1ChangeNum, valid.size());
                            if (anyNumber) {
                                changeNum = valid.size(); // always take all
                            }
                            for (j = 0; j < changeNum; j++) {
                                Card chosen = CardFactoryUtil.getBestAI(valid);
                                if (sa.getActivatingPlayer().isHuman() && p.isHuman()) {
                                    chosen = CardFactoryUtil.getWorstAI(valid);
                                }
                                if (chosen == null) {
                                    break;
                                }
                                if (changeValid.length() > 0) {
                                    GuiChoose.one("Computer picked: ", chosen);
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = andOrCards;
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                            }
                        }
                    }
                    if (sa.hasParam("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    Collections.reverse(movedCards);
                    for (Card c : movedCards) {
                        if (c.equals(dummy)) {
                            continue;
                        }
                        final PlayerZone zone = c.getOwner().getZone(destZone1);
                        if (zone.is(ZoneType.Library)) {
                            Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPosition);
                        } else {
                            c = Singletons.getModel().getGame().getAction().moveTo(zone, c);
                            if (destZone1.equals(ZoneType.Battlefield)) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                                if (sa.hasParam("Tapped")) {
                                    c.setTapped(true);
                                }
                            }
                            if (sa.hasParam("ExileFaceDown")) {
                                c.setState(CardCharacteristicName.FaceDown);
                            }
                            if (sa.hasParam("Imprint")) {
                                host.addImprinted(c);
                            }
                        }
                        if (sa.hasParam("ForgetOtherRemembered")) {
                            host.clearRemembered();
                        }
                        if (sa.hasParam("RememberChanged")) {
                            host.addRemembered(c);
                        }
                        rest.remove(c);
                    }
                    if (rest.contains(dummy)) {
                        rest.remove(dummy);
                    }

                    // now, move the rest to destZone2
                    if (destZone2.equals(ZoneType.Library)) {
                        if (choser.isHuman()) {
                            // put them in any order
                            while (rest.size() > 0) {
                                Card chosen;
                                if (!skipReorder && rest.size() > 1) {
                                    String prompt = "Put the rest on top of the library in any order";
                                    if (libraryPosition2 == -1) {
                                        prompt = "Put the rest on the bottom of the library in any order";
                                    }
                                    chosen = GuiChoose.one(prompt, rest);
                                } else {
                                    chosen = rest.get(0);
                                }
                                Singletons.getModel().getGame().getAction().moveToLibrary(chosen, libraryPosition2);
                                rest.remove(chosen);
                            }
                        } else { // Computer
                            for (int i = 0; i < rest.size(); i++) {
                                Singletons.getModel().getGame().getAction().moveToLibrary(rest.get(i), libraryPosition2);
                            }
                        }
                    } else {
                        // just move them randomly
                        for (int i = 0; i < rest.size(); i++) {
                            Card c = rest.get(i);
                            final PlayerZone toZone = c.getOwner().getZone(destZone2);
                            c = Singletons.getModel().getGame().getAction().moveTo(toZone, c);
                            if (destZone2.equals(ZoneType.Battlefield) && !keywords.isEmpty()) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                            }
                        }

                    }
                }
            }
        } // end foreach player
    } // end resolve

    // returns a List<Card> that is a subset of list with cards that share a name
    // with a permanent on the battlefield
    /**
     * <p>
     * sharesNameWithCardOnBattlefield.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> sharesNameWithCardOnBattlefield(final List<Card> list) {
        final List<Card> toReturn = new ArrayList<Card>();
        final List<Card> play = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        for (final Card c : list) {
            for (final Card p : play) {
                if (p.getName().equals(c.getName()) && !toReturn.contains(c)) {
                    toReturn.add(c);
                }
            }
        }
        return toReturn;
    }

}
