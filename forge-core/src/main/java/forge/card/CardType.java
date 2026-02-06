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
package forge.card;

import com.google.common.collect.*;

import forge.util.ITranslatable;
import forge.util.Localizer;
import forge.util.Settable;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * <p>
 * Immutable Card type. Can be built only from parsing a string.
 * </p>
 *
 * @author Forge
 */
public final class CardType implements Comparable<CardType>, CardTypeView {
    private static final long serialVersionUID = 4629853583167022151L;

    public static final CardTypeView EMPTY = new CardType(false);

    public enum CoreType implements ITranslatable {
        Kindred(false, "kindreds", "lblKindred"), // always printed first
        Artifact(true, "artifacts", "lblArtifact"),
        Battle(true, "battles", "lblBattle"),
        Conspiracy(false, "conspiracies", "lblConspiracy"),
        Enchantment(true, "enchantments", "lblEnchantment"),
        Creature(true, "creatures", "lblCreature"),
        Dungeon(false, "dungeons", "lblDungeon"),
        Instant(false, "instants", "lblInstant"),
        Land(true, "lands", "lblLand"),
        Phenomenon(false, "phenomenons", "lblPhenomenon"),
        Plane(false, "planes", "lblPlane"),
        Planeswalker(true, "planeswalkers", "lblPlaneswalker"),
        Scheme(false, "schemes", "lblScheme"),
        Sorcery(false, "sorceries", "lblSorcery"),
        Vanguard(false, "vanguards", "lblVanguard");

        public final boolean isPermanent;
        public final String pluralName;
        public final String label;
        private static Map<String, CoreType> stringToCoreType = EnumUtils.getEnumMap(CoreType.class);
        private static final Set<String> allCoreTypeNames = stringToCoreType.keySet();
        public static final Set<CoreType> spellTypes = ImmutableSet.of(Instant, Sorcery);

        public static CoreType getEnum(String name) {
            return stringToCoreType.get(name);
        }

        public static boolean isValidEnum(String name) {
            return stringToCoreType.containsKey(name);
        }

        CoreType(final boolean permanent, final String plural, final String label) {
            isPermanent = permanent;
            pluralName = plural;
            this.label = label;
        }

        /**
         * Converts this core type to whichever GamePieceType is typical of it.
         * Be aware that this will not catch GamePieceTypes derived from subtypes,
         * such as Attractions.
         * @return a GamePieceType appropriate for this core type.
         */
        public GamePieceType toGamePieceType() {
            return switch (this) {
            case Plane, Phenomenon -> GamePieceType.PLANAR;
            case Scheme -> GamePieceType.SCHEME;
            case Dungeon -> GamePieceType.DUNGEON;
            case Vanguard -> GamePieceType.AVATAR;
            default -> GamePieceType.CARD;
            };
        }

        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public String getTranslatedName() {
            return Localizer.getInstance().getMessage(label);
        }
    }

    public enum Supertype implements ITranslatable {
        Basic("lblBasic"),
        Elite("lblElite"),
        Host("lblHost"),
        Legendary("lblLegendary"),
        Snow("lblSnow"),
        Ongoing("lblOngoing"),
        World("lblWorld");

        public final String label;

        private static Map<String, Supertype> stringToSupertype = EnumUtils.getEnumMap(Supertype.class);

        public static Supertype getEnum(String name) {
            return stringToSupertype.get(name);
        }

        public static boolean isValidEnum(String name) {
            return stringToSupertype.containsKey(name);
        }

        Supertype(final String label) {
            this.label = label;
        }


        @Override
        public String getName() {
            return this.name();
        }

        @Override
        public String getTranslatedName() {
            return Localizer.getInstance().getMessage(label);
        }
    }

    protected final Set<CoreType> coreTypes = EnumSet.noneOf(CoreType.class);
    protected final Set<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
    protected final Set<String> subtypes = Sets.newLinkedHashSet();
    protected boolean allCreatureTypes = false;
    protected final Set<String> excludedCreatureSubtypes = Sets.newLinkedHashSet();

    private boolean incomplete = false;
    private transient String calculatedType = null;

