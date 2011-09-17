package forge.card.trigger;

import forge.Card;
import forge.AllZone;
import forge.AllZoneUtil;
import forge.Player;
import forge.CardList;
import forge.Command;
import forge.CommandArgs;
import forge.GameActionUtil;
import forge.ComputerUtil;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility_Restriction;
import forge.card.spellability.Target;
import forge.gui.input.Input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>TriggerHandler class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class TriggerHandler {

    private ArrayList<String> registeredModes = new ArrayList<String>();
    private ArrayList<Trigger> registeredTriggers = new ArrayList<Trigger>();
    private ArrayList<String> suppressedModes = new ArrayList<String>();

    private ArrayList<Trigger> delayedTriggers = new ArrayList<Trigger>();

    /**
     * <p>suppressMode.</p>
     *
     * @param mode a {@link java.lang.String} object.
     */
    public final void suppressMode(final String mode) {
        suppressedModes.add(mode);
    }

    /**
     * <p>clearSuppression.</p>
     *
     * @param mode a {@link java.lang.String} object.
     */
    public final void clearSuppression(final String mode) {
        suppressedModes.remove(mode);
    }

    /**
     * <p>parseTrigger.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param trigParse a {@link java.lang.String} object.
     * @param host a {@link forge.Card} object.
     * @param intrinsic a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public static Trigger parseTrigger(final String name, final String trigParse,
            final Card host, final boolean intrinsic)
    {
        Trigger ret = TriggerHandler.parseTrigger(trigParse, host, intrinsic);
        ret.setName(name);
        return ret;
    }

    /**
     * <p>parseTrigger.</p>
     *
     * @param trigParse a {@link java.lang.String} object.
     * @param host a {@link forge.Card} object.
     * @param intrinsic a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public static Trigger parseTrigger(final String trigParse, final Card host, final boolean intrinsic)
    {
        HashMap<String, String> mapParams = parseParams(trigParse);
        return parseTrigger(mapParams, host, intrinsic);
    }

    /**
     * <p>parseTrigger.</p>
     *
     * @param mapParams a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     * @param intrinsic a boolean.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public static Trigger parseTrigger(final HashMap<String, String> mapParams,
            final Card host, final boolean intrinsic)
    {
        Trigger ret = null;

        String mode = mapParams.get("Mode");
        if (mode.equals("AbilityCast")) {
            ret = new Trigger_SpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("Always")) {
            ret = new Trigger_Always(mapParams, host, intrinsic);
        } else if (mode.equals("AttackerBlocked")) {
            ret = new Trigger_AttackerBlocked(mapParams, host, intrinsic);
        } else if (mode.equals("AttackersDeclared")) {
            ret = new Trigger_AttackersDeclared(mapParams, host, intrinsic);
        } else if (mode.equals("AttackerUnblocked")) {
            ret = new Trigger_AttackerUnblocked(mapParams, host, intrinsic);
        } else if (mode.equals("Attacks")) {
            ret = new Trigger_Attacks(mapParams, host, intrinsic);
        } else if (mode.equals("BecomesTarget")) {
            ret = new Trigger_BecomesTarget(mapParams, host, intrinsic);
        } else if (mode.equals("Blocks")) {
            ret = new Trigger_Blocks(mapParams, host, intrinsic);
        } else if (mode.equals("Championed")) {
            ret = new Trigger_Championed(mapParams, host, intrinsic);
        } else if (mode.equals("ChangesZone")) {
            ret = new Trigger_ChangesZone(mapParams, host, intrinsic);
        } else if (mode.equals("Clashed")) {
            ret = new Trigger_Clashed(mapParams, host, intrinsic);
        } else if (mode.equals("CounterAdded")) {
            ret = new Trigger_CounterAdded(mapParams, host, intrinsic);
        } else if (mode.equals("Cycled")) {
            ret = new Trigger_Cycled(mapParams, host, intrinsic);
        } else if (mode.equals("DamageDone")) {
            ret = new Trigger_DamageDone(mapParams, host, intrinsic);
        } else if (mode.equals("Discarded")) {
            ret = new Trigger_Discarded(mapParams, host, intrinsic);
        } else if (mode.equals("Drawn")) {
            ret = new Trigger_Drawn(mapParams, host, intrinsic);
        } else if (mode.equals("LandPlayed")) {
            ret = new Trigger_LandPlayed(mapParams, host, intrinsic);
        } else if (mode.equals("LifeGained")) {
            ret = new Trigger_LifeGained(mapParams, host, intrinsic);
        } else if (mode.equals("LifeLost")) {
            ret = new Trigger_LifeLost(mapParams, host, intrinsic);
        } else if (mode.equals("Phase")) {
            ret = new Trigger_Phase(mapParams, host, intrinsic);
        } else if (mode.equals("Sacrificed")) {
            ret = new Trigger_Sacrificed(mapParams, host, intrinsic);
        } else if (mode.equals("Shuffled")) {
            ret = new Trigger_Shuffled(mapParams, host, intrinsic);
        } else if (mode.equals("SpellAbilityCast")) {
            ret = new Trigger_SpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("SpellCast")) {
            ret = new Trigger_SpellAbilityCast(mapParams, host, intrinsic);
        } else if (mode.equals("Taps")) {
            ret = new Trigger_Taps(mapParams, host, intrinsic);
        } else if (mode.equals("TapsForMana")) {
            ret = new Trigger_TapsForMana(mapParams, host, intrinsic);
        } else if (mode.equals("TurnFaceUp")) {
            ret = new Trigger_TurnFaceUp(mapParams, host, intrinsic);
        } else if (mode.equals("Unequip")) {
            ret = new Trigger_Unequip(mapParams, host, intrinsic);
        } else if (mode.equals("Untaps")) {
            ret = new Trigger_Untaps(mapParams, host, intrinsic);
        }

        return ret;
    }

    /**
     * <p>parseParams.</p>
     *
     * @param trigParse a {@link java.lang.String} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static HashMap<String, String> parseParams(final String trigParse) {
        HashMap<String, String> mapParams = new HashMap<String, String>();

        if (trigParse.length() == 0) {
            throw new RuntimeException("TriggerFactory : registerTrigger -- trigParse too short");
        }

        String[] params = trigParse.split("\\|");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }

        for (String param : params) {
            String[] splitParam = param.split("\\$");
            for (int i = 0; i < splitParam.length; i++) {
                splitParam[i] = splitParam[i].trim();
            }

            if (splitParam.length != 2) {
                StringBuilder sb = new StringBuilder();
                sb.append("TriggerFactory Parsing Error in registerTrigger() : Split length of ");
                sb.append(param).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParams.put(splitParam[0], splitParam[1]);
        }

        return mapParams;
    }

    /**
     * <p>registerDelayedTrigger.</p>
     *
     * @param trig a {@link forge.card.trigger.Trigger} object.
     */
    public final void registerDelayedTrigger(final Trigger trig) {
        delayedTriggers.add(trig);

        String mode = trig.getMapParams().get("Mode");
        if (!registeredModes.contains(mode)) {
            registeredModes.add(mode);
        }
    }

    /**
     * <p>registerTrigger.</p>
     *
     * @param trig a {@link forge.card.trigger.Trigger} object.
     */
    public final void registerTrigger(final Trigger trig) {
        registeredTriggers.add(trig);

        String mode = trig.getMapParams().get("Mode");
        if (!registeredModes.contains(mode)) {
            registeredModes.add(mode);
        }
    }

    /**
     * <p>clearRegistered.</p>
     */
    public final void clearRegistered() {
        delayedTriggers.clear();
        registeredTriggers.clear();
        registeredModes.clear();
    }

    /**
     * <p>removeRegisteredTrigger.</p>
     *
     * @param trig a {@link forge.card.trigger.Trigger} object.
     */
    public final void removeRegisteredTrigger(final Trigger trig) {
        for (int i = 0; i < registeredTriggers.size(); i++) {
            if (registeredTriggers.get(i).equals(trig)) {
                registeredTriggers.remove(i);
            }
        }
    }

    /**
     * <p>removeTemporaryTriggers.</p>
     * 
     */
    public final void cleanUpTemporaryTriggers() {
        for (int i = 0; i < registeredTriggers.size(); i++) {
            if (registeredTriggers.get(i).isTemporary()) {
                registeredTriggers.get(i).hostCard.removeTrigger(registeredTriggers.get(i));
                registeredTriggers.remove(i);
                i--;
            }
        }
        for (int i = 0; i < registeredTriggers.size(); i++) {
            registeredTriggers.get(i).setTemporarilySuppressed(false);
        }
    }

    /**
     * <p>Getter for the field <code>registeredTriggers</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Trigger> getRegisteredTriggers() {
        return registeredTriggers;
    }

    /**
     * <p>removeAllFromCard.</p>
     *
     * @param crd a {@link forge.Card} object.
     */
    public final void removeAllFromCard(final Card crd) {
        for (int i = 0; i < registeredTriggers.size(); i++) {
            if (registeredTriggers.get(i).getHostCard().equals(crd)) {
                registeredTriggers.remove(i);
                i--;
            }
        }
    }

    /**
     * <p>runTrigger.</p>
     *
     * @param mode a {@link java.lang.String} object.
     * @param runParams a {@link java.util.Map} object.
     */
    public final void runTrigger(final String mode, final Map<String, Object> runParams) {
        if (suppressedModes.contains(mode) || !registeredModes.contains(mode)) {
            return;
        }

        Player playerAP = AllZone.getPhase().getPlayerTurn();
        
        //This is done to allow the list of triggers to be modified while triggers are running.
        ArrayList<Trigger> registeredTriggersWorkingCopy = new ArrayList<Trigger>(registeredTriggers);
        ArrayList<Trigger> delayedTriggersWorkingCopy = new ArrayList<Trigger>(delayedTriggers);

        //AP
        for (int i = 0; i < registeredTriggersWorkingCopy.size(); i++) {
            if (registeredTriggersWorkingCopy.get(i).getHostCard().getController().equals(playerAP)) {
                runSingleTrigger(registeredTriggersWorkingCopy.get(i), mode, runParams);
            }
        }
        for (int i = 0; i < delayedTriggersWorkingCopy.size(); i++) {
            Trigger deltrig = delayedTriggersWorkingCopy.get(i);
            if (deltrig.getHostCard().getController().equals(playerAP)) {
                if (runSingleTrigger(deltrig, mode, runParams)) {
                    delayedTriggers.remove(deltrig);
                    i--;
                }
            }
        }

        //NAP
        for (int i = 0; i < registeredTriggersWorkingCopy.size(); i++) {
            if (registeredTriggersWorkingCopy.get(i).getHostCard().getController().equals(playerAP.getOpponent())) {
                runSingleTrigger(registeredTriggersWorkingCopy.get(i), mode, runParams);
            }
        }
        for (int i = 0; i < delayedTriggersWorkingCopy.size(); i++) {
            Trigger deltrig = delayedTriggersWorkingCopy.get(i);
            if (deltrig.getHostCard().getController().equals(playerAP.getOpponent())) {
                if (runSingleTrigger(deltrig, mode, runParams)) {
                    delayedTriggers.remove(deltrig);
                    i--;
                }
            }
        }
    }

    //Checks if the conditions are right for a single trigger to go off, and runs it if so.
    //Return true if the trigger went off, false otherwise.
    /**
     * <p>runSingleTrigger.</p>
     *
     * @param regtrig a {@link forge.card.trigger.Trigger} object.
     * @param mode a {@link java.lang.String} object.
     * @param runParams a {@link java.util.HashMap} object.
     * @return a boolean.
     */
    private boolean runSingleTrigger(final Trigger regtrig, final String mode, final Map<String, Object> runParams) {
        if (!regtrig.getMapParams().get("Mode").equals(mode)) {
            return false; //Not the right mode.
        }
        if (!regtrig.zonesCheck()) {
            return false; //Host card isn't where it needs to be.
        }
        if (!regtrig.phasesCheck()) {
            return false; //It's not the right phase to go off.
        }
        if (!regtrig.requirementsCheck()) {
            return false; //Conditions aren't right.
        }
        if (regtrig.getHostCard().isFaceDown() && regtrig.getIsIntrinsic()) {
            return false; //Morphed cards only have pumped triggers go off.
        }
        if (regtrig instanceof Trigger_Always) {
            if (AllZone.getStack().hasStateTrigger(regtrig.ID)) {
                return false; //State triggers that are already on the stack don't trigger again.
            }
        }
        if (!regtrig.performTest(runParams)) {
                return false; //Test failed.
        }
        if (regtrig.isSuppressed()) {
            return false; //Trigger removed by effect
    }

        //Torpor Orb check
        CardList torporOrbs = AllZoneUtil.getCardsInPlay("Torpor Orb");

        if(torporOrbs.size() != 0)
        {
            if(regtrig.getMapParams().containsKey("Destination"))
            {
                if ((regtrig.getMapParams().get("Destination").equals("Battlefield") || regtrig.getMapParams().get("Destination").equals("Any")) && mode.equals("ChangesZone") && ((regtrig.getMapParams().get("ValidCard").contains("Creature")) || (regtrig.getMapParams().get("ValidCard").contains("Self") && regtrig.getHostCard().isCreature() )))
                {
                    return false;
                }
            }
            else
            {
                if (mode.equals("ChangesZone") && ((regtrig.getMapParams().get("ValidCard").contains("Creature")) || (regtrig.getMapParams().get("ValidCard").contains("Self") && regtrig.getHostCard().isCreature() )))
                {
                    return false;
                }
            }
            
        }
        
        HashMap<String, String> trigParams = regtrig.getMapParams();
        final Player[] decider = new Player[1];

        // Any trigger should cause the phase not to skip
        AllZone.getPhase().setSkipPhase(false);

        regtrig.setRunParams(runParams);

        //All tests passed, execute ability.
        if (regtrig instanceof Trigger_TapsForMana) {
            Ability_Mana abMana = (Ability_Mana) runParams.get("Ability_Mana");
            if (null != abMana) {
                abMana.setUndoable(false);
            }
        }

        AbilityFactory abilityFactory = new AbilityFactory();

        final SpellAbility[] sa = new SpellAbility[1];
        Card host = AllZoneUtil.getCardState(regtrig.getHostCard());

        if (host == null) {
            host = regtrig.getHostCard();
        }
        // This will fix the Oblivion Ring issue, but is this the right fix?
        for (Object o : regtrig.getHostCard().getRemembered()) {
            if (!host.getRemembered().contains(o)) {
                host.addRemembered(o);
            }
        }

        sa[0] = regtrig.getOverridingAbility();
        if (sa[0] == null) {
            if (!trigParams.containsKey("Execute")) {
                sa[0] = new Ability(regtrig.getHostCard(), "0") {
                    @Override
                    public void resolve() {
                    }
                };
            } else {
                sa[0] = abilityFactory.getAbility(host.getSVar(trigParams.get("Execute")), host);
            }
        }
        sa[0].setTrigger(true);
        sa[0].setSourceTrigger(regtrig.ID);
        regtrig.setTriggeringObjects(sa[0]);
        if (regtrig.getStoredTriggeredObjects() != null) {
            sa[0].setAllTriggeringObjects(regtrig.getStoredTriggeredObjects());
        }

        sa[0].setActivatingPlayer(host.getController());
        sa[0].setStackDescription(sa[0].toString());
        boolean mand = false;
        if (trigParams.containsKey("OptionalDecider")) {
        	sa[0].setOptionalTrigger(true);
            mand = false;
            decider[0] = AbilityFactory.getDefinedPlayers(host, trigParams.get("OptionalDecider"), sa[0]).get(0);
        } else {
            mand = true;

            SpellAbility ability = sa[0];
            while (ability != null) {
                Target tgt = ability.getTarget();

                if (tgt != null) {
                    tgt.setMandatory(true);
                }
                ability = ability.getSubAbility();
            }
        }
        final boolean isMandatory = mand;

        //Wrapper ability that checks the requirements again just before resolving, for intervening if clauses.
        //Yes, it must wrap ALL SpellAbility methods in order to handle possible corner cases.
        //(The trigger can have a hardcoded OverridingAbility which can make use of any of the methods)
        final Ability wrapperAbility = new Ability(regtrig.getHostCard(), "0") {

            @Override
            public boolean isWrapper() {
                return true;
            }

            @Override
            public void setPaidHash(HashMap<String, CardList> hash) {
                sa[0].setPaidHash(hash);
            }

            @Override
            public HashMap<String, CardList> getPaidHash() {
                return sa[0].getPaidHash();
            }

            @Override
            public void setPaidList(CardList list, String str) {
                sa[0].setPaidList(list, str);
            }

            @Override
            public CardList getPaidList(String str) {
                return sa[0].getPaidList(str);
            }

            @Override
            public void addCostToHashList(Card c, String str) {
                sa[0].addCostToHashList(c, str);
            }

            @Override
            public void resetPaidHash() {
                sa[0].resetPaidHash();
            }

            @Override
            public HashMap<String, Object> getTriggeringObjects() {
                return sa[0].getTriggeringObjects();
            }

            @Override
            public void setAllTriggeringObjects(HashMap<String, Object> triggeredObjects) {
                sa[0].setAllTriggeringObjects(triggeredObjects);
            }

            @Override
            public void setTriggeringObject(String type, Object o) {
                sa[0].setTriggeringObject(type, o);
            }

            @Override
            public Object getTriggeringObject(String type) {
                return sa[0].getTriggeringObject(type);
            }

            @Override
            public boolean hasTriggeringObject(String type) {
                return sa[0].hasTriggeringObject(type);
            }

            @Override
            public void resetTriggeringObjects() {
                sa[0].resetTriggeringObjects();
            }

            @Override
            public boolean canPlay() {
                return sa[0].canPlay();
            }

            @Override
            public boolean canPlayAI() {
                return sa[0].canPlayAI();
            }

            @Override
            public void chooseTargetAI() {
                sa[0].chooseTargetAI();
            }

            @Override
            public SpellAbility copy() {
                return sa[0].copy();
            }

            @Override
            public boolean doTrigger(boolean mandatory) {
                return sa[0].doTrigger(mandatory);
            }

            @Override
            public AbilityFactory getAbilityFactory() {
                return sa[0].getAbilityFactory();
            }

            @Override
            public Player getActivatingPlayer() {
                return sa[0].getActivatingPlayer();
            }

            @Override
            public Input getAfterPayMana() {
                return sa[0].getAfterPayMana();
            }

            @Override
            public Input getAfterResolve() {
                return sa[0].getAfterResolve();
            }

            @Override
            public Input getBeforePayMana() {
                return sa[0].getBeforePayMana();
            }

            @Override
            public Command getBeforePayManaAI() {
                return sa[0].getBeforePayManaAI();
            }

            @Override
            public Command getCancelCommand() {
                return sa[0].getCancelCommand();
            }

            @Override
            public CommandArgs getChooseTargetAI() {
                return sa[0].getChooseTargetAI();
            }

            @Override
            public String getDescription() {
                return sa[0].getDescription();
            }

            @Override
            public String getMultiKickerManaCost() {
                return sa[0].getMultiKickerManaCost();
            }

            @Override
            public String getReplicateManaCost() {
                return sa[0].getReplicateManaCost();
            }

            @Override
            public SpellAbility_Restriction getRestrictions() {
                return sa[0].getRestrictions();
            }

            @Override
            public Card getSourceCard() {
                return sa[0].getSourceCard();
            }

            @Override
            public String getStackDescription() {
                StringBuilder sb = new StringBuilder(regtrig.toString());
                if (getTarget() != null) {
                    sb.append(" (Targeting ");
                    for (Object o : getTarget().getTargets()) {
                        sb.append(o.toString());
                        sb.append(", ");
                    }
                    if (sb.toString().endsWith(", ")) {
                        sb.setLength(sb.length() - 2);
                    } else {
                        sb.append("ERROR");
                    }
                    sb.append(")");
                }

                return sb.toString();
            }

            @Override
            public Ability_Sub getSubAbility() {
                return sa[0].getSubAbility();
            }

            @Override
            public Target getTarget() {
                return sa[0].getTarget();
            }

            @Override
            public Card getTargetCard() {
                return sa[0].getTargetCard();
            }

            @Override
            public CardList getTargetList() {
                return sa[0].getTargetList();
            }

            @Override
            public Player getTargetPlayer() {
                return sa[0].getTargetPlayer();
            }

            @Override
            public String getXManaCost() {
                return sa[0].getXManaCost();
            }

            @Override
            public boolean isAbility() {
                return sa[0].isAbility();
            }

            @Override
            public boolean isBuyBackAbility() {
                return sa[0].isBuyBackAbility();
            }

            @Override
            public boolean isCycling() {
                return sa[0].isCycling();
            }

            @Override
            public boolean isExtrinsic() {
                return sa[0].isExtrinsic();
            }

            @Override
            public boolean isFlashBackAbility() {
                return sa[0].isFlashBackAbility();
            }

            @Override
            public boolean isIntrinsic() {
                return sa[0].isIntrinsic();
            }

            @Override
            public boolean isKickerAbility() {
                return sa[0].isKickerAbility();
            }

            @Override
            public boolean isKothThirdAbility() {
                return sa[0].isKothThirdAbility();
            }

            @Override
            public boolean isMultiKicker() {
                return sa[0].isMultiKicker();
            }

            @Override
            public boolean isReplicate() {
                return sa[0].isReplicate();
            }

            @Override
            public boolean isSpell() {
                return sa[0].isSpell();
            }

            @Override
            public boolean isTapAbility() {
                return sa[0].isTapAbility();
            }

            @Override
            public boolean isUntapAbility() {
                return sa[0].isUntapAbility();
            }

            @Override
            public boolean isXCost() {
                return sa[0].isXCost();
            }

            @Override
            public void resetOnceResolved() {
                // Fixing an issue with Targeting + Paying Mana
                //sa[0].resetOnceResolved();
            }

            @Override
            public void setAbilityFactory(AbilityFactory af) {
                sa[0].setAbilityFactory(af);
            }

            @Override
            public void setActivatingPlayer(Player player) {
                sa[0].setActivatingPlayer(player);
            }

            @Override
            public void setAdditionalManaCost(String cost) {
                sa[0].setAdditionalManaCost(cost);
            }

            @Override
            public void setAfterPayMana(Input in) {
                sa[0].setAfterPayMana(in);
            }

            @Override
            public void setAfterResolve(Input in) {
                sa[0].setAfterResolve(in);
            }

            @Override
            public void setBeforePayMana(Input in) {
                sa[0].setBeforePayMana(in);
            }

            @Override
            public void setBeforePayManaAI(Command c) {
                sa[0].setBeforePayManaAI(c);
            }

            @Override
            public void setCancelCommand(Command cancelCommand) {
                sa[0].setCancelCommand(cancelCommand);
            }

            @Override
            public void setChooseTargetAI(CommandArgs c) {
                sa[0].setChooseTargetAI(c);
            }

            @Override
            public void setDescription(String s) {
                sa[0].setDescription(s);
            }

            @Override
            public void setFlashBackAbility(boolean flashBackAbility) {
                sa[0].setFlashBackAbility(flashBackAbility);
            }

            @Override
            public void setIsBuyBackAbility(boolean b) {
                sa[0].setIsBuyBackAbility(b);
            }

            @Override
            public void setIsCycling(boolean b) {
                sa[0].setIsCycling(b);
            }

            @Override
            public void setIsMultiKicker(boolean b) {
                sa[0].setIsMultiKicker(b);
            }

            @Override
            public void setIsReplicate(boolean b) {
                sa[0].setIsReplicate(b);
            }

            @Override
            public void setIsXCost(boolean b) {
                sa[0].setIsXCost(b);
            }

            @Override
            public void setKickerAbility(boolean kab) {
                sa[0].setKickerAbility(kab);
            }

            @Override
            public void setKothThirdAbility(boolean kothThirdAbility) {
                sa[0].setKothThirdAbility(kothThirdAbility);
            }

            @Override
            public void setManaCost(String cost) {
                sa[0].setManaCost(cost);
            }

            @Override
            public void setMultiKickerManaCost(String cost) {
                sa[0].setMultiKickerManaCost(cost);
            }

            @Override
            public void setReplicateManaCost(String cost) {
                sa[0].setReplicateManaCost(cost);
            }

            @Override
            public void setPayCosts(Cost abCost) {
                sa[0].setPayCosts(abCost);
            }

            @Override
            public void setRestrictions(SpellAbility_Restriction restrict) {
                sa[0].setRestrictions(restrict);
            }

            @Override
            public void setSourceCard(Card c) {
                sa[0].setSourceCard(c);
            }

            @Override
            public void setStackDescription(String s) {
                sa[0].setStackDescription(s);
            }

            @Override
            public void setSubAbility(Ability_Sub subAbility) {
                sa[0].setSubAbility(subAbility);
            }

            @Override
            public void setTarget(Target tgt) {
                sa[0].setTarget(tgt);
            }

            @Override
            public void setTargetCard(Card card) {
                sa[0].setTargetCard(card);
            }

            @Override
            public void setTargetList(CardList list) {
                sa[0].setTargetList(list);
            }

            @Override
            public void setTargetPlayer(Player p) {
                sa[0].setTargetPlayer(p);
            }

            @Override
            public void setType(String s) {
                sa[0].setType(s);
            }

            @Override
            public void setXManaCost(String cost) {
                sa[0].setXManaCost(cost);
            }

            @Override
            public boolean wasCancelled() {
                return sa[0].wasCancelled();
            }

            @Override
            public void setSourceTrigger(int ID) {
                sa[0].setSourceTrigger(ID);
            }

            @Override
            public int getSourceTrigger() {
                return sa[0].getSourceTrigger();
            }
            
            @Override
            public void setOptionalTrigger(boolean b) {
            	sa[0].setOptionalTrigger(b);
            }
            
            @Override
            public boolean isOptionalTrigger() {
            	return sa[0].isOptionalTrigger();
            }

            ////////////////////////////////////////
            //THIS ONE IS ALL THAT MATTERS
            ////////////////////////////////////////
            @Override
            public void resolve() {
                if (!(regtrig instanceof Trigger_Always)) //State triggers don't do the whole "Intervening If" thing.
                {
                    if (!regtrig.requirementsCheck()) {
                        return;
                    }
                }

                if (decider[0] != null) {
                    if (decider[0].isHuman()) {
                        if(triggersAlwaysAccept.contains(getSourceTrigger()))
                        {
                            //No need to do anything.
                        }
                        else if(triggersAlwaysDecline.contains(getSourceTrigger()))
                        {
                            return;
                        }
                        else
                        {
                            StringBuilder buildQuestion = new StringBuilder("Use triggered ability of ");
                            buildQuestion.append(regtrig.getHostCard().getName()).append("(").append(regtrig.getHostCard().getUniqueNumber()).append(")?");
                            buildQuestion.append("\r\n(");
                            buildQuestion.append(regtrig.getMapParams().get("TriggerDescription").replace("CARDNAME", regtrig.getHostCard().getName()));
                            buildQuestion.append(")");
                            if (!GameActionUtil.showYesNoDialog(regtrig.getHostCard(), buildQuestion.toString())) {
                                return;
                            }
                        }
                    } else {
                        // This isn't quite right, but better than canPlayAI
                        if (!sa[0].doTrigger(isMandatory)) {
                            return;
                        }
                    }
                }

                if (sa[0].getSourceCard().getController().isHuman()) {
                    //Card src = (Card)(sa[0].getSourceCard().getTriggeringObject("Card"));
                    //System.out.println("Trigger resolving for "+mode+".  Card = "+src);
                    AllZone.getGameAction().playSpellAbility_NoStack(sa[0], true);
                } else {
                    // commented out because i don't think this should be called again here
                    //sa[0].doTrigger(isMandatory);
                    ComputerUtil.playNoStack(sa[0]);
                }

                //Add eventual delayed trigger.
                if (regtrig.getMapParams().containsKey("DelayedTrigger")) {
                    String sVarName = regtrig.getMapParams().get("DelayedTrigger");
                    Trigger deltrig = parseTrigger(regtrig.getHostCard().getSVar(sVarName), regtrig.getHostCard(), true);
                    deltrig.setStoredTriggeredObjects(this.getTriggeringObjects());
                    registerDelayedTrigger(deltrig);
                }
            }
        };
        wrapperAbility.setTrigger(true);
        wrapperAbility.setMandatory(isMandatory);
        wrapperAbility.setDescription(wrapperAbility.getStackDescription());
        /*
           if(host.getController().isHuman())
           {
               AllZone.getGameAction().playSpellAbility(wrapperAbility);
           }
           else
           {
               wrapperAbility.doTrigger(isMandatory);
               ComputerUtil.playStack(wrapperAbility);
           }
            */

        //Card src = (Card)(sa[0].getSourceCard().getTriggeringObject("Card"));
        //System.out.println("Trigger going on stack for "+mode+".  Card = "+src);

        if (regtrig.getMapParams().containsKey("Static")) {
            if (regtrig.getMapParams().get("Static").equals("True")) {
                AllZone.getGameAction().playSpellAbility_NoStack(wrapperAbility, false);
            }
            else {
                AllZone.getStack().addSimultaneousStackEntry(wrapperAbility);
            }
        }
        else {
            AllZone.getStack().addSimultaneousStackEntry(wrapperAbility);
        }
        return true;
    }
    
    private final ArrayList<Integer> triggersAlwaysAccept = new ArrayList<Integer>();
    private final ArrayList<Integer> triggersAlwaysDecline = new ArrayList<Integer>();
    
    public void setAlwaysAcceptTrigger(final int trigID) {
        if(triggersAlwaysDecline.contains(trigID))
        {
            triggersAlwaysDecline.remove((Object)trigID);
        }
        
        if(!triggersAlwaysAccept.contains(trigID))
        {
            triggersAlwaysAccept.add(trigID);
        }
    }
    
    public void setAlwaysDeclineTrigger(final int trigID) {
        if(triggersAlwaysAccept.contains(trigID))
        {
            triggersAlwaysAccept.remove((Object)trigID);
        }
        
        if(!triggersAlwaysDecline.contains(trigID))
        {
            triggersAlwaysDecline.add(trigID);
        }
    }
    
    public void setAlwaysAskTrigger(final int trigID) {
    	triggersAlwaysAccept.remove((Object)trigID);
    	triggersAlwaysDecline.remove((Object)trigID);
    }
    
    public boolean isAlwaysAccepted(final int trigID) {
        return triggersAlwaysAccept.contains(trigID);
    }
    
    public boolean isAlwaysDeclined(final int trigID) {
        return triggersAlwaysDecline.contains(trigID);
    }
    
    public void clearTriggerSettings() {
        triggersAlwaysAccept.clear();
        triggersAlwaysDecline.clear();
    }
}
