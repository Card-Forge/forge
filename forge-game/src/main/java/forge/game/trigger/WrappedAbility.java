package forge.game.trigger;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;

// Wrapper ability that checks the requirements again just before
// resolving, for intervening if clauses.
// Yes, it must wrap ALL SpellAbility methods in order to handle
// possible corner cases.
// (The trigger can have a hardcoded OverridingAbility which can make
// use of any of the methods)
public class WrappedAbility extends Ability {

    private final SpellAbility sa;
    private final Trigger regtrig;
    private final Player decider;

    boolean mandatory = false;

    public WrappedAbility(final Trigger regtrig0, final SpellAbility sa0, final Player decider0) {
        super(regtrig0.getHostCard(), ManaCost.ZERO, sa0.getView());
        regtrig = regtrig0;
        sa = sa0;
        decider = decider0;
    }
    
    public SpellAbility getWrappedAbility() { 
        return sa;
    }

    @Override
    public boolean isWrapper() {
        return true;
    }

    public Trigger getTrigger(){
        return regtrig;
    }

    public Player getDecider() {
        return decider;
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
    public void setPaidHash(final HashMap<String, CardCollection> hash) {
        sa.setPaidHash(hash);
    }

    @Override
    public HashMap<String, CardCollection> getPaidHash() {
        return sa.getPaidHash();
    }

    @Override
    public CardCollection getPaidList(final String str) {
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
    public void setTriggeringObjects(final HashMap<String, Object> triggeredObjects) {
        sa.setTriggeringObjects(triggeredObjects);
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
    public List<Object> getTriggerRemembered() {
        return sa.getTriggerRemembered();
    }

    @Override
    public void resetTriggerRemembered() {
        sa.resetTriggerRemembered();
    }

    @Override
    public void setTriggerRemembered(List<Object> list) {
        sa.setTriggerRemembered(list);
    }

    @Override
    public boolean canPlay() {
        return sa.canPlay();
    }

    @Override
    public SpellAbility copy() {
        return sa.copy();
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
    public SpellAbilityRestriction getRestrictions() {
        return sa.getRestrictions();
    }

    @Override
    public Card getHostCard() {
        return sa.getHostCard();
    }

    @Override
    public SpellAbilityView getView() {
        return sa.getView();
    }

    @Override
    public String toUnsuppressedString() {
        return regtrig.toString();
    }

    @Override
    public String getStackDescription() {
        final StringBuilder sb = new StringBuilder(toUnsuppressedString());
        if (this.getTargetRestrictions() != null) {
            sb.append(" (Targeting ");
            for (final GameObject o : this.getTargets().getTargets()) {
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

        sb.append(" [");
        sb.append(regtrig.getImportantStackObjects(this));
        sb.append("]");

        return sb.toString();
    }

    @Override
    public AbilitySub getSubAbility() {
        return sa.getSubAbility();
    }

    @Override
    public TargetRestrictions getTargetRestrictions() {
        return sa.getTargetRestrictions();
    }

    @Override
    public Card getTargetCard() {
        return sa.getTargetCard();
    }

    @Override
    public TargetChoices getTargets() {
        return sa.getTargets();
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
    public boolean isSpell() {
        return sa.isSpell();
    }

    @Override
    public boolean isXCost() {
        return sa.isXCost();
    }

    
    
    @Override
	public String getSvarWithFallback(String name) {
		return sa.getSvarWithFallback(name);
	}

	@Override
	public String getSVar(String name) {
		return sa.getSVar(name);
	}

	@Override
	public Integer getSVarInt(String name) {
		return sa.getSVarInt(name);
	}

	@Override
	public Set<String> getSVars() {
		return sa.getSVars();
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
    public void setHostCard(final Card c) {
        sa.setHostCard(c);
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
    public void setTargetRestrictions(final TargetRestrictions tgt) {
        sa.setTargetRestrictions(tgt);
    }

    @Override
    public void setTargets(TargetChoices targets) {
        sa.setTargets(targets);
    }

    @Override
    public void setTargetCard(final Card card) {
        sa.setTargetCard(card);
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

    @Override
    public boolean usesTargeting() {
        return sa.usesTargeting();
    }

    // //////////////////////////////////////
    // THIS ONE IS ALL THAT MATTERS
    // //////////////////////////////////////
    @Override
    public void resolve() {
        final Game game = sa.getActivatingPlayer().getGame();
        Map<String, String> triggerParams = regtrig.getMapParams();

        if (!(regtrig instanceof TriggerAlways) && !triggerParams.containsKey("NoResolvingCheck")) {
            // Most State triggers don't have "Intervening If"
            if (!regtrig.requirementsCheck(game)) {
                return;
            }
            // Since basic requirements check only cares about whether it's "Activated"
            // Also check on triggered object specific requirements on resolution (e.g. evolve)
            if (!regtrig.meetsRequirementsOnTriggeredObjects(game, regtrig.getRunParams())) {
                return;
            }
        }
        
        if (triggerParams.containsKey("ResolvingCheck")) {
            // rare cases: Hidden Predators (state trigger, but have "Intervening If" to check IsPresent2) etc.
            Map<String, String> recheck = new HashMap<String, String>();
            String key = triggerParams.get("ResolvingCheck");
            String value = regtrig.getMapParams().get(key);
            recheck.put(key, value);
            if (!meetsCommonRequirements(recheck)) {
                return;
            }
        }

        TriggerHandler th = game.getTriggerHandler();

        if (decider != null && !decider.getController().confirmTrigger(sa, regtrig, triggerParams, this.isMandatory())) {
            return;
        }

        // Check timestamps of triggered objects
        final List<Object> original = Lists.newArrayList(sa.getTriggerRemembered());
        for (Object o : original) {
            if (o instanceof Card) {
                Card card = (Card) o;
                Card current = game.getCardState(card);
                if (current.getTimestamp() != card.getTimestamp()) {
                    sa.getTriggerRemembered().remove(o);
                }
            }
        }
        final Map<String, Object> triggerMap = new HashMap<String, Object>(sa.getTriggeringObjects());
        for (Entry<String, Object> ev : triggerMap.entrySet()) {
            if (ev.getValue() instanceof Card) {
                Card card = (Card) ev.getValue();
                Card current = game.getCardState(card);
                if (card.isInPlay() && current.isInPlay() && current.getTimestamp() != card.getTimestamp()) {
                    sa.getTriggeringObjects().remove(ev.getKey());
                }
            }
        }
        // TODO: CardCollection
        
        getActivatingPlayer().getController().playSpellAbilityNoStack(sa, false);

        // Add eventual delayed trigger.
        if (triggerParams.containsKey("DelayedTrigger")) {
            final String sVarName = triggerParams.get("DelayedTrigger");
            final Trigger deltrig = TriggerHandler.parseTrigger(regtrig.getHostCard().getSVar(sVarName),
                    regtrig.getHostCard(), true);
            deltrig.setStoredTriggeredObjects(this.getTriggeringObjects());
            th.registerDelayedTrigger(deltrig);
        }
    }
}
