package forge.card.trigger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.card.ability.ApiType;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.ISpellAbility;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRestriction;
import forge.card.spellability.Target;
import forge.card.spellability.TargetChoices;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.gui.GuiDialog;

// Wrapper ability that checks the requirements again just before
// resolving, for intervening if clauses.
// Yes, it must wrap ALL SpellAbility methods in order to handle
// possible corner cases.
// (The trigger can have a hardcoded OverridingAbility which can make
// use of any of the methods)
public class WrappedAbility extends Ability implements ISpellAbility {

    private final SpellAbility sa;
    private final Trigger regtrig;
    private final Player decider;

    boolean mandatory = false;

    public WrappedAbility(final Trigger regTrig, final SpellAbility sa0, final Player decider0) {
        super(regTrig.getHostCard(), ManaCost.ZERO);
        regtrig = regTrig;
        sa = sa0;
        decider = decider0;
    }


    @Override
    public boolean isWrapper() {
        return true;
    }


    public final void setMandatory(final boolean mand) {
        this.mandatory = mand;
    }

    /**
     * @return the mandatory
     */
    @Override
    public boolean isMandatory() {
        return mandatory;
    }

    @Override
    public String getParam(String key) { return sa.getParam(key); }

    @Override
    public boolean hasParam(String key) { return sa.hasParam(key); }

    @Override
    public ApiType getApi() {
        return sa.getApi();
    }

    @Override
    public void setPaidHash(final HashMap<String, List<Card>> hash) {
        sa.setPaidHash(hash);
    }

    @Override
    public HashMap<String, List<Card>> getPaidHash() {
        return sa.getPaidHash();
    }

    @Override
    public List<Card> getPaidList(final String str) {
        return sa.getPaidList(str);
    }

    @Override
    public void addCostToHashList(final Card c, final String str) {
        sa.addCostToHashList(c, str);
    }

    @Override
    public void resetPaidHash() {
        sa.resetPaidHash();
    }

    @Override
    public HashMap<String, Object> getTriggeringObjects() {
        return sa.getTriggeringObjects();
    }

    @Override
    public void setAllTriggeringObjects(final HashMap<String, Object> triggeredObjects) {
        sa.setAllTriggeringObjects(triggeredObjects);
    }

    @Override
    public void setTriggeringObject(final String type, final Object o) {
        sa.setTriggeringObject(type, o);
    }

    @Override
    public Object getTriggeringObject(final String type) {
        return sa.getTriggeringObject(type);
    }

    @Override
    public boolean hasTriggeringObject(final String type) {
        return sa.hasTriggeringObject(type);
    }

    @Override
    public void resetTriggeringObjects() {
        sa.resetTriggeringObjects();
    }

    @Override
    public boolean canPlay() {
        return sa.canPlay();
    }

    @Override
    public boolean canPlayAI() {
        return sa.canPlayAI();
    }

    @Override
    public SpellAbility copy() {
        return sa.copy();
    }

    @Override
    public boolean doTrigger(final boolean mandatory, Player ai) {
        return sa.doTrigger(mandatory, ai);
    }

    @Override
    public Player getActivatingPlayer() {
        return sa.getActivatingPlayer();
    }

    @Override
    public String getDescription() {
        return sa.getDescription();
    }

    @Override
    public ManaCost getMultiKickerManaCost() {
        return sa.getMultiKickerManaCost();
    }

    @Override
    public ManaCost getReplicateManaCost() {
        return sa.getReplicateManaCost();
    }

    @Override
    public SpellAbilityRestriction getRestrictions() {
        return sa.getRestrictions();
    }

    @Override
    public Card getSourceCard() {
        return sa.getSourceCard();
    }

