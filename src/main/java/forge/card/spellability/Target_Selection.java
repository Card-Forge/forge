package forge.card.spellability;

import forge.*;
import forge.card.cardFactory.CardFactoryUtil;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>Target_Selection class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Target_Selection {
    private Target target = null;
    private SpellAbility ability = null;
    private Card card = null;
    private Target_Selection subSelection = null;

    /**
     * <p>getTgt.</p>
     *
     * @return a {@link forge.card.spellability.Target} object.
     */
    public Target getTgt() {
        return target;
    }

    /**
     * <p>Getter for the field <code>ability</code>.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getAbility() {
        return ability;
    }

    /**
     * <p>Getter for the field <code>card</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return card;
    }

    private SpellAbility_Requirements req = null;

    /**
     * <p>setRequirements.</p>
     *
     * @param reqs a {@link forge.card.spellability.SpellAbility_Requirements} object.
     */
    public void setRequirements(SpellAbility_Requirements reqs) {
        req = reqs;
    }

    private boolean bCancel = false;

    /**
     * <p>setCancel.</p>
     *
     * @param done a boolean.
     */
    public void setCancel(boolean done) {
        bCancel = done;
    }

    /**
     * <p>isCanceled.</p>
     *
     * @return a boolean.
     */
    public boolean isCanceled() {
    	if (bCancel)
    		return bCancel;
    	
    	if (subSelection == null)
    		return false;

        return subSelection.isCanceled();
    }

    private boolean bDoneTarget = false;

    /**
     * <p>setDoneTarget.</p>
     *
     * @param done a boolean.
     */
    public void setDoneTarget(boolean done) {
        bDoneTarget = done;
    }

    /**
     * <p>Constructor for Target_Selection.</p>
     *
     * @param tgt a {@link forge.card.spellability.Target} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public Target_Selection(Target tgt, SpellAbility sa) {
        target = tgt;
        ability = sa;
        card = sa.getSourceCard();
    }

    /**
     * <p>doesTarget.</p>
     *
     * @return a boolean.
     */
    public boolean doesTarget() {
        if (target == null)
            return false;
        return target.doesTarget();
    }

    /**
     * <p>resetTargets.</p>
     */
    public void resetTargets() {
        if (target != null)
            target.resetTargets();
    }

    /**
     * <p>chooseTargets.</p>
     *
     * @return a boolean.
     */
    public boolean chooseTargets() {
        // if not enough targets chosen, reset and cancel Ability
        if (bCancel || (bDoneTarget && !target.isMinTargetsChosen(card, ability))) {
            bCancel = true;
            req.finishedTargeting();
            return false;
        } else if (!doesTarget() || bDoneTarget && target.isMinTargetsChosen(card, ability) || target.isMaxTargetsChosen(card, ability)) {
            Ability_Sub abSub = ability.getSubAbility();

            if (abSub == null) {
                // if no more SubAbilities finish targeting
                req.finishedTargeting();
                return true;
            } else {
                // Has Sub Ability
            	subSelection = new Target_Selection(abSub.getTarget(), abSub);
            	subSelection.setRequirements(req);
            	subSelection.resetTargets();
                return subSelection.chooseTargets();
            }
        }

        chooseValidInput();

        return false;
    }

    public ArrayList<Object> getUniqueTargets(SpellAbility ability){
        ArrayList<Object> targets = new ArrayList<Object>();
        SpellAbility child = ability;
        while(child instanceof Ability_Sub){
            child = ((Ability_Sub)child).getParent();
            targets.addAll(child.getTarget().getTargets());
        }
        
        return targets;
    }
    
    // these have been copied over from CardFactoryUtil as they need two extra parameters for target selection.
    // however, due to the changes necessary for SA_Requirements this is much different than the original

    /**
     * <p>chooseValidInput.</p>
     */
    public void chooseValidInput() {
        Target tgt = this.getTgt();
        String zone = tgt.getZone();
        final boolean mandatory = target.getMandatory() ? target.hasCandidates(true) : false;

        if (zone.equals(Constant.Zone.Stack)) {
            // If Zone is Stack, the choices are handled slightly differently
            chooseCardFromStack(mandatory);
            return;
        }

        CardList choices = AllZoneUtil.getCardsInZone(zone).getValidCards(target.getValidTgts(), ability.getActivatingPlayer(), ability.getSourceCard());

        ArrayList<Object> objects = new ArrayList<Object>();
        if (tgt.isUniqueTargets()){
            objects = getUniqueTargets(ability);
            for (Object o : objects) {
                if (o instanceof Card && objects.contains(o)){
                    choices.remove((Card)o);
                }
            }
        }
        
        // Remove cards already targeted
        ArrayList<Card> targeted = tgt.getTargetCards();
        for (Card c : targeted) {
            if (choices.contains(c)){
                choices.remove(c);
            }
        }
        
        if (zone.equals(Constant.Zone.Battlefield)) {
            AllZone.getInputControl().setInput(input_targetSpecific(choices, true, mandatory, objects));
        } else{
            chooseCardFromList(choices, true, mandatory);
        }
    }//input_targetValid

    //CardList choices are the only cards the user can successful select
    /**
     * <p>input_targetSpecific.</p>
     *
     * @param choices a {@link forge.CardList} object.
     * @param targeted a boolean.
     * @param mandatory a boolean.
     * @param objects TODO
     * @return a {@link forge.gui.input.Input} object.
     */
    public Input input_targetSpecific(final CardList choices, final boolean targeted, final boolean mandatory, final ArrayList<Object> alreadyTargeted) {
        final SpellAbility sa = this.ability;
        final Target_Selection select = this;
        final Target tgt = this.target;
        final SpellAbility_Requirements req = this.req;

        Input target = new Input() {
            private static final long serialVersionUID = -1091595663541356356L;

            @Override
            public void showMessage() {
                StringBuilder sb = new StringBuilder();
                sb.append("Targeted: ");
                for(Object o : alreadyTargeted){
                    sb.append(o).append(" ");
                }
                sb.append(tgt.getTargetedString());
                sb.append("\n");
                sb.append(tgt.getVTSelection());

                AllZone.getDisplay().showMessage(sb.toString());

                // If reached Minimum targets, enable OK button
                if (!tgt.isMinTargetsChosen(sa.getSourceCard(), sa))
                    ButtonUtil.enableOnlyCancel();
                else
                    ButtonUtil.enableAll();

                if (mandatory)
                    ButtonUtil.disableCancel();
            }

            @Override
            public void selectButtonCancel() {
                select.setCancel(true);
                stop();
                req.finishedTargeting();
            }

            @Override
            public void selectButtonOK() {
                select.setDoneTarget(true);
                done();
            }

            @Override
            public void selectCard(Card card, PlayerZone zone) {
                // leave this in temporarily, there some seriously wrong things going on here
                if (targeted && !CardFactoryUtil.canTarget(sa, card)) {
                    AllZone.getDisplay().showMessage("Cannot target this card (Shroud? Protection? Restrictions?).");
                } else if (choices.contains(card)) {
                    tgt.addTarget(card);
                    done();
                }
            }//selectCard()

            @Override
            public void selectPlayer(Player player) {
                if (alreadyTargeted.contains(player)){
                    return;
                }
                
                if ((tgt.canTgtPlayer() || (tgt.canOnlyTgtOpponent() && player.equals(sa.getActivatingPlayer().getOpponent()))) &&
                        player.canTarget(sa)) {
                    tgt.addTarget(player);
                    done();
                }
            }

            void done() {
                stop();

                select.chooseTargets();
            }
        };

        return target;
    }//input_targetSpecific()


    /**
     * <p>chooseCardFromList.</p>
     *
     * @param choices a {@link forge.CardList} object.
     * @param targeted a boolean.
     * @param mandatory a boolean.
     */
    public void chooseCardFromList(final CardList choices, boolean targeted, final boolean mandatory) {
        // Send in a list of valid cards, and popup a choice box to target
        final Card dummy = new Card();
        dummy.setName("[FINISH TARGETING]");
        final SpellAbility sa = this.ability;
        final String message = this.target.getVTSelection();

        Target tgt = this.getTgt();

        CardList choicesWithDone = choices;
        if (tgt.isMinTargetsChosen(sa.getSourceCard(), sa)) {
            // is there a more elegant way of doing this?
            choicesWithDone.add(dummy);
        }
        Object check = GuiUtils.getChoiceOptional(message, choicesWithDone.toArray());
        if (check != null) {
            Card c = (Card) check;
            if (c.equals(dummy))
                this.setDoneTarget(true);
            else
                tgt.addTarget(c);
        } else
            this.setCancel(true);

        this.chooseTargets();
    }

    /**
     * <p>chooseCardFromStack.</p>
     *
     * @param mandatory a boolean.
     */
    public void chooseCardFromStack(final boolean mandatory) {
        Target tgt = this.target;
        String message = tgt.getVTSelection();
        Target_Selection select = this;

        // Find what's targetable, then allow human to choose
        ArrayList<SpellAbility> choosables = getTargetableOnStack(this.ability, select.getTgt());

        HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();

        for (SpellAbility sa : choosables) {
            map.put(sa.getStackDescription(), sa);
        }

        String[] choices = new String[map.keySet().size()];
        choices = map.keySet().toArray(choices);

        if (choices.length == 0) {
            select.setCancel(true);
        } else {
            String madeChoice = GuiUtils.getChoiceOptional(message, choices);

            if (madeChoice != null) {
                tgt.addTarget(map.get(madeChoice));
            } else
                select.setCancel(true);
        }

        select.chooseTargets();
    }

    // TODO: The following three functions are Utility functions for TargetOnStack, probably should be moved
    // The following should be select.getTargetableOnStack()
    /**
     * <p>getTargetableOnStack.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt a {@link forge.card.spellability.Target} object.
     * @return a {@link java.util.ArrayList} object.
     */
    public static ArrayList<SpellAbility> getTargetableOnStack(SpellAbility sa, Target tgt) {
        ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

        for (int i = 0; i < AllZone.getStack().size(); i++) {
            choosables.add(AllZone.getStack().peekAbility(i));
        }

        for (int i = 0; i < choosables.size(); i++) {
            if (!matchSpellAbility(sa, choosables.get(i), tgt)) {
                choosables.remove(i);
            }
        }
        return choosables;
    }

    /**
     * <p>matchSpellAbility.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param topSA a {@link forge.card.spellability.SpellAbility} object.
     * @param tgt a {@link forge.card.spellability.Target} object.
     * @return a boolean.
     */
    public static boolean matchSpellAbility(SpellAbility sa, SpellAbility topSA, Target tgt) {
        String saType = tgt.getTargetSpellAbilityType();

        if (null == saType) {
            //just take this to mean no restrictions - carry on.
        } else if (topSA.isSpell()) {
            if (!saType.contains("Spell"))
                return false;
        } else if (topSA.isTrigger()) {
            if (!saType.contains("Triggered"))
                return false;
        } else if (topSA.isAbility()) {
            if (!saType.contains("Activated"))
                return false;
        }

        String splitTargetRestrictions = tgt.getSAValidTargeting();
        if (splitTargetRestrictions != null) {
            // TODO: What about spells with SubAbilities with Targets?

            Target matchTgt = topSA.getTarget();

            if (matchTgt == null)
                return false;

            boolean result = false;

            for (Object o : matchTgt.getTargets()) {
                if (matchesValid(o, splitTargetRestrictions.split(","), sa)) {
                    result = true;
                    break;
                }
            }

            if (!result)
                return false;
        }

        if (!matchesValid(topSA, tgt.getValidTgts(), sa)) {
            return false;
        }

        return true;
    }

    /**
     * <p>matchesValid.</p>
     *
     * @param o a {@link java.lang.Object} object.
     * @param valids an array of {@link java.lang.String} objects.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private static boolean matchesValid(Object o, String[] valids, SpellAbility sa) {
        Card srcCard = sa.getSourceCard();
        Player activatingPlayer = sa.getActivatingPlayer();
        if (o instanceof Card) {
            Card c = (Card) o;
            return c.isValidCard(valids, activatingPlayer, srcCard);
        }

        if (o instanceof Player) {
            for (String v : valids) {
                if (v.equalsIgnoreCase("Player"))
                    return true;

                if (v.equalsIgnoreCase("Opponent")) {
                    if (o.equals(activatingPlayer.getOpponent())) {
                        return true;
                    }
                }
                if (v.equalsIgnoreCase("You"))
                    return o.equals(activatingPlayer);
            }
        }

        if (o instanceof SpellAbility) {
            Card c = ((SpellAbility) o).getSourceCard();
            return c.isValidCard(valids, activatingPlayer, srcCard);
        }

        return false;
    }
}
