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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.mana.Mana;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.util.TextUtil;

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

    private transient List<Mana> lastManaProduced = Lists.newArrayList();

    private transient Card sourceCard;


    // Spells paid with this mana spell can't be countered.


    /**
     * <p>
     * Constructor for AbilityMana.
     * </p>
     *
     * @param sourceCard
     *            a {@link forge.game.card.Card} object.
     */
    public AbilityManaPart(final Card sourceCard, final Map<String, String> params) {
        this.sourceCard = sourceCard;

        origProduced = params.containsKey("Produced") ? params.get("Produced") : "1";
        this.manaRestrictions = params.containsKey("RestrictValid") ? params.get("RestrictValid") : "";
        this.cannotCounterSpell = params.get("AddsNoCounter");
        this.addsKeywords = params.get("AddsKeywords");
        this.addsKeywordsType = params.get("AddsKeywordsType");
        this.addsKeywordsUntil = params.get("AddsKeywordsUntil");
        this.addsCounters = params.get("AddsCounters");
        this.triggersWhenSpent = params.get("TriggersWhenSpent");
        this.persistentMana = (null != params.get("PersistentMana")) && "True".equalsIgnoreCase(params.get("PersistentMana"));
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
     */
    public final void produceMana(final String produced, final Player player, SpellAbility sa) {
        final Card source = this.getSourceCard();
        final ManaPool manaPool = player.getManaPool();
        String afterReplace = produced;

        SpellAbility root = sa == null ? null : sa.getRootAbility();

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(source);
        repParams.put(AbilityKey.Mana, afterReplace);
        repParams.put(AbilityKey.Player, player);
        repParams.put(AbilityKey.AbilityMana, root);
        repParams.put(AbilityKey.Activator, root == null ? null : root.getActivatingPlayer());

        switch (player.getGame().getReplacementHandler().run(ReplacementType.ProduceMana, repParams)) {
        case NotReplaced:
            break;
        case Updated:
            afterReplace = (String) repParams.get(AbilityKey.Mana);
            break;
        default:
            return;
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

        player.getGame().getTriggerHandler().runTrigger(TriggerType.TapsForMana, runParams, false);
        if (source.isLand() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost() ) {
            player.setTappedLandForManaThisTurn(true);
        }
    } // end produceMana(String)

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

    public void addTriggersWhenSpent(SpellAbility saBeingPaid, Card card) {
        if (this.triggersWhenSpent == null)
            return;

        TriggerHandler handler = card.getGame().getTriggerHandler();
        Trigger trig = TriggerHandler.parseTrigger(sourceCard.getSVar(this.triggersWhenSpent), sourceCard, false);
        handler.registerOneTrigger(trig);
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
                if (sa.isCumulativeupkeep()) {
                    return true;
                } else {
                    continue;
                }
            }

            if (restriction.startsWith("CostContainsX")) {
                if (sa.costHasManaX()) {
                    return true;
                }
                continue;
            }
            if (restriction.equals("MorphOrManifest")) {
                if ((sa.isSpell() && sa.getHostCard().isCreature() && sa.isCastFaceDown())
                        || sa.isManifestUp() || sa.isMorphUp()) {
                    return true;
                } else {
                    continue;
                }
            }

            if (sa.isValid(restriction, this.getSourceCard().getController(), this.getSourceCard(), null)) {
                return true;
            }

            if (restriction.equals("CantPayGenericCosts")) {
                return true;
            }

            if (sa.isAbility()) {
                if (restriction.startsWith("Activated")) {
                    restriction = TextUtil.fastReplace(restriction, "Activated", "Card");
                } else {
                    continue;
                }
            }


            if (sa.getHostCard() != null) {
                if (sa.getHostCard().isValid(restriction, this.getSourceCard().getController(), this.getSourceCard(), null)) {
                    return true;
                }
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
    public final String mana() {
        if (this.getOrigProduced().contains("Chosen")) {
            if (this.getSourceCard() != null && this.getSourceCard().hasChosenColor()) {
                return MagicColor.toShortString(this.getSourceCard().getChosenColor());
            }
        }
        return this.getOrigProduced();
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
        return this.getOrigProduced().contains("Combo");
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
        // TODO: need to handle replacement effects like 106.7
        
        // Any mana never means Colorless?
        if (isAnyMana() && !s.equals("C")) {
            return true;
        }

        String origProduced = getOrigProduced();
        if (origProduced.contains("Chosen") && sourceCard != null ) {
            if (getSourceCard().hasChosenColor() && MagicColor.toShortString(getSourceCard().getChosenColor()).contains(s)) {
                return true;
            }
        }
        if (isComboMana()) {
            return getComboColors().contains(s);
        }
        return origProduced.contains(s);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        // Mana abilities with same Descriptions are "equal"
        if ((o == null) || !(o instanceof AbilityManaPart)) {
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
    public String getComboColors() {
        String origProduced = getOrigProduced();
        if (!origProduced.contains("Combo")) {
            return "";
        }
        if (origProduced.contains("Any")) {
            return "W U B R G";
        }
        if (!origProduced.contains("ColorIdentity")) {
            return TextUtil.fastReplace(origProduced, "Combo ", "");
        }
        // ColorIdentity
        List<Card> commanders = getSourceCard().getController().getCommanders();
        if (commanders.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        ColorSet identity = getSourceCard().getController().getCommanderColorID();
        if (identity.hasWhite()) { sb.append("W "); }
        if (identity.hasBlue())  { sb.append("U "); }
        if (identity.hasBlack()) { sb.append("B "); }
        if (identity.hasRed())   { sb.append("R "); }
        if (identity.hasGreen()) { sb.append("G "); }
        // TODO: Add support for {C}.
        return sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1);
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
        final Game g = source.getGame();
        final Map<AbilityKey, Object> repParams = AbilityKey.newMap();
        repParams.put(AbilityKey.Mana, getOrigProduced());
        repParams.put(AbilityKey.Affected, source);
        repParams.put(AbilityKey.Player, activator);
        repParams.put(AbilityKey.AbilityMana, am.getRootAbility());

        for (final Player p : g.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (replacementEffect.requirementsCheck(g)
                            && replacementEffect.getMode() == ReplacementType.ProduceMana
                            && replacementEffect.canReplace(repParams)
                            && replacementEffect.zonesCheck(g.getZoneOf(crd))) {
                        return true;
                    }
                }
            }
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
            String colorsProduced = isComboMana() ? getComboColors() : mana();
            for (final String color : colorsProduced.split(" ")) {
                if (0 != (neededColor & ManaAtom.fromName(color))) {
                    return true;
                }
            }
        }
        return false;
    }

}
