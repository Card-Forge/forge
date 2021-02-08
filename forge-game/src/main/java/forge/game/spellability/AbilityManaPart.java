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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CounterType;
import forge.game.mana.Mana;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.replacement.*;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final String cannotCounterSpell;
    private final String addsKeywords;
    private final String addsKeywordsType;
    private final String addsKeywordsUntil;
    private final String addsCounters;
    private final String triggersWhenSpent;
    private final boolean persistentMana;
    private String manaReplaceType;

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
        this.manaReplaceType = params.containsKey("ManaReplaceType") ? params.get("ManaReplaceType") : "";
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
        String afterReplace = applyManaReplacement(sa, produced);
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(source);
        repParams.put(AbilityKey.Mana, produced);
        repParams.put(AbilityKey.Player, player);
        repParams.put(AbilityKey.AbilityMana, sa);
        if (player.getGame().getReplacementHandler().run(ReplacementType.ProduceMana, repParams) != ReplacementResult.NotReplaced) {
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
        runParams.put(AbilityKey.AbilityMana, sa);
        runParams.put(AbilityKey.Produced, afterReplace);
        player.getGame().getTriggerHandler().runTrigger(TriggerType.TapsForMana, runParams, false);
        if (source.isLand()) {
        	player.setTappedLandForManaThisTurn(true);
        }
        // Clear Mana replacement
        this.manaReplaceType = "";
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
            final Game game = this.sourceCard.getGame();
            final Card eff = new Card(game.nextCardId(), game);
            eff.setTimestamp(game.getNextTimestamp());
            eff.setName(sourceCard.getName() + "'s Effect");
            eff.addType("Effect");
            eff.setOwner(controller);

            eff.setImageKey(sourceCard.getImageKey());
            eff.setColor(MagicColor.COLORLESS);
            eff.setImmutable(true);
            // try to get the SpellAbility from the mana ability
            //eff.setEffectSource((SpellAbility)null);

            eff.addRemembered(c);

            String abStr = "DB$ PutCounter | Defined$ ReplacedCard | CounterType$ " + parse[1]
                    + " | ETB$ True | CounterNum$ " + parse[2];

            SpellAbility sa = AbilityFactory.getAbility(abStr, c);
            if (!StringUtils.isNumeric(parse[2])) {
                sa.setSVar(parse[2], sourceCard.getSVar(parse[2]));
            }
            CardFactoryUtil.setupETBReplacementAbility(sa);

            String desc = "It enters the battlefield with ";
            desc += Lang.nounWithNumeral(parse[2], CounterType.getType(parse[1]).getName() + " counter");
            desc += " on it.";

            String repeffstr = "Event$ Moved | ValidCard$ Card.IsRemembered | Destination$ Battlefield | Description$ " + desc;

            ReplacementEffect re = ReplacementHandler.parseReplacement(repeffstr, eff, true);
            re.setLayer(ReplacementLayer.Other);
            re.setOverridingAbility(sa);

            eff.addReplacementEffect(re);

            // Forgot Trigger
            String trig = "Mode$ ChangesZone | ValidCard$ Card.IsRemembered | Origin$ Stack | Destination$ Any | TriggerZones$ Command | Static$ True";
            String forgetEffect = "DB$ Pump | ForgetObjects$ TriggeredCard";
            String exileEffect = "DB$ ChangeZone | Defined$ Self | Origin$ Command | Destination$ Exile"
                    + " | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0";

            SpellAbility saForget = AbilityFactory.getAbility(forgetEffect, eff);
            AbilitySub saExile = (AbilitySub) AbilityFactory.getAbility(exileEffect, eff);
            saForget.setSubAbility(saExile);

            final Trigger parsedTrigger = TriggerHandler.parseTrigger(trig, eff, true);
            parsedTrigger.setOverridingAbility(saForget);
            eff.addTrigger(parsedTrigger);
            eff.updateStateForView();

            // TODO: Add targeting to the effect so it knows who it's dealing with
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            game.getAction().moveTo(ZoneType.Command, eff, null);
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
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
        return this.manaRestrictions;
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
    public boolean meetsManaRestrictions(final SpellAbility sa) {
        // No restrictions
        if (this.manaRestrictions.isEmpty()) {
            return true;
        }

        // Loop over restrictions
        for (String restriction : this.manaRestrictions.split(",")) {
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
                }
                else {
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
     * 			  the color of mana being paid
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
     * 			  the color of mana being paid
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

    public final boolean canProduce(final String s) {
        return canProduce(s, null);
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
        if (sa != null) {
            return applyManaReplacement(sa, origProduced).contains(s);
        }
        return origProduced.contains(s);
    }

    /**
     * <p>
     * isBasic.
     * </p>
     *
     * @return a boolean.
     */
    public final boolean isBasic() {
        return this.getOrigProduced().length() == 1 || this.getOrigProduced().contains("Any")
                || this.getOrigProduced().contains("Chosen");
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

    /**
     * @return the manaReplaceType
     */
    public String getManaReplaceType() {
        return manaReplaceType;
    }

    /**
     * setManaReplaceType.
     */
    public void setManaReplaceType(final String type) {
        this.manaReplaceType = type;
    }
    /**
     * <p>
     * applyManaReplacement.
     * </p>
     * @return a String
     */
    public static String applyManaReplacement(final SpellAbility sa, final String original) {
        final Map<String, String> repMap = Maps.newHashMap();
        final Player act = sa != null ? sa.getActivatingPlayer() : null;
        final String manaReplace = sa != null ? sa.getManaPart().getManaReplaceType(): "";
        if (manaReplace.isEmpty()) {
            if (act != null && act.getLandsPlayedThisTurn() > 0 && sa.hasParam("ReplaceIfLandPlayed")) {
                return sa.getParam("ReplaceIfLandPlayed");
            }
            return original;
        }
        if (manaReplace.startsWith("Any")) {
            // Replace any type and amount
            String replaced = manaReplace.split("->")[1];
            if (replaced.equals("Any")) {
                byte rs = MagicColor.GREEN;
                if (act != null) {
                    rs = act.getController().chooseColor("Choose a color", sa, ColorSet.ALL_COLORS);
                }
                replaced = MagicColor.toShortString(rs);
            }
            return replaced;
        }
        final Pattern splitter = Pattern.compile("->");
        // Replace any type
        for (String part : manaReplace.split(" & ")) {
            final String[] v = splitter.split(part, 2);
            // TODO Colorless mana replacement is probably different now?
            if (v[0].equals("Colorless")) {
                repMap.put("[0-9][0-9]?", v.length > 1 ? v[1].trim() : "");
            } else {
                repMap.put(v[0], v.length > 1 ? v[1].trim() : "");
            }
        }
        // Handle different replacement simultaneously
        Pattern pattern = Pattern.compile(StringUtils.join(repMap.keySet().iterator(), "|"));
        Matcher m = pattern.matcher(original);
        StringBuffer sb = new StringBuffer();
        while(m.find()) {
            if (m.group().matches("[0-9][0-9]?")) {
                final String rep = StringUtils.repeat(repMap.get("[0-9][0-9]?") + " ",
                        Integer.parseInt(m.group())).trim();
                m.appendReplacement(sb, rep);
            } else {
                m.appendReplacement(sb, repMap.get(m.group()));
            }
        }
        m.appendTail(sb);
        String replaced = sb.toString();
        while (replaced.contains("Any")) {
            byte rs = MagicColor.GREEN;
            if (act != null) {
                rs = act.getController().chooseColor("Choose a color", sa, ColorSet.ALL_COLORS);
            }
            replaced = replaced.replaceFirst("Any", MagicColor.toShortString(rs));
        }
        return replaced;
    }

} // end class AbilityMana

