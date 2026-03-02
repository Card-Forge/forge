package forge.game.trigger;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;

import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageMap;
import forge.game.card.CardState;
import forge.game.card.CardZoneTable;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.AlternativeCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityRestriction;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;

// Wrapper ability that checks the requirements again just before
// resolving, for intervening if clauses.
// Yes, it must wrap ALL SpellAbility methods in order to handle
// possible corner cases.
// (The trigger can have a hardcoded OverridingAbility which can make
// use of any of the methods)
public class WrappedAbility extends Ability {

    private final SpellAbility sa;
    private Player decider;

    public WrappedAbility(final Trigger regtrig0, final SpellAbility sa0, final Player decider0) {
        super(sa0.getHostCard(), ManaCost.ZERO);
        setTrigger(regtrig0);
        sa = sa0;
        sa.setTrigger(regtrig0);
        decider = decider0;
    }

    public SpellAbility getWrappedAbility() {
        return sa;
    }

    @Override
    public boolean isWrapper() {
        return true;
    }

    public Player getDecider() {
        return decider;
    }

    @Override
    public String getParam(String key) { return sa.getParam(key); }

    @Override
    public boolean hasParam(String key) { return sa.hasParam(key); }

    @Override
    public String getParamOrDefault(String key, String defaultValue) { return sa.getParamOrDefault(key, defaultValue); }

    @Override
    public ApiType getApi() {
        return sa.getApi();
    }

    @Override
    public void setPaidHash(final TreeBasedTable<String, Boolean, CardCollection> hash) {
        sa.setPaidHash(hash);
    }

    @Override
    public TreeBasedTable<String, Boolean, CardCollection> getPaidHash() {
        return sa.getPaidHash();
    }

    @Override
    public CardCollection getPaidList(final String str, boolean intrinsic) {
        return sa.getPaidList(str, intrinsic);
    }

    @Override
    public void addCostToHashList(final Card c, final String str, final boolean intrinsic) {
        sa.addCostToHashList(c, str, intrinsic);
    }

    @Override
    public void resetPaidHash() {
        sa.resetPaidHash();
    }

    @Override
    public Map<AbilityKey, Object> getTriggeringObjects() {
        return sa.getTriggeringObjects();
    }

    @Override
    public void setTriggeringObjects(final Map<AbilityKey, Object> triggeredObjects) {
        sa.setTriggeringObjects(triggeredObjects);
    }

    @Override
    public void setTriggeringObject(final AbilityKey type, final Object o) {
        sa.setTriggeringObject(type, o);
    }

    @Override
    public Object getTriggeringObject(final AbilityKey type) {
        return sa.getTriggeringObject(type);
    }