    public CardType(boolean incomplete) {
        this.incomplete = incomplete;
    }
    public CardType(final Iterable<String> from0, boolean incomplete) {
        this.incomplete = incomplete;
        addAll(from0);
    }
    public CardType(final CardType from0) {
        addAll(from0);
        allCreatureTypes = from0.allCreatureTypes;
        excludedCreatureSubtypes.addAll(from0.excludedCreatureSubtypes);
    }
    public CardType(final CardTypeView from0) {
        addAll(from0);
    }

    public boolean add(final String t) {
        boolean changed;
        final CoreType ct = CoreType.getEnum(t);
        if (ct != null) {
            changed = coreTypes.add(ct);
        } else {
            final Supertype st = Supertype.getEnum(t);
            if (st != null) {
                changed = supertypes.add(st);
            } else {
                // If not recognized by super- and core- this must be subtype
                changed = subtypes.add(t);
            }
        }
        if (changed) {
            calculatedType = null; //ensure this is recalculated
            return true;
        }
        return false;
    }
    public boolean addAll(final Iterable<String> types) {
        if (types == null) {
            return false;
        }
        boolean changed = false;
        for (final String t : types) {
            if (add(t)) {
                changed = true;
            }
        }
        sanisfySubtypes();
        return changed;
    }
    public boolean addAll(final CardType type) {
        boolean changed = false;
        if (coreTypes.addAll(type.coreTypes)) { changed = true; }
        if (supertypes.addAll(type.supertypes)) { changed = true; }
        if (subtypes.addAll(type.subtypes)) { changed = true; }
        sanisfySubtypes();
        return changed;
    }
    public boolean addAll(final CardTypeView type) {
        boolean changed = false;
        if (Iterables.addAll(coreTypes, type.getCoreTypes())) { changed = true; }
        if (Iterables.addAll(supertypes, type.getSupertypes())) { changed = true; }
        if (Iterables.addAll(subtypes, type.getSubtypes())) { changed = true; }
        sanisfySubtypes();
        return changed;
    }

    public boolean removeAll(final CardTypeView type) {
        boolean changed = false;
        if (coreTypes.removeAll(type.getCoreTypes())) { changed = true; }
        if (supertypes.removeAll(type.getSupertypes())) { changed = true; }
        if (subtypes.removeAll(type.getSubtypes())) { changed = true; }
        if (changed) {
            sanisfySubtypes();
            calculatedType = null;
            return true;
        }
        return false;
    }

    public void clear() {
        if (isEmpty()) { return; }
        coreTypes.clear();
        supertypes.clear();
        subtypes.clear();
        calculatedType = null;
    }

    public void removeCardTypes() {
        coreTypes.clear();
    }

    public boolean remove(final Supertype st) {
        return supertypes.remove(st);
    }

    public boolean remove(final String str) {
        boolean changed = false;

        // try to remove sub type first if able
        if (subtypes.remove(str)) {
            changed = true;
        } else {
            Supertype st = Supertype.getEnum(str);
            if (st != null && supertypes.remove(st)) {
                changed = true;
            }
            CoreType ct = CoreType.getEnum(str);
            if (ct != null && coreTypes.remove(ct)) {
                changed = true;
            }
        }

        if (changed) {
            sanisfySubtypes();
            calculatedType = null;
        }
        return changed;
    }

    public boolean setCreatureTypes(Collection<String> ctypes) {
        // if it isn't a creature then this has no effect
        if (!isCreature() && !isKindred()) {
            return false;
        }
        boolean changed = subtypes.removeIf(CardType::isACreatureType);
        // need to remove AllCreatureTypes too when setting Creature Type
        if (allCreatureTypes) {
            changed = true;
        }
        allCreatureTypes = false;
        subtypes.addAll(ctypes);
        return changed;
    }

    @Override
    public boolean isEmpty() {
        return coreTypes.isEmpty() && supertypes.isEmpty() && subtypes.isEmpty() && excludedCreatureSubtypes.isEmpty();
    }

    @Override
    public Collection<CoreType> getCoreTypes() {
        return coreTypes;
    }
    @Override
    public Collection<Supertype> getSupertypes() {
        return supertypes;
    }
    @Override
    public Collection<String> getSubtypes() {
        return subtypes;
    }

    @Override
    public Iterable<String> getExcludedCreatureSubTypes() {
        return excludedCreatureSubtypes;
    }

