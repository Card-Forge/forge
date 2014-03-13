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

import java.util.ArrayList;
import java.util.List;

import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.screens.match.FControl;
import forge.screens.match.events.UiEventBlockerAssigned;
import forge.toolbox.FOptionPane;
import forge.toolbox.VCardZoom.ZoomController;

/**
 * <p>
 * Input_Block class.
 * </p>
 * 
 * @author Forge
 * @version $Id: InputBlock.java 24769 2014-02-09 13:56:04Z Hellfish $
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
        }
        else {
            final String attackerName = this.currentAttacker.isFaceDown() ? "Morph" : this.currentAttacker.getName();
            sb.append("Select a creature to block ").append(attackerName).append(" (");
            sb.append(this.currentAttacker.getUniqueNumber()).append("). ");
            sb.append("To cancel a block right-click on your blocker");
        }

        showMessage(sb.toString());
        FControl.showCombat(combat);
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
        }
        else {
            FOptionPane.showMessageDialog(blockErrors);
        }
    }

    public enum Option {
        DECLARE_AS_BLOCKER("Declare as Blocker"),
        REMOVE_FROM_COMBAT("Remove from Combat"),
        BLOCK_THIS_ATTACKER("Block this Attacker"),;

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
    public final void onCardSelected(final Card card, final List<Card> orderedCardOptions) {
        FControl.getView().getCardZoom().show(FControl.getView().getPrompt().getMessage(),
                card, orderedCardOptions, new ZoomController<Option>() {
            @Override
            public List<Option> getOptions(final Card card) {
                List<Option> options = new ArrayList<Option>();

                if (combat.isAttacking(card)) {
                    options.add(Option.BLOCK_THIS_ATTACKER);
                }
                else if (card.getController() == defender) {
                    if (combat.isBlocking(card)) {
                        options.add(Option.REMOVE_FROM_COMBAT);
                    }
                    else if (currentAttacker != null && card.isCreature() &&
                            defender.getZone(ZoneType.Battlefield).contains(card) &&
                            CombatUtil.canBlock(currentAttacker, card, combat)) {
                        options.add(Option.DECLARE_AS_BLOCKER);
                    }
                }

                return options;
            }

            @Override
            public boolean selectOption(final Card card, final Option option) {
                boolean hideZoomView = false; //keep zoom open while declaring blockers by default

                switch (option) {
                case DECLARE_AS_BLOCKER:
                    combat.addBlocker(currentAttacker, card);
                    FControl.fireEvent(new UiEventBlockerAssigned(card, currentAttacker));
                    break;
                case REMOVE_FROM_COMBAT:
                    combat.removeFromCombat(card);
                    FControl.fireEvent(new UiEventBlockerAssigned(card, (Card)null));
                    break;
                case BLOCK_THIS_ATTACKER:
                    setCurrentAttacker(card);
                    hideZoomView = true; //don't keep zoom open if choosing attacker
                    break;
                }
                showMessage();
                return hideZoomView; //keep zoom open while declaring blockers
            }
        });
    }

    private void setCurrentAttacker(Card card) {
        currentAttacker = card;
        for(Card c : combat.getAttackers()) {
            FControl.setUsedToPay(c, card == c);
        }
    }
}
