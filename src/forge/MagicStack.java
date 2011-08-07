package forge;

import com.esotericsoftware.minlog.Log;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.spellability.*;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost_Ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * <p>MagicStack class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class MagicStack extends MyObservable {
    private ArrayList<SpellAbility> simultaneousStackEntryList = new ArrayList<SpellAbility>();

    private Stack<SpellAbility_StackInstance> stack = new Stack<SpellAbility_StackInstance>();
    private Stack<SpellAbility_StackInstance> frozenStack = new Stack<SpellAbility_StackInstance>();

    private boolean frozen = false;
    private boolean bResolving = false;
    private int splitSecondOnStack = 0;

    /**
     * <p>isFrozen.</p>
     *
     * @return a boolean.
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * <p>Setter for the field <code>frozen</code>.</p>
     *
     * @param frozen a boolean.
     */
    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /**
     * <p>reset.</p>
     */
    public void reset() {
        stack.clear();
        frozen = false;
        splitSecondOnStack = 0;
        frozenStack.clear();
        this.updateObservers();
    }

    /**
     * <p>isSplitSecondOnStack.</p>
     *
     * @return a boolean.
     */
    public boolean isSplitSecondOnStack() {
        return splitSecondOnStack > 0;
    }

    /**
     * <p>incrementSplitSecond.</p>
     *
     * @param sp a {@link forge.card.spellability.SpellAbility} object.
     */
    public void incrementSplitSecond(SpellAbility sp) {
        if (sp.getSourceCard().hasKeyword("Split second"))
            splitSecondOnStack++;
    }

    /**
     * <p>decrementSplitSecond.</p>
     *
     * @param sp a {@link forge.card.spellability.SpellAbility} object.
     */
    public void decrementSplitSecond(SpellAbility sp) {
        if (sp.getSourceCard().hasKeyword("Split second"))
            splitSecondOnStack--;

        if (splitSecondOnStack < 0)
            splitSecondOnStack = 0;
    }

    /**
     * <p>freezeStack.</p>
     */
    public void freezeStack() {
        frozen = true;
    }

    /**
     * <p>addAndUnfreeze.</p>
     *
     * @param ability a {@link forge.card.spellability.SpellAbility} object.
     */
    public void addAndUnfreeze(SpellAbility ability) {
        ability.getRestrictions().abilityActivated();
        if (ability.getRestrictions().getActivationNumberSacrifice() != -1 &&
                ability.getRestrictions().getNumberTurnActivations() >= ability.getRestrictions().getActivationNumberSacrifice()) {
            ability.getSourceCard().addExtrinsicKeyword("At the beginning of the end step, sacrifice CARDNAME.");
        }
        // triggered abilities should go on the frozen stack
        if (!ability.isTrigger())
            frozen = false;

        this.add(ability);

        // if the ability is a spell, but not a copied spell and its not already on the stack zone, move there
        if (ability.isSpell()) {
            Card source = ability.getSourceCard();
            if (!source.isCopiedSpell() && !AllZone.getZone(source).is(Constant.Zone.Stack))
                AllZone.getGameAction().moveToStack(source);
        }

        if (ability.isTrigger())
            unfreezeStack();
    }

    /**
     * <p>unfreezeStack.</p>
     */
    public void unfreezeStack() {
        frozen = false;
        boolean checkState = !frozenStack.isEmpty();
        while (!frozenStack.isEmpty()) {
            SpellAbility sa = frozenStack.pop().getSpellAbility();
            this.add(sa);
        }
        if (checkState)
            AllZone.getGameAction().checkStateEffects();
    }

    /**
     * <p>clearFrozen.</p>
     */
    public void clearFrozen() {
        // TODO: frozen triggered abilities and undoable costs have nasty consequences
        frozen = false;
        frozenStack.clear();
    }

    /**
     * <p>setResolving.</p>
     *
     * @param b a boolean.
     */
    public void setResolving(boolean b) {
        bResolving = b;
        if (!bResolving) chooseOrderOfSimultaneousStackEntryAll();
    }

    /**
     * <p>getResolving.</p>
     *
     * @return a boolean.
     */
    public boolean getResolving() {
        return bResolving;
    }

    /**
     * <p>add.</p>
     *
     * @param sp a {@link forge.card.spellability.SpellAbility} object.
     * @param useX a boolean.
     */
    public void add(SpellAbility sp, boolean useX) {
        if (!useX)
            this.add(sp);
        else {

            // TODO make working triggered abilities!
            if (sp instanceof Ability_Mana || sp instanceof Ability_Triggered)
                sp.resolve();
            else {
                push(sp);
                /*if (sp.getTargetCard() != null)
                        CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());*/
            }
        }
    }

    /**
     * <p>getMultiKickerSpellCostChange.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public ManaCost getMultiKickerSpellCostChange(SpellAbility sa) {
        int Max = 25;
        String[] Numbers = new String[Max];
        for (int no = 0; no < Max; no++)
            Numbers[no] = String.valueOf(no);

        ManaCost manaCost = new ManaCost(sa.getManaCost());
        String Mana = manaCost.toString();

        int MultiKickerPaid = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid;

        String Number_ManaCost = " ";

        if (Mana.toString().length() == 1)
            Number_ManaCost = Mana.toString().substring(0, 1);

        else if (Mana.toString().length() == 0)
            Number_ManaCost = "0"; // Should Never Occur

        else
            Number_ManaCost = Mana.toString().substring(0, 2);
        Number_ManaCost = Number_ManaCost.trim();

        for (int check = 0; check < Max; check++) {
            if (Number_ManaCost.equals(Numbers[check])) {

                if (check - MultiKickerPaid < 0) {
                    MultiKickerPaid = MultiKickerPaid - check;
                    AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
                    Mana = Mana.replaceFirst(String.valueOf(check), "0");
                } else {
                    Mana = Mana.replaceFirst(String.valueOf(check), String.valueOf(check - MultiKickerPaid));
                    MultiKickerPaid = 0;
                    AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid = MultiKickerPaid;
                }
            }
            Mana = Mana.trim();
            if (Mana.equals(""))
                Mana = "0";
            manaCost = new ManaCost(Mana);
        }
        String Color_cut = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored;

        for (int Colored_Cut = 0; Colored_Cut < Color_cut.length(); Colored_Cut++) {
            if ("WUGRB".contains(Color_cut.substring(Colored_Cut, Colored_Cut + 1))) {

                if (!Mana.equals(Mana.replaceFirst((Color_cut.substring(Colored_Cut, Colored_Cut + 1)), ""))) {
                    Mana = Mana.replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
                    AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored = AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored
                            .replaceFirst(Color_cut.substring(Colored_Cut, Colored_Cut + 1), "");
                    Mana = Mana.trim();
                    if (Mana.equals(""))
                        Mana = "0";
                    manaCost = new ManaCost(Mana);
                }
            }
        }

        return manaCost;
    }

    //TODO - this may be able to use a straight copy of MultiKicker cost change
    /**
     * <p>getReplicateSpellCostChange.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public ManaCost getReplicateSpellCostChange(SpellAbility sa) {
        ManaCost manaCost = new ManaCost(sa.getManaCost());
        //String Mana = manaCost.toString();
        return manaCost;
    }

    /**
     * <p>add.</p>
     *
     * @param sp a {@link forge.card.spellability.SpellAbility} object.
     */
    public void add(final SpellAbility sp) {
        ArrayList<Target_Choices> chosenTargets = sp.getAllTargetChoices();

        if (sp instanceof Ability_Mana) { // Mana Abilities go straight through
            sp.resolve();
            sp.resetOnceResolved();
            return;
        }

        if (frozen) {
            SpellAbility_StackInstance si = new SpellAbility_StackInstance(sp);
            frozenStack.push(si);
            return;
        }

        // if activating player slips through the cracks, assign activating
        // Player to the controller here
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        if (AllZone.getPhase().is(Constant.Phase.Cleanup)) {    // If something triggers during Cleanup, need to repeat
            AllZone.getPhase().repeatPhase();
        }

        // TODO: triggered abilities need to be fixed
        if (!(sp instanceof Ability_Triggered || sp instanceof Ability_Static))
            AllZone.getPhase().setPriority(sp.getActivatingPlayer());    // when something is added we need to setPriority

        if (sp instanceof Ability_Triggered || sp instanceof Ability_Static)
            // TODO make working triggered ability
            sp.resolve();
        else {
            if (sp.isKickerAbility()) {
                sp.getSourceCard().setKicked(true);
                SpellAbility[] sa = sp.getSourceCard().getSpellAbility();
                int AbilityNumber = 0;

                for (int i = 0; i < sa.length; i++)
                    if (sa[i] == sp)
                        AbilityNumber = i;

                sp.getSourceCard().setAbilityUsed(AbilityNumber);
            }
            if (sp.getSourceCard().isCopiedSpell())
                push(sp);

            else if (!sp.isMultiKicker() && !sp.isReplicate() && !sp.isXCost()) {
                push(sp);
            } else if (sp.getPayCosts() != null && !sp.isMultiKicker() && !sp.isReplicate()) {
                push(sp);
            } else if (sp.isXCost()) {
                // TODO: convert any X costs to use abCost so it happens earlier
                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sa.getXManaCost()) {
                    public void resolve() {
                        Card crd = this.getSourceCard();
                        crd.addXManaCostPaid(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    public void execute() {
                        push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -2224875229611007788L;

                    public void execute() {
                        ability.resolve();
                        Card crd = sa.getSourceCard();
                        AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Pay X cost for " + crd.getName()
                                + " (X=" + crd.getXManaCostPaid() + ")\r\n",
                                ability.getManaCost(), this, unpaidCommand, true));
                    }
                };

                Card crd = sa.getSourceCard();
                if (sp.getSourceCard().getController().isHuman()) {
                    AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Pay X cost for " +
                            sp.getSourceCard().getName() + " (X=" + crd.getXManaCostPaid() + ")\r\n",
                            ability.getManaCost(), paidCommand, unpaidCommand, true));
                } else // computer
                {
                    int neededDamage = CardFactoryUtil.getNeededXDamage(sa);

                    while (ComputerUtil.canPayCost(ability) && neededDamage != sa.getSourceCard().getXManaCostPaid()) {
                        ComputerUtil.playNoStack(ability);
                    }
                    push(sa);
                }
            } else if (sp.isMultiKicker()) {
                // TODO: convert multikicker support in abCost so this doesn't happen here
                // both X and multi is not supported yet

                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sp.getMultiKickerManaCost()) {
                    public void resolve() {
                        this.getSourceCard().addMultiKickerMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3342222770086269767L;

                    public void execute() {
                        push(sa);
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = -6037161763374971106L;

                    public void execute() {
                        ability.resolve();
                        ManaCost manaCost = getMultiKickerSpellCostChange(ability);
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {
                            if (AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid == 0
                                    && AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {

                                AllZone.getInputControl().setInput(new Input_PayManaCost_Ability(
                                        "Multikicker for " + sa.getSourceCard() + "\r\n"
                                                + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                        manaCost.toString(), this, unpaidCommand));
                            } else {
                                AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Multikicker for "
                                        + sa.getSourceCard() + "\r\n" + "Mana in Reserve: "
                                        + ((AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid != 0) ?
                                        AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid : "")
                                        + AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored + "\r\n"
                                        + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                        manaCost.toString(), this, unpaidCommand));
                            }
                        }
                    }
                };

                if (sp.getActivatingPlayer().isHuman()) {
                    ManaCost manaCost = getMultiKickerSpellCostChange(ability);

                    if (manaCost.isPaid()) {
                        paidCommand.execute();
                    } else {
                        if (AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid == 0
                                && AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {
                            AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Multikicker for "
                                    + sa.getSourceCard() + "\r\n" + "Times Kicked: "
                                    + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                    manaCost.toString(), paidCommand, unpaidCommand));
                        } else {
                            AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Multikicker for "
                                    + sa.getSourceCard() + "\r\n" + "Mana in Reserve: " +
                                    ((AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid != 0) ?
                                            AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid : "")
                                    + AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored
                                    + "\r\n" + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                    manaCost.toString(), paidCommand, unpaidCommand));
                        }
                    }
                } else // computer
                {
                    while (ComputerUtil.canPayCost(ability))
                        ComputerUtil.playNoStack(ability);
                    push(sa);
                }
            } else if (sp.isReplicate()) {
                // TODO: convert multikicker/replicate support in abCost so this doesn't happen here
                // X and multi and replicate are not supported yet

                final SpellAbility sa = sp;
                final Ability ability = new Ability(sp.getSourceCard(), sp.getReplicateManaCost()) {
                    public void resolve() {
                        this.getSourceCard().addReplicateMagnitude(1);
                    }
                };

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -3180458633098297855L;

                    public void execute() {
                        push(sa);
                        for (int i = 0; i < sp.getSourceCard().getReplicateMagnitude(); i++) {
                            AllZone.getCardFactory().copySpellontoStack(sp.getSourceCard(), sp.getSourceCard(), false);
                        }
                    }
                };

                final Command paidCommand = new Command() {
                    private static final long serialVersionUID = 132624005072267304L;

                    public void execute() {
                        ability.resolve();
                        ManaCost manaCost = getReplicateSpellCostChange(ability);
                        if (manaCost.isPaid()) {
                            this.execute();
                        } else {
                            /*
                                       if (AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid == 0
                                               && AllZone.getGameAction().CostCutting_GetMultiMickerManaCostPaid_Colored.equals("")) {

                                           AllZone.getInputControl().setInput(new Input_PayManaCost_Ability(
                                                   "Replicate for "+ sa.getSourceCard() + "\r\n"
                                                   + "Times Kicked: " + sa.getSourceCard().getMultiKickerMagnitude() + "\r\n",
                                                   manaCost.toString(), this, unpaidCommand));
                                       }

                                       else {*/
                            AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Replicate for "
                                    + sa.getSourceCard() + "\r\n"
                                    + "Times Replicated: " + sa.getSourceCard().getReplicateMagnitude() + "\r\n",
                                    manaCost.toString(), this, unpaidCommand));
                            //}
                        }
                    }
                };

                if (sp.getSourceCard().getController().equals(
                        AllZone.getHumanPlayer())) {
                    ManaCost manaCost = getMultiKickerSpellCostChange(ability);

                    if (manaCost.isPaid()) {
                        paidCommand.execute();
                    } else {
                        AllZone.getInputControl().setInput(new Input_PayManaCost_Ability("Replicate for "
                                + sa.getSourceCard() + "\r\n" +
                                "Times Replicated: " + sa.getSourceCard().getReplicateMagnitude() + "\r\n",
                                manaCost.toString(), paidCommand, unpaidCommand));
                    }
                } else // computer
                {
                    while (ComputerUtil.canPayCost(ability))
                        ComputerUtil.playNoStack(ability);
                    push(sa);
                }
            }

        }

        if (!sp.getSourceCard().isCopiedSpell()) //Copied spells aren't cast per se so triggers shouldn't run for them.
        {
            //Run SpellAbilityCast triggers
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Cost", sp.getPayCosts());
            runParams.put("Player", sp.getSourceCard().getController());
            runParams.put("Activator", sp.getActivatingPlayer());
            runParams.put("CastSA", sp);
            AllZone.getTriggerHandler().runTrigger("SpellAbilityCast", runParams);

            //Run SpellCast triggers
            if (sp.isSpell()) {
                AllZone.getTriggerHandler().runTrigger("SpellCast", runParams);
            }

            //Run AbilityCast triggers
            if (sp.isAbility()) {
                AllZone.getTriggerHandler().runTrigger("AbilityCast", runParams);
            }

            //Run Cycled triggers
            if (sp.isCycling()) {
                runParams.clear();
                runParams.put("Card", sp.getSourceCard());
                AllZone.getTriggerHandler().runTrigger("Cycled", runParams);
            }

            //Run BecomesTarget triggers
            runParams.clear();
            runParams.put("SourceSA", sp);
            if (chosenTargets.size() > 0) {
                for (Target_Choices tc : chosenTargets) {
                    if (tc != null) {
                        if (tc.getTargetCards() != null) {
                            for (Object tgt : tc.getTargets()) {
                                runParams.put("Target", tgt);

                                AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
                            }
                        }
                    }
                }
            }
            //Not sure these clauses are necessary. Consider it a precaution for backwards compatibility for hardcoded cards.
            if (sp.getTargetCard() != null) {
                runParams.put("Target", sp.getTargetCard());

                AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
            }
            if (sp.getTargetList() != null) {
                if (sp.getTargetList().size() > 0) {
                    for (Card ctgt : sp.getTargetList()) {
                        runParams.put("Target", ctgt);

                        AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
                    }
                }
            }
            if (sp.getTargetPlayer() != null) {
                runParams.put("Target", sp.getTargetPlayer());

                AllZone.getTriggerHandler().runTrigger("BecomesTarget", runParams);
            }
        }

        if (sp instanceof Spell_Permanent && sp.getSourceCard().getName().equals("Mana Vortex")) {
            final SpellAbility counter = new Ability(sp.getSourceCard(), "0") {
                @Override
                public void resolve() {
                    Input in = new Input() {
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
                            stop();
                        }

                        @Override
                        public void selectCard(Card c, PlayerZone zone) {
                            if (zone.is(Constant.Zone.Battlefield) && c.getController().isHuman()
                                    && c.isLand()) {
                                AllZone.getGameAction().sacrifice(c);
                                stop();
                            }
                        }
                    };
                    SpellAbility_StackInstance prev = peekInstance();
                    if (prev.isSpell() && prev.getSourceCard().getName().equals("Mana Vortex")) {
                        if (sp.getSourceCard().getController().isHuman()) {
                            AllZone.getInputControl().setInput(in);
                        } else {//Computer
                            CardList lands = AllZoneUtil.getPlayerLandsInPlay(AllZone.getComputerPlayer());
                            if (!lands.isEmpty()) {
                                AllZone.getComputerPlayer().sacrificePermanent("prompt", lands);
                            } else {
                                AllZone.getStack().pop();
                                AllZone.getGameAction().moveToGraveyard(sp.getSourceCard());
                            }
                        }
                    }

                }//resolve()
            };//SpellAbility
            counter.setStackDescription(sp.getSourceCard().getName() + " - counter Mana Vortex unless you sacrifice a land.");
            add(counter);
        }

        /*
           * Whenever a player casts a spell, counter it if a card with the same name
           * is in a graveyard or a nontoken permanent with the same name is on the battlefield.
           */
        if (sp.isSpell() && AllZoneUtil.isCardInPlay("Bazaar of Wonders")) {
            boolean found = false;
            CardList all = AllZoneUtil.getCardsInPlay();
            all = all.filter(AllZoneUtil.nonToken);
            CardList graves = AllZoneUtil.getCardsInGraveyard();
            all.addAll(graves);

            for (Card c : all) {
                if (sp.getSourceCard().getName().equals(c.getName())) found = true;
            }

            if (found) {
                CardList bazaars = AllZoneUtil.getCardsInPlay("Bazaar of Wonders");  //should only be 1...
                for (final Card bazaar : bazaars) {
                    final SpellAbility counter = new Ability(bazaar, "0") {
                        @Override
                        public void resolve() {
                            if (AllZone.getStack().size() > 0) AllZone.getStack().pop();
                        }//resolve()
                    };//SpellAbility
                    counter.setStackDescription(bazaar.getName() + " - counter " + sp.getSourceCard().getName() + ".");
                    add(counter);
                }
            }
        }

        //Lurking Predators
        if (sp.isSpell()) {
            Player player = sp.getSourceCard().getController();
            CardList lurkingPredators = AllZoneUtil.getPlayerCardsInPlay(player, "Lurking Predators");

            for (int i = 0; i < lurkingPredators.size(); i++) {
                StringBuilder revealMsg = new StringBuilder("");
                if (lurkingPredators.get(i).getController().isHuman()) {
                    revealMsg.append("You reveal: ");
                    if (AllZone.getHumanLibrary().size() == 0) {
                        revealMsg.append("Nothing!");
                        GameActionUtil.showInfoDialg(revealMsg.toString());
                        continue;
                    }
                    Card revealed = AllZone.getHumanLibrary().get(0);

                    revealMsg.append(revealed.getName());
                    if (!revealed.isCreature()) {
                        revealMsg.append("\n\rPut it on the bottom of your library?");
                        if (GameActionUtil.showYesNoDialog(lurkingPredators.get(i), revealMsg.toString())) {
                            AllZone.getGameAction().moveToBottomOfLibrary(revealed);
                        } else {
                            AllZone.getGameAction().moveToLibrary(revealed);
                        }
                    } else {
                        GameActionUtil.showInfoDialg(revealMsg.toString());
                        AllZone.getGameAction().moveToPlay(revealed);
                    }
                } else {
                    revealMsg.append("Computer reveals: ");
                    if (AllZone.getComputerLibrary().size() == 0) {
                        revealMsg.append("Nothing!");
                        GameActionUtil.showInfoDialg(revealMsg.toString());
                        continue;
                    }
                    Card revealed = AllZone.getComputerLibrary().get(0);
                    revealMsg.append(revealed.getName());
                    if (!revealed.isCreature()) {
                        GameActionUtil.showInfoDialg(revealMsg.toString());
                        if (lurkingPredators.size() > i) {
                            AllZone.getGameAction().moveToBottomOfLibrary(revealed);
                        } else {
                            AllZone.getGameAction().moveToLibrary(revealed);
                        }
                    } else {
                        GameActionUtil.showInfoDialg(revealMsg.toString());
                        AllZone.getGameAction().moveToPlay(revealed);
                    }

                }
            }
        }

        /*if (sp.getTargetCard() != null)
              CardFactoryUtil.checkTargetingEffects(sp, sp.getTargetCard());*/

        if (simultaneousStackEntryList.size() > 0)
            AllZone.getPhase().passPriority();
    }

    /**
     * <p>size.</p>
     *
     * @return a int.
     */
    public int size() {
        return stack.size();
    }

    // Push should only be used by add.
    /**
     * <p>push.</p>
     *
     * @param sp a {@link forge.card.spellability.SpellAbility} object.
     */
    private void push(SpellAbility sp) {
        if (null == sp.getActivatingPlayer()) {
            sp.setActivatingPlayer(sp.getSourceCard().getController());
            System.out.println(sp.getSourceCard().getName() + " - activatingPlayer not set before adding to stack.");
        }

        incrementSplitSecond(sp);

        SpellAbility_StackInstance si = new SpellAbility_StackInstance(sp);
        stack.push(si);

        this.updateObservers();

        if (sp.isSpell() && !sp.getSourceCard().isCopiedSpell()) {
            Phase.increaseSpellCount(sp);

            GameActionUtil.executePlayCardEffects(sp);
        }
    }

    /**
     * <p>resolveStack.</p>
     */
    public void resolveStack() {
        // Resolving the Stack
        GuiDisplayUtil.updateGUI();
        this.freezeStack();    // freeze the stack while we're in the middle of resolving
        setResolving(true);

        SpellAbility sa = AllZone.getStack().pop();

        AllZone.getPhase().resetPriority();    // ActivePlayer gains priority first after Resolve
        Card source = sa.getSourceCard();

        if (hasFizzled(sa, source)) {//Fizzle
            // TODO: Spell fizzles, what's the best way to alert player?
            Log.debug(source.getName() + " ability fizzles.");
            finishResolving(sa, true);
        } else if (sa.getAbilityFactory() != null) {
            AbilityFactory.handleRemembering(sa.getAbilityFactory());
            AbilityFactory.resolve(sa, true);
        } else {
            sa.resolve();
            finishResolving(sa, false);
        }

    }

    /**
     * <p>removeCardFromStack.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle a boolean.
     * @since 1.0.15
     */
    public void removeCardFromStack(SpellAbility sa, boolean fizzle) {
        Card source = sa.getSourceCard();

        //do nothing
        if (sa.getSourceCard().isCopiedSpell() || sa.isAbility()) {
        }
        // Handle cards that need to be moved differently
        else if (sa.isBuyBackAbility() && !fizzle) {
            AllZone.getGameAction().moveToHand(source);
        } else if (sa.isFlashBackAbility()) {
            AllZone.getGameAction().exile(source);
            sa.setFlashBackAbility(false);
        }

        // If Spell and still on the Stack then let it goto the graveyard or replace its own movement
        else if (!source.isCopiedSpell() && (source.isInstant() || source.isSorcery() || fizzle) &&
                AllZone.getZone(source).is(Constant.Zone.Stack)) {
            if (source.getReplaceMoveToGraveyard().size() == 0)
                AllZone.getGameAction().moveToGraveyard(source);
            else {
                source.replaceMoveToGraveyard();
            }
        }
    }

    /**
     * <p>finishResolving.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param fizzle a boolean.
     * @since 1.0.15
     */
    public void finishResolving(SpellAbility sa, boolean fizzle) {

        //remove card from the stack
        removeCardFromStack(sa, fizzle);

        // After SA resolves we have to do a handful of things
        setResolving(false);
        this.unfreezeStack();
        sa.resetOnceResolved();

        AllZone.getGameAction().checkStateEffects();

        AllZone.getPhase().setNeedToNextPhase(false);

        if (AllZone.getPhase().inCombat())
            CombatUtil.showCombat();

        GuiDisplayUtil.updateGUI();

        //TODO - this is a huge hack.  Why is this necessary?
        //hostCard in AF is not the same object that's on the battlefield
        //verified by System.identityHashCode(card);
        Card tmp = sa.getSourceCard();
        if (tmp.getClones().size() > 0) {
            for (Card c : AllZoneUtil.getCardsInPlay()) {
                if (c.equals(tmp)) {
                    c.setClones(tmp.getClones());
                }
            }

        }
    }

    /**
     * <p>hasFizzled.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean hasFizzled(SpellAbility sa, Card source) {
        // By default this has not fizzled
        boolean fizzle = false;

        boolean firstTarget = true;

        SpellAbility fizzSA = sa;

        while (true) {
            Target tgt = fizzSA.getTarget();
            if (tgt != null && tgt.getMinTargets(source, fizzSA) == 0 && tgt.getNumTargeted() == 0) {
                // Don't assume fizzled for minTargets == 0 and nothing is targeted
            } else if (firstTarget && (tgt != null || fizzSA.getTargetCard() != null || fizzSA.getTargetPlayer() != null)) {
                // If there is at least 1 target, fizzle switches because ALL targets need to be invalid
                fizzle = true;
                firstTarget = false;
            }

            if (tgt != null) {
                // With multi-targets, as long as one target is still legal, we'll try to go through as much as possible
                ArrayList<Object> tgts = tgt.getTargets();
                for (Object o : tgts) {
                    if (o instanceof Player) {
                        Player p = (Player) o;
                        fizzle &= !(p.canTarget(fizzSA));
                    }
                    if (o instanceof Card) {
                        Card card = (Card) o;
                        fizzle &= !(CardFactoryUtil.isTargetStillValid(fizzSA, card));
                    }
                    if (o instanceof SpellAbility) {
                        SpellAbility tgtSA = (SpellAbility) o;
                        fizzle &= !(Target_Selection.matchSpellAbility(fizzSA, tgtSA, tgt));
                    }
                }
            } else if (fizzSA.getTargetCard() != null) {
                // Fizzling will only work for Abilities that use the Target class,
                // since the info isn't available otherwise
                fizzle &= !CardFactoryUtil.isTargetStillValid(fizzSA, fizzSA.getTargetCard());
            } else if (fizzSA.getTargetPlayer() != null) {
                fizzle &= !fizzSA.getTargetPlayer().canTarget(fizzSA);
            }

            if (fizzSA.getSubAbility() != null)
                fizzSA = fizzSA.getSubAbility();
            else
                break;
        }

        return fizzle;
    }

    /**
     * <p>pop.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility pop() {
        SpellAbility sp = stack.pop().getSpellAbility();
        decrementSplitSecond(sp);
        this.updateObservers();
        return sp;
    }

    // CAREFUL! Peeking while an SAs Targets are being choosen may cause issues
    // index = 0 is the top, index = 1 is the next to top, etc...
    /**
     * <p>peekInstance.</p>
     *
     * @param index a int.
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance} object.
     */
    public SpellAbility_StackInstance peekInstance(int index) {
        return stack.get(index);
    }

    /**
     * <p>peekAbility.</p>
     *
     * @param index a int.
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility peekAbility(int index) {
        return stack.get(index).getSpellAbility();
    }

    /**
     * <p>peekInstance.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance} object.
     */
    public SpellAbility_StackInstance peekInstance() {
        return stack.peek();
    }

    /**
     * <p>peekAbility.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility peekAbility() {
        return stack.peek().getSpellAbility();
    }

    /**
     * <p>remove.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void remove(SpellAbility sa) {
        SpellAbility_StackInstance si = getInstanceFromSpellAbility(sa);
        if (si == null)
            return;

        remove(si);
    }

    /**
     * <p>remove.</p>
     *
     * @param si a {@link forge.card.spellability.SpellAbility_StackInstance} object.
     */
    public void remove(SpellAbility_StackInstance si) {
        if (stack.remove(si)) {
            decrementSplitSecond(si.getSpellAbility());
        }
        frozenStack.remove(si);
        this.updateObservers();
    }

    /**
     * <p>getInstanceFromSpellAbility.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.card.spellability.SpellAbility_StackInstance} object.
     */
    public SpellAbility_StackInstance getInstanceFromSpellAbility(SpellAbility sa) {
        // TODO: Confirm this works!
        for (SpellAbility_StackInstance si : stack) {
            if (si.getSpellAbility().equals(sa))
                return si;
        }
        return null;
    }

    /**
     * <p>hasSimultaneousStackEntries.</p>
     *
     * @return a boolean.
     */
    public boolean hasSimultaneousStackEntries() {
        return simultaneousStackEntryList.size() > 0;
    }

    /**
     * <p>addSimultaneousStackEntry.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void addSimultaneousStackEntry(SpellAbility sa) {
        simultaneousStackEntryList.add(sa);
    }

    /**
     * <p>chooseOrderOfSimultaneousStackEntryAll.</p>
     */
    public void chooseOrderOfSimultaneousStackEntryAll() {
        chooseOrderOfSimultaneousStackEntry(AllZone.getPhase().getPlayerTurn());
        chooseOrderOfSimultaneousStackEntry(AllZone.getPhase().getPlayerTurn().getOpponent());
    }

    /**
     * <p>chooseOrderOfSimultaneousStackEntry.</p>
     *
     * @param activePlayer a {@link forge.Player} object.
     */
    public void chooseOrderOfSimultaneousStackEntry(Player activePlayer) {
        if (simultaneousStackEntryList.size() == 0)
            return;

        ArrayList<SpellAbility> activePlayerSAs = new ArrayList<SpellAbility>();
        for (int i = 0; i < simultaneousStackEntryList.size(); i++) {
            if (simultaneousStackEntryList.get(i).getActivatingPlayer() == null) {
                if (simultaneousStackEntryList.get(i).getSourceCard().getController().equals(activePlayer)) {
                    activePlayerSAs.add(simultaneousStackEntryList.get(i));
                    simultaneousStackEntryList.remove(i);
                    i--;
                }
            } else {
                if (simultaneousStackEntryList.get(i).getActivatingPlayer().equals(activePlayer)) {
                    activePlayerSAs.add(simultaneousStackEntryList.get(i));
                    simultaneousStackEntryList.remove(i);
                    i--;
                }
            }
        }
        if (activePlayerSAs.size() == 0)
            return;

        if (activePlayer.isComputer()) {
            for (SpellAbility sa : activePlayerSAs) {
                sa.doTrigger(sa.isMandatory());
                ComputerUtil.playStack(sa);
            }
        } else {
            while (activePlayerSAs.size() > 1) {
                SpellAbility next = (SpellAbility) GuiUtils.getChoice("Choose which spell or ability to put on the stack next.", activePlayerSAs.toArray());
                activePlayerSAs.remove(next);

                if (next.isTrigger()) {
                    System.out.println("Stack order: AllZone.getGameAction().playSpellAbility(next)");
                    AllZone.getGameAction().playSpellAbility(next);
                } else {
                    System.out.println("Stack order: AllZone.getStack().add(next)");
                    add(next);
                }
            }

            if (activePlayerSAs.get(0).isTrigger())
                AllZone.getGameAction().playSpellAbility(activePlayerSAs.get(0));
            else
                add(activePlayerSAs.get(0));
            //AllZone.getGameAction().playSpellAbility(activePlayerSAs.get(0));
        }
    }

    public boolean hasStateTrigger(int triggerID) {
        for(SpellAbility_StackInstance SI : stack)
        {
            if(SI.isStateTrigger(triggerID))
            {
                return true;
            }
        }

        return false;
    }

}
