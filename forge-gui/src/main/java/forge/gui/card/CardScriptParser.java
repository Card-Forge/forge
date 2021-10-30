package forge.gui.card;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.card.CardType;
import forge.game.ability.ApiType;
import forge.game.trigger.TriggerType;

public final class CardScriptParser {

    private final String script;
    private final Set<String> sVars = Sets.newTreeSet(), sVarAbilities = Sets.newTreeSet();
    public CardScriptParser(final String script) {
        this.script = script;

        final String[] lines = StringUtils.split(script, "\r\n");
        for (final String line : lines) {
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            if (line.startsWith("SVar:")) {
                final String[] sVarParts = StringUtils.split(line, ':');
                if (sVarParts.length != 3) {
                    continue;
                }
                sVars.add(sVarParts[1]);
            }
        }
    }

    public Map<Integer, Integer> getErrorRegions() {
        return getErrorRegions(false);
    }

    /**
     * Find all erroneous regions of this script.
     *
     * @param quick
     *            if {@code true}, stop when the first region is found.
     * @return a {@link Map} mapping the starting index of each error region to
     *         the length of that region.
     */
    private Map<Integer, Integer> getErrorRegions(final boolean quick) {
        final Map<Integer, Integer> result = Maps.newTreeMap();

        final String[] lines = StringUtils.split(script, '\n');
        int index = 0;
        for (final String line : lines) {
            final String trimLine = line.trim();
            if (StringUtils.isEmpty(line)) {
                continue;
            }
            boolean bad = false;
            if (trimLine.startsWith("Name:") && trimLine.length() > "Name:".length()) {
                // whatever, nonempty name is always ok!
            } else if (trimLine.startsWith("ManaCost:")) {
                if (!isManaCostLegal(trimLine.substring("ManaCost:".length()))) {
                    bad = true;
                }
            } else if (trimLine.startsWith("Types:")) {
                if (!isTypeLegal(trimLine.substring("Types:".length()))) {
                    bad = true;
                }
            } else if (trimLine.startsWith("A:")) {
                result.putAll(getActivatedAbilityErrors(trimLine.substring("A:".length()), index + "A:".length()));
            } else if (trimLine.startsWith("T:")) {
                result.putAll(getTriggerErrors(trimLine.substring("T:".length()), index + "T:".length()));
            } else if (trimLine.startsWith("SVar:")) {
                final String[] sVarParts = trimLine.split(":", 3);
                if (sVarParts.length != 3) {
                    bad = true;
                }
                if (sVarAbilities.contains(sVarParts[1])) {
                    result.putAll(getSubAbilityErrors(sVarParts[2], index + "SVar:".length() + 1 + sVarParts[1].length() + 1));
                }
            }
            if (bad) {
                result.put(index, trimLine.length());
            }
            index += line.length() + 1;
            if (quick && !result.isEmpty()) {
                break;
            }
        }
        return result;
    }

    private static boolean isManaCostLegal(final String manaCost) {
        if (manaCost.equals("no cost")) {
            return true;
        }
        if (StringUtils.isEmpty(manaCost) || StringUtils.isWhitespace(manaCost)) {
            return false;
        }

        for (final String part : StringUtils.split(manaCost, ' ')) {
            if (StringUtils.isNumeric(part) || part.equals("X")) {
                continue;
            }
            return isManaCostPart(part);
        }
        return true;
    }

    private static boolean isManaCostPart(final String part) {
        if (part.length() == 1) {
            return isManaSymbol(part.charAt(0));
        } else if (part.length() == 2) {
            if (!(part.startsWith("P") || part.startsWith("2") || isManaSymbol(part.charAt(0)))) {
                return false;
            }
            if ((!isManaSymbol(part.charAt(1))) || part.charAt(0) == part.charAt(1)) {
                return false;
            }
            return true;
        }
        return false;
    }
    private static boolean isManaSymbol(final char c) {
        return c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G';
    }

