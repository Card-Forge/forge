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
package forge.gui.input;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.GameEntity;
import forge.game.phase.Combat;
import forge.game.phase.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.match.CMatchUI;
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
public class InputAttack extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=7849903731842214245L</code>. */
    private static final long serialVersionUID = 7849903731842214245L;

    
    private final Combat combat;
    private final List<GameEntity> defenders;
    private GameEntity currentDefender;
    private final Player playerAttacks;
    private final Player playerDeclares;
    
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
            if (!c.hasKeyword("CARDNAME attacks each turn if able."))
                continue; // do not force

            for(GameEntity def : defenders ) {
                if( CombatUtil.canAttack(c, def, combat) ) {
                    combat.addAttacker(c, currentDefender);
                    break;
                }
            }
        }
    }
    
    private void showCombat() {
        playerAttacks.getZone(ZoneType.Battlefield).updateObservers(); // redraw sword icons
        CMatchUI.SINGLETON_INSTANCE.showCombat();
    }
    
    /** {@inheritDoc} */
    @Override
    protected final void onOk() {
        // Propaganda costs could have been paid here.
        setCurrentDefender(null); // remove highlights
        stop();
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
    protected final void onCardSelected(final Card card, boolean isMetaDown) {
        final List<Card> att = combat.getAttackers();
        if (isMetaDown && att.contains(card) && !card.hasKeyword("CARDNAME attacks each turn if able.")) {
            combat.removeFromCombat(card);
            showCombat();
            return;
        }

        if (card.isAttacking(currentDefender)) {
            return;
        }
    
        if ( card.getController().isOpponentOf(playerAttacks) ) {
            if ( defenders.contains(card) ) { // planeswalker?
                setCurrentDefender(card);
                return;
            }
        }

        if (playerAttacks.getZone(ZoneType.Battlefield).contains(card) && CombatUtil.canAttack(card, currentDefender, combat)) {
            if( combat.isAttacking(card)) {
                combat.removeFromCombat(card);
            }
            combat.addAttacker(card, currentDefender);
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

        String header = playerAttacks == playerDeclares ? "declare attackers." : "declare attackers for " + playerAttacks.getName(); 
        showMessage(playerDeclares.getName() + ", " + header + "\nSelecting Creatures to Attack " + currentDefender + 
                "\n\nTo attack other players or their planewalkers just click on them");

        // This will instantly highlight targets
        for(MyObservable updateable : toUpdate) {
            updateable.updateObservers();
        }
    }
}
