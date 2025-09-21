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
package forge.game.spellability;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import forge.card.ColorSet;
import forge.card.GamePieceType;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.IHasSVars;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.mana.Mana;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Abstract AbilityMana class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class AbilityManaPart implements java.io.Serializable {
    /** Constant <code>serialVersionUID=-6816356991224950520L</code>. */
    private static final long serialVersionUID = -6816356991224950520L;

    private final String origProduced;
    private String lastExpressChoice = "";
    private final String manaRestrictions;
    private String extraManaRestrictions = "";
    private final String cannotCounterSpell;
    private final String addsKeywords;
    private final String addsKeywordsType;
    private final String addsKeywordsUntil;
    private final String addsCounters;
    private final String triggersWhenSpent;
    private final boolean persistentMana;
    private final boolean combatMana;

    private transient List<Mana> lastManaProduced = Lists.newArrayList();

    private transient Card sourceCard;
    private transient IHasSVars sVarHolder;


    // Spells paid with this mana spell can't be countered.

    /**
     * <p>
     * Dev Mode Constructor for AbilityMana.
     * </p>
     */
    public AbilityManaPart(final SpellAbility sourceSA, final Map<String, String> params) {
        this(sourceSA.getHostCard(), params);
        sVarHolder = sourceSA;
    }
    public AbilityManaPart(final Card sourceCard, final Map<String, String> params) {
        this.sourceCard = sourceCard;
        sVarHolder = sourceCard;

        origProduced = params.getOrDefault("Produced", "1");
        this.manaRestrictions = params.getOrDefault("RestrictValid", "");
        this.cannotCounterSpell = params.get("AddsNoCounter");
        this.addsKeywords = params.get("AddsKeywords");
        this.addsKeywordsType = params.get("AddsKeywordsValid");
        this.addsKeywordsUntil = params.get("AddsKeywordsUntil");
        this.addsCounters = params.get("AddsCounters");
        this.triggersWhenSpent = params.get("TriggersWhenSpent");
        this.persistentMana = params.containsKey("PersistentMana");
        this.combatMana = params.containsKey("CombatMana");
    }

    public AbilityManaPart(final Card newSource, AbilityManaPart oldMana) {
        this.sourceCard = newSource;
        this.origProduced = oldMana.origProduced;
        this.manaRestrictions = oldMana.manaRestrictions;
        this.cannotCounterSpell = oldMana.cannotCounterSpell;
        this.addsKeywords = oldMana.addsKeywords;
        this.addsKeywordsType = oldMana.addsKeywordsType;
        this.addsKeywordsUntil = oldMana.addsKeywordsUntil;
        this.addsCounters = oldMana.addsCounters;
        this.triggersWhenSpent = oldMana.triggersWhenSpent;
        this.persistentMana = oldMana.persistentMana;
        this.combatMana = oldMana.combatMana;
        // Do we need to copy over last mana produced somehow? Its kinda gross
    }

    /**
     * <p>
     * produceMana.
     * </p>
     * @param sa
     */
    public final void produceMana(SpellAbility sa) {
        this.produceMana(this.getOrigProduced(), this.getSourceCard().getController(), sa);
    }

    /**
     * <p>
     * produceMana.
     * </p>
     *
     * @param produced
     *            a {@link java.lang.String} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param sa
     *
     */
    public final String produceMana(final String produced, final Player player, SpellAbility sa) {
        final Card source = this.getSourceCard();
        final ManaPool manaPool = player.getManaPool();
        final Game game = player.getGame();
        String afterReplace = produced;

        SpellAbility root = sa == null ? null : sa.getRootAbility();

        if (root != null) {
            final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(source);
            repParams.put(AbilityKey.Mana, afterReplace);
            repParams.put(AbilityKey.Player, player);
            repParams.put(AbilityKey.AbilityMana, root);
            repParams.put(AbilityKey.Activator, root.getActivatingPlayer());

            switch (game.getReplacementHandler().run(ReplacementType.ProduceMana, repParams)) {
            case NotReplaced:
                break;
            case Updated:
                afterReplace = (String) repParams.get(AbilityKey.Mana);
                break;
            default:
                return "";
            }
        }

        //clear lastProduced
        this.lastManaProduced.clear();

        // loop over mana produced string
        for (final String c : afterReplace.split(" ")) {
            if (StringUtils.isNumeric(c)) {
                for (int i = Integer.parseInt(c); i > 0; i--) {
                    this.lastManaProduced.add(new Mana((byte) ManaAtom.COLORLESS, source, this));
                }
            } else {
                byte attemptedMana = MagicColor.fromName(c);
                if (attemptedMana == 0) {
                    attemptedMana = (byte)ManaAtom.COLORLESS;
                }

                this.lastManaProduced.add(new Mana(attemptedMana, source, this));
            }
        }

        // add the mana produced to the mana pool
        manaPool.add(this.lastManaProduced);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(source);
        runParams.put(AbilityKey.Player, player);
        runParams.put(AbilityKey.Produced, afterReplace);
        runParams.put(AbilityKey.AbilityMana, root);
        runParams.put(AbilityKey.Activator, root == null ? null : root.getActivatingPlayer());

        game.getTriggerHandler().runTrigger(TriggerType.ManaAdded, runParams, false);

        return afterReplace;
    }

    public void tapsForMana(final SpellAbility root, String mana) {
        if (!root.isManaAbility() || root.getPayCosts() == null || !root.getPayCosts().hasTapCost()) {
            return;
        }

        if (getSourceCard().isLand()) {
            root.getActivatingPlayer().setTappedLandForManaThisTurn(true);
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(getSourceCard());
        runParams.put(AbilityKey.Produced, mana);
        runParams.put(AbilityKey.AbilityMana, root);
        runParams.put(AbilityKey.Activator, root.getActivatingPlayer());

        getSourceCard().getGame().getTriggerHandler().runTrigger(TriggerType.TapsForMana, runParams, false);
    }

    /**
     * <p>
     * cannotCounterPaidWith.
     * </p>
     * @param saBeingPaid
     *
     * @return a {@link java.lang.String} object.
     */
    public boolean cannotCounterPaidWith(SpellAbility saBeingPaid) {
        if (null == cannotCounterSpell) return false;
        if ("True".equalsIgnoreCase(cannotCounterSpell)) return true;

        Card source = saBeingPaid.getHostCard();
        if (source == null) return false;
        return source.isValid(cannotCounterSpell, sourceCard.getController(), sourceCard, null);
    }

    public boolean isCannotCounterPaidWith() {
        return null != cannotCounterSpell;
    }

    public void addNoCounterEffect(SpellAbility saBeingPaid) {
        final Game game = sourceCard.getGame();
        final Card eff = new Card(game.nextCardId(), game);
        eff.setGameTimestamp(game.getNextTimestamp());
        eff.setName(sourceCard + "'s Effect");
        eff.setOwner(sourceCard.getController());

        eff.setImageKey(sourceCard.getImageKey());
        eff.setColor(ColorSet.NO_COLORS);
        eff.setGamePieceType(GamePieceType.EFFECT);

        String cantcounterstr = "Event$ Counter | ValidSA$ Spell.IsRemembered | Description$ That spell can't be countered.";
        ReplacementEffect re = ReplacementHandler.parseReplacement(cantcounterstr, eff, true);
        re.setLayer(ReplacementLayer.CantHappen);
        eff.addReplacementEffect(re);

        eff.addRemembered(saBeingPaid.getHostCard());

        SpellAbilityEffect.addForgetOnMovedTrigger(eff, "Stack");

        game.getAction().moveToCommand(eff, null);
    }

    /**
     * <p>
     * addKeywords.
     * </p>
     * @param saBeingPaid
     *
     * @return a {@link java.lang.String} object.
     */
    public boolean addKeywords(SpellAbility saBeingPaid) {
        return this.addsKeywords != null;
    }

    public String getAddsKeywordsType() {
        return addsKeywordsType;
    }

    public String getAddsKeywordsUntil() {
        return addsKeywordsUntil;
    }

    /**
     * <p>
     * getKeywords.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getKeywords() {
        return this.addsKeywords;
    }

    /**
     * <p>
     * addsCounters.
     * </p>
     * @param saBeingPaid
     *
     * @return a {@link java.lang.String} object.
     */
    public boolean addsCounters(SpellAbility saBeingPaid) {
        return this.addsCounters != null;
    }

    /**
     * createETBCounters
     */
    public void createETBCounters(Card c, Player controller) {
        String[] parse = this.addsCounters.split("_");
        // Convert random SVars if there are other cards with this effect
        if (c.isValid(parse[0], c.getController(), c, null)) {
            GameActionUtil.createETBCountersEffect(sourceCard, c, controller, parse[1], parse[2]);
        }
    }

    public boolean getTriggersWhenSpent() {
        return this.triggersWhenSpent != null;
    }

    public void addTriggersWhenSpent(SpellAbility saBeingPaid) {
        Trigger trig = TriggerHandler.parseTrigger(sVarHolder.getSVar(this.triggersWhenSpent), sourceCard, false, sVarHolder);
        trig.addRemembered(saBeingPaid);
        if (getSourceSA() != null) {
            trig.setSpawningAbility(getSourceSA());
        }
        saBeingPaid.getHostCard().getGame().getTriggerHandler().registerThisTurnDelayedTrigger(trig);
    }

    public SpellAbility getSourceSA() {
        return sVarHolder instanceof SpellAbility ? (SpellAbility) sVarHolder : null;
    }

    /**
     * <p>
     * getManaRestrictions.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getManaRestrictions() {
        return manaRestrictions;
    }

    public void setExtraManaRestriction(String str) {
        this.extraManaRestrictions = str;
    }

    public String getExtraManaRestriction() {
        return extraManaRestrictions;
    }

    public boolean meetsManaRestrictions(final SpellAbility sa) {
        return meetsManaRestrictions(sa, this.manaRestrictions) && meetsManaRestrictions(sa, this.extraManaRestrictions);
    }

    /**
     * <p>
     * meetsManaRestrictions.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public boolean meetsManaRestrictions(final SpellAbility sa, String restrictions) {
        // No restrictions
        if (restrictions.isEmpty()) {
            return true;
        }

        // Loop over restrictions
        for (String restriction : restrictions.split(",")) {
            if (restriction.equals("nonSpell")) {
                return !sa.isSpell();
            }

            if (restriction.equals("CumulativeUpkeep")) {
                if (sa.isCumulativeUpkeep()) {
                    return true;
                }
                continue;
            }

            if (restriction.startsWith("CostContains")) {
                Game game = sa.getHostCard().getGame();
                Cost payment = sa.getPayCosts();
                if (game.getStack().isResolving() && sa.hasParam("UnlessCost")) {
                    payment = AbilityUtils.calculateUnlessCost(sa, sa.getParam("UnlessCost"), false);
                }
                if (payment.hasNoManaCost()) {
                    continue;
                }
                // TODO Thassa's Intervention with "twice {X}" is tricky
                // TODO Volcano Hellion can't be handled without refactor to just have it passed down directly
                if (restriction.endsWith("X") && payment.getCostMana().getAmountOfX() > 0) {
                    return true;
                }
                if (restriction.endsWith("C") && payment.getCostMana().getMana().getShardCount(ManaCostShard.COLORLESS) > 0) {
                    return true;
                }
                continue;
            }

            if (restriction.equals("MorphOrManifest")) {
                if ((sa.isSpell() && sa.getHostCard().isCreature() && sa.isCastFaceDown())
                        || sa.isManifestUp() || sa.isMorphUp()) {
                    return true;
                }
                continue;
            }

            //handled in meetsManaShardRestrictions
            if (restriction.equals("CantPayGenericCosts")) {
                return true;
            }

            // "can't" zone restriction – shouldn't be mixed with other restrictions
            if (restriction.startsWith("CantCastSpellFrom")) {
                if (!sa.isSpell()) {
                    return true;
                }
                final ZoneType badZone = ZoneType.smartValueOf(restriction.substring(17));
                final Card host = sa.getHostCard();
                final Zone castFrom = host.getCastFrom();
                //ComputerUtilMana looks at this to see if AI can cast things, so need a fallback zone
                final ZoneType zone = castFrom == null ? host.getZone().getZoneType() : castFrom.getZoneType();
                if (!badZone.equals(zone)) {
                    return true;
                }
            }

            if (restriction.equals("CantCastNonArtifactSpells")) {
                return !sa.isSpell() || sa.getHostCard().isArtifact();
            }

            // TODO refactor to differ between ForCost and ForEffect
            // the payment is for a resolving SA, currently no other restrictions would allow that
            if (getSourceCard().getGame().getStack().getInstanceMatchingSpellAbilityID(sa.getRootAbility()) != null) {
                return false;
            }

            if (sa.isValid(restriction, this.getSourceCard().getController(), this.getSourceCard(), null)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * meetsManaShardRestrictions.
     * </p>
     *
     * @param shard
     *            a {@link forge.card.mana.ManaCostShard} object.
     * @param color
     *               the color of mana being paid
     * @return a boolean.
     */
    public boolean meetsManaShardRestrictions(final ManaCostShard shard, final byte color) {
        if (this.manaRestrictions.isEmpty()) {
            return true;
        }
        for (String restriction : this.manaRestrictions.split(",")) {
            if (restriction.equals("CantPayGenericCosts")) {
                if (shard.isGeneric()) {
                    if (shard.isOr2Generic() && shard.isColor(color)) {
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    continue;
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * meetsSpellAndShardRestrictions.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param shard
     *            a {@link forge.card.mana.ManaCostShard} object.
     * @param color
     *               the color of mana being paid
     * @return a boolean.
     */
    public boolean meetsSpellAndShardRestrictions(final SpellAbility sa, final ManaCostShard shard, final byte color) {
        return this.meetsManaRestrictions(sa) && this.meetsManaShardRestrictions(shard, color);
    }

    /**
     * <p>
     * mana.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String mana(SpellAbility sa) {
        if (isComboMana()) { // when asking combo, just go there
            return getComboColors(sa);
        }
        String produced = this.getOrigProduced();
        if (produced.contains("Chosen")) {
            produced = produced.replace("Chosen", getChosenColor(sa, sa.getHostCard().getChosenColors()));
        }
        return produced;
    }

    /**
     * <p>
     * setAnyChoice.
     * </p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void setExpressChoice(String s) {
        this.lastExpressChoice = s;
    }

    public void setExpressChoice(ColorSet cs) {
        StringBuilder sb = new StringBuilder();
        if(cs.hasBlack()) sb.append("B ");
        if(cs.hasBlue()) sb.append("U ");
        if(cs.hasWhite()) sb.append("W ");
        if(cs.hasRed()) sb.append("R ");
        if(cs.hasGreen()) sb.append("G ");
        this.lastExpressChoice = sb.toString().trim();
    }

    /**
     * <p>
     * Getter for the field <code>lastAnyChoice</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExpressChoice() {
        return this.lastExpressChoice;
    }

    /**
     * <p>
     * clearExpressChoice.
     * </p>
     *
     */
    public void clearExpressChoice() {
        this.lastExpressChoice = "";
    }

    /**
     * <p>
     * Getter for the field <code>lastProduced</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public List<Mana> getLastManaProduced() {
        return this.lastManaProduced;
    }

    public final boolean isSnow() {
        return this.getSourceCard().isSnow();
    }

    public boolean isAnyMana() {
        return this.getOrigProduced().contains("Any");
    }

    public boolean isComboMana() {
        return this.getOrigProduced().startsWith("Combo");
    }

    public boolean isSpecialMana() {
        return this.getOrigProduced().contains("Special");
    }

    /**
     * <p>
     * canProduce.
     * </p>
     *
     * @param s
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean canProduce(final String s, final SpellAbility sa) {
        // TODO: need to handle replacement effects like 106.7 before deciding no mana is produced
        //if (sa.amountOfManaGenerated(false) == 0) {
        //    return false;
        //}

        // Any mana never means Colorless?
        if (isAnyMana() && !s.equals("C")) {
            return true;
        }

        return mana(sa).contains(s);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        // Mana abilities with same Descriptions are "equal"
        if (!(o instanceof AbilityManaPart)) {
            return false;
        }

        final AbilityManaPart abm = (AbilityManaPart) o;

        return sourceCard.equals(abm.sourceCard) && origProduced.equals(abm.getOrigProduced());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getSourceCard().hashCode()));
    }

    /**
     * @return the origProduced
     */
    public String getOrigProduced() {
        return origProduced;
    }

    /**
     * @return the color available in combination mana
     */
    public String getComboColors(SpellAbility sa) {
        String origProduced = getOrigProduced();
        if (!origProduced.startsWith("Combo")) {
            return "";
        }
        if (origProduced.contains("Any")) {
            return "W U B R G";
        }
        // replace Chosen for Combo colors
        if (origProduced.contains("Chosen")) {
            origProduced = origProduced.replace("Chosen", getChosenColor(sa, sa.getHostCard().getChosenColors()));
        }
        // replace Chosen for Spire colors
        if (origProduced.contains("ColorID")) {
            Iterator<String> colors = Iterators.transform(sa.getHostCard().getMarkedColors().iterator(), MagicColor::toLongString);
            origProduced = origProduced.replace("ColorID", getChosenColor(sa, () -> colors));
        }
        if (origProduced.contains("NotedColors")) {
            // Should only be used for Paliano, the High City
            if (sa.getActivatingPlayer() == null) {
                return "";
            }

            String colors = sa.getActivatingPlayer().getDraftNotes().get("Paliano, the High City");
            if (colors == null) {
                return "";
            }
            // Colors here is an comma separated color list, potentially with duplicates
            // We need to remove duplicates and convert to single letters
            StringBuilder sb = new StringBuilder();
            for (String color : colors.split(",")) {
                String shortColor = MagicColor.toShortString(color);
                if (sb.indexOf(shortColor) == -1) {
                    sb.append(shortColor).append(" ");
                }
            }
            origProduced = origProduced.replace("NotedColors", sb.toString().trim());
        }

        if (!origProduced.contains("ColorIdentity")) {
            return TextUtil.fastReplace(origProduced, "Combo ", "");
        }
        // ColorIdentity
        StringBuilder sb = new StringBuilder();
        if (getSourceCard().getController() != null) {
            List<Card> commanders = getSourceCard().getController().getCommanders();
            if (commanders.isEmpty()) {
                return "";
            }
            ColorSet identity = getSourceCard().getController().getCommanderColorID();
            if (identity.hasWhite()) { sb.append("W "); }
            if (identity.hasBlue())  { sb.append("U "); }
            if (identity.hasBlack()) { sb.append("B "); }
            if (identity.hasRed())   { sb.append("R "); }
            if (identity.hasGreen()) { sb.append("G "); }
        }
        // TODO: Add support for {C}.
        return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1);
    }

    public String getChosenColor(SpellAbility sa, Iterable<String> colors) {
        if (sa == null) {
            return "";
        }
        Card card = sa.getHostCard();
        if (card != null) {
            StringBuilder values = new StringBuilder();
            for (String c : colors) {
                values.append(MagicColor.toShortString(c)).append(" ");
            }
            return values.toString().trim();
        }
        return "";
    }

    public Card getSourceCard() {
        return sourceCard;
    }
    public void setSourceCard(final Card host) {
        sourceCard = host;
    }

    /**
     * <p>
     * isPersistentMana.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isPersistentMana() {
        return this.persistentMana;
    }
    public boolean isCombatMana() {
        return this.combatMana;
    }

    boolean abilityProducesManaColor(final SpellAbility am, final byte neededColor) {
        if (0 != (neededColor & ManaAtom.GENERIC)) {
            return true;
        }

        if (isAnyMana()) {
            return true;
        }

        // check for produce mana replacement effects - they mess this up, so just use the mana ability
        final Card source = am.getHostCard();
        final Player activator = am.getActivatingPlayer();
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(source);
        repParams.put(AbilityKey.Mana, getOrigProduced());
        repParams.put(AbilityKey.Activator, activator);
        repParams.put(AbilityKey.AbilityMana, am.getRootAbility());

        if (!source.getGame().getReplacementHandler().getReplacementList(ReplacementType.ProduceMana, repParams, ReplacementLayer.Other).isEmpty()) {
            return true;
        }

        if (am.getApi() == ApiType.ManaReflected) {
            final Iterable<String> reflectableColors = CardUtil.getReflectableManaColors(am);
            for (final String color : reflectableColors) {
                if (0 != (neededColor & ManaAtom.fromName(color))) {
                    return true;
                }
            }
        } else {
            // treat special mana if it always can be paid
            if (isSpecialMana()) {
                return true;
            }
            String colorsProduced = mana(am);
            for (final String color : colorsProduced.split(" ")) {
                if (0 != (neededColor & ManaAtom.fromName(color))) {
                    return true;
                }
            }
        }
        return false;
    }

}
