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
package forge.screens.match.input;

import com.google.common.collect.Iterables;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.screens.match.FControl;
import forge.screens.match.events.UiEventAttackerDeclared;
import forge.toolbox.FCardZoom;
import forge.toolbox.FCardZoom.ZoomController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * InputAttack class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputAttack.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public class InputAttack extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    private final Combat combat;
    private final List<GameEntity> defenders;
    private GameEntity currentDefender;
    private final Player playerAttacks;
    private final Player playerDeclares;
    private AttackingBand activeBand = null;

    public InputAttack(Player attacks, Player declares, Combat combat) {
        this.playerAttacks = attacks;
        this.playerDeclares = declares;
        this.combat = combat;
        this.defenders = combat.getDefenders();
    }

    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers

        ButtonUtil.enableOnlyOk();

        setCurrentDefender(defenders.isEmpty() ? null : defenders.get(0));

        if ( null == currentDefender ) {
            System.err.println("InputAttack has no potential defenders!");
            return; // should even throw here!
        }

        List<Card> possibleAttackers = playerAttacks.getCardsIn(ZoneType.Battlefield);
        for (Card c : Iterables.filter(possibleAttackers, CardPredicates.Presets.CREATURES)) {
            if (c.hasKeyword("CARDNAME attacks each turn if able.")) {
                for(GameEntity def : defenders ) {
                    if( CombatUtil.canAttack(c, def, combat) ) {
                        combat.addAttacker(c, currentDefender);
                        FControl.fireEvent(new UiEventAttackerDeclared(c, currentDefender));
                        break;
                    }
                }
            } else if (c.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
                final int i = c.getKeywordPosition("CARDNAME attacks specific player each combat if able");
                final String defined = c.getKeyword().get(i).split(":")[1];
                final Player player = AbilityUtils.getDefinedPlayers(c, defined, null).get(0);
                if (player != null && CombatUtil.canAttack(c, player, combat)) {
                    combat.addAttacker(c, player);
                    FControl.fireEvent(new UiEventAttackerDeclared(c, player));
                }
            }
        }
    }

    private void showCombat() {
        // redraw sword icons
        FControl.showCombat(combat);
    }

    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        // TODO Add check to see if each must attack creature is attacking
        // Propaganda costs could have been paid here.
        setCurrentDefender(null); // remove highlights
        activateBand(null);
        stop();
    }

    @Override
    protected final void onPlayerSelected(Player selected) {
        if (defenders.contains(selected)) {
            setCurrentDefender(selected);
        }
        else {
            flashIncorrectAction(); // cannot attack that player
        }
    }

    public enum Option {
        DECLARE_AS_ATTACKER("Declare as Attacker"),
        REMOVE_FROM_COMBAT("Remove from Combat"),
        ATTACK_THIS_DEFENDER("Attack this Defender"),
        ACTIVATE_BAND("Activate Band"),
        JOIN_BAND("Join Band");

        private String text;

        private Option(String text0) {
            text = text0;
        }

        public String toString() {
            return text;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void onCardSelected(final Card card, final List<Card> orderedCardOptions) {
        FCardZoom.show(FControl.getView().getPrompt().getMessage(),
                card, orderedCardOptions, new ZoomController<Option>() {
            @Override
            public List<Option> getOptions(final Card card) {
                List<Option> options = new ArrayList<Option>();

                if (card.getController().isOpponentOf(playerAttacks)) {
                    if (defenders.contains(card)) { // planeswalker?
                        options.add(Option.ATTACK_THIS_DEFENDER);
                    }
                }
                else if (combat.getAttackers().contains(card)) {
                    if (!card.hasKeyword("CARDNAME attacks each turn if able.") &&
                            !card.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
                        options.add(Option.REMOVE_FROM_COMBAT);
                    }

                    if (combat.isAttacking(card, currentDefender)) {
                        // Activate band by selecting/deselecting a band member
                        if (activeBand == null) {
                            options.add(Option.ACTIVATE_BAND);
                        }
                        else if (!activeBand.getAttackers().contains(card) && activeBand.canJoinBand(card)) {
                            options.add(Option.JOIN_BAND);
                        }
                    }
                }
                else if (playerAttacks.getZone(ZoneType.Battlefield).contains(card) &&
                        CombatUtil.canAttack(card, currentDefender, combat)) {
                    options.add(Option.DECLARE_AS_ATTACKER);

                    if (activeBand != null && activeBand.canJoinBand(card)) {
                        options.add(Option.JOIN_BAND);
                    }
                }

                return options;
            }

            @Override
            public boolean selectOption(final Card card, final Option option) {
                boolean hideZoomView = false; //keep zoom open while declaring attackers by default

                switch (option) {
                case DECLARE_AS_ATTACKER:
                    if (combat.isAttacking(card)) {
                        combat.removeFromCombat(card);
                    }
                    declareAttacker(card);
                    showCombat();
                    break;
                case REMOVE_FROM_COMBAT:
                    combat.removeFromCombat(card);
                    FControl.setUsedToPay(card, false);
                    showCombat();
                    activateBand(null); //When removing an attacker clear the attacking band
                    FControl.fireEvent(new UiEventAttackerDeclared(card, null));
                    break;
                case ATTACK_THIS_DEFENDER:
                    setCurrentDefender(card);
                    hideZoomView = true; //don't keep zoom open if choosing defender
                    break;
                case ACTIVATE_BAND:
                    activateBand(combat.getBandOfAttacker(card));
                    break;
                case JOIN_BAND: //Join a band by selecting a non-active band member after activating a band
                    combat.removeFromCombat(card);
                    declareAttacker(card);
                    break;
                }
                showMessage();
                return hideZoomView;
            }
        });
    }

    private void declareAttacker(final Card card) {
        combat.addAttacker(card, currentDefender, this.activeBand);
        this.activateBand(this.activeBand);
        updateMessage();

        FControl.fireEvent(new UiEventAttackerDeclared(card, currentDefender));
    }

    private final void setCurrentDefender(GameEntity def) {
        currentDefender = def;
        for( GameEntity ge: defenders ) {
            if ( ge instanceof Card) {
                FControl.setUsedToPay((Card)ge, ge == def);
            }
            else if (ge instanceof Player) {
                FControl.setHighlighted((Player) ge, ge == def);
            }
        }

        updateMessage();

        // update UI
    }

    private final void activateBand(AttackingBand band) {
        if (this.activeBand != null) {
            for(Card card : this.activeBand.getAttackers()) {
                FControl.setUsedToPay(card, false);
            }
        }
        this.activeBand = band;

        if (this.activeBand != null) {
            for(Card card : this.activeBand.getAttackers()) {
                FControl.setUsedToPay(card, true);
            }
        }

        // update UI
    }

    private void updateMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(playerDeclares.getName()).append(", ");
        sb.append(playerAttacks == playerDeclares ? "declare attackers." : "declare attackers for " + playerAttacks.getName()).append("\n");
        sb.append("Selecting Creatures to Attack ").append(currentDefender).append("\n\n");
        sb.append("To change the current defender, click on the player or planeswalker you wish to attack.\n");
        sb.append("To attack as a band, click an attacking creature to activate its 'band', select another to join the band.");

        showMessage(sb.toString());
    }
}