    @Override
    public Set<String> getCreatureTypes() {
        final Set<String> creatureTypes = Sets.newLinkedHashSet();
        if (!isCreature() && !isKindred()) {
            return creatureTypes;
        }
        if (hasAllCreatureTypes()) { // it should return list of all creature types
            creatureTypes.addAll(getAllCreatureTypes());
            creatureTypes.removeAll(this.excludedCreatureSubtypes);
        } else {
            subtypes.stream().filter(CardType::isACreatureType).forEach(creatureTypes::add);
        }
        return creatureTypes;
    }

    @Override
    public Set<String> getLandTypes() {
        final Set<String> landTypes = Sets.newLinkedHashSet();
        if (isLand()) {
            for (final String t : subtypes) {
                if (isALandType(t)) {
                    landTypes.add(t);
                }
            }
        }
        return landTypes;
    }

    public Set<String> getBattleTypes() {
        if(!isBattle())
            return Set.of();
        return subtypes.stream().filter(CardType::isABattleType).collect(Collectors.toSet());
    }

    @Override
    public boolean hasStringType(String t) {
        if (t.isEmpty()) {
            return false;
        }
        if (hasSubtype(t)) {
            return true;
        }

        t = StringUtils.capitalize(t);
        final CoreType type = CoreType.getEnum(t);
        if (type != null) {
            return hasType(type);
        }
        final Supertype supertype = Supertype.getEnum(t);
        if (supertype != null) {
            return hasSupertype(supertype);
        }
        return false;
    }

    @Override
    public boolean hasType(final CoreType type) {
        return coreTypes.contains(type);
    }

    @Override
    public boolean hasSupertype(final Supertype supertype) {
        return supertypes.contains(supertype);
    }

    @Override
    public boolean hasAllCreatureTypes() {
        if (!isCreature() && !isKindred()) { return false; }
        return this.allCreatureTypes;
    }

    @Override
    public boolean hasSubtype(final String subtype) {
        if (hasCreatureType(subtype)) {
            return true;
        }
        return subtypes.contains(subtype);
    }

    @Override
    public boolean hasCreatureType(String creatureType) {
        if (!isCreature() && !isKindred()) { return false; }

        creatureType = toMixedCase(creatureType);
        if (!isACreatureType(creatureType)) { return false; }

        if (excludedCreatureSubtypes.contains(creatureType)) {
            return false;
        }
        if (allCreatureTypes) {
            return true;
        }
        return subtypes.contains(creatureType);
    }
    private static String toMixedCase(final String s) {
        if (s.isEmpty()) {
            return s;
        }
        final StringBuilder sb = new StringBuilder();
        // to handle hyphenated Types
        // TODO checkout WordUtils for this
        final String[] types = s.split("-");
        for (int i = 0; i < types.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            sb.append(StringUtils.capitalize(types[i]));
        }
        return sb.toString();
    }

    @Override
    public boolean hasABasicLandType() {
        return this.subtypes.stream().anyMatch(CardType::isABasicLandType);
    }
    @Override
    public boolean hasANonBasicLandType() {
        return !Collections.disjoint(this.subtypes, getNonBasicTypes());
    }

