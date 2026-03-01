package forge.gui.card;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ICardFace;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.card.mana.ManaAtom;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Equip;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordAction;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordWithTypeInterface;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.util.collect.FCollectionView;

/**
 * Platform-neutral utility for building keyword info (names + reminder text)
 * from card data. Returns raw strings with {W}-style mana symbols — callers
 * are responsible for platform-specific symbol rendering (e.g. FSkin on desktop).
 */
public final class KeywordInfoUtil {

    private KeywordInfoUtil() { }

    /** A keyword name paired with its reminder text. Both are raw strings. */
    public static class KeywordData {
        public final String name;
        public final String reminderText;
        /** Valid type expression for type-parameterised keywords (e.g. Affinity). */
        public final String typeParam;

        public KeywordData(final String name, final String reminderText) {
            this(name, reminderText, null);
        }

        public KeywordData(final String name, final String reminderText,
                           final String typeParam) {
            this.name = name;
            this.reminderText = reminderText;
            this.typeParam = typeParam;
        }
    }

    /**
     * Parse a comma-separated keyword key string into keyword data entries.
     * @param keywordKey the comma-separated keyword string from CardStateView
     * @param addedNames tracks already-added keyword names (lowercase) to deduplicate
     * @return list of keyword data with raw name and reminder text
     */
    public static List<KeywordData> buildKeywords(final String keywordKey,
                                                   final Set<String> addedNames) {
        final String[] tokens = keywordKey.split(",");
        final java.util.Map<Keyword, Integer> seenIdx = new java.util.LinkedHashMap<>();
        final java.util.Map<Integer, String> rawTitles = new java.util.HashMap<>();
        final List<KeywordData> result = new ArrayList<>();

        for (final String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                final KeywordInterface inst = Keyword.getInstance(token);
                final Keyword kw = inst.getKeyword();
                if (kw == Keyword.UNDEFINED || kw == Keyword.ENCHANT) {
                    continue;
                }
                if (seenIdx.containsKey(kw) && kw != Keyword.EQUIP
                        && kw != Keyword.TRAMPLE) {
                    // Merge parameterised duplicates (e.g. multiple Protections)
                    final String title = inst.getTitle();
                    final String prefix = kw.toString() + " ";
                    if (title.startsWith(prefix)) {
                        final int idx = seenIdx.get(kw);
                        final String rawTitle = rawTitles.get(idx);
                        final String extra = title.substring(
                                kw.toString().length() + 1); // "from Dragon"
                        final String combined = rawTitle + " and " + extra;
                        rawTitles.put(idx, combined);
                        // Merge reminder text: append new "by X" subject
                        String mergedReminder = result.get(idx).reminderText;
                        try {
                            final String newReminder = inst.getReminderText();
                            final int byIdx = newReminder.lastIndexOf(" by ");
                            final int existByIdx = mergedReminder.lastIndexOf(".");
                            if (byIdx >= 0 && existByIdx >= 0) {
                                final String newSubject = newReminder.substring(
                                        byIdx + 4).replaceAll("\\.$", "");
                                mergedReminder = mergedReminder.substring(
                                        0, existByIdx) + " or " + newSubject + ".";
                            }
                        } catch (Exception ex) { /* keep existing */ }
                        result.set(idx, new KeywordData(
                                colorNamesToSymbols(combined),
                                mergedReminder,
                                result.get(idx).typeParam));
                    }
                    continue;
                }
                String reminderText;
                try {
                    reminderText = inst.getReminderText();
                } catch (Exception ex) {
                    reminderText = "";
                }
                final String title;
                if (kw == Keyword.EQUIP
                        && inst instanceof Equip) {
                    // Include type qualifier for non-default equip variants
                    // (e.g. "Equip commander {3}" vs plain "Equip {5}")
                    final String equipType = ((Equip) inst).getValidDescription();
                    if (!"creature".equals(equipType)) {
                        title = "Equip " + equipType + " " + inst.getTitle()
                                .substring(kw.toString().length()).trim();
                    } else {
                        title = inst.getTitle();
                    }
                } else {
                    title = inst.getTitle();
                }
                seenIdx.put(kw, result.size());
                rawTitles.put(result.size(), title);
                String typeParam = null;
                if (kw == Keyword.AFFINITY
                        && inst instanceof KeywordWithTypeInterface) {
                    typeParam = ((KeywordWithTypeInterface) inst)
                            .getValidType();
                }
                result.add(new KeywordData(
                        colorNamesToSymbols(title), reminderText, typeParam));
                addedNames.add(kw.toString().toLowerCase());
            } catch (Exception e) {
                // Skip malformed keyword tokens
            }
        }
        return result;
    }

    /**
     * Scan oracle text for keyword actions that aren't already shown as keyword
     * abilities, and append them to the keyword list.
     */
    public static void addKeywordActions(final List<KeywordData> result,
                                          final String oracleText,
                                          final Set<String> existingNames,
                                          final String cardName) {
        if (oracleText == null || oracleText.isEmpty()) {
            return;
        }
        // Strip card name to avoid false positives (e.g., "Boseiju, Who Endures")
        String lowerText = oracleText.toLowerCase();
        if (!cardName.isEmpty()) {
            lowerText = lowerText.replace(cardName.toLowerCase(), "");
        }
        // Collect matches with their position in the oracle text, then sort by position
        // so keyword actions appear top-to-bottom in card text order
        final List<int[]> pendingIndices = new ArrayList<>(); // [oraclePos, enumOrdinal]
        for (final KeywordAction action : KeywordAction.values()) {
            if (action.basic) {
                continue;
            }
            final String name = action.getDisplayName();
            if (existingNames.contains(name.toLowerCase())) {
                continue;
            }
            // Transform is redundant when Craft is present (craft explains the transformation)
            if (action == KeywordAction.TRANSFORM && existingNames.contains("craft")) {
                continue;
            }
            // Match whole word (case-insensitive): "goad", "goads", "goaded"
            final String lowerName = name.toLowerCase();
            // Try base form first, then -ies conjugation for -y verbs (scry → scries)
            String matchTerm = lowerName;
            if (!lowerText.contains(matchTerm) && lowerName.endsWith("y")) {
                matchTerm = lowerName.substring(0, lowerName.length() - 1) + "ies";
            }
            if (lowerText.contains(matchTerm)) {
                // Verify word boundaries: start must not be preceded by a letter,
                // and end must be followed by a verb suffix (s/d/ed/es/ing) or
                // non-letter — prevents "planeswalk" matching "planeswalker"
                int idx = lowerText.indexOf(matchTerm);
                while (idx >= 0) {
                    final boolean startOk = idx == 0
                            || !Character.isLetter(lowerText.charAt(idx - 1));
                    final int endIdx = idx + matchTerm.length();
                    final boolean endOk = endIdx >= lowerText.length()
                            || !Character.isLetter(lowerText.charAt(endIdx))
                            || isVerbSuffix(lowerText, endIdx);
                    if (startOk && endOk) {
                        pendingIndices.add(new int[]{idx, action.ordinal()});
                        existingNames.add(lowerName);
                        break;
                    }
                    idx = lowerText.indexOf(matchTerm, idx + 1);
                }
            }
        }
        pendingIndices.sort((a, b) -> Integer.compare(a[0], b[0]));
        final KeywordAction[] allActions = KeywordAction.values();
        for (final int[] pair : pendingIndices) {
            final KeywordAction action = allActions[pair[1]];
            final String lowerName = action.getDisplayName().toLowerCase();
            String displayName = action.getDisplayName();
            String reminder = action.getReminderText();
            if (reminder.contains("N")) {
                int pos = pair[0] + lowerName.length();
                // Skip inflected suffix letters (s, ed, ing, etc.)
                while (pos < lowerText.length()
                        && Character.isLetter(lowerText.charAt(pos))) {
                    pos++;
                }
                // Skip whitespace to reach the number (or a type word before it)
                if (pos < lowerText.length()
                        && lowerText.charAt(pos) == ' ') {
                    pos++;
                    String resolved = parseNumber(lowerText, pos);
                    // If no number found, a type word may precede it
                    // (e.g. "amass zombies 2") — capture the word and skip past it
                    String typeWord = null;
                    if (resolved == null) {
                        final int wordStart = pos;
                        while (pos < lowerText.length()
                                && Character.isLetter(lowerText.charAt(pos))) {
                            pos++;
                        }
                        if (pos > wordStart && pos < lowerText.length()
                                && lowerText.charAt(pos) == ' ') {
                            typeWord = lowerText.substring(wordStart, pos);
                            pos++;
                            resolved = parseNumber(lowerText, pos);
                        }
                    }
                    if (resolved != null) {
                        reminder = reminder.replace("N", resolved);
                        if (typeWord != null) {
                            // Capitalize type word for display
                            final String capType = Character.toUpperCase(
                                    typeWord.charAt(0)) + typeWord.substring(1);
                            displayName = displayName + " " + capType
                                    + " " + resolved;
                        } else {
                            displayName = displayName + " " + resolved;
                        }
                        if ("1".equals(resolved)) {
                            reminder = reminder.replace("counters", "counter")
                                    .replace("cards", "card");
                        }
                    } else {
                        // Can't resolve number (e.g. "mill half their library")
                        // — omit the N so text still reads naturally
                        reminder = reminder.replace("N ", "")
                                .replace(" N", "");
                    }
                }
            }
            result.add(new KeywordData(displayName, reminder));
        }
    }

    /**
     * Cross-check CardStateView boolean keyword flags against already-parsed
     * keywords. Adds any keywords whose flag is true but were missed by
     * keywordKey string parsing.
     */
    public static void addMissingKeywordsFromFlags(final List<KeywordData> result,
                                                    final CardStateView state,
                                                    final Set<String> addedNames) {
        if (state == null) {
            return;
        }
        addFlagKeyword(result, addedNames, state.hasFlying(), Keyword.FLYING);
        addFlagKeyword(result, addedNames, state.hasFirstStrike(), Keyword.FIRST_STRIKE);
        addFlagKeyword(result, addedNames, state.hasDoubleStrike(), Keyword.DOUBLE_STRIKE);
        addFlagKeyword(result, addedNames, state.hasDeathtouch(), Keyword.DEATHTOUCH);
        addFlagKeyword(result, addedNames, state.hasDefender(), Keyword.DEFENDER);
        addFlagKeyword(result, addedNames, state.hasFear(), Keyword.FEAR);
        addFlagKeyword(result, addedNames, state.hasHaste(), Keyword.HASTE);
        addFlagKeyword(result, addedNames, state.hasHexproof(), Keyword.HEXPROOF);
        addFlagKeyword(result, addedNames, state.hasIndestructible(), Keyword.INDESTRUCTIBLE);
        addFlagKeyword(result, addedNames, state.hasIntimidate(), Keyword.INTIMIDATE);
        addFlagKeyword(result, addedNames, state.hasLifelink(), Keyword.LIFELINK);
        addFlagKeyword(result, addedNames, state.hasMenace(), Keyword.MENACE);
        addFlagKeyword(result, addedNames, state.hasReach(), Keyword.REACH);
        addFlagKeyword(result, addedNames, state.hasShadow(), Keyword.SHADOW);
        addFlagKeyword(result, addedNames, state.hasShroud(), Keyword.SHROUD);
        addFlagKeyword(result, addedNames, state.hasTrample(), Keyword.TRAMPLE);
        addFlagKeyword(result, addedNames, state.hasVigilance(), Keyword.VIGILANCE);
        addFlagKeyword(result, addedNames, state.hasInfect(), Keyword.INFECT);
        addFlagKeyword(result, addedNames, state.hasWither(), Keyword.WITHER);
        addFlagKeyword(result, addedNames, state.hasHorsemanship(), Keyword.HORSEMANSHIP);
    }

    /**
     * Sort keywords so they appear in the same order they are mentioned in the
     * oracle text, rather than alphabetical or parse order.
     */
    public static void sortByOracleText(final List<KeywordData> keywords,
                                         final String oracleText) {
        if (oracleText == null || oracleText.isEmpty() || keywords.size() <= 1) {
            return;
        }
        final String lowerText = oracleText.toLowerCase();
        keywords.sort((a, b) -> {
            final int posA = findKeywordPosition(lowerText, a.name);
            final int posB = findKeywordPosition(lowerText, b.name);
            return Integer.compare(posA, posB);
        });
    }

    /**
     * Post-process keyword entries to append dynamic count annotations where
     * applicable (e.g. Affinity, Devotion, Domain, Metalcraft, Threshold,
     * Delirium). Counts are computed client-side from view objects.
     * Modifies both the keyword name (short count) and reminder text (detail).
     */
    public static void annotateKeywordCounts(final List<KeywordData> keywords,
                                              final CardView cardView) {
        if (keywords.isEmpty() || cardView == null) {
            return;
        }
        final PlayerView controller = cardView.getController();
        if (controller == null || controller.getBattlefield() == null
                || controller.getGraveyard() == null) {
            return;
        }
        final Localizer localizer = Localizer.getInstance();
        for (int i = 0; i < keywords.size(); i++) {
            final KeywordData kw = keywords.get(i);
            final String lowerName = kw.name.toLowerCase()
                    .replaceAll("\\{[^}]+}", "").trim();
            KeywordData annotated = null;

            if (lowerName.startsWith("affinity for ")) {
                annotated = annotateAffinity(kw, lowerName, controller, localizer);
            } else if (lowerName.equals("devotion")) {
                annotated = annotateDevotion(kw, cardView, controller, localizer);
            } else if (lowerName.equals("domain")) {
                annotated = annotateDomain(kw, controller, localizer);
            } else if (lowerName.equals("metalcraft")) {
                annotated = annotateMetalcraft(kw, controller, localizer);
            } else if (lowerName.equals("threshold")) {
                annotated = annotateThreshold(kw, controller, localizer);
            } else if (lowerName.equals("delirium")) {
                annotated = annotateDelirium(kw, controller, localizer);
            } else if (lowerName.equals("the ring tempts you")) {
                annotated = annotateRingLevel(kw, controller);
            }

            if (annotated != null) {
                keywords.set(i, annotated);
            }
        }
    }

    private static KeywordData annotateAffinity(final KeywordData kw,
                                                 final String lowerName,
                                                 final PlayerView controller,
                                                 final Localizer localizer) {
        // Use typeParam (valid type expression from keyword parser) for
        // accurate matching — properly cased for hasStringType.
        String matchType = kw.typeParam;
        if (matchType == null) {
            // Fallback: extract from display name (works for core types only)
            matchType = lowerName.substring("affinity for ".length()).trim();
            if (matchType.endsWith("s")) {
                matchType = matchType.substring(0, matchType.length() - 1);
            }
        }
        int count = 0;
        for (final CardView c : controller.getBattlefield()) {
            if (cardMatchesAffinityType(c, matchType)) {
                count++;
            }
        }
        // Build display text from typeParam (properly cased singular),
        // falling back to parsing the title
        final String singular;
        if (kw.typeParam != null) {
            singular = kw.typeParam.toLowerCase();
        } else {
            final String typeText = lowerName.substring(
                    "affinity for ".length()).trim();
            singular = typeText.endsWith("s")
                    ? typeText.substring(0, typeText.length() - 1) : typeText;
        }
        // Types already ending in "s" (e.g. "plains") are their own plural
        final String displayType = count == 1 ? singular
                : (singular.endsWith("s") ? singular : singular + "s");
        final String reminder = localizer.getMessage("lblAffinityCount",
                count, displayType);
        return new KeywordData(kw.name + " (" + count + ")",
                appendAnnotation(kw.reminderText, reminder), kw.typeParam);
    }

    /** Check whether a card matches an Affinity type expression. */
    private static boolean cardMatchesAffinityType(final CardView card,
                                                    final String typeExpr) {
        final CardStateView st = card.getCurrentState();
        if (st == null || st.getType() == null) {
            return false;
        }
        if (!typeExpr.contains(".")) {
            return st.getType().hasStringType(typeExpr);
        }
        // Compound expression: all dot-separated parts must match
        for (final String part : typeExpr.split("\\.")) {
            if (!matchTypePart(card, st, part)) {
                return false;
            }
        }
        return true;
    }

    /** Match a single component of a dot-separated type expression. */
    private static boolean matchTypePart(final CardView card,
                                          final CardStateView st,
                                          final String part) {
        // "Permanent" — any card on the battlefield qualifies
        if ("Permanent".equalsIgnoreCase(part)) {
            return true;
        }
        // "token" — check CardView flag
        if ("token".equalsIgnoreCase(part)) {
            return card.isToken();
        }
        // "Historic" — artifact, legendary, or Saga
        if ("Historic".equalsIgnoreCase(part)) {
            return st.getType().isArtifact()
                    || st.getType().hasSupertype(
                            CardType.Supertype.Legendary)
                    || st.getType().hasSubtype("Saga");
        }
        // "Outlaw" — Assassin, Mercenary, Pirate, Rogue, or Warlock
        if ("Outlaw".equalsIgnoreCase(part)) {
            final CardTypeView t = st.getType();
            return t.hasCreatureType("Assassin")
                    || t.hasCreatureType("Mercenary")
                    || t.hasCreatureType("Pirate")
                    || t.hasCreatureType("Rogue")
                    || t.hasCreatureType("Warlock");
        }
        // "withAffinity" — check keyword key for "Affinity"
        if ("withAffinity".equalsIgnoreCase(part)) {
            final String keys = st.getKeywordKey();
            return keys != null
                    && keys.toLowerCase().contains("affinity");
        }
        // Standard type/subtype/supertype
        return st.getType().hasStringType(part);
    }

    private static KeywordData annotateDevotion(final KeywordData kw,
                                                 final CardView cardView,
                                                 final PlayerView controller,
                                                 final Localizer localizer) {
        // Look up devotion colors from the card's SVars (handles both single
        // and dual devotion correctly, including hybrid mana counting)
        String color1 = null;
        String color2 = null;
        try {
            final StaticData data = StaticData.instance();
            if (data != null) {
                final CardRules rules = data.getCommonCards()
                        .getRules(cardView.getOracleName());
                if (rules != null) {
                    final ICardFace face = rules.getMainPart();
                    if (face != null && face.getVariables() != null) {
                        for (final Map.Entry<String, String> svar
                                : face.getVariables()) {
                            final String val = svar.getValue();
                            if (val.startsWith("Count$DevotionDual.")) {
                                final String[] parts = val.split("\\.");
                                if (parts.length >= 3) {
                                    color1 = parts[1];
                                    color2 = parts[2];
                                }
                                break;
                            } else if (val.startsWith("Count$Devotion.")) {
                                final String[] parts = val.split("\\.");
                                if (parts.length >= 2) {
                                    color1 = parts[1];
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to oracle text fallback
        }

        // Fallback: parse oracle text for single-color devotion
        if (color1 == null) {
            final CardStateView state = cardView.getCurrentState();
            if (state == null) {
                return null;
            }
            final String oracle = state.getOracleText();
            if (oracle == null) {
                return null;
            }
            final String lowerOracle = oracle.toLowerCase();
            final String[] colorNames = {
                "white", "blue", "black", "red", "green"
            };
            for (final String name : colorNames) {
                if (lowerOracle.contains("devotion to " + name)) {
                    color1 = name;
                    break;
                }
            }
        }
        if (color1 == null) {
            return null;
        }

        // Build combined color code (handles both single and dual)
        byte colorCode = ManaAtom.fromName(color1);
        if (color2 != null) {
            colorCode |= ManaAtom.fromName(color2);
        }

        // Count devotion using isColor — correctly counts hybrid mana once
        int devotion = 0;
        for (final CardView c : controller.getBattlefield()) {
            final CardStateView st = c.getCurrentState();
            if (st == null) {
                continue;
            }
            final ManaCost cost = st.getManaCost();
            if (cost == null) {
                continue;
            }
            for (final ManaCostShard shard : cost) {
                if (shard.isColor(colorCode)) {
                    devotion++;
                }
            }
        }

        // Format display strings — use mana symbols in both header and reminder
        final String symbol1 = MagicColor.toSymbol(color1);
        if (color2 != null) {
            final String symbol2 = MagicColor.toSymbol(color2);
            final String newName = localizer.getMessage(
                    "lblDevotionDualCountTitle",
                    symbol1, symbol2, devotion);
            final String reminder = localizer.getMessage(
                    "lblDevotionDualCount",
                    symbol1, symbol2, devotion);
            return new KeywordData(newName,
                    appendAnnotation(kw.reminderText, reminder));
        }
        final String newName = localizer.getMessage("lblDevotionCountTitle",
                symbol1, devotion);
        final String reminder = localizer.getMessage("lblDevotionCount",
                symbol1, devotion);
        return new KeywordData(newName,
                appendAnnotation(kw.reminderText, reminder));
    }

    private static KeywordData annotateDomain(final KeywordData kw,
                                               final PlayerView controller,
                                               final Localizer localizer) {
        final String[] basicLandTypes = {
            "Plains", "Island", "Swamp", "Mountain", "Forest"
        };
        final Set<String> found = new HashSet<>();
        for (final CardView c : controller.getBattlefield()) {
            final CardStateView st = c.getCurrentState();
            if (st == null || st.getType() == null) {
                continue;
            }
            final CardTypeView type = st.getType();
            if (!type.isLand()) {
                continue;
            }
            for (final String blt : basicLandTypes) {
                if (type.hasSubtype(blt)) {
                    found.add(blt);
                }
            }
        }
        final int count = found.size();
        final String reminder = localizer.getMessage("lblDomainCount",
                count, count == 1 ? "" : "s");
        return new KeywordData(kw.name + " (" + count + ")",
                appendAnnotation(kw.reminderText, reminder));
    }

    private static KeywordData annotateMetalcraft(final KeywordData kw,
                                                   final PlayerView controller,
                                                   final Localizer localizer) {
        int count = 0;
        for (final CardView c : controller.getBattlefield()) {
            final CardStateView st = c.getCurrentState();
            if (st != null && st.getType() != null && st.getType().isArtifact()) {
                count++;
            }
        }
        final String reminder = localizer.getMessage("lblMetalcraftCount",
                count, count == 1 ? "" : "s");
        return new KeywordData(kw.name + " (" + count + ")",
                appendAnnotation(kw.reminderText, reminder));
    }

    private static KeywordData annotateThreshold(final KeywordData kw,
                                                  final PlayerView controller,
                                                  final Localizer localizer) {
        final int count = controller.getGraveyard().size();
        final String reminder = localizer.getMessage("lblThresholdCount",
                count, count == 1 ? "" : "s");
        return new KeywordData(kw.name + " (" + count + ")",
                appendAnnotation(kw.reminderText, reminder));
    }

    private static KeywordData annotateDelirium(final KeywordData kw,
                                                 final PlayerView controller,
                                                 final Localizer localizer) {
        final Set<CardType.CoreType> types = new HashSet<>();
        for (final CardView c : controller.getGraveyard()) {
            final CardStateView st = c.getCurrentState();
            if (st == null || st.getType() == null) {
                continue;
            }
            for (final CardType.CoreType ct : st.getType().getCoreTypes()) {
                types.add(ct);
            }
        }
        final int count = types.size();
        final String reminder = localizer.getMessage("lblDeliriumCount",
                count, count == 1 ? "" : "s");
        return new KeywordData(kw.name + " (" + count + ")",
                appendAnnotation(kw.reminderText, reminder));
    }

    private static KeywordData annotateRingLevel(final KeywordData kw,
                                                  final PlayerView controller) {
        final FCollectionView<CardView> commandZone =
                controller.getCards(ZoneType.Command);
        if (commandZone == null) {
            return null;
        }
        for (final CardView c : commandZone) {
            final int level = c.getRingLevel();
            if (level > 0) {
                final String annotation = "(Currently at level " + level + ")";
                return new KeywordData(kw.name + " (" + level + ")",
                        appendAnnotation(kw.reminderText, annotation));
            }
        }
        return null;
    }

    private static String appendAnnotation(final String reminderText,
                                              final String annotation) {
        return reminderText.isEmpty() ? annotation
                : reminderText + " " + annotation;
    }

    // --- Private helpers ---

    private static void addFlagKeyword(final List<KeywordData> result,
                                        final Set<String> addedNames,
                                        final boolean hasFlag, final Keyword kw) {
        if (!hasFlag) {
            return;
        }
        if (addedNames.contains(kw.toString().toLowerCase())) {
            return;
        }
        addedNames.add(kw.toString().toLowerCase());
        result.add(new KeywordData(kw.toString(), kw.getReminderText()));
    }

    private static int findKeywordPosition(final String lowerOracleText,
                                            final String keywordName) {
        // Strip markup (e.g. {W} symbols) to get plain text for matching
        final String plain = keywordName.replaceAll("\\{[^}]+}", "").trim().toLowerCase();
        int pos = lowerOracleText.indexOf(plain);
        if (pos >= 0) {
            return pos;
        }
        // Try first word only (e.g. "bestow" from "Bestow {2}{W}{U}{B}{R}{G}")
        final int space = plain.indexOf(' ');
        if (space > 0) {
            pos = lowerOracleText.indexOf(plain.substring(0, space));
            if (pos >= 0) {
                return pos;
            }
        }
        return Integer.MAX_VALUE;
    }

    /** Replace standalone MTG color names with mana symbols for display. */
    private static String colorNamesToSymbols(final String text) {
        // Use word boundaries so "red" inside "multicolored" isn't matched
        return text
                .replaceAll("\\b[Ww]hite\\b", "{W}")
                .replaceAll("\\b[Bb]lue\\b", "{U}")
                .replaceAll("\\b[Bb]lack\\b", "{B}")
                .replaceAll("\\b[Rr]ed\\b", "{R}")
                .replaceAll("\\b[Gg]reen\\b", "{G}");
    }

    /** Parse a digit or number word at the given position. Returns the digit string or null. */
    private static String parseNumber(final String text, final int pos) {
        // Try digits first: "3", "10"
        int numEnd = pos;
        while (numEnd < text.length() && Character.isDigit(text.charAt(numEnd))) {
            numEnd++;
        }
        if (numEnd > pos) {
            return text.substring(pos, numEnd);
        }
        // Try "a"/"an" as 1 (e.g. "mill a card", "create an artifact")
        if (text.startsWith("a ", pos) || text.startsWith("an ", pos)) {
            return "1";
        }
        // Try variable "X" (e.g. "monstrosity x", "collect evidence x")
        if (text.startsWith("x", pos)) {
            final int end = pos + 1;
            if (end >= text.length() || !Character.isLetter(text.charAt(end))) {
                return "X";
            }
        }
        // Try number words: "one" through "twenty"
        final String[] words = {
            "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
            "sixteen", "seventeen", "eighteen", "nineteen", "twenty"
        };
        for (int i = 0; i < words.length; i++) {
            if (text.startsWith(words[i], pos)) {
                final int end = pos + words[i].length();
                if (end >= text.length() || !Character.isLetter(text.charAt(end))) {
                    return String.valueOf(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * Check if the text at {@code pos} starts with a common English verb suffix
     * (s, d, ed, es, ing) followed by a non-letter. This allows matching
     * "goads"/"goaded"/"goading" but rejects "planeswalker" (suffix "er").
     */
    private static boolean isVerbSuffix(final String text, final int pos) {
        final String[] suffixes = {"ing", "ed", "es", "s", "d"};
        for (final String suffix : suffixes) {
            if (text.startsWith(suffix, pos)) {
                final int end = pos + suffix.length();
                if (end >= text.length() || !Character.isLetter(text.charAt(end))) {
                    return true;
                }
            }
        }
        return false;
    }
}
