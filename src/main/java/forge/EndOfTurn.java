package forge;

import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

//handles "until end of turn" and "at end of turn" commands from cards
/**
 * <p>EndOfTurn class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class EndOfTurn implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-3656715295379727275L</code>. */
    private static final long serialVersionUID = -3656715295379727275L;

    private CommandList at = new CommandList();
    private CommandList until = new CommandList();
    private CommandList last = new CommandList();

    /**
     * <p>addAt.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addAt(final Command c) {
        at.add(c);
    }

    /**
     * <p>addUntil.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addUntil(final Command c) {
        until.add(c);
    }

    /**
     * <p>addLast.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addLast(final Command c) {
        last.add(c);
    }

    /**
     * <p>executeAt.</p>
     */
    public final void executeAt() {

        //Pyrohemia and Pestilence
        CardList all = AllZoneUtil.getCardsInPlay();

        GameActionUtil.endOfTurn_Predatory_Advantage();
        GameActionUtil.endOfTurn_Wall_Of_Reverence();
        GameActionUtil.endOfTurn_Lighthouse_Chronologist();

        //reset mustAttackEntity for me
        AllZone.getPhase().getPlayerTurn().setMustAttackEntity(null);

        GameActionUtil.removeAttackedBlockedThisTurn();

        AllZone.getStaticEffects().rePopulateStateBasedList();

        for (Card c : all) {
            if (!c.isFaceDown()
                    && c.hasKeyword("At the beginning of the end step, sacrifice CARDNAME."))
            {
                final Card card = c;
                final SpellAbility sac = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append("Sacrifice ").append(card);
                sac.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sac);

            }
            if (!c.isFaceDown()
                    && c.hasKeyword("At the beginning of the end step, exile CARDNAME."))
            {
                final Card card = c;
                final SpellAbility exile = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().exile(card);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append("Exile ").append(card);
                exile.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(exile);

            }
            if (!c.isFaceDown()
                    && c.hasKeyword("At the beginning of the end step, destroy CARDNAME."))
            {
                final Card card = c;
                final SpellAbility destroy = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(card)) {
                            AllZone.getGameAction().destroy(card);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append("Destroy ").append(card);
                destroy.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(destroy);

            }
            //Berserk is using this, so don't check isFaceDown()
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
                    StringBuilder sb = new StringBuilder();
                    sb.append("Destroy ").append(card);
                    sac.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(sac);

                } else {
                    c.removeExtrinsicKeyword("At the beginning of the next end step, destroy CARDNAME if it attacked this turn.");
                }
            }
            if (c.hasKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.")) {
                final Card vale = c;
                final SpellAbility change = new Ability(vale, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(vale)) {
                            vale.addController(vale.getController().getOpponent());
                            //AllZone.getGameAction().changeController(
                            //      new CardList(vale), vale.getController(), vale.getController().getOpponent());

                            vale.removeExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(vale.getName()).append(" changes controllers.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.getName().equals("Erg Raiders") && !c.getCreatureAttackedThisTurn()
                    && !c.isSick() && AllZone.getPhase().isPlayerTurn(c.getController()))
            {
                final Card raider = c;
                final SpellAbility change = new Ability(raider, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(raider)) {
                            raider.getController().addDamage(2, raider);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(raider).append(" deals 2 damage to controller.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.hasKeyword("At the beginning of your end step, sacrifice this creature unless it attacked this turn.")
                    && !c.getCreatureAttackedThisTurn()
                    /* && !(c.getTurnInZone() == AllZone.getPhase().getTurn())*/
                    && AllZone.getPhase().isPlayerTurn(c.getController()))
            {
                final Card source = c;
                final SpellAbility change = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(source)) {
                            AllZone.getGameAction().sacrifice(source);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(source.getName()).append(" - sacrifice ").append(source.getName()).append(".");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.hasKeyword("At the beginning of your end step, destroy this creature if it didn't attack this turn.")
                    && !c.getCreatureAttackedThisTurn()
                    && AllZone.getPhase().isPlayerTurn(c.getController()))
            {
                final Card source = c;
                final SpellAbility change = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(source)) {
                            AllZone.getGameAction().destroy(source);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(source.getName()).append(" - destroy ").append(source.getName()).append(".");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }
            if (c.hasKeyword("At the beginning of your end step, return CARDNAME to its owner's hand.")
                    && AllZone.getPhase().isPlayerTurn(c.getController()))
            {
                final Card source = c;
                final SpellAbility change = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(source)) {
                            AllZone.getGameAction().moveToHand(source);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(source).append(" - At the beginning of your end step, return CARDNAME to its owner's hand.");
                change.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(change);

            }

        }


        execute(at);


        CardList all2 = AllZoneUtil.getCardsInPlay();
        for (Card c : all2) {
            if (c.getCreatureAttackedThisTurn()) {
                c.setCreatureAttackedThisTurn(false);
            }
        }

    } //executeAt()


    /**
     * <p>executeUntil.</p>
     */
    public final void executeUntil() {
        execute(until);
        execute(last);
    }

    /**
     * <p>sizeAt.</p>
     *
     * @return a int.
     */
    public final int sizeAt() {
        return at.size();
    }

    /**
     * <p>sizeUntil.</p>
     *
     * @return a int.
     */
    public final int sizeUntil() {
        return until.size();
    }

    /**
     * <p>sizeLast.</p>
     *
     * @return a int.
     */
    public final int sizeLast() {
        return last.size();
    }

    /**
     * <p>execute.</p>
     *
     * @param c a {@link forge.CommandList} object.
     */
    private void execute(final CommandList c) {
        int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).execute();
        }
    }

} //end class EndOfTurn
