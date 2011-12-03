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
package forge;

import forge.Constant.Zone;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

//handles "until end of turn" and "at end of turn" commands from cards
/**
 * <p>
 * EndOfTurn class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class EndOfTurn implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-3656715295379727275L</code>. */
    private static final long serialVersionUID = -3656715295379727275L;

    private final CommandList at = new CommandList();
    private final CommandList until = new CommandList();
    private final CommandList last = new CommandList();

    /**
     * <p>
     * addAt.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addAt(final Command c) {
        this.at.add(c);
    }

    /**
     * <p>
     * addUntil.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntil(final Command c) {
        this.until.add(c);
    }

    /**
     * <p>
     * addLast.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addLast(final Command c) {
        this.last.add(c);
    }

    /**
     * <p>
     * executeAt.
     * </p>
     */
    public final void executeAt() {

        // Pyrohemia and Pestilence
        final CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);

        GameActionUtil.endOfTurnWallOfReverence();
        GameActionUtil.endOfTurnLighthouseChronologist();

        // reset mustAttackEntity for me
        AllZone.getPhase().getPlayerTurn().setMustAttackEntity(null);

        GameActionUtil.removeAttackedBlockedThisTurn();

        AllZone.getStaticEffects().rePopulateStateBasedList();

        for (final Card c : all) {
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.")) {
                final Card card = c;
                final SpellAbility sac = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Sacrifice ").append(card);
                sac.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sac);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, exile CARDNAME.")) {
                final Card card = c;
                final SpellAbility exile = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().exile(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Exile ").append(card);
                exile.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(exile);

            }
            if (!c.isFaceDown() && c.hasKeyword("At the beginning of the end step, destroy CARDNAME.")) {
                final Card card = c;
                final SpellAbility destroy = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().destroy(card);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append("Destroy ").append(card);
                destroy.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(destroy);

            }
            // Berserk is using this, so don't check isFaceDown()
            if (c.hasKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.")) {
                if (c.getCreatureAttackedThisTurn()) {
                    final Card card = c;
                    final SpellAbility sac = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            if (AllZoneUtil.isCardInPlay(card)) {
                                AllZone.getGameAction().destroy(card);
                            }
                        }
                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Destroy ").append(card);
                    sac.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(sac);

                } else {
                    c.removeAllExtrinsicKeyword("At the beginning of the next end step, "
                            + "destroy CARDNAME if it attacked this turn.");
                }
            }
            if (c.hasKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.")) {
                final Card vale = c;
                final SpellAbility change = new Ability(vale, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(vale)) {
                            vale.addController(vale.getController().getOpponent());
                            // AllZone.getGameAction().changeController(
                            // new CardList(vale), vale.getController(),
                            // vale.getController().getOpponent());

                            vale.removeAllExtrinsicKeyword("An opponent gains control of CARDNAME "
                                    + "at the beginning of the next end step.");
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(vale.getName()).append(" changes controllers.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.getName().equals("Erg Raiders") && !c.getCreatureAttackedThisTurn() && !c.hasSickness()
                    && AllZone.getPhase().isPlayerTurn(c.getController())) {
                final Card raider = c;
                final SpellAbility change = new Ability(raider, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(raider)) {
                            raider.getController().addDamage(2, raider);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(raider).append(" deals 2 damage to controller.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.hasKeyword("At the beginning of your end step, return CARDNAME to its owner's hand.")
                    && AllZone.getPhase().isPlayerTurn(c.getController())) {
                final Card source = c;
                final SpellAbility change = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(source)) {
                            AllZone.getGameAction().moveToHand(source);
                        }
                    }
                };
                final StringBuilder sb = new StringBuilder();
                sb.append(source).append(" - At the beginning of your end step, return CARDNAME to its owner's hand.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }

        }

        this.execute(this.at);

    } // executeAt()

    /**
     * <p>
     * executeUntil.
     * </p>
     */
    public final void executeUntil() {
        this.execute(this.until);
        this.execute(this.last);
    }

    /**
     * <p>
     * sizeAt.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeAt() {
        return this.at.size();
    }

    /**
     * <p>
     * sizeUntil.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeUntil() {
        return this.until.size();
    }

    /**
     * <p>
     * sizeLast.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeLast() {
        return this.last.size();
    }

    /**
     * <p>
     * execute.
     * </p>
     * 
     * @param c
     *            a {@link forge.CommandList} object.
     */
    private void execute(final CommandList c) {
        final int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).execute();
        }
    }

} // end class EndOfTurn