    private static boolean isTypeLegal(final String type) {
        for (final String t : StringUtils.split(type, ' ')) {
            if (!isSingleTypeLegal(t)) {
                return false;
            }
        }
        return true;
    }
    private static boolean isSingleTypeLegal(final String type) {
        return CardType.isACardType(type) || CardType.isASupertype(type) || CardType.isASubType(type);
    }

    private static List<KeyValuePair> getParams(final String ability, final int offset, final Map<Integer, Integer> errorRegions) {
        final String[] parts = StringUtils.split(ability, '|');
        final List<KeyValuePair> params = Lists.newArrayList();
        int currentIndex = offset;
        for (final String part : parts) {
            final String[] subParts = StringUtils.split(part, '$');
            if (subParts.length > 0) {
                params.add(new KeyValuePair(subParts[0], subParts.length > 1 ? subParts[1] : "", currentIndex));
            } else {
                errorRegions.put(currentIndex, part.length());
            }
            currentIndex += part.length() + 1;
        }

        // Check spacing
        for (final KeyValuePair param : params) {
            if (!param.getKey().startsWith(" ") && param.startIndex() != offset) {
                errorRegions.put(param.startIndex() - 1, 2);
            }
            if (!param.getValue().startsWith(" ")) {
                errorRegions.put(param.startIndexValue() - 1, 2);
            }
            if (!param.getValue().endsWith(" ") && param.endIndex() != offset + ability.length()) {
                errorRegions.put(param.endIndex() - 1, 2);
            }
        }
        return params;
    }

