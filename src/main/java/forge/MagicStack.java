package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.Ability_Static;
import forge.card.spellability.Ability_Triggered;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_StackInstance;
import forge.card.spellability.Spell_Permanent;
import forge.card.spellability.Target;
import forge.card.spellability.Target_Choices;
import forge.card.spellability.Target_Selection;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost_Ability;

/**
 * <p>
 * MagicStack class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MagicStack extends MyObservable {
    private final List<SpellAbility> simultaneousStackEntryList = new ArrayList<SpellAbility>();

    private final Stack<SpellAbility_StackInstance> stack = new Stack<SpellAbility_StackInstance>();
    private final Stack<SpellAbility_StackInstance> frozenStack = new Stack<SpellAbility_StackInstance>();

    private boolean frozen = false;
    private boolean bResolving = false;
    private int splitSecondOnStack = 0;

    private final CardList thisTurnCast = new CardList();
    private CardList lastTurnCast = new CardList();

    /**
     * <p>
     * isFrozen.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFrozen() {
        return this.frozen;
    }

    /**
     * <p>
     * Setter for the field <code>frozen</code>.
     * </p>
     * 
     * @param frozen0
     *            a boolean.
     */
    public final void setFrozen(final boolean frozen0) {
        this.frozen = frozen0;
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        this.getStack().clear();
        this.frozen = false;
        this.splitSecondOnStack = 0;
        this.getFrozenStack().clear();
        this.updateObservers();
    }

    /**
     * <p>
     * isSplitSecondOnStack.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSplitSecondOnStack() {
        return this.splitSecondOnStack > 0;
    }

    /**
     * <p>
     * incrementSplitSecond.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void incrementSplitSecond(final SpellAbility sp) {
        if (sp.getSourceCard().hasKeyword("Split second")) {
            this.splitSecondOnStack++;
        }
    }

    /**
     * <p>
     * decrementSplitSecond.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void decrementSplitSecond(final SpellAbility sp) {
        if (sp.getSourceCard().hasKeyword("Split second")) {
            this.splitSecondOnStack--;
        }

        if (this.splitSecondOnStack < 0) {
            this.splitSecondOnStack = 0;
        }
    }

    /**
     * <p>
     * freezeStack.
     * </p>
     */
    public final void freezeStack() {
        this.frozen = true;
    }

    /**
     * <p>
     * addAndUnfreeze.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addAndUnfreeze(final SpellAbility ability) {
        ability.getRestrictions().abilityActivated();
        if ((ability.getRestrictions().getActivationNumberSacrifice() != -1)
                && (ability.getRestrictions().getNumberTurnActivations() >= ability.getRestrictions()
                        .getActivationNumberSacrifice())) {
            ability.getSourceCard().addExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
        }
        // triggered abilities should go on the frozen stack
        if (!ability.isTrigger()) {
            this.frozen = false;
        }

        this.add(ability);

        // if the ability is a spell, but not a copied spell and its not already
        // on the stack zone, move there
        if (ability.isSpell()) {
            final Card source = ability.getSourceCard();
            if (!source.isCopiedSpell() && !AllZone.getZoneOf(source).is(Constant.Zone.Stack)) {
                //ability.setSourceCard(AllZone.getGameAction().moveToStack(source));
            }
        }

        if (ability.isTrigger()) {
            this.unfreezeStack();
        }
    }

    /**
     * <p>
     * unfreezeStack.
     * </p>
     */
    public final void unfreezeStack() {
        this.frozen = false;
        final boolean checkState = !this.getFrozenStack().isEmpty();
        while (!this.getFrozenStack().isEmpty()) {
            final SpellAbility sa = this.getFrozenStack().pop().getSpellAbility();
            this.add(sa);
        }
        if (checkState) {
            AllZone.getGameAction().checkStateEffects();
        }
    }

    /**
     * <p>
     * clearFrozen.
     * </p>
     */
    public final void clearFrozen() {
        // TODO: frozen triggered abilities and undoable costs have nasty
        // consequences
        this.frozen = false;
        this.getFrozenStack().clear();
    }

    /**
     * <p>
     * setResolving.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setResolving(final boolean b) {
        this.bResolving = b;
        if (!this.bResolving) {
            this.chooseOrderOfSimultaneousStackEntryAll();
        }
    }

    /**
     * <p>
     * getResolving.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getResolving() {
        return this.bResolving;
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param useX
     *            a boolean.
     */
    public final void add(final SpellAbility sp, final boolean useX) {
        if (!useX) {
            this.add(sp);
        } else {

            // TODO: make working triggered abilities!
            if ((sp instanceof Ability_Mana) || (sp instanceof Ability_Triggered)) {
                sp.resolve();
            } else {
                this.push(sp);
                /*
                 * if (sp.getTargetCard() != null)
                 * CardFactoryUtil.checkTargetingEffects(sp,
                 * sp.getTargetCard());
                 */
            }
        }
    }

    /**
     * <p>
     * getMultiKickerSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost getMultiKickerSpellCostChange(final SpellAbility sa) {
        final int max = 25;
        final String[] numbers = new String[max];
        for (int no = 0; no < max; no++) {
            numbers[no] = String.valueOf(no);
        }

        ManaCost manaCost = new ManaCost(sa.getManaCost());
        String mana = manaCost.toString();

        int multiKickerPaid = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid;

        String numberManaCost = " ";

        if (mana.toString().length() == 1) {
            numberManaCost = mana.toString().substring(0, 1);
        } else if (mana.toString().length() == 0) {
            numberManaCost = "0"; // Should Never Occur
        } else {
            numberManaCost = mana.toString().substring(0, 2);
        }

        numberManaCost = numberManaCost.trim();

        for (int check = 0; check < max; check++) {
            if (numberManaCost.equals(numbers[check])) {

                if ((check - multiKickerPaid) < 0) {
                    multiKickerPaid = multiKickerPaid - check;
                    AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid = multiKickerPaid;
                    mana = mana.replaceFirst(String.valueOf(check), "0");
                } else {
                    mana = mana.replaceFirst(String.valueOf(check), String.valueOf(check - multiKickerPaid));
                    multiKickerPaid = 0;
                    AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid = multiKickerPaid;
                }
            }
            mana = mana.trim();
            if (mana.equals("")) {
                mana = "0";
            }
            manaCost = new ManaCost(mana);
        }
        final String colorCut = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored;

        for (int colorCutIx = 0; colorCutIx < colorCut.length(); colorCutIx++) {
            if ("WUGRB".contains(colorCut.substring(colorCutIx, colorCutIx + 1))
                    && !mana.equals(mana.replaceFirst((colorCut.substring(colorCutIx, colorCutIx + 1)), ""))) {
                mana = mana.replaceFirst(colorCut.substring(colorCutIx, colorCutIx + 1), "");

                AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored
                = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored
                        .replaceFirst(colorCut.substring(colorCutIx, colorCutIx + 1), "");

                mana = mana.trim();
                if (mana.equals("")) {
                    mana = "0";
                }
                manaCost = new ManaCost(mana);
            }
        }

        return manaCost;
    }

    // TODO: this may be able to use a straight copy of MultiKicker cost change
    /**
     * <p>
     * getReplicateSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost getReplicateSpellCostChange(final SpellAbility sa) {
        final ManaCost manaCost = new ManaCost(sa.getManaCost());
        // String Mana = manaCost.toString();
        return manaCost;
    }

    /**
     * <p>
     * add.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void add(final SpellAbility sp) {
        final ArrayList<Target_Choices> chosenTargets = sp.getAllTargetChoices();

        if (sp instanceof Ability_Mana) { // Mana Abilities go straight through
            sp.resolve();
            sp.resetOnceResolved();
            return;
        }

        if (this.frozen) {
            final SpellAbility_StackInstance si = new SpellAbility_StackInstance(sp);
            this.getFrozenStack().push(si);
            return;
        }

        // if activating player slips through the cracks, assign activating
        // Player to the controller here
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        if (AllZone.getPhase().is(Constant.Phase.CLEANUP)) { // If something
                                                             // triggers during
                                                             // Cleanup, need to
                                                             // repeat
            AllZone.getPhase().repeatPhase();
        }

        // TODO: triggered abilities need to be fixed
        if (!((sp instanceof Ability_Triggered) || (sp instanceof Ability_Static))) {
            // when something is added we need to setPriority
            AllZone.getPhase().setPriority(sp.getActivatingPlayer());
        }

        if ((sp instanceof Ability_Triggered) || (sp instanceof Ability_Static)) {
            // TODO: make working triggered ability
            sp.resolve();
        } else {
            if (sp.isKickerAbility()) {
                sp.getSourceCard().setKicked(true);
                final SpellAbility[] sa = sp.getSourceCard().getSpellAbility();
                int abilityNumber = 0;

                for (int i = 0; i < sa.length; i++) {
                    if (sa[i] == sp) {
                        abilityNumber = i;
                    }
                }

                sp.getSourceCard().setAbilityUsed(abilityNumber);
            }
            if (sp.getSourceCard().isCopiedSpell()) {
                this.push(sp);
            } else if (!sp.isMultiKicker() && !sp.isReplicate() && !sp.isXCost()) {
                this.push(sp);
            } else if ((sp.getPayCosts() != null) && !sp.isMultiKicker() && !sp.isReplicate()) {
                this.push(sp);
            } else if (sp.isXCost()) {
                // TODO: convert any X costs to use abCost so it happens earlier
                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sa.getXManaCost()) {
                    @Override
                    public void resolve() {
                        final Card crd = this.getSourceCard();
                        crd.addXManaCostPaid(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    @Override
                    public void execute() {
                        MagicStack.this.push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -2224875229611007788L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        final Card crd = sa.getSourceCard();
                        AllZone.getInputControl().setInput(
                                new Input_PayManaCost_Ability("Pay X cost for " + crd.getName() + " (X="
                                        + crd.getXManaCostPaid() + ")\r\n", ability.getManaCost(), this, unpaidCommand,
                                        true));
                    }
                };

                final Card crd = sa.getSourceCard();
                if (sp.getSourceCard().getController().isHuman()) {
                    AllZone.getInputControl().setInput(
                            new Input_PayManaCost_Ability("Pay X cost for " + sp.getSourceCard().getName() + " (X="
                                    + crd.getXManaCostPaid() + ")\r\n", ability.getManaCost(), paidCommand,
                                    unpaidCommand, true));
                } else {
                    // computer
                    final int neededDamage = CardFactoryUtil.getNeededXDamage(sa);

                    while (ComputerUtil.canPayCost(ability)
                            && (neededDamage != sa.getSourceCard().getXManaCostPaid())) {
                        ComputerUtil.playNoStack(ability);
                    }
                    this.push(sa);
                }
            } else if (sp.isMultiKicker()) {
                // TODO: convert multikicker support in abCost so this doesn't
                // happen here
                // both X and multi is not supported yet

                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost()) {
                    @Override
                    public void resolve() {
                        this.getSourceCard().addMultiKickerMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    @Override
                    public void execute() {
                        MagicStack.this.push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -6037161763374971106L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        final ManaCost manaCost = MagicStack.this.getMultiKickerSpellCostChange(ability);
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {
                            if ((AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid == 0)
                                    && AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored
                                            .equals("")) {

                                AllZone.getInputControl().setInput(
                                        new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n"
                                                + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude()
                                                + "\r\n", manaCost.toString(), this, unpaidCommand));
                            } else {
                                AllZone.getInputControl()
                                        .setInput(
                                                new Input_PayManaCost_Ability(
                                                        "Multikicker for "
                                                                + sa.getSourceCard()
                                                                + "\r\n"
                                                                + "Mana in Reserve: "
                                                                + ((AllZone.getGameAction()
                                                                        .CostCutting_GetMultiMickerManaCostPaid != 0)
                                                                        ? AllZone
                                                                        .getGameAction()
                                                                        .CostCutting_GetMultiMickerManaCostPaid
                                                                        : "")
                                                                + AllZone.getGameAction()
                                                                .CostCutting_GetMultiMickerManaCostPaid_Colored
                                                                + "\r\n" + "Times Kicked: "
                                                                + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                                        manaCost.toString(), this, unpaidCommand));
                            }
                        }
                    }
                };

                if (sp.getActivatingPlayer().isHuman()) {
                    final ManaCost manaCost = this.getMultiKickerSpellCostChange(ability);

                    if (manaCost.isPaid()) {
                        paidCommand.execute();
                    } else {
                        if ((AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid == 0)
                                && AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
                            AllZone.getInputControl().setInput(
                                    new Input_PayManaCost_Ability("Multikicker for " + sa.getSourceCard() + "\r\n"
                                            + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                            manaCost.toString(), paidCommand, unpaidCommand));
                        } else {
                            AllZone.getInputControl()
                                    .setInput(
                                            new Input_PayManaCost_Ability(
                                                    "Multikicker for "
                                                            + sa.getSourceCard()
                                                            + "\r\n"
                                                            + "Mana in Reserve: "
                                                            + ((AllZone.getGameAction()
                                                                    .CostCutting_GetMultiMickerManaCostPaid != 0)
                                                                    ? AllZone
                                                                    .getGameAction()
                                                                    .CostCutting_GetMultiMickerManaCostPaid
                                                                    : "")
                                                            + AllZone.getGameAction()
                                                            .CostCutting_GetMultiMickerManaCostPaid_Colored
                                                            + "\r\n" + "Times Kicked: "
                                                            + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                                    manaCost.toString(), paidCommand, unpaidCommand));
                        }
                    }
                } else {
                    // computer

                    while (ComputerUtil.canPayCost(ability)) {
                        ComputerUtil.playNoStack(ability);
                    }

                    this.push(sa);
                }
            } else if (sp.isReplicate()) {
                // TODO: convert multikicker/replicate support in abCost so this
                // doesn't happen here
                // X and multi and replicate are not supported yet

                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sp.getReplicateManaCost()) {
                    @Override
                    public void resolve() {
                        this.getSourceCard().addReplicateMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3180458633098297855L;

                    @Override
                    public void execute() {
                        MagicStack.this.push(sa);
                        for (int i = 0; i < sp.getSourceCard().getReplicateMagnitude(); i++) {
                            AllZone.getCardFactory().copySpellontoStack(sp.getSourceCard(), sp.getSourceCard(), false);
                        }
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = 132624005072267304L;

                    @Override
                    public void execute() {
                        ability.resolve();
                        final ManaCost manaCost = MagicStack.this.getReplicateSpellCostChange(ability);
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {

                            AllZone.getInputControl().setInput(
                                    new Input_PayManaCost_Ability("Replicate for " + sa.getSourceCard() + "\r\n"
                                            + "Times Replicated: " + sa.getSourceCard().getReplicateMagnitude()
                                            + "\r\n", manaCost.toString(), this, unpaidCommand));
                        }
                    }
                };

                if (sp.getSourceCard().getController().equals(AllZone.getHumanPlayer())) {
                    final ManaCost manaCost = this.getMultiKickerSpellCostChange(ability);

                    if (manaCost.isPaid()) {
                        paidCommand.execute();
                    } else {
                        AllZone.getInputControl().setInput(
                                new Input_PayManaCost_Ability("Replicate for " + sa.getSourceCard() + "\r\n"
                                        + "Times Replicated: " + sa.getSourceCard().getReplicateMagnitude() + "\r\n",
                                        manaCost.toString(), paidCommand, unpaidCommand));
                    }
                } else {
                    // computer
                    while (ComputerUtil.canPayCost(ability)) {
                        ComputerUtil.playNoStack(ability);
                    }

                    this.push(sa);
                }
            }

        }

        // Copied spells aren't cast
        // per se so triggers shouldn't
        // run for them.
        if (!sp.getSourceCard().isCopiedSpell()) {
            // Run SpellAbilityCast triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Cost", sp.getPayCosts());
            runParams.put("Player", sp.getSourceCard().getController());
            runParams.put("Activator", sp.getActivatingPlayer());
            runParams.put("CastSA", sp);
            AllZone.getTriggerHandler().runTrigger("SpellAbilityCast", runParams);

            // Run SpellCast triggers
            if (sp.isSpell()) {
                AllZone.getTriggerHandler().runTrigger("SpellCast", runParams);
            }

            // Run AbilityCast triggers
            if (sp.isAbility() && !sp.isTrigger()) {
                AllZone.getTriggerHandler().runTrigger("AbilityCast", runParams);
            }

            // Run Cycled triggers
            if (sp.isCycling()) {
                runParams.clear();
                runParams.put("Card", sp.getSourceCard());
                AllZone.getTriggerHandler().runTrigger("Cycled", runParams);
            }

            // Run BecomesTarget triggers
            runParams.clear();
            runParams.put("SourceSA", sp);
            if (chosenTargets.size() > 0) {
                for (final Target_Choices tc : chosenTargets) {
                    if ((tc != null) && (tc.getTargetCards() != null)) {
                        for (final Object tgt : tc.getTargets()) {
                            runParams.put("Target", tgt);

                            AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
                        }
                    }
                }
            }

            // Not sure these clauses are necessary. Consider it a precaution
            // for backwards compatibility for hardcoded cards.
            else if (sp.getTargetCard() != null) {
                runParams.put("Target", sp.getTargetCard());

                AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
            } else if ((sp.getTargetList() != null) && (sp.getTargetList().size() > 0)) {
                for (final Card ctgt : sp.getTargetList()) {
                    runParams.put("Target", ctgt);

                    AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
                }
            } else if (sp.getTargetPlayer() != null) {
                runParams.put("Target", sp.getTargetPlayer());

                AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
            }
        }

        if ((sp instanceof Spell_Permanent) && sp.getSourceCard().getName().equals("Mana Vortex")) {
            final SpellAbility counter = new Ability(sp.getSourceCard(), "0") {
                @Override
                public void resolve() {
                    final Input in = new Input() {
                        private static final long serialVersionUID = -2042489457719935420L;

                        @Override
                        public void showMessage() {
                            AllZone.getDisplay().showMessage("Mana Vortex - select a land to sacrifice");
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectButtonCancel() {
                            AllZone.getStack().pop();
                            AllZone.getGameAction().moveToGraveyard(sp.getSourceCard());
                            this.stop();
                        }

                        @Override
                        public void selectCard(final Card c, final PlayerZone zone) {
                            if (zone.is(Constant.Zone.Battlefield) && c.getController().isHuman() && c.isLand()) {
                                AllZone.getGameAction().sacrifice(c);
                                this.stop();
                            }
                        }
                    };
                    final SpellAbility_StackInstance prev = MagicStack.this.peekInstance();
                    if (prev.isSpell() && prev.getSourceCard().getName().equals("Mana Vortex")) {
                        if (sp.getSourceCard().getController().isHuman()) {
                            AllZone.getInputControl().setInput(in);
                        } else { // Computer
                            final CardList lands = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                            if (!lands.isEmpty()) {
                                AllZone.getComputerPlayer().sacrificePermanent("prompt", lands);
                            } else {
                                AllZone.getStack().pop();
                                AllZone.getGameAction().moveToGraveyard(sp.getSourceCard());
                            }
                        }
                    }

                } // resolve()
            }; // SpellAbility

            counter.setStackDescription(sp.getSourceCard().getName()
                    + " - counter Mana Vortex unless you sacrifice a land.");

            this.add(counter);
        }

        /*
         * Whenever a player casts a spell, counter it if a card with the same
         * name is in a graveyard or a nontoken permanent with the same name is
         * on the battlefield.
         */
        if (sp.isSpell() && AllZoneUtil.isCardInPlay("Bazaar of Wonders")) {
            boolean found = false;
            CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);
            all = all.filter(CardListFilter.NON_TOKEN);
            final CardList graves = AllZoneUtil.getCardsIn(Zone.Graveyard);
            all.addAll(graves);

            for (final Card c : all) {
                if (sp.getSourceCard().getName().equals(c.getName())) {
                    found = true;
                }
            }

            if (found) {
                final CardList bazaars = AllZoneUtil.getCardsIn(Zone.Battlefield, "Bazaar of Wonders"); // should
                // only
                // be
                // 1...
                for (final Card bazaar : bazaars) {
                    final SpellAbility counter = new Ability(bazaar, "0") {
                        @Override
                        public void resolve() {
                            if (AllZone.getStack().size() > 0) {
                                AllZone.getStack().pop();
                            }
                        } // resolve()
                    }; // SpellAbility
                    counter.setStackDescription(bazaar.getName() + " - counter " + sp.getSourceCard().getName() + ".");
                    this.add(counter);
                }
            }
        }

        /*
         * if (sp.getTargetCard() != null)
         * CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());
         */

        if (this.getSimultaneousStackEntryList().size() > 0) {
            AllZone.getPhase().passPriority();
        }
    }

    /**
     * <p>
     * size.
     * </p>
     * 
     * @return a int.
     */
    public final int size() {
        return this.getStack().size();
    }

    // Push should only be used by add.
    /**
     * <p>
     * push.
     * </p>
     * 
     * @param sp
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private void push(final SpellAbility sp) {
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        this.incrementSplitSecond(sp);

        final SpellAbility_StackInstance si = new SpellAbility_StackInstance(sp);
        this.getStack().push(si);

        this.updateObservers();

        if (sp.isSpell() && !sp.getSourceCard().isCopiedSpell()) {
            this.thisTurnCast.add(sp.getSourceCard());

            GameActionUtil.executePlayCardEffects(sp);
        }
    }

    /**
     * <p>
     * resolveStack.
     * </p>
     */
    public final void resolveStack() {
        // Resolving the Stack

        // TODO: change to use forge.view.FView?
        GuiDisplayUtil.updateGUI();

        this.freezeStack(); // freeze the stack while we're in the middle of
                            // resolving
        this.setResolving(true);

        final SpellAbility sa = AllZone.getStack().pop();

        AllZone.getPhase().resetPriority(); // ActivePlayer gains priority first
                                            // after Resolve
        final Card source = sa.getSourceCard();

        if (this.hasFizzled(sa, source)) { // Fizzle
            // TODO: Spell fizzles, what's the best way to alert player?
            Log.debug(source.getName() + " ability fizzles.");
            this.finishResolving(sa, true);
        } else if (sa.getAbilityFactory() != null) {
            AbilityFactory.handleRemembering(sa.getAbilityFactory());
            AbilityFactory.resolve(sa, true);
        } else {
            sa.resolve();
            this.finishResolving(sa, false);
        }

        if (source.hasStartOfKeyword("Haunt") && !source.isCreature()
                && AllZone.getZoneOf(source).is(Constant.Zone.Graveyard)) {
            final CardList creats = AllZoneUtil.getCreaturesInPlay();
            for (int i = 0; i < creats.size(); i++) {
                if (!CardFactoryUtil.canTarget(source, creats.get(i))) {
                    creats.remove(i);
                    i--;
                }
            }
            if (creats.size() != 0) {
                final Ability haunterDiesWork = new Ability(source, "0") {
                    @Override
                    public void resolve() {
                        AllZone.getGameAction().exile(source);
                        this.getTargetCard().addHauntedBy(source);
                    }
                };
                haunterDiesWork.setDescription("");

                final Input target = new Input() {
                    private static final long serialVersionUID = 1981791992623774490L;

                    @Override
                    public void showMessage() {
                        AllZone.getDisplay().showMessage("Choose target creature to haunt.");
                        ButtonUtil.disableAll();
                    }

                    @Override
                    public void selectCard(final Card c, final PlayerZone zone) {
                        if (!zone.is(Constant.Zone.Battlefield) || !c.isCreature()) {
                            return;
                        }
                        if (CardFactoryUtil.canTarget(source, c)) {
                            haunterDiesWork.setTargetCard(c);
                            MagicStack.this.add(haunterDiesWork);
                            this.stop();
                        } else {
                            AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection?).");
                        }
                    }
                };

                if (source.getController().isHuman()) {
                    AllZone.getInputControl().setInput(target);
                } else {
                    // AI choosing what to haunt
                    final CardList oppCreats = creats.getController(AllZone.getHumanPlayer());
                    if (oppCreats.size() != 0) {
                        haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(oppCreats));
                    } else {
                        haunterDiesWork.setTargetCard(CardFactoryUtil.getWorstCreatureAI(creats));
                    }
                    this.add(haunterDiesWork);
                }
            }
        }

    }

    /**
     * <p>
     * removeCardFromStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle
     *            a boolean.
     * @since 1.0.15
     */
    public final void removeCardFromStack(final SpellAbility sa, final boolean fizzle) {
        final Card source = sa.getSourceCard();

        // do nothing
        if (sa.getSourceCard().isCopiedSpell() || sa.isAbility()) {
        }
        // Handle cards that need to be moved differently
        else if (sa.isBuyBackAbility() && !fizzle) {
            AllZone.getGameAction().moveToHand(source);
        } else if (sa.isFlashBackAbility()) {
            AllZone.getGameAction().exile(source);
            sa.setFlashBackAbility(false);
        }

        // If Spell and still on the Stack then let it goto the graveyard or
        // replace its own movement
        else if (!source.isCopiedSpell() && (source.isInstant() || source.isSorcery() || fizzle)
                && AllZone.getZoneOf(source).is(Constant.Zone.Stack)) {
            AllZone.getGameAction().moveToGraveyard(source);
        }
    }

    /**
     * <p>
     * finishResolving.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle
     *            a boolean.
     * @since 1.0.15
     */
    public final void finishResolving(final SpellAbility sa, final boolean fizzle) {

        // remove card from the stack
        this.removeCardFromStack(sa, fizzle);

        // After SA resolves we have to do a handful of things
        this.setResolving(false);
        this.unfreezeStack();
        sa.resetOnceResolved();

        AllZone.getGameAction().checkStateEffects();

        AllZone.getPhase().setNeedToNextPhase(false);

        if (AllZone.getPhase().inCombat()) {
            CombatUtil.showCombat();
        }

        // TODO: change to use forge.view.FView?
        GuiDisplayUtil.updateGUI();

        // TODO: this is a huge hack. Why is this necessary?
        // hostCard in AF is not the same object that's on the battlefield
        // verified by System.identityHashCode(card);
        final Card tmp = sa.getSourceCard();
        if (tmp.getClones().size() > 0) {
            for (final Card c : AllZoneUtil.getCardsIn(Zone.Battlefield)) {
                if (c.equals(tmp)) {
                    c.setClones(tmp.getClones());
                }
            }

        }
    }

    /**
     * <p>
     * hasFizzled.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean hasFizzled(final SpellAbility sa, final Card source) {
        // By default this has not fizzled
        boolean fizzle = false;

        boolean firstTarget = true;

        SpellAbility fizzSA = sa;

        while (true) {
            final Target tgt = fizzSA.getTarget();
            if ((tgt != null) && (tgt.getMinTargets(source, fizzSA) == 0) && (tgt.getNumTargeted() == 0)) {
                // Don't assume fizzled for minTargets == 0 and nothing is
                // targeted
            } else if (firstTarget
                    && ((tgt != null) || (fizzSA.getTargetCard() != null) || (fizzSA.getTargetPlayer() != null))) {
                // If there is at least 1 target, fizzle switches because ALL
                // targets need to be invalid
                fizzle = true;
                firstTarget = false;
            }

            if (tgt != null) {
                // With multi-targets, as long as one target is still legal,
                // we'll try to go through as much as possible
                final ArrayList<Object> tgts = tgt.getTargets();
                for (final Object o : tgts) {
                    if (o instanceof Player) {
                        final Player p = (Player) o;
                        fizzle &= !(p.canTarget(fizzSA));
                    }
                    if (o instanceof Card) {
                        final Card card = (Card) o;
                        fizzle &= !(CardFactoryUtil.isTargetStillValid(fizzSA, card));
                    }
                    if (o instanceof SpellAbility) {
                        final SpellAbility tgtSA = (SpellAbility) o;
                        fizzle &= !(Target_Selection.matchSpellAbility(fizzSA, tgtSA, tgt));
                    }
                }
            } else if (fizzSA.getTargetCard() != null) {
                // Fizzling will only work for Abilities that use the Target
                // class,
                // since the info isn't available otherwise
                fizzle &= !CardFactoryUtil.isTargetStillValid(fizzSA, fizzSA.getTargetCard());
            } else if (fizzSA.getTargetPlayer() != null) {
                fizzle &= !fizzSA.getTargetPlayer().canTarget(fizzSA);
            }

            if (fizzSA.getSubAbility() != null) {
                fizzSA = fizzSA.getSubAbility();
            } else {
                break;
            }
        }

        return fizzle;
    }

    /**
     * <p>
     * pop.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility pop() {
        final SpellAbility sp = this.getStack().pop().getSpellAbility();
        this.decrementSplitSecond(sp);
        this.updateObservers();
        return sp;
    }

    // CAREFUL! Peeking while an SAs Targets are being choosen may cause issues
    // index = 0 is the top, index = 1 is the next to top, etc...
    /**
     * <p>
     * peekInstance.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance}
     *         object.
     */
    public final SpellAbility_StackInstance peekInstance(final int index) {
        return this.getStack().get(index);
    }

    /**
     * <p>
     * peekAbility.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility peekAbility(final int index) {
        return this.getStack().get(index).getSpellAbility();
    }

    /**
     * <p>
     * peekInstance.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance}
     *         object.
     */
    public final SpellAbility_StackInstance peekInstance() {
        return this.getStack().peek();
    }

    /**
     * <p>
     * peekAbility.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility peekAbility() {
        return this.getStack().peek().getSpellAbility();
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void remove(final SpellAbility sa) {
        final SpellAbility_StackInstance si = this.getInstanceFromSpellAbility(sa);

        if (si == null) {
            return;
        }

        this.remove(si);
    }

    /**
     * <p>
     * remove.
     * </p>
     * 
     * @param si
     *            a {@link forge.card.spellability.SpellAbility_StackInstance}
     *            object.
     */
    public final void remove(final SpellAbility_StackInstance si) {
        if (this.getStack().remove(si)) {
            this.decrementSplitSecond(si.getSpellAbility());
        }
        this.getFrozenStack().remove(si);
        this.updateObservers();
    }

    /**
     * <p>
     * getInstanceFromSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance}
     *         object.
     */
    public final SpellAbility_StackInstance getInstanceFromSpellAbility(final SpellAbility sa) {
        // TODO: Confirm this works!
        for (final SpellAbility_StackInstance si : this.getStack()) {
            if (si.getSpellAbility().equals(sa)) {
                return si;
            }
        }
        return null;
    }

    /**
     * <p>
     * hasSimultaneousStackEntries.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSimultaneousStackEntries() {
        return this.getSimultaneousStackEntryList().size() > 0;
    }

    /**
     * <p>
     * addSimultaneousStackEntry.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addSimultaneousStackEntry(final SpellAbility sa) {
        this.getSimultaneousStackEntryList().add(sa);
    }

    /**
     * <p>
     * chooseOrderOfSimultaneousStackEntryAll.
     * </p>
     */
    public final void chooseOrderOfSimultaneousStackEntryAll() {
        final Player playerTurn = AllZone.getPhase().getPlayerTurn();

        this.chooseOrderOfSimultaneousStackEntry(playerTurn);

        if (playerTurn != null) {
            this.chooseOrderOfSimultaneousStackEntry(playerTurn.getOpponent());
        }
    }

    /**
     * <p>
     * chooseOrderOfSimultaneousStackEntry.
     * </p>
     * 
     * @param activePlayer
     *            a {@link forge.Player} object.
     */
    public final void chooseOrderOfSimultaneousStackEntry(final Player activePlayer) {
        if (this.getSimultaneousStackEntryList().size() == 0) {
            return;
        }

        final ArrayList<SpellAbility> activePlayerSAs = new ArrayList<SpellAbility>();
        for (int i = 0; i < this.getSimultaneousStackEntryList().size(); i++) {
            if (this.getSimultaneousStackEntryList().get(i).getActivatingPlayer() == null) {
                if (this.getSimultaneousStackEntryList().get(i).getSourceCard().getController().equals(activePlayer)) {
                    activePlayerSAs.add(this.getSimultaneousStackEntryList().get(i));
                    this.getSimultaneousStackEntryList().remove(i);
                    i--;
                }
            } else {
                if (this.getSimultaneousStackEntryList().get(i).getActivatingPlayer().equals(activePlayer)) {
                    activePlayerSAs.add(this.getSimultaneousStackEntryList().get(i));
                    this.getSimultaneousStackEntryList().remove(i);
                    i--;
                }
            }
        }
        if (activePlayerSAs.size() == 0) {
            return;
        }

        if (activePlayer.isComputer()) {
            for (final SpellAbility sa : activePlayerSAs) {
                sa.doTrigger(sa.isMandatory());
                ComputerUtil.playStack(sa);
            }
        } else {
            while (activePlayerSAs.size() > 1) {

                final SpellAbility next = (SpellAbility) GuiUtils.getChoice(
                        "Choose which spell or ability to put on the stack next.", activePlayerSAs.toArray());

                activePlayerSAs.remove(next);

                if (next.isTrigger()) {
                    System.out.println("Stack order: AllZone.getGameAction().playSpellAbility(next)");
                    AllZone.getGameAction().playSpellAbility(next);
                } else {
                    System.out.println("Stack order: AllZone.getStack().add(next)");
                    this.add(next);
                }
            }

            if (activePlayerSAs.get(0).isTrigger()) {
                AllZone.getGameAction().playSpellAbility(activePlayerSAs.get(0));
            } else {
                this.add(activePlayerSAs.get(0));
            }
            // AllZone.getGameAction().playSpellAbility(activePlayerSAs.get(0));
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * 
     * @param triggerID
     *            the trigger id
     * @return true, if successful
     */
    public final boolean hasStateTrigger(final int triggerID) {
        for (final SpellAbility_StackInstance sasi : this.getStack()) {
            if (sasi.isStateTrigger(triggerID)) {
                return true;
            }
        }

        for (final SpellAbility sa : this.simultaneousStackEntryList) {
            if (sa.getSourceTrigger() == triggerID) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the simultaneous stack entry list.
     * 
     * @return the simultaneousStackEntryList
     */
    public final List<SpellAbility> getSimultaneousStackEntryList() {
        return this.simultaneousStackEntryList;
    }

    /**
     * Gets the stack.
     * 
     * @return the stack
     */
    public final Stack<SpellAbility_StackInstance> getStack() {
        return this.stack;
    }

    /**
     * Gets the frozen stack.
     * 
     * @return the frozenStack
     */
    public final Stack<SpellAbility_StackInstance> getFrozenStack() {
        return this.frozenStack;
    }

    /**
     * Accessor for the field thisTurnCast.
     * 
     * @return a CardList.
     */
    public final CardList getCardsCastThisTurn() {
        return this.thisTurnCast;
    }

    /**
     * clearCardsCastThisTurn.
     */
    public final void clearCardsCastThisTurn() {
        this.thisTurnCast.clear();
    }

    /**
     * 
     * setCardsCastLastTurn.
     */
    public final void setCardsCastLastTurn() {
        this.lastTurnCast = new CardList(this.thisTurnCast);
    }

    /**
     * Accessor for the field lastTurnCast.
     * 
     * @return a CardList.
     */
    public final CardList getCardsCastLastTurn() {
        return this.lastTurnCast;
    }
}