    @Override
    public String getStackDescription() {
        final StringBuilder sb = new StringBuilder(regtrig.toString());
        if (this.getTarget() != null) {
            sb.append(" (Targeting ");
            for (final Object o : this.getTarget().getTargets()) {
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
    public AbilitySub getSubAbility() {
        return sa.getSubAbility();
    }

    @Override
    public Target getTarget() {
        return sa.getTarget();
    }

    @Override
    public Card getTargetCard() {
        return sa.getTargetCard();
    }

    @Override
    public Player getTargetPlayer() {
        return sa.getTargetPlayer();
    }

    @Override
    public boolean isAbility() {
        return sa.isAbility();
    }

    @Override
    public boolean isBuyBackAbility() {
        return sa.isBuyBackAbility();
    }

    @Override
    public boolean isCycling() {
        return sa.isCycling();
    }

    @Override
    public boolean isFlashBackAbility() {
        return sa.isFlashBackAbility();
    }

    @Override
    public boolean isMultiKicker() {
        return sa.isMultiKicker();
    }

    @Override
    public boolean isReplicate() {
        return sa.isReplicate();
    }

    @Override
    public boolean isSpell() {
        return sa.isSpell();
    }

    @Override
    public boolean isXCost() {
        return sa.isXCost();
    }

    @Override
    public void resetOnceResolved() {
        // Fixing an issue with Targeting + Paying Mana
        // sa.resetOnceResolved();
    }

    @Override
    public void setActivatingPlayer(final Player player) {
        sa.setActivatingPlayer(player);
    }

    @Override
    public void setDescription(final String s) {
        sa.setDescription(s);
    }

    @Override
    public void setFlashBackAbility(final boolean flashBackAbility) {
        sa.setFlashBackAbility(flashBackAbility);
    }

    @Override
    public void setMultiKickerManaCost(final ManaCost cost) {
        sa.setMultiKickerManaCost(cost);
    }

    @Override
    public void setPayCosts(final Cost abCost) {
        sa.setPayCosts(abCost);
    }

    @Override
    public void setRestrictions(final SpellAbilityRestriction restrict) {
        sa.setRestrictions(restrict);
    }

    @Override
    public void setSourceCard(final Card c) {
        sa.setSourceCard(c);
    }

    @Override
    public void setStackDescription(final String s) {
        sa.setStackDescription(s);
    }

    @Override
    public void setSubAbility(final AbilitySub subAbility) {
        sa.setSubAbility(subAbility);
    }

    @Override
    public void setTarget(final Target tgt) {
        sa.setTarget(tgt);
    }

    @Override
    public void setTargetCard(final Card card) {
        sa.setTargetCard(card);
    }

    @Override
    public void setType(final String s) {
        sa.setType(s);
    }

    @Override
    public void setSourceTrigger(final int id) {
        sa.setSourceTrigger(id);
    }

    @Override
    public int getSourceTrigger() {
        return sa.getSourceTrigger();
    }

    @Override
    public void setOptionalTrigger(final boolean b) {
        sa.setOptionalTrigger(b);
    }

    @Override
    public boolean isOptionalTrigger() {
        return sa.isOptionalTrigger();
    }

    // //////////////////////////////////////
    // THIS ONE IS ALL THAT MATTERS
    // //////////////////////////////////////
    @Override
    public void resolve() {
        final Game game = sa.getActivatingPlayer().getGame();

        if (!(regtrig instanceof TriggerAlways)) {
            // State triggers don't have "Intervening If"
            if (!regtrig.requirementsCheck(game)) {
                return;
            }
        }

        TriggerHandler th = game.getTriggerHandler();
        Map<String, String> triggerParams = regtrig.getMapParams();

        if (decider != null && !confirmTrigger(decider, triggerParams)) 
            return;

        if (getActivatingPlayer().isHuman()) {
            HumanPlay.playSpellAbilityNoStack(getActivatingPlayer(), sa, true);
        } else {
            // commented out because i don't think this should be called
            // again here
            // sa.doTrigger(isMandatory);
            ComputerUtil.playNoStack(getActivatingPlayer(), sa, game);
        }

        // Add eventual delayed trigger.
        if (triggerParams.containsKey("DelayedTrigger")) {
            final String sVarName = triggerParams.get("DelayedTrigger");
            final Trigger deltrig = TriggerHandler.parseTrigger(regtrig.getHostCard().getSVar(sVarName),
                    regtrig.getHostCard(), true);
            deltrig.setStoredTriggeredObjects(this.getTriggeringObjects());
            th.registerDelayedTrigger(deltrig);
        }
    }
    
    private boolean confirmTrigger(Player decider, Map<String, String> triggerParams) {
        if (decider.isHuman()) {
            if(decider.getController().shouldAlwaysAcceptTrigger(regtrig.getId()))
                return true;
            else if(decider.getController().shouldAlwaysDeclineTrigger(regtrig.getId()))
                return false;
            
            String triggerDesc = triggerParams.get("TriggerDescription").replace("CARDNAME", regtrig.getHostCard().getName());
            final StringBuilder buildQuestion = new StringBuilder("Use triggered ability of ");
            buildQuestion.append(regtrig.getHostCard().toString()).append("?");
            buildQuestion.append("\r\n(").append(triggerDesc).append(")\r\n");
            if (sa.getTriggeringObjects().containsKey("Attacker")) {
                buildQuestion.append("[Attacker: " + sa.getTriggeringObjects().get("Attacker") + "]");
            }
            if (!GuiDialog.confirm(regtrig.getHostCard(), buildQuestion.toString())) {
                return false;
            }
        } else {
            if (triggerParams.containsKey("DelayedTrigger")) {
                //TODO: The only card with an optional delayed trigger is Shirei, Shizo's Caretaker, 
                //      needs to be expanded when a more difficult cards comes up
            } else {
                // Store/replace target choices more properly to get this SA cleared.
                Target tgt = sa.getTarget();
                TargetChoices tc = null;
                boolean storeChoices = tgt != null && tgt.getTargetChoices() != null;

                if (storeChoices) {
                    tc = tgt.getTargetChoices();
                    tgt.resetTargets();
                }
                // There is no way this doTrigger here will have the same target as stored above
                // So it's possible it's making a different decision here than will actually happen
                if (!sa.doTrigger(this.isMandatory(), decider)) {
                    return false;
                }
                if (storeChoices) {
                    tgt.resetTargets();
                    tgt.setTargetChoices(tc);
                }
            }
        }
        return true;
    }

}