    private Map<Integer, Integer> getActivatedAbilityErrors(final String ability, final int offset) {
        return getAbilityErrors(ability, offset, true);
    }
    private Map<Integer, Integer> getSubAbilityErrors(final String ability, final int offset) {
        return getAbilityErrors(ability, offset, false);
    }
    private Map<Integer, Integer> getAbilityErrors(final String ability, final int offset, final boolean requireCost) {
        final Map<Integer, Integer> result = Maps.newTreeMap();
        final List<KeyValuePair> params = getParams(ability, offset, result);

        // First parameter should be Api declaration
        if (!isAbilityApiDeclarerLegal(params.get(0).getKey())) {
            result.put(params.get(0).startIndex(), params.get(0).length());
        }
        // If present, second parameter should be cost
        if (requireCost && !params.get(1).getKey().trim().equals("Cost")) {
            result.put(params.get(1).startIndex(), params.get(1).length());
        }

        // Now, check all parameters
        for (final KeyValuePair param : params) {
            boolean isBadValue = false;
            final String trimKey = param.getKey().trim(), trimValue = param.getValue().trim();
            if (isAbilityApiDeclarerLegal(trimKey)) {
                if (!isAbilityApiLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("Cost")) {
                if (!isCostLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("ValidTgts") || trimKey.equals("ValidCards")) {
                if (!isValidLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("Defined")) {
                if (!isDefinedLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("TgtPrompt") || trimKey.equals("StackDescription") || trimKey.equals("SpellDescription")) {
                if (trimValue.isEmpty()) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("SubAbility")) {
                if (sVars.contains(trimValue)) {
                    sVarAbilities.add(trimValue);
                } else {
                    isBadValue = true;
                }
            } else {
                result.put(param.startIndex(), param.keyLength());
            }
            if (isBadValue) {
                result.put(param.startIndexValue(), param.valueLength());
            }
        }
        return result;
    }

    private Map<Integer, Integer> getTriggerErrors(final String trigger, final int offset) {
        final Map<Integer, Integer> result = Maps.newTreeMap();
        final List<KeyValuePair> params = getParams(trigger, offset, result);

        // Check all parameters
        for (final KeyValuePair param : params) {
            boolean isBadValue = false;
            final String trimKey = param.getKey().trim(), trimValue = param.getValue().trim();
            if (trimKey.equals("Mode")) {
                if (!isTriggerApiLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("Cost")) {
                if (!isCostLegal(trimValue)) {
                    isBadValue = true;
                }
            } else if (trimKey.equals("Execute")) {
                if (sVars.contains(trimValue)) {
                    sVarAbilities.add(trimValue);
                } else {
                    isBadValue = true;
                }
            } else if (trimKey.equals("TriggerDescription")) {
                if (trimValue.isEmpty()) {
                    //isBadValue = true;
                }
            } else if (trimKey.equals("ValidCard")) {
                if (!isValidLegal(trimValue)) {
                    isBadValue = true;
                }
            } else {
                result.put(param.startIndex(), param.keyLength());
            }
            if (isBadValue) {
                result.put(param.startIndexValue(), param.valueLength());
            }
        }
        return result;
    }

    private static boolean isCostLegal(final String cost) {
        return isManaCostLegal(cost.trim()); // TODO include other costs (tap, sacrifice, etc.)
    }

    private static boolean isAbilityApiDeclarerLegal(final String declarer) {
        final String tDeclarer = declarer.trim();
        return tDeclarer.equals("AB") || tDeclarer.equals("DB") || tDeclarer.equals("SP");
    }
    private static boolean isAbilityApiLegal(final String api) {
        try {
            return ApiType.smartValueOf(api.trim()) != null;
        } catch (final RuntimeException e) {
            return false;
        }
    }
    private static boolean isTriggerApiLegal(final String api) {
        try {
            return TriggerType.smartValueOf(api.trim()) != null;
        } catch (final RuntimeException e) {
            return false;
        }
    }

    private static final Predicate<String> startsWith(final String s) {
        return new Predicate<String>() {
            @Override public boolean apply(final String input) {
                return s.startsWith(input);
            }};
    }

    /**
     * Literal defined strings for cards and spellabilities.
     */
    private static final Set<String> DEFINED_CARDS = ImmutableSortedSet.of(
            "Self", "OriginalHost", "EffectSource", "Equipped", "Enchanted",
            "TopOfLibrary", "BottomOfLibrary", "Targeted", "ThisTargetedCard",
            "ParentTarget", "Remembered", "DirectRemembered",
            "DelayTriggerRemembered", "FirstRemembered", "Clones", "Imprinted",
            "ChosenCard", "SacrificedCards", "Sacrificed", "DiscardedCards",
            "Discarded", "ExiledCards", "Exiled", "TappedCards", "Tapped",
            "UntappedCards", "Untapped", "Parent", "SourceFirstSpell");
    /**
     * Defined starting strings for cards and spellabilities.
     */
    private static final Set<String> DEFINED_CARDS_STARTSWITH = ImmutableSortedSet
            .of("Triggered", "Replaced", "ThisTurnEntered");
    /**
     * Literal defined strings for players.
     */
    private static final Set<String> DEFINED_PLAYERS = ImmutableSortedSet.of(
            "Targeted", "TargetedPlayer", "ParentTarget", "TargetedController",
            "TargetedOwner", "TargetedAndYou", "ParentTargetedController",
            "Remembered", "DelayTriggerRemembered", "RememberedOpponents",
            "RememberedController", "RememberedOwner", "ImprintedController",
            "ImprintedOwner", "EnchantedController", "EnchantedOwner",
            "EnchantedPlayer", "AttackingPlayer", "DefendingPlayer",
            "ChosenPlayer", "ChosenAndYou", "SourceController", "CardOwner",
            "ActivePlayer", "You", "Opponent");
    /**
     * Defined starting strings for players.
     */
    private static final Set<String> DEFINED_PLAYERS_STARTSWITH = ImmutableSortedSet
            .of("Triggered", "OppNonTriggered", "Replaced");

    private static boolean isDefinedLegal(final String defined) {
        return isDefinedCardOrSaLegal(defined) || isDefinedPlayerLegal(defined);
    }
    private static boolean isDefinedCardOrSaLegal(final String defined) {
        if (defined.startsWith("Valid")) {
            return isValidLegal(defined.substring("Valid".length()));
        }
        if (DEFINED_CARDS.contains(defined)) {
            return true;
        }
        return Iterables.any(DEFINED_CARDS_STARTSWITH, startsWith(defined));
    }
    private static boolean isDefinedPlayerLegal(final String defined) {
        final boolean non = defined.startsWith("Non"), flipped = defined.startsWith("Flipped");
        if (non || flipped) {
            String newDefined = null;
            if (non) {
                newDefined = defined.substring("Non".length());
            } else if (flipped) {
                newDefined = defined.substring("Flipped".length());
            }
            return isDefinedPlayerLegal(newDefined);
        }
        if (DEFINED_PLAYERS.contains(defined)) {
            return true;
        }
        return Iterables.any(DEFINED_PLAYERS_STARTSWITH, startsWith(defined));
    }

    private static final Set<String> VALID_INCLUSIVE = ImmutableSortedSet.of(
            "Spell", "Permanent", "Card");
    private static boolean isValidLegal(final String valid) {
        String remaining = valid;
        if (remaining.charAt(0) == '!') {
            remaining = valid.substring(1);
        }
        final String[] splitDot = remaining.split("\\.");
        if (!(VALID_INCLUSIVE.contains(splitDot[0]) || isSingleTypeLegal(splitDot[0]))) {
            return false;
        }
        if (splitDot.length < 2) {
            return true;
        }

        final String[] splitPlus = StringUtils.split(splitDot[1], '+');
        for (final String excl : splitPlus) {
            if (!isValidExclusive(excl)) {
                return false;
            }
        }
        return true;
    }

    private static final Set<String> VALID_EXCLUSIVE = ImmutableSortedSet.of(
            "sameName", "namedCard", "NamedByRememberedPlayer", "Permanent",
            "ChosenCard", "nonChosenCard", "White", "Blue", "Black", "Red",
            "Green", "nonWhite", "nonBlue", "nonBlack", "nonRed", "nonGreen",
            "Colorless", "nonColorless", "Multicolor", "nonMulticolor",
            "Monocolor", "nonMonocolor", "ChosenColor", "AllChosenColors",
            "AnyChosenColor", "DoubleFaced", "Flip", "YouCtrl", "YourTeamCtrl",
            "YouDontCtrl", "OppCtrl", "ChosenCtrl", "DefenderCtrl",
            "DefenderCtrlForRemembered", "DefendingPlayerCtrl",
            "EnchantedPlayerCtrl", "EnchantedControllerCtrl",
            "RememberedPlayer", "RememberedPlayerCtrl",
            "nonRememberedPlayerCtrl", "TargetedPlayerCtrl",
            "TargetedControllerCtrl", "ActivePlayerCtrl",
            "NonActivePlayerCtrl", "YouOwn", "YouDontOwn", "OppOwn",
            "TargetedPlayerOwn", "OwnerDoesntControl", "Other", "Self",
            "AttachedBy", "Attached", "NameNotEnchantingEnchantedPlayer",
            "NotAttachedTo", "Enchanted", "CanEnchantRemembered",
            "CanEnchantSource", "CanBeEnchantedBy", "CanBeEnchantedByTargeted",
            "CanBeEnchantedByAllRemembered", "EquippedBy",
            "EquippedByTargeted", "EquippedByEnchanted", "FortifiedBy",
            "CanBeEquippedBy", "Equipped", "Fortified", "HauntedBy",
            "notTributed", "madness", "Paired", "NotPaired", "PairedWith",
            "Above", "DirectlyAbove", "TopGraveyardCreature",
            "BottomGraveyard", "TopLibrary", "BottomLibrary", "Cloned", "DamagedBy", "Damaged",
            "sharesPermanentTypeWith", "canProduceSameManaTypeWith", "SecondSpellCastThisTurn",
            "ThisTurnCast", "withFlashback", "tapped", "untapped", "faceDown",
            "faceUp", "hasLevelUp", "DrawnThisTurn", "notDrawnThisTurn",
            "firstTurnControlled", "notFirstTurnControlled",
            "startedTheTurnUntapped", "attackedOrBlockedSinceYourLastUpkeep",
            "blockedOrBeenBlockedSinceYourLastUpkeep",
            "dealtDamageToYouThisTurn", "dealtDamageToOppThisTurn",
            "controllerWasDealtCombatDamageByThisTurn",
            "controllerWasDealtDamageByThisTurn", "wasDealtDamageThisTurn",
            "wasDealtDamageByHostThisTurn", "wasDealtDamageByEquipeeThisTurn",
            "wasDealtDamageByEnchantedThisTurn", "dealtDamageThisTurn",
            "attackedThisTurn", "attackedLastTurn", "blockedThisTurn",
            "gotBlockedThisTurn", "notAttackedThisTurn", "notAttackedLastTurn",
            "notBlockedThisTurn", "greatestPower", "yardGreatestPower",
            "leastPower", "leastToughness", "greatestCMC",
            "greatestRememberedCMC", "lowestRememberedCMC", "lowestCMC",
            "enchanted", "unenchanted", "enchanting", "equipped", "unequipped",
            "equipping", "token", "nonToken", "hasXCost", "suspended",
            "delved", "attacking", "attackingYou", "notattacking",
            "attackedBySourceThisCombat", "blocking", "blockingSource",
            "blockingCreatureYouCtrl", "blockingRemembered",
            "sharesBlockingAssignmentWith", "notblocking", "blocked",
            "blockedBySource", "blockedThisTurn", "blockedByThisTurn",
            "blockedBySourceThisTurn", "blockedSource",
            "isBlockedByRemembered", "blockedRemembered",
            "blockedByRemembered", "unblocked", "attackersBandedWith",
            "kicked", "kicked1", "kicked2", "notkicked", "evoked",
            "HasDevoured", "HasNotDevoured", "IsMonstrous", "IsNotMonstrous",
            "CostsPhyrexianMana", "IsRemembered", "IsNotRemembered",
            "IsImprinted", "IsNotImprinted", "hasActivatedAbilityWithTapCost",
            "hasActivatedAbility", "hasManaAbility",
            "hasNonManaActivatedAbility", "NoAbilities", "HasCounters",
            "wasNotCast", "ChosenType", "IsNotChosenType", "IsCommander",
            "IsNotCommander","IsRenowned", "IsNotRenowned");
    private static final Set<String> VALID_EXCLUSIVE_STARTSWITH = ImmutableSortedSet
            .of("named", "notnamed", "OwnedBy", "ControlledBy",
                    "ControllerControls", "AttachedTo", "EnchantedBy",
                    "NotEnchantedBy", "TopGraveyard", "SharesColorWith",
                    "MostProminentColor", "notSharesColorWith",
                    "sharesCreatureTypeWith", "sharesCardTypeWith",
                    "sharesNameWith", "doesNotShareNameWith",
                    "sharesControllerWith", "sharesOwnerWith",
                    "ThisTurnEntered", "ControlledByPlayerInTheDirection",
                    "sharesTypeWith", "hasKeyword", "with",
                    "greatestPowerControlledBy", "greatestCMCControlledBy",
                    "power", "toughness", "cmc", "totalPT", "counters", "non",
                    "RememberMap", "wasCastFrom", "wasNotCastFrom", "set",
                    "inZone", "HasSVar");

    private static boolean isValidExclusive(final String valid) {
        if (VALID_EXCLUSIVE.contains(valid)) {
            return true;
        }
        return Iterables.any(VALID_EXCLUSIVE_STARTSWITH, startsWith(valid));
    }

    private static final class KeyValuePair {
        private final String key, value;
        private final int index;

        private KeyValuePair(final String key, final String value, final int index) {
            this.key = key;
            this.value = value;
            this.index = index;
        }

        private String getKey() {
            return key;
        }
        private String getValue() {
            return value;
        }
        private int length() {
            return keyLength() + 1 + valueLength();
        }
        private int keyLength() {
            return key.length();
        }
        private int valueLength() {
            return value.length();
        }
        private int startIndex() {
            return index;
        }
        private int endIndexKey() {
            return startIndex() + key.length();
        }
        private int startIndexValue() {
            return endIndexKey() + 1;
        }
        private int endIndex() {
            return startIndex() + length();
        }
    }
}