    @Override
    public boolean isPermanent() {
        for (final CoreType type : coreTypes) {
            if (type.isPermanent) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCreature() {
        return coreTypes.contains(CoreType.Creature);
    }

    @Override
    public boolean isPlaneswalker() {
        return coreTypes.contains(CoreType.Planeswalker);
    }

    @Override
    public boolean isBattle() {
        return coreTypes.contains(CoreType.Battle);
    }

    @Override
    public boolean isLand() {
        return coreTypes.contains(CoreType.Land);
    }

    @Override
    public boolean isArtifact() {
        return coreTypes.contains(CoreType.Artifact);
    }

    @Override
    public boolean isInstant() {
        return coreTypes.contains(CoreType.Instant);
    }

    @Override
    public boolean isSorcery() {
        return coreTypes.contains(CoreType.Sorcery);
    }

    @Override
    public boolean isConspiracy() {
        return coreTypes.contains(CoreType.Conspiracy);
    }

    @Override
    public boolean isVanguard() {
        return coreTypes.contains(CoreType.Vanguard);
    }

    @Override
    public boolean isScheme() {
        return coreTypes.contains(CoreType.Scheme);
    }

    @Override
    public boolean isEnchantment() {
        return coreTypes.contains(CoreType.Enchantment);
    }

    @Override
    public boolean isBasic() {
        return supertypes.contains(Supertype.Basic);
    }

    @Override
    public boolean isLegendary() {
        return supertypes.contains(Supertype.Legendary);
    }

    @Override
    public boolean isSnow() {
        return supertypes.contains(Supertype.Snow);
    }

    @Override
    public boolean isBasicLand() {
        return isBasic() && isLand();
    }

    @Override
    public boolean isPlane() {
        return coreTypes.contains(CoreType.Plane);
    }

    @Override
    public boolean isPhenomenon() {
        return coreTypes.contains(CoreType.Phenomenon);
    }

    @Override
    public boolean isKindred() {
        return coreTypes.contains(CoreType.Kindred);
    }

    @Override
    public boolean isDungeon() {
        return coreTypes.contains(CoreType.Dungeon);
    }

    @Override
    public boolean isAttachment() { return isAura() || isEquipment() || isFortification(); }
    @Override
    public boolean isAura() { return hasSubtype("Aura"); }
    @Override
    public boolean isEquipment()  { return hasSubtype("Equipment"); }
    @Override
    public boolean isFortification()  { return hasSubtype("Fortification"); }
    public boolean isAttraction() {
        return hasSubtype("Attraction");
    }

    public boolean isContraption() {
        return hasSubtype("Contraption");
    }

    public boolean isVehicle() { return hasSubtype("Vehicle"); }
    public boolean isSpacecraft() { return hasSubtype("Spacecraft"); }

    @Override
    public boolean isSaga() {
        return hasSubtype("Saga");
    }

    @Override
    public boolean isHistoric() {
        return isLegendary() || isArtifact() || isSaga();
    }

    @Override
    public boolean isOutlaw() {
        if (!isCreature() && !isKindred()) {
            return false;
        }
        return Constant.OUTLAW_TYPES.stream().anyMatch(s -> hasCreatureType(s));
    }
    @Override
    public boolean isParty() {
        if (!isCreature() && !isKindred()) {
            return false;
        }
        return Constant.PARTY_TYPES.stream().anyMatch(s -> hasCreatureType(s));
    }

    @Override
    public String toString() {
        if (calculatedType == null) {
            StringBuilder sb = new StringBuilder(StringUtils.join(getTypesBeforeDash(), ' '));
            if (!subtypes.isEmpty() || hasAllCreatureTypes()) {
                sb.append(" - ");
            }
            if (!subtypes.isEmpty()) {
                sb.append(StringUtils.join(subtypes, " "));
            }
            if (hasAllCreatureTypes()) {
                if (!subtypes.isEmpty()) {
                    sb.append(" ");
                }
                sb.append("(All");
                if (!excludedCreatureSubtypes.isEmpty()) {
                    sb.append(" except ").append(StringUtils.join(excludedCreatureSubtypes, " "));
                }
                sb.append(")");
            }

            calculatedType = sb.toString();
        }
        return calculatedType;
    }

    private Set<String> getTypesBeforeDash() {
        final Set<String> types = Sets.newLinkedHashSet();
        for (final Supertype st : supertypes) {
            types.add(st.name());
        }
        for (final CoreType ct : coreTypes) {
            types.add(ct.name());
        }
        return types;
    }

    @Override
    public CardTypeView getTypeWithChanges(final Iterable<ICardChangedType> changedCardTypes) {
        if (Iterables.isEmpty(changedCardTypes)) {
            return this;
        }

        CardType newType = new CardType(CardType.this);
        // we assume that changes are already correctly ordered (taken from TreeMap.values())
        for (final ICardChangedType ct : changedCardTypes) {
            newType = ct.applyChanges(newType);
        }
        // sanisfy subtypes
        if (!newType.subtypes.isEmpty()) {
            newType.sanisfySubtypes();
        }
        return newType;
    }

    public void sanisfySubtypes() {
        // incomplete types are used for changing effects
        if (this.incomplete) {
            return;
        }
        if (!isCreature() && !isKindred()) {
            allCreatureTypes = false;
        }
        if (subtypes.isEmpty()) {
            return;
        }
        Predicate<String> allowedTypes = x -> false;
        if (isCreature() || isKindred()) {
            allowedTypes = allowedTypes.or(CardType::isACreatureType);
        }
        if (isLand()) {
            allowedTypes = allowedTypes.or(CardType::isALandType);
        }
        if (isArtifact()) {
            allowedTypes = allowedTypes.or(CardType::isAnArtifactType);
        }
        if (isEnchantment()) {
            allowedTypes = allowedTypes.or(CardType::isAnEnchantmentType);
        }
        if (isInstant() || isSorcery()) {
            allowedTypes = allowedTypes.or(CardType::isASpellType);
        }
        if (isPlaneswalker()) {
            allowedTypes = allowedTypes.or(CardType::isAPlaneswalkerType);
        }
        if (isDungeon()) {
            allowedTypes = allowedTypes.or(CardType::isADungeonType);
        }
        if (isBattle()) {
            allowedTypes = allowedTypes.or(CardType::isABattleType);
        }
        if (isPlane()) {
            allowedTypes = allowedTypes.or(CardType::isAPlanarType);
        }

        subtypes.removeIf(allowedTypes.negate());
    }

    @Override
    public int compareTo(final CardType o) {
        return toString().compareTo(o.toString());
    }

    public boolean sharesCreaturetypeWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }
        if (!isCreature() && !isKindred()) {
            return false;
        }
        if (!ctOther.isCreature() && !ctOther.isKindred()) {
            return false;
        }

        // special cases for if any of them is all creature types
        if (this.allCreatureTypes && ctOther.hasAllCreatureTypes()) {
            // no type is exluded so they should share all creature types
            if (excludedCreatureSubtypes.isEmpty() && Iterables.isEmpty(ctOther.getExcludedCreatureSubTypes())) {
                return true;
            }
        }

        for (final String type : getCreatureTypes()) {
            if (ctOther.hasCreatureType(type)) {
                return true;
            }
        }
        for (final String type : ctOther.getCreatureTypes()) {
            if (this.hasCreatureType(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean sharesLandTypeWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }

        for (final String type : getLandTypes()) {
            if (ctOther.hasSubtype(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean sharesPermanentTypeWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }

        for (final CoreType type : getCoreTypes()) {
            if (type.isPermanent && ctOther.hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean sharesCardTypeWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }
        for (final CoreType type : getCoreTypes()) {
            if (ctOther.hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public boolean sharesAllCardTypesWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }
        for (final CoreType type : getCoreTypes()) {
            if (!ctOther.hasType(type)) {
                return false;
            }
        }
        for (final CoreType type : ctOther.getCoreTypes()) {
            if (!this.hasType(type)) {
                return false;
            }
        }
        return true;
    }

    public boolean sharesSubtypeWith(final CardTypeView ctOther) {
        if (ctOther == null) {
            return false;
        }
        if (sharesCreaturetypeWith(ctOther)) {
            return true;
        }
        for (final String t : ctOther.getSubtypes()) {
            if (hasSubtype(t)) {
                return true;
            }
        }
        return false;
    }

    public GamePieceType getGamePieceType() {
        if(this.isAttraction())
            return GamePieceType.ATTRACTION;
        if(this.isContraption())
            return GamePieceType.CONTRAPTION;
        for(CoreType type : coreTypes) {
            GamePieceType r = type.toGamePieceType();
            if(r != GamePieceType.CARD)
                return r;
        }
        return GamePieceType.CARD;
    }

    public static CardType parse(final String typeText, boolean incomplete) {
        // Most types and subtypes, except "Serra's Realm" and
        // "Bolas's Meditation Realm" consist of only one word
        final char space = ' ';
        final CardType result = new CardType(incomplete);

        int iTypeStart = 0;
        int max = typeText.length();
        boolean hasMoreTypes = max > 0;
        while (hasMoreTypes) {
            final String rest = typeText.substring(iTypeStart);
            String type = getMultiwordType(rest);
            if (type == null) {
                int iSpace = typeText.indexOf(space, iTypeStart);
                type = typeText.substring(iTypeStart, iSpace == -1 ? max : iSpace);
            }
            result.add(type);
            iTypeStart += type.length() + 1;
            hasMoreTypes = iTypeStart < max;
        }
        return result;
    }

    public static CardType combine(final CardType a, final CardType b) {
        final CardType result = new CardType(false);
        result.supertypes.addAll(a.supertypes);
        result.supertypes.addAll(b.supertypes);
        result.coreTypes.addAll(a.coreTypes);
        result.coreTypes.addAll(b.coreTypes);
        result.subtypes.addAll(a.subtypes);
        result.subtypes.addAll(b.subtypes);
        return result;
    }

    private static String getMultiwordType(final String type) {
        for (String multi : Constant.MultiwordTypes) {
            if (type.startsWith(multi)) {
                return multi;
            }
        }
        return null;
    }

    public static class Constant {
        public static final Settable LOADED = new Settable();
        public static final Set<String> BASIC_TYPES = Sets.newHashSet();
        public static final Set<String> LAND_TYPES = Sets.newHashSet();
        public static final Set<String> CREATURE_TYPES = Sets.newHashSet();
        public static final Set<String> SPELL_TYPES = Sets.newHashSet();
        public static final Set<String> ENCHANTMENT_TYPES = Sets.newHashSet();
        public static final Set<String> ARTIFACT_TYPES = Sets.newHashSet();
        public static final Set<String> WALKER_TYPES = Sets.newHashSet();
        public static final Set<String> DUNGEON_TYPES = Sets.newHashSet();
        public static final Set<String> BATTLE_TYPES = Sets.newHashSet();
        public static final Set<String> PLANAR_TYPES = Sets.newHashSet();

        public static final Set<String> MultiwordTypes = Sets.newHashSet();

        // singular -> plural
        public static final BiMap<String,String> pluralTypes = HashBiMap.create();
        // plural -> singular
        public static final BiMap<String,String> singularTypes = pluralTypes.inverse();

        static {
            for (CoreType c : CoreType.values()) {
                pluralTypes.put(c.name(), c.pluralName);
            }
        }


        public static final Set<String> OUTLAW_TYPES = Sets.newHashSet(
                "Assassin",
                "Mercenary",
                "Pirate",
                "Rogue",
                "Warlock");

        public static final Set<String> PARTY_TYPES = Sets.newHashSet(
                "Cleric",
                "Rogue",
                "Warrior",
                "Wizard");
    }

    ///////// Utility methods
    public static boolean isACardType(final String cardType) {
        return CoreType.isValidEnum(cardType);
    }

    public static Set<String> getAllCardTypes() {
        return CoreType.allCoreTypeNames;
    }

    private static List<String> sortedSubTypes;
    public static List<String> getSortedSubTypes() {
        if (sortedSubTypes == null) {
            sortedSubTypes = Lists.newArrayList();
            sortedSubTypes.addAll(Constant.BASIC_TYPES);
            sortedSubTypes.addAll(Constant.LAND_TYPES);
            sortedSubTypes.addAll(Constant.CREATURE_TYPES);
            sortedSubTypes.addAll(Constant.SPELL_TYPES);
            sortedSubTypes.addAll(Constant.ENCHANTMENT_TYPES);
            sortedSubTypes.addAll(Constant.ARTIFACT_TYPES);
            sortedSubTypes.addAll(Constant.WALKER_TYPES);
            sortedSubTypes.addAll(Constant.DUNGEON_TYPES);
            sortedSubTypes.addAll(Constant.BATTLE_TYPES);
            sortedSubTypes.addAll(Constant.PLANAR_TYPES);
            Collections.sort(sortedSubTypes);
        }
        return sortedSubTypes;
    }

    public static Collection<String> getBasicTypes() {
        return Collections.unmodifiableCollection(Constant.BASIC_TYPES);
    }
    public static Collection<String> getNonBasicTypes() {
        return Collections.unmodifiableCollection(Constant.LAND_TYPES);
    }

    public static Collection<String> getAllCreatureTypes() {
        return Collections.unmodifiableCollection(Constant.CREATURE_TYPES);
    }
    public static Collection<String> getAllWalkerTypes() {
        return Collections.unmodifiableCollection(Constant.WALKER_TYPES);
    }
    public static List<String> getAllLandTypes() {
        return ImmutableList.<String>builder()
                .addAll(getBasicTypes())
                .addAll(Constant.LAND_TYPES)
                .build();
    }

    public static boolean isASupertype(final String cardType) {
        return Supertype.isValidEnum(cardType);
    }

    public static boolean isASubType(final String cardType) {
        return getSortedSubTypes().contains(cardType);
    }

    public static boolean isAnArtifactType(final String cardType) {
        return Constant.ARTIFACT_TYPES.contains(cardType);
    }

    public static boolean isACreatureType(final String cardType) {
        return Constant.CREATURE_TYPES.contains(cardType);
    }

    public static boolean isALandType(final String cardType) {
        return Constant.LAND_TYPES.contains(cardType) || isABasicLandType(cardType);
    }

    public static boolean isAPlaneswalkerType(final String cardType) {
        return Constant.WALKER_TYPES.contains(cardType);
    }

    public static boolean isABasicLandType(final String cardType) {
        return Constant.BASIC_TYPES.contains(cardType);
    }

    public static boolean isAnEnchantmentType(final String cardType) {
        return Constant.ENCHANTMENT_TYPES.contains(cardType);
    }

    public static boolean isASpellType(final String cardType) {
        return Constant.SPELL_TYPES.contains(cardType);
    }

    public static boolean isADungeonType(final String cardType) {
        return Constant.DUNGEON_TYPES.contains(cardType);
    }
    public static boolean isABattleType(final String cardType) {
        return Constant.BATTLE_TYPES.contains(cardType);
    }
    public static boolean isAPlanarType(final String cardType) {
        return Constant.PLANAR_TYPES.contains(cardType);
    }
    /**
     * If the input is a plural type, return the corresponding singular form.
     * Otherwise, simply return the input.
     * @param type a String.
     * @return the corresponding type.
     *
     * @deprecated
     */
    public static String getSingularType(final String type) {
        if (Constant.singularTypes.containsKey(type)) {
            return Constant.singularTypes.get(type);
        }
        return type;
    }

    /**
     * If the input is a singular type, return the corresponding plural form.
     * Otherwise, simply return the input.
     * @param type a String.
     * @return the corresponding type.
     */
    public static String getPluralType(final String type) {
        if (Constant.pluralTypes.containsKey(type)) {
            return Constant.pluralTypes.get(type);
        }
        return type;
    }

    public static class Helper {
        public static final void parseTypes(String sectionName, List<String> content) {
            Set<String> addToSection = null;

            switch (sectionName) {
                case "BasicTypes":
                    addToSection = CardType.Constant.BASIC_TYPES;
                    break;
                case "LandTypes":
                    addToSection = CardType.Constant.LAND_TYPES;
                    break;
                case "CreatureTypes":
                    addToSection = CardType.Constant.CREATURE_TYPES;
                    break;
                case "SpellTypes":
                    addToSection = CardType.Constant.SPELL_TYPES;
                    break;
                case "EnchantmentTypes":
                    addToSection = CardType.Constant.ENCHANTMENT_TYPES;
                    break;
                case "ArtifactTypes":
                    addToSection = CardType.Constant.ARTIFACT_TYPES;
                    break;
                case "WalkerTypes":
                    addToSection = CardType.Constant.WALKER_TYPES;
                    break;
                case "DungeonTypes":
                    addToSection = CardType.Constant.DUNGEON_TYPES;
                    break;
                case "BattleTypes":
                    addToSection = CardType.Constant.BATTLE_TYPES;
                    break;
                case "PlanarTypes":
                    addToSection = CardType.Constant.PLANAR_TYPES;
                    break;
            }

            if (addToSection == null) {
                return;
            }

            for(String line : content) {
                if (line.length() == 0) continue;

                if (line.contains(":")) {
                    String[] k = line.split(":");

                    if (addToSection.contains(k[0])) {
                        continue;
                    }

                    addToSection.add(k[0]);
                    CardType.Constant.pluralTypes.put(k[0], k[1]);

                    if (k[0].contains(" ")) {
                        CardType.Constant.MultiwordTypes.add(k[0]);
                    }
                } else {
                    if (addToSection.contains(line)) {
                        continue;
                    }

                    addToSection.add(line);
                    if (line.contains(" ")) {
                        CardType.Constant.MultiwordTypes.add(line);
                    }
                }
            }
        }
    }
}
