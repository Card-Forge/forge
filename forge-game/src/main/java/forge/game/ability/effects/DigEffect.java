package forge.game.ability.effects;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DigEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final StringBuilder sb = new StringBuilder();
        final int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        sb.append(host.getController()).append(" looks at the top ");
        sb.append(Lang.nounWithAmount(numToDig, "card")).append(" of ");

        if (tgtPlayers.contains(host.getController())) {
            sb.append("his or her ");
        }
        else {
            for (final Player p : tgtPlayers) {
                sb.append(Lang.getPossesive(p.getName())).append(" ");
            }
        }
        sb.append("library.");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();
        Player chooser = player;
        int numToDig = AbilityUtils.calculateAmount(host, sa.getParam("DigNum"), sa);

        final ZoneType srcZone = sa.hasParam("SourceZone") ? ZoneType.smartValueOf(sa.getParam("SourceZone")) : ZoneType.Library;

        final ZoneType destZone1 = sa.hasParam("DestinationZone") ? ZoneType.smartValueOf(sa.getParam("DestinationZone")) : ZoneType.Hand;
        final ZoneType destZone2 = sa.hasParam("DestinationZone2") ? ZoneType.smartValueOf(sa.getParam("DestinationZone2")) : ZoneType.Library;

        int libraryPosition = sa.hasParam("LibraryPosition") ? Integer.parseInt(sa.getParam("LibraryPosition")) : -1;
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

        // A hack for cards like Explorer's Scope that need to ensure that a card is revealed to the player activating the ability
        final boolean forceRevealToController = sa.hasParam("ForceRevealToController");

        // These parameters are used to indicate that a dialog box must be show to the player asking if the player wants to proceed
        // with an optional ability, otherwise the optional ability is skipped.
        final boolean mayBeSkipped = sa.hasParam("PromptToSkipOptionalAbility");
        final String optionalAbilityPrompt = sa.hasParam("OptionalAbilityPrompt") ? sa.getParam("OptionalAbilityPrompt") : "";

        boolean changeAll = false;
        boolean allButOne = false;
        final List<String> keywords = new ArrayList<String>();
        if (sa.hasParam("Keywords")) {
            keywords.addAll(Arrays.asList(sa.getParam("Keywords").split(" & ")));
        }

        if (sa.hasParam("ChangeNum")) {
            if (sa.getParam("ChangeNum").equalsIgnoreCase("All")) {
                changeAll = true;
            }
            else if (sa.getParam("ChangeNum").equalsIgnoreCase("AllButOne")) {
                allButOne = true;
            }
            else {
                destZone1ChangeNum = AbilityUtils.calculateAmount(host, sa.getParam("ChangeNum"), sa);
            }
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("Choser")) {
            final List<Player> choosers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Choser"), sa);
            if (!choosers.isEmpty()) {
                chooser = choosers.get(0);
            }
        }

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            final CardCollection top = new CardCollection();
            final CardCollection rest = new CardCollection();
            final PlayerZone sourceZone = p.getZone(srcZone);

            numToDig = Math.min(numToDig, sourceZone.size());
            for (int i = 0; i < numToDig; i++) {
                top.add(sourceZone.get(i));
            }

            if (!top.isEmpty()) {
                DelayedReveal delayedReveal = null;
                boolean hasRevealed = true;
                if (sa.hasParam("Reveal")) {
                    game.getAction().reveal(top, p, false);
                }
                else if (sa.hasParam("RevealOptional")) {
                    String question = "Reveal: " + Lang.joinHomogenous(top) +"?";

                    hasRevealed = p.getController().confirmAction(sa, null, question);
                    if (hasRevealed) {
                        game.getAction().reveal(top, p);
                    }
                }
                else if (sa.hasParam("RevealValid")) {
                    final String revealValid = sa.getParam("RevealValid");
                    final CardCollection toReveal = CardLists.getValidCards(top, revealValid, host.getController(), host);
                    if (!toReveal.isEmpty()) {
                        game.getAction().reveal(toReveal, host.getController());
                        if (sa.hasParam("RememberRevealed")) {
                            for (final Card one : toReveal) {
                                host.addRemembered(one);
                            }
                        }
                    }
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                }
                else if (!sa.hasParam("NoLooking")) {
                    // show the user the revealed cards
                    delayedReveal = new DelayedReveal(top, srcZone, PlayerView.get(p));

                    if (noMove) {
                        // Let the activating player see the cards even if they're not moved
                        game.getAction().revealTo(top, player);
                    }
                }

                if (sa.hasParam("RememberRevealed") && !sa.hasParam("RevealValid") && hasRevealed) {
                    for (final Card one : top) {
                        host.addRemembered(one);
                    }
                }

                if (!noMove) {
                    CardCollection movedCards;
                    CardCollection andOrCards;
                    for (final Card c : top) {
                        rest.add(c);
                    }
                    CardCollection valid;
                    if (mitosis) {
                        valid = sharesNameWithCardOnBattlefield(game, top);
                        andOrCards = new CardCollection();
                    }
                    else if (!changeValid.isEmpty()) {
                        if (changeValid.contains("ChosenType")) {
                            changeValid = changeValid.replace("ChosenType", host.getChosenType());
                        }
                        valid = CardLists.getValidCards(top, changeValid.split(","), host.getController(), host, sa);
                        if (!andOrValid.equals("")) {
                            andOrCards = CardLists.getValidCards(top, andOrValid.split(","), host.getController(), host, sa);
                            andOrCards.removeAll((Collection<?>)valid);
                            valid.addAll(andOrCards);
                        }
                        else {
                            andOrCards = new CardCollection();
                        }
                    }
                    else {
                        // If all the cards are valid choices, no need for a separate reveal dialog to the chooser.
                        if (p == chooser) {
                            delayedReveal = null;
                        }
                        valid = top;
                        andOrCards = new CardCollection();
                    }

                    if (forceRevealToController) {
                        // Force revealing the card to the player activating the ability (e.g. Explorer's Scope)
                        game.getAction().revealTo(top, player);
                    }

                    // Optional abilities that use a dialog box to prompt the user to skip the ability (e.g. Explorer's Scope, Quest for Ula's Temple)
                    if (optional && mayBeSkipped && !valid.isEmpty()) {
                        String prompt = !optionalAbilityPrompt.isEmpty() ? optionalAbilityPrompt : "Would you like to proceed with the optional ability for " + sa.getHostCard() + "?\n\n(" + sa.getDescription() + ")";
                        if (!p.getController().confirmAction(sa, null, prompt.replace("CARDNAME", sa.getHostCard().getName()))) {
                            return;
                        }
                    }

                    if (changeAll) {
                        movedCards = new CardCollection(valid);
                    }
                    else if (sa.hasParam("RandomChange")) {
                        int numChanging = Math.min(destZone1ChangeNum, valid.size());
                        movedCards = CardLists.getRandomSubList(valid, numChanging);
                    }
                    else if (allButOne) {
                        movedCards = new CardCollection(valid);
                        String prompt;
                        if (destZone2.equals(ZoneType.Library) && libraryPosition2 == 0) {
                            prompt = "Choose a card to leave on top of {player's} library";
                        }
                        else {
                            prompt = "Choose a card to leave in {player's} " + destZone2.name();
                        }

                        Card chosen = chooser.getController().chooseSingleEntityForEffect(valid, delayedReveal, sa, prompt, false, p);
                        movedCards.remove(chosen);
                        if (sa.hasParam("RandomOrder")) {
                            CardLists.shuffle(movedCards);
                        }
                    }
                    else {
                        String prompt;

                        if (sa.hasParam("PrimaryPrompt")) {
                            prompt = sa.getParam("PrimaryPrompt");
                        } else {
                            prompt = "Choose a card to put into " + destZone1.name();
                            if (destZone1.equals(ZoneType.Library)) {
                                if (libraryPosition == -1) {
                                    prompt = "Choose a card to put on the bottom of {player's} library";
                                }
                                else if (libraryPosition == 0) {
                                    prompt = "Choose a card to put on top of {player's} library";
                                }
                            }
                        }

                        movedCards = new CardCollection();
                        for (int i = 0; i < destZone1ChangeNum || (anyNumber && i < numToDig); i++) {
                            // let user get choice
                            Card chosen = null;
                            if (!valid.isEmpty()) {
                                // If we're choosing multiple cards, only need to show the reveal dialog the first time through.
                                boolean shouldReveal = (i == 0);
                                chosen = chooser.getController().chooseSingleEntityForEffect(valid, shouldReveal ? delayedReveal : null, sa, prompt, anyNumber || optional, p);
                            }
                            else {
                                chooser.getController().notifyOfValue(sa, null, "No valid cards");
                            }

                            if (chosen == null) {
                                break;
                            }

                            movedCards.add(chosen);
                            valid.remove(chosen);
                            if (!andOrValid.equals("")) {
                                andOrCards.remove(chosen);
                                if (!chosen.isValid(andOrValid.split(","), host.getController(), host, sa)) {
                                    valid = new CardCollection(andOrCards);
                                }
                                else if (!chosen.isValid(changeValid.split(","), host.getController(), host, sa)) {
                                    valid.removeAll((Collection<?>)andOrCards);
                                }
                            }
                        }

                        if (!changeValid.isEmpty()) {
                            game.getAction().reveal(movedCards, chooser, true,
                                    chooser + " picked " + (movedCards.size() == 1 ? "this card" : "these cards") + " from ");
                        }
                    }
                    if (sa.hasParam("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    Collections.reverse(movedCards);
                    for (Card c : movedCards) {
                        final PlayerZone zone = c.getOwner().getZone(destZone1);

                        if (zone.is(ZoneType.Library) || zone.is(ZoneType.PlanarDeck) || zone.is(ZoneType.SchemeDeck)) {
                            if (libraryPosition == -1 || libraryPosition > zone.size()) {
                                libraryPosition = zone.size();
                            }
                            c = game.getAction().moveTo(zone, c, libraryPosition);
                        }
                        else {
                            c = game.getAction().moveTo(zone, c);
                            if (destZone1.equals(ZoneType.Battlefield)) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                                if (sa.hasParam("Tapped")) {
                                    c.setTapped(true);
                                }
                            }
                        }

                        if (sa.hasParam("ExileFaceDown")) {
                            c.setState(CardStateName.FaceDown, true);
                        }
                        if (sa.hasParam("Imprint")) {
                            host.addImprintedCard(c);
                        }
                        if (sa.hasParam("ForgetOtherRemembered")) {
                            host.clearRemembered();
                        }
                        if (sa.hasParam("RememberChanged")) {
                            host.addRemembered(c);
                        }
                        rest.remove(c);
                    }

                    // now, move the rest to destZone2
                    if (destZone2 == ZoneType.Library || destZone2 == ZoneType.PlanarDeck || destZone2 == ZoneType.SchemeDeck) {
                        CardCollection afterOrder = rest;
                        if (sa.hasParam("RestRandomOrder")) {
                            CardLists.shuffle(afterOrder);
                        }
                        else if (!skipReorder && rest.size() > 1) {
                            afterOrder = (CardCollection)chooser.getController().orderMoveToZoneList(rest, destZone2);
                        }
                        if (libraryPosition2 != -1) {
                            // Closest to top
                            Collections.reverse(afterOrder);
                        }
                        for (final Card c : afterOrder) {
                            if (destZone2 == ZoneType.Library) {
                                game.getAction().moveToLibrary(c, libraryPosition2);
                            }
                            else {
                                game.getAction().moveToVariantDeck(c, destZone2, libraryPosition2);
                            }
                        }
                    }
                    else {
                        // just move them randomly
                        for (int i = 0; i < rest.size(); i++) {
                            Card c = rest.get(i);
                            final PlayerZone toZone = c.getOwner().getZone(destZone2);
                            c = game.getAction().moveTo(toZone, c);
                            if (destZone2.equals(ZoneType.Battlefield) && !keywords.isEmpty()) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // returns a List<Card> that is a subset of list with cards that share a name
    // with a permanent on the battlefield
    private static CardCollection sharesNameWithCardOnBattlefield(final Game game, final List<Card> list) {
        final CardCollection toReturn = new CardCollection();
        final CardCollectionView play = game.getCardsIn(ZoneType.Battlefield);
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