    @Override
    public boolean hasTriggeringObject(final AbilityKey type) {
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
    public SpellAbility copy() {
        return sa.copy();
    }

    @Override
    public SpellAbilityRestriction getRestrictions() {
        return sa.getRestrictions();
    }

    @Override
    public SpellAbility getSATargetingCard() {
        return sa.getSATargetingCard();
    }

    // key for autoyield - if there is a trigger use its description as the wrapper now has triggering information in its description
    @Override
    public String yieldKey() {
        if (getTrigger() != null) {
            if (getHostCard() != null) {
                return getHostCard().toString() + ": " + getTrigger().toString();
            } else {
                return getTrigger().toString();
            }
        } else {
            return super.yieldKey();
        }
    }

    // include triggering information so that different effects look different
    // this information is in the stack description so just use that
    // a real solution would include only the triggering information that actually is used, but that's a major change
    @Override
    public String toUnsuppressedString() {
        String desc = this.getStackDescription(false); /* use augmented stack description as string for wrapped things */
        String card = getHostCard().toString();
        if (!desc.contains(card) && desc.contains(" this ")) { /* a hack for Evolve and similar that don't have CARDNAME */
                return card + ": " + desc;
        } else return desc;
    }

    @Override
    public String getStackDescription() {
        return getStackDescription(true);
    }

    public String getStackDescription(boolean withTargets) {
        final Trigger regtrig = getTrigger();
        if (regtrig == null) return "";
        final StringBuilder sb =
                new StringBuilder(regtrig.replaceAbilityText(regtrig.toString(true), this, true));

        // prevent text growing too long when SA target other in a chain and also potential StackOverflow
        if (withTargets) {
            List<TargetChoices> allTargets = sa.getAllTargetChoices();
            if (!allTargets.isEmpty() && !ApiType.Charm.equals(sa.getApi())) {
                sb.append(" (Targeting: ");
                sb.append(allTargets);
                sb.append(")");
            }
        }

        String important = regtrig.getImportantStackObjects(this);
        if (!important.isEmpty()) {
            sb.append(" [");
            sb.append(important);
            sb.append("]");
        }

        return sb.toString();
    }

    @Override
    public void setStackDescription(final String s) {
        sa.setStackDescription(s);
    }

    @Override
    public TargetRestrictions getTargetRestrictions() {
        return sa.getTargetRestrictions();
    }
    @Override
    public void setTargetRestrictions(final TargetRestrictions tgt) {
        sa.setTargetRestrictions(tgt);
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
    public void setTargets(TargetChoices targets) {
        sa.setTargets(targets);
    }

    @Override
    public boolean isAbility() {
        return sa.isAbility();
    }

    @Override
    public boolean isBuyback() {
        return sa.isBuyback();
    }

    @Override
    public boolean isCycling() {
        return sa.isCycling();
    }

    @Override
    public boolean isChapter() {
        return sa.isChapter();
    }

    @Override
    public Integer getChapter() {
        return sa.getChapter();
    }

    @Override
    public boolean isFlashback() {
        return sa.isFlashback();
    }

    @Override
    public boolean isSpell() {
        return sa.isSpell();
    }

    @Override
    public boolean hasSVar(String name) {
        return sa.hasSVar(name);
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
    public void setSVar(final String name, final String value) {
        sa.setSVar(name, value);
    }

    @Override
    public Map<String, String> getSVars() {
        return sa.getSVars();
    }

    @Override
    public void resetOnceResolved() {
        // Fixing an issue with Targeting + Paying Mana
        // sa.resetOnceResolved();
    }

    @Override
    public Player getActivatingPlayer() {
        return sa.getActivatingPlayer();
    }
    @Override
    public void setActivatingPlayer(final Player player) {
        sa.setActivatingPlayer(player);
    }

    @Override
    public String getDescription() {
        return sa.getDescription();
    }
    @Override
    public void setDescription(final String s) {
        sa.setDescription(s);
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
    public AbilitySub getSubAbility() {
        return sa.getSubAbility();
    }
    @Override
    public void setSubAbility(final AbilitySub subAbility) {
        sa.setSubAbility(subAbility);
    }

    @Override
    public void setTargetCard(final Card card) {
        sa.setTargetCard(card);
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

    @Override
    public boolean hasAdditionalAbility(String ability) {
        return sa.hasAdditionalAbility(ability);
    }

    @Override
    public SpellAbility getAdditionalAbility(String ability) {
        return sa.getAdditionalAbility(ability);
    }

    public Map<String, List<AbilitySub>> getAdditionalAbilityLists() {
        return sa.getAdditionalAbilityLists();
    }
    public List<AbilitySub> getAdditionalAbilityList(final String name) {
        return sa.getAdditionalAbilityList(name);
    }
    public void setAdditionalAbilityList(final String name, final List<AbilitySub> list) {
        sa.setAdditionalAbilityList(name, list);
    }

    @Override
    public void resetTargets() {
        sa.resetTargets();
    }

    // //////////////////////////////////////
    // THIS ONE IS ALL THAT MATTERS
    // //////////////////////////////////////
    @Override
    public void resolve() {
        final Game game = getActivatingPlayer().getGame();
        final Trigger regtrig = getTrigger();

        if (!(TriggerType.Always.equals(regtrig.getMode())) && !regtrig.hasParam("NoResolvingCheck")) {
            // Most State triggers don't have "Intervening If"
            if (!regtrig.requirementsCheck(game)) {
                return;
            }
            // Since basic requirements check only cares about whether it's "Activated"
            // Also check on triggered object specific requirements on resolution (e.g. evolve)
            if (!regtrig.meetsRequirementsOnTriggeredObjects(game, getTriggeringObjects())) {
                return;
            }
        }

        if (!regtrig.checkResolvedLimit(getActivatingPlayer())) {
            return;
        }

        if (regtrig.hasParam("ResolvingCheck")) {
            // rare cases: Hidden Predators (state trigger, but have "Intervening If" to check IsPresent2) etc.
            Map<String, String> recheck = Maps.newHashMap();
            String key = regtrig.getParam("ResolvingCheck");
            recheck.put(key, regtrig.getParam(key));
            if (!meetsCommonRequirements(recheck)) {
                return;
            }
        }

        if (decider != null) {
            if (!decider.isInGame()) {
                decider = SpellAbilityEffect.getNewChooser(sa, decider);
            }
            if (!decider.getController().confirmTrigger(this)) {
                return;
            }
        }

        getActivatingPlayer().getController().playSpellAbilityNoStack(sa, false);
    }

    @Override
    public CardDamageMap getDamageMap() {
        return sa.getDamageMap();
    }
    @Override
    public CardDamageMap getPreventMap() {
        return sa.getPreventMap();
    }
    @Override
    public GameEntityCounterTable getCounterTable() {
        return sa.getCounterTable();
    }
    @Override
    public CardZoneTable getChangeZoneTable() {
        return sa.getChangeZoneTable();
    }
    @Override
    public void setDamageMap(final CardDamageMap map) {
        sa.setDamageMap(map);
    }
    @Override
    public void setPreventMap(final CardDamageMap map) {
        sa.setPreventMap(map);
    }
    @Override
    public void setCounterTable(final GameEntityCounterTable table) {
        sa.setCounterTable(table);
    }
    @Override
    public void setChangeZoneTable(final CardZoneTable table) {
        sa.setChangeZoneTable(table);
    }

    public boolean isAlternativeCost(AlternativeCost ac) {
        return sa.isAlternativeCost(ac);
    }

    public AlternativeCost getAlternativeCost() {
        return sa.getAlternativeCost();
    }

    public void setAlternativeCost(AlternativeCost ac) {
        sa.setAlternativeCost(ac);
    }

    public Integer getXManaCostPaid() {
        return sa.getXManaCostPaid();
    }
    public void setXManaCostPaid(final Integer n) {
        sa.setXManaCostPaid(n);
    }

    public CardState getCardState() {
        return sa.getCardState();
    }
    public void setCardState(CardState state) {
        sa.setCardState(state);
    }

    public List<AbilitySub> getChosenList() {
        return sa.getChosenList();
    }
    public void setChosenList(List<AbilitySub> choices) {
        sa.setChosenList(choices);
    }

    public boolean isIntrinsic() {
        return sa.isIntrinsic();
    }

    public boolean isKeyword(Keyword kw) {
        return sa.isKeyword(kw);
    }
}