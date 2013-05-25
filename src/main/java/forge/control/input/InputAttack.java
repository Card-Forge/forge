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
package forge.control.input;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.GameEntity;
import forge.game.GameState;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.MyObservable;
import forge.view.ButtonUtil;

/**
 * <p>
 * InputAttack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputAttack extends InputPassPriorityBase {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    
    private final GameState game;
    private List<GameEntity> defenders;
    private GameEntity currentDefender;
    
    public InputAttack(Player human) {
        super(human);
        game = human.getGame();
    }
    
    
    /** {@inheritDoc} */
    @Override
    public final void showMessage() {
        // TODO still seems to have some issues with multiple planeswalkers

        ButtonUtil.enableOnlyOk();
        defenders = game.getCombat().getDefenders();
        setCurrentDefender(defenders.isEmpty() ? null : defenders.get(0));

        if ( null == currentDefender ) {
            System.err.println("InputAttack has no potential defenders!");
            return; // should even throw here!
        }

        List<Card> possibleAttackers = player.getCardsIn(ZoneType.Battlefield);
        for (Card c : Iterables.filter(possibleAttackers, CardPredicates.Presets.CREATURES)) {
            if (!c.hasKeyword("CARDNAME attacks each turn if able."))
                continue; // do not force

            for(GameEntity def : defenders ) {
                if( CombatUtil.canAttack(c, def, game.getCombat()) ) {
                    game.getCombat().addAttacker(c, currentDefender);
                    break;
                }
            }
        }
    }
    
    private void showCombat() {
        player.getZone(ZoneType.Battlefield).updateObservers(); // redraw sword icons
        game.getPhaseHandler().setCombat(!game.getCombat().getAttackers().isEmpty());
        CombatUtil.showCombat(game);
    }
    
    /** {@inheritDoc} */
    @Override
    public final void selectButtonOK() {
        // Propaganda costs could have been paid here.
        setCurrentDefender(null); // remove highlights
        game.getPhaseHandler().setCombat(!game.getCombat().getAttackers().isEmpty());
        game.getPhaseHandler().setPlayersPriorityPermission(false);

        pass();
    }

    @Override
    public void selectPlayer(Player selected) {
        if(defenders.contains(selected))
            setCurrentDefender(selected);
        else
            flashIncorrectAction(); // cannot attack that player
    }
    
    /** {@inheritDoc} */
    @Override
    public final void selectCard(final Card card, boolean isMetaDown) {
        final List<Card> att = game.getCombat().getAttackers();
        if (isMetaDown && att.contains(card) && !card.hasKeyword("CARDNAME attacks each turn if able.")) {
            game.getCombat().removeFromCombat(card);
            showCombat();
            return;
        }

        if (card.isAttacking(currentDefender)) {
            return;
        }
    
        if ( card.getController().isOpponentOf(player) ) {
            if ( defenders.contains(card) ) { // planeswalker?
                setCurrentDefender(card);
                return;
            }
        }

        Zone zone = game.getZoneOf(card);
        if (zone.is(ZoneType.Battlefield, player) && CombatUtil.canAttack(card, currentDefender, game.getCombat())) {
            if( game.getCombat().isAttacking(card)) {
                game.getCombat().removeFromCombat(card);
            }
            game.getCombat().addAttacker(card, currentDefender);
            showCombat();
        }
        else {
            flashIncorrectAction();
        }
    } // selectCard()

    private final void setCurrentDefender(GameEntity def) {
        Set<MyObservable> toUpdate = new HashSet<MyObservable>();
        currentDefender = def; 
        for( GameEntity ge: defenders ) {
            if ( ge instanceof Card) {
                ((Card) ge).setUsedToPay(ge == def);
                toUpdate.add(((Card) ge).getController().getZone(ZoneType.Battlefield));
            }
            else if (ge instanceof Player) {
                ((Player) ge).setHighlited(ge == def);
                toUpdate.add(ge);
            }
        }

        showMessage("Declare Attackers.\nSelecting Creatures to Attack " + currentDefender + 
                "\n\nTo attack other players or their planewalkers just click on them");

        // This will instantly highlight targets
        for(MyObservable updateable : toUpdate) {
            updateable.updateObservers();
        }
    }
}
