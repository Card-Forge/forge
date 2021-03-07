package forge.game.staticability;

import java.util.List;

import com.google.common.collect.Lists;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityAlternativeCost {

    static String AlternativeCost = "AlternativeCost";
    static String AlternativeZone = "AlternativeZone";
    
    public static List<SpellAbility> getAlternativeCostAndZones(final SpellAbility spell, final Card card, final Player activator) {
        List<SpellAbility> result = Lists.newArrayList();
        if (!spell.isSpell() && !spell.isLandAbility()) {
            return result;
        }
        result.addAll(getAlternativeZones(spell, card, activator));
        for (SpellAbility sp : getAlternativeCost(spell, card, activator)) {
            result.add(sp);
            result.addAll(getAlternativeZones(sp, card, activator));
        }
        return result;
    }

    public static List<SpellAbility> getAlternativeCost(final SpellAbility spell, final Card card, final Player activator) {
        List<SpellAbility> result = Lists.newArrayList();
        if (!spell.isSpell() && !spell.isLandAbility()) {
            return result;
        }

        final Game game = activator.getGame();
        final CardCollection allp = new CardCollection(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        allp.add(card);
        for (final Card ca : allp) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(AlternativeCost) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                SpellAbility alt = applyAlternativeCost(stAb, spell, card, activator);
                if (alt != null) {
                    result.add(alt);
                }
            }
        }
        return result;
    }

    public static List<SpellAbility> getAlternativeZones(final SpellAbility spell, final Card card, final Player activator) {
        List<SpellAbility> result = Lists.newArrayList();
        if (!spell.isSpell() && !spell.isLandAbility()) {
            return result;
        }

        final Game game = activator.getGame();
        final CardCollection allp = new CardCollection(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        allp.add(card);
        for (final Card ca : allp) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(AlternativeZone) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                SpellAbility alt = applyAlternativeCost(stAb, spell, card, activator);
                if (alt != null) {
                    result.add(alt);
                }
            }
        }
        return result;
    }

    public static SpellAbility applyAlternativeCost(final StaticAbility stAb, final SpellAbility spell, final Card card, final Player activator) {
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("Caster") && (activator != null)
                && !activator.isValid(stAb.getParam("Caster"), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("ValidSA")
                && !spell.isValid(stAb.getParam("ValidSA").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));
            if (!src.contains(card.getLastKnownZone().getZoneType())) {
                return null;
            }
        }

        final SpellAbility newSA = spell.copy(card, activator, false);
        newSA.setBasicSpell(false);
        // if Origin flag is set, skip zone check in spellRestrictions
        if (stAb.hasParam("Origin")) {
            newSA.getRestrictions().setZone(null);
        }
        // Activator already checked with Caster, bypass restrictions
        if (stAb.hasParam("Caster")) {
            newSA.getRestrictions().setActivator(null);
        }

        if (stAb.hasParam("MayPlayWithFlash")) {
            newSA.getRestrictions().setInstantSpeed(true);
        }

        if (stAb.hasParam("Cost") && !spell.isLandAbility()) {
            String costStr = stAb.getParam("Cost");
            
            // this is for Kentaro, the Smiling Cat, assuming there is no Samurai with X in its mana cost
            if (costStr.equals("ConvertedManaCost")) {
                costStr = Integer.toString(card.getCMC());
            }
            final Cost cost = new Cost(costStr, false).add(spell.getPayCosts().copyWithNoMana());
            newSA.setPayCosts(cost);
            newSA.setDescription(spell.getDescription() + " (by paying " + cost.toSimpleString() + " instead of its mana cost)");
        }

        if (stAb.hasParam("WithCountersType")) {
            newSA.putParam("WithCountersType", stAb.getParam("WithCountersType"));
            newSA.putParam("WithCountersAmount", stAb.getParamOrDefault("WithCountersAmount", "1"));
        }

        // Add this Static Ability to the ones used to play the spell
        newSA.addMayPlay(stAb);

        return newSA;
    }
    

    public static SpellAbility applyAlternativeZone(final StaticAbility stAb, final SpellAbility spell, final Card card, final Player activator) {
        final Card hostCard = stAb.getHostCard();

        // zone check already overwritten
        if (spell.getRestrictions().getZone() == null) {
            return null;
        }

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("Caster") && (activator != null)
                && !activator.isValid(stAb.getParam("Caster"), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("ValidSA")
                && !spell.isValid(stAb.getParam("ValidSA").split(","), hostCard.getController(), hostCard, null)) {
            return null;
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));
            if (!src.contains(card.getLastKnownZone().getZoneType())) {
                return null;
            }
        }

        final SpellAbility newSA = spell.copy(card, activator, false);
        newSA.setBasicSpell(false);
        // if Origin flag is set, skip zone check in spellRestrictions
        if (stAb.hasParam("Origin")) {
            newSA.getRestrictions().setZone(null);
        }
        // Activator already checked with Caster, bypass restrictions
        if (stAb.hasParam("Caster")) {
            newSA.getRestrictions().setActivator(null);
        }

        if (stAb.hasParam("MayPlayWithFlash")) {
            newSA.getRestrictions().setInstantSpeed(true);
        }

        if (stAb.hasParam("ExtraCost")) {
            String costStr = stAb.getParam("ExtraCost");
            if (stAb.hasSVar(costStr)) {
                costStr = Integer.toString(AbilityUtils.calculateAmount(hostCard, costStr, stAb));
            }
            Cost cost = new Cost(costStr, false);
            newSA.getPayCosts().add(cost);
            newSA.setDescription(spell.getDescription() + " (by paying " + cost.toSimpleString() + " extra)");
        }

        if (stAb.hasParam("WithCountersType")) {
            newSA.putParam("WithCountersType", stAb.getParam("WithCountersType"));
            newSA.putParam("WithCountersAmount", stAb.getParamOrDefault("WithCountersAmount", "1"));
        }

        // Add this Static Ability to the ones used to play the spell
        newSA.addMayPlay(stAb);

        return newSA;
    }
}
