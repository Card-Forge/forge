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

import forge.Card;
import forge.Singletons;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.gui.match.CMatchUI;
import forge.sound.SoundEffectType;
import forge.view.ButtonUtil;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class InputBlock extends InputSyncronizedBase {
    /** Constant <code>serialVersionUID=6120743598368928128L</code>. */
    private static final long serialVersionUID = 6120743598368928128L;

    private Card currentAttacker = null;
    // some cards may block several creatures at a time. (ex:  Two-Headed Dragon, Vanguard's Shield)
    private final Combat combat;
    private final Player defender;
    private final Player declarer;
    
    /**
     * TODO: Write javadoc for Constructor.
     * @param priority
     */
    public InputBlock(Player whoDeclares, Player whoDefends, Combat combat) {
        defender = whoDefends;
        declarer = whoDeclares;
        this.combat = combat;
    }

    /** {@inheritDoc} */
    @Override
    protected final void showMessage() {
        // could add "Reset Blockers" button
        ButtonUtil.enableOnlyOk();
        
        String prompt = declarer == defender ? "declare blockers." : "declare blockers for " + defender.getName(); 
        
        final StringBuilder sb = new StringBuilder(declarer.getName());
        sb.append(", ").append(prompt).append("\n\n");

        if (this.currentAttacker == null) {
            sb.append("To Block, click on your opponent's attacker first, then your blocker(s).\n");
            sb.append("To cancel a block right-click on your blocker");
            showMessage(sb.toString());
        } else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            sb.append("Select a creature to block ").append(attackerName).append(" (");
            sb.append(this.currentAttacker.getUniqueNumber()).append("). ");
            sb.append("To cancel a block right-click on your blocker");
            showMessage(sb.toString());
        }

        CMatchUI.SINGLETON_INSTANCE.showCombat(combat);
    }

    /** {@inheritDoc} */
    @Override
    public final void onOk() {
        String blockErrors = CombatUtil.validateBlocks(combat, defender);
        if( null == blockErrors ) {
            // Done blocking
            ButtonUtil.reset();
            setCurrentAttacker(null);
            stop();
        } else {
            GuiDialog.message(blockErrors);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void onCardSelected(final Card card, boolean isMetaDown) {

        if (isMetaDown && card.getController() == defender) {
            combat.removeFromCombat(card);
            CMatchUI.SINGLETON_INSTANCE.showCombat(combat);
            return;
        }
        
        // is attacking?
        boolean isCorrectAction = false;

        if (combat.isAttacking(card)) {
            setCurrentAttacker(card);
            isCorrectAction = true;
        } else {
            // Make sure this card is valid to even be a blocker
            if (this.currentAttacker != null && card.isCreature() && defender.getZone(ZoneType.Battlefield).contains(card)) {
                isCorrectAction = CombatUtil.canBlock(this.currentAttacker, card, combat);
                if ( isCorrectAction ) {
                    combat.addBlocker(this.currentAttacker, card);
                    // This call is performed from GUI and is not intended to propagate to log or net. 
                    // No need to use event bus then 
                    Singletons.getControl().getSoundSystem().play(SoundEffectType.Block);
                }
            }
        }

        if (!isCorrectAction) {
            flashIncorrectAction();
        }

        this.showMessage();
    } // selectCard()


    private void setCurrentAttacker(Card card) {
        currentAttacker = card;
        Player attacker = null;
        for(Card c : combat.getAttackers()) {
            c.setUsedToPay(card == c);
            if ( attacker == null )
                attacker = c.getController();
        }
        // request redraw from here
        attacker.getZone(ZoneType.Battlefield).updateObservers();
    }
}
