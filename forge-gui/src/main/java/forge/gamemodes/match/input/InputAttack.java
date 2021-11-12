/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.match.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardView;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.event.GameEventCombatUpdate;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.events.UiEventAttackerDeclared;
import forge.player.PlayerControllerHuman;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

/**
 * <p>
 * InputAttack class.
 * </p>
 *
 * @author Forge
 * @version $Id: InputAttack.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputAttack extends InputSyncronizedBase {
    private static final long serialVersionUID = 7849903731842214245L;

    private final Combat combat;
    private final FCollectionView<GameEntity> defenders;
    private GameEntity currentDefender;
    private final Player playerAttacks;
    private AttackingBand activeBand = null;
    private boolean potentialBanding;

    public InputAttack(PlayerControllerHuman controller, Player attacks0, Combat combat0) {
        super(controller);
        playerAttacks = attacks0;
        combat = combat0;
        defenders = combat.getDefenders();
        potentialBanding = isBandingPossible();
    }

    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers
        setCurrentDefender(defenders.getFirst());

        if (currentDefender == null) {
            System.err.println("InputAttack has no potential defenders!");
            updatePrompt();
            return; // should even throw here!
        }

        updateMessage();
    }

    //determine whether currently attackers can be called back (undeclared)
    private boolean canCallBackAttackers() {
        return !combat.getAttackers().isEmpty();
    }

    private void updatePrompt() {
        Localizer localizer = Localizer.getInstance();
        String alphaLabel = canCallBackAttackers() ? localizer.getMessage("lblCallBack") : localizer.getMessage("lblAlphaStrike");
        getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblOk"), alphaLabel, true, true, true);
    }

    private void disablePrompt() {
        Localizer localizer = Localizer.getInstance();
        getController().getGui().updateButtons(getOwner(), localizer.getMessage("lblDisabled"), localizer.getMessage("lblDisabled"), false, false, false);
    }

    @Override
    protected final void onOk() {
        // Propaganda costs could have been paid here.
        setCurrentDefender(null); // remove highlights
        activateBand(null);
        stop();
    }

    @Override
    protected final void onCancel() {
        //either alpha strike or undeclare all attackers based on whether any attackers have been declared
        if (canCallBackAttackers()) {
            //undeclare all attackers
            List<Card> attackers = new ArrayList<>(combat.getAttackers()); //must copy list since it will be modified
            for (Card c : attackers) {
                undeclareAttacker(c);
            }
        } else {
            alphaStrike();
        }
        updateMessage();
    }

    void alphaStrike() {
        //alpha strike
        final List<Player> defenders = playerAttacks.getOpponents();
        final Set<CardView> refreshCards = Sets.newHashSet();

        for (final Card c : playerAttacks.getCreaturesInPlay()) {
            if (combat.isAttacking(c)) {
                continue;
            }

            if (currentDefender != null && CombatUtil.canAttack(c, currentDefender)) {
                combat.addAttacker(c, currentDefender);
                refreshCards.add(CardView.get(c));
                continue;
            }

            for (final Player defender : defenders) {
                if (CombatUtil.canAttack(c, defender)) {
                    combat.addAttacker(c, defender);
                    refreshCards.add(CardView.get(c));
                    break;
                }
            }
        }
        getController().getGui().updateCards(refreshCards);
        updateMessage();
    }

    @Override
    protected final void onPlayerSelected(Player selected, final ITriggerEvent triggerEvent) {
        if (defenders.contains(selected)) {
            setCurrentDefender(selected);
        }
        else {
            getController().getGui().flashIncorrectAction(); // cannot attack that player
        }
    }

    @Override
    protected final boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        disablePrompt();
        final List<Card> att = combat.getAttackers();
        if (triggerEvent != null && triggerEvent.getButton() == 3 && att.contains(card)) {
            undeclareAttacker(card);
            if (otherCardsToSelect != null) {
                for (Card c : otherCardsToSelect) {
                    undeclareAttacker(c);
                }
            }
            updateMessage();
            return true;
        }

        if (combat.isAttacking(card, currentDefender)) {
            boolean validAction = true;
            if (potentialBanding) {
                // Activate band by selecting/deselecting a band member
                if (activeBand == null) {
                    activateBand(combat.getBandOfAttacker(card));
                }
                else if (activeBand.getAttackers().contains(card)) {
                    activateBand(null);
                }
                else { // Join a band by selecting a non-active band member after activating a band
                    if (activeBand.canJoinBand(card)) {
                        declareAttacker(card);
                        if (otherCardsToSelect != null) {
                            for (Card c : otherCardsToSelect) {
                                if (activeBand.canJoinBand(c)) {
                                    declareAttacker(c);
                                }
                            }
                        }
                    }
                    else {
                        validAction = false;
                    }
                }
            }
            else {
                //if banding not possible, just undeclare attacker
                undeclareAttacker(card);
                if (otherCardsToSelect != null) {
                    for (Card c : otherCardsToSelect) {
                        undeclareAttacker(c);
                    }
                }
            }

            updateMessage();
            return validAction;
        }

        if (card.getController().isOpponentOf(playerAttacks)) {
            if (defenders.contains(card)) { // planeswalker?
                setCurrentDefender(card);
                updateMessage();
                return true;
            }
        }

        if (playerAttacks.getZone(ZoneType.Battlefield).contains(card) && CombatUtil.canAttack(card, currentDefender)) {
            if (activeBand != null && !activeBand.canJoinBand(card)) {
                activateBand(null);
                updateMessage();
                return false;
            }

            declareAttacker(card);
            if (otherCardsToSelect != null) {
                for (Card c : otherCardsToSelect) {
                    if (CombatUtil.canAttack(c, currentDefender)) {
                        declareAttacker(c);
                    }
                }
            }

            updateMessage();
            return true;
        }

        updatePrompt();
        return false;
    }

    @Override
    public String getActivateAction(Card card) {
        if (combat.isAttacking(card, currentDefender)) {
            if (potentialBanding) {
                return "activate band with card";
            }
            return "remove card from combat";
        }
        if (card.getController().isOpponentOf(playerAttacks)) {
            if (defenders.contains(card)) {
                return "declare attackers for card";
            }
            return null;
        }
        if (playerAttacks.getZone(ZoneType.Battlefield).contains(card) && CombatUtil.canAttack(card, currentDefender)) {
            return "attack with card";
        }
        return null;
    }

    private void declareAttacker(final Card card) {
        combat.removeFromCombat(card);
        combat.addAttacker(card, currentDefender, activeBand);
        activateBand(activeBand);

        card.getGame().getMatch().fireEvent(new UiEventAttackerDeclared(
                CardView.get(card),
                GameEntityView.get(currentDefender)));
    }

    private boolean undeclareAttacker(final Card card) {
        combat.removeFromCombat(card);
        getController().getGui().setUsedToPay(CardView.get(card), false);
        // When removing an attacker clear the attacking band
        activateBand(null);

        card.getGame().getMatch().fireEvent(new UiEventAttackerDeclared(
                CardView.get(card), null));
        return true;
    }

    private void setCurrentDefender(final GameEntity def) {
        currentDefender = def;
        for (final GameEntity ge : defenders) {
            if (ge instanceof Card) {
                getController().getGui().setUsedToPay(CardView.get((Card) ge), ge == def);
            }
            else if (ge instanceof Player) {
                getController().getGui().setHighlighted(PlayerView.get((Player) ge), ge == def);
            }
        }

        updateMessage();
    }

    private void activateBand(final AttackingBand band) {
        if (activeBand != null) {
            for (final Card card : activeBand.getAttackers()) {
                getController().getGui().setUsedToPay(CardView.get(card), false);
            }
        }
        activeBand = band;

        if (activeBand != null) {
            for (final Card card : activeBand.getAttackers()) {
                getController().getGui().setUsedToPay(CardView.get(card), true);
            }
        }
    }

    //only enable banding message and actions if a creature that can attack has banding
    private boolean isBandingPossible() {
        final CardCollectionView possibleAttackers = playerAttacks.getCardsIn(ZoneType.Battlefield);
        for (final Card c : possibleAttackers) {
            if ((c.hasKeyword(Keyword.BANDING) || c.hasStartOfKeyword("Bands with Other")) &&
                    CombatUtil.canAttack(c, currentDefender)) {
                return true;
            }
        }
        return false;
    }

    private void updateMessage() {
        Localizer localizer = Localizer.getInstance();
        String message = localizer.getMessage("lblSelectAttackCreatures") + " " + currentDefender + " " + localizer.getMessage("lblSelectAttackTarget");
        if (potentialBanding) {
            message += localizer.getMessage("lblSelectBandingTarget");
        }
        showMessage(message);

        updatePrompt();

        if (combat != null)
            getController().getGame().fireEvent(new GameEventCombatUpdate(combat.getAttackers(), combat.getAllBlockers()));

        getController().getGui().showCombat(); // redraw sword icons
    }
}
