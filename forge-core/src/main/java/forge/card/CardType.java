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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import forge.util.EnumUtil;
import forge.util.Settable;

/**
 * <p>
 * Immutable Card type. Can be built only from parsing a string.
 * </p>
 *
 * @author Forge
 * @version $Id: java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardType implements Comparable<CardType>, CardTypeView {
    private static final long serialVersionUID = 4629853583167022151L;

    public static final CardTypeView EMPTY = new CardType();

    public enum CoreType {
        Artifact(true),
        Conspiracy(false),
        Creature(true),
        Emblem(false),
        Enchantment(true),
        Instant(false),
        Land(true),
        Phenomenon(false),
        Plane(false),
        Planeswalker(true),
        Scheme(false),
        Sorcery(false),
        Tribal(false),
        Vanguard(false);

        public final boolean isPermanent;
        private static final ImmutableList<String> allCoreTypeNames = EnumUtil.getNames(CoreType.class);

        private CoreType(final boolean permanent) {
            isPermanent = permanent;
        }
    }

    public enum Supertype {
        Basic,
        Elite,
        Legendary,
        Snow,
        Ongoing,
        World;

        private static final ImmutableList<String> allSuperTypeNames = EnumUtil.getNames(Supertype.class);
    }

    // This will be useful for faster parses
    private static Map<String, CoreType> stringToCoreType = new HashMap<String, CoreType>();
    private static Map<String, Supertype> stringToSupertype = new HashMap<String, Supertype>();
    static {
        for (final Supertype st : Supertype.values()) {
            stringToSupertype.put(st.name(), st);
        }
        for (final CoreType ct : CoreType.values()) {
            stringToCoreType.put(ct.name(), ct);
        }
    }

    private final Set<CoreType> coreTypes = EnumSet.noneOf(CoreType.class);
    private final Set<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
    private final Set<String> subtypes = new LinkedHashSet<String>();
    private transient String calculatedType = null;

    public CardType() {
    }
    public CardType(final Iterable<String> from0) {
        addAll(from0);
    }
    public CardType(final CardType from0) {
        addAll(from0);
    }
    public CardType(final CardTypeView from0) {
        addAll(from0);
    }

    public boolean add(final String t) {
        boolean changed;
        final CoreType ct = stringToCoreType.get(t);
        if (ct != null) {
            changed = coreTypes.add(ct);
        }
        else {
            final Supertype st = stringToSupertype.get(t);
            if (st != null) {
                changed = supertypes.add(st);
            }
            else {
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
        boolean changed = false;
        for (final String t : types) {
            if (add(t)) {
                changed = true;
            }
        }
        return changed;
    }
    public boolean addAll(final CardType type) {
        boolean changed = false;
        if (coreTypes.addAll(type.coreTypes)) { changed = true; }
        if (supertypes.addAll(type.supertypes)) { changed = true; }
        if (subtypes.addAll(type.subtypes)) { changed = true; }
        return changed;
    }
    public boolean addAll(final CardTypeView type) {
        boolean changed = false;
        if (Iterables.addAll(coreTypes, type.getCoreTypes())) { changed = true; }
        if (Iterables.addAll(supertypes, type.getSupertypes())) { changed = true; }
        if (Iterables.addAll(subtypes, type.getSubtypes())) { changed = true; }
        return changed;
    }

    public boolean removeAll(final CardType type) {
        boolean changed = false;
        if (coreTypes.removeAll(type.coreTypes)) { changed = true; }
        if (supertypes.removeAll(type.supertypes)) { changed = true; }
        if (subtypes.removeAll(type.subtypes)) { changed = true; }
        if (changed) {
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

    @Override
    public boolean isEmpty() {
        return coreTypes.isEmpty() && supertypes.isEmpty() && subtypes.isEmpty();
    }

    @Override
    public Iterable<CoreType> getCoreTypes() {
        return coreTypes;
    }
    @Override
    public Iterable<Supertype> getSupertypes() {
        return supertypes;
    }
    @Override
    public Iterable<String> getSubtypes() {
        return subtypes;
    }
    @Override
    public Set<String> getCreatureTypes() {
        final Set<String> creatureTypes = new HashSet<String>();
        if (isCreature() || isTribal()) {
            for (final String t : subtypes) {
                if (isACreatureType(t) || t.equals("AllCreatureTypes")) {
                    creatureTypes.add(t);
                }
            }
        }
        return creatureTypes;
    }
    @Override
    public Set<String> getLandTypes() {
        final Set<String> landTypes = new HashSet<String>();
        if (isLand()) {
            for (final String t : subtypes) {
                if (isALandType(t) || isABasicLandType(t)) {
                    landTypes.add(t);
                }
            }
        }
        return landTypes;
    }

    @Override
    public boolean hasStringType(String t) {
        if (t.isEmpty()) {
            return false;
        }
        if (hasSubtype(t)) {
            return true;
        }
        final char firstChar = t.charAt(0);
        if (Character.isLowerCase(firstChar)) {
            t = Character.toUpperCase(firstChar) + t.substring(1); //ensure string is proper case for enum types
        }
        final CoreType type = stringToCoreType.get(t);
        if (type != null) {
            return hasType(type);
        }
        final Supertype supertype = stringToSupertype.get(t);
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
    public boolean hasSubtype(final String subtype) {
        if (isACreatureType(subtype) && subtypes.contains("AllCreatureTypes")) {
            return true;
        }
        return subtypes.contains(subtype);
    }
    @Override
    public boolean hasCreatureType(String creatureType) {
        if (subtypes.isEmpty()) { return false; }
        if (!isCreature() && !isTribal()) { return false; }

        creatureType = toMixedCase(creatureType);
        if (!isACreatureType(creatureType)) { return false; }

        return subtypes.contains(creatureType) || subtypes.contains("AllCreatureTypes");
    }
    private static String toMixedCase(final String s) {
        if (s.equals("")) {
            return s;
        }
        final StringBuilder sb = new StringBuilder();
        // to handle hyphenated Types
        final String[] types = s.split("-");
        for (int i = 0; i < types.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            sb.append(types[i].substring(0, 1).toUpperCase());
            sb.append(types[i].substring(1).toLowerCase());
        }
        return sb.toString();
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
    public boolean isEmblem() {
        return coreTypes.contains(CoreType.Emblem);
    }

    @Override
    public boolean isTribal() {
        return coreTypes.contains(CoreType.Tribal);
    }

    @Override
    public String toString() {
        if (calculatedType == null) {
            if (subtypes.isEmpty()) {
                calculatedType = StringUtils.join(getTypesBeforeDash(), ' ');
            }
            else {
                calculatedType = String.format("%s - %s", StringUtils.join(getTypesBeforeDash(), ' '), StringUtils.join(subtypes, " "));
            }
        }
        return calculatedType;
    }

    private Set<String> getTypesBeforeDash() {
        final Set<String> types = new LinkedHashSet<String>();
        for (final Supertype st : supertypes) {
            types.add(st.name());
        }
        for (final CoreType ct : coreTypes) {
            types.add(ct.name());
        }
        return types;
    }

    @Override
    public CardTypeView getTypeWithChanges(final Map<Long, CardChangedType> changedCardTypes) {
        if (changedCardTypes.isEmpty()) { return this; }

        final CardType newType = new CardType(CardType.this);
        for (final CardChangedType ct : changedCardTypes.values()) {
            if (ct.isRemoveCardTypes()) {
                newType.coreTypes.clear();
            }
            if (ct.isRemoveSuperTypes()) {
                newType.supertypes.clear();
            }
            if (ct.isRemoveSubTypes()) {
                newType.subtypes.clear();
            }
            else if (ct.isRemoveCreatureTypes()) {
                final Iterator<String> subtypes = newType.subtypes.iterator();
                while (subtypes.hasNext()) {
                    final String t = subtypes.next();
                    if (isACreatureType(t) || t.equals("AllCreatureTypes")) {
                        subtypes.remove();
                    }
                }
            }
            if (ct.getRemoveType() != null) {
                newType.removeAll(ct.getRemoveType());
            }
            if (ct.getAddType() != null) {
                newType.addAll(ct.getAddType());
            }
        }
        return newType;
    }

    @Override
    public Iterator<String> iterator() {
        final Iterator<CoreType> coreTypeIterator = coreTypes.iterator();
        final Iterator<Supertype> supertypeIterator = supertypes.iterator();
        final Iterator<String> subtypeIterator = subtypes.iterator();
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return coreTypeIterator.hasNext() || supertypeIterator.hasNext() || subtypeIterator.hasNext();
            }

            @Override
            public String next() {
                if (coreTypeIterator.hasNext()) {
                    return coreTypeIterator.next().name();
                }
                if (supertypeIterator.hasNext()) {
                    return supertypeIterator.next().name();
                }
                return subtypeIterator.next();
            }

            @Override
            public void remove() {
                throw new NotImplementedException("Removing this way not supported");
            }
        };
    }

    @Override
    public int compareTo(final CardType o) {
        return toString().compareTo(o.toString());
    }

    public boolean sharesSubtypeWith(final CardType ctOther) {
        for (final String t : ctOther.getSubtypes()) {
            if (hasSubtype(t)) {
                return true;
            }
        }
        return false;
    }

    public static CardType parse(final String typeText) {
        // Most types and subtypes, except "Serra's Realm" and
        // "Bolas's Meditation Realm" consist of only one word
        final char space = ' ';
        final CardType result = new CardType();

        int iTypeStart = 0;
        int iSpace = typeText.indexOf(space);
        boolean hasMoreTypes = typeText.length() > 0;
        while (hasMoreTypes) {
            final String type = typeText.substring(iTypeStart, iSpace == -1 ? typeText.length() : iSpace);
            hasMoreTypes = iSpace != -1;
            if (!isMultiwordType(type) || !hasMoreTypes) {
                iTypeStart = iSpace + 1;
                if (!"-".equals(type)) {
                    result.add(type);
                }
            }
            iSpace = typeText.indexOf(space, iSpace + 1);
        }
        return result;
    }

    public static CardType combine(final CardType a, final CardType b) {
        final CardType result = new CardType();
        result.supertypes.addAll(a.supertypes);
        result.supertypes.addAll(b.supertypes);
        result.coreTypes.addAll(a.coreTypes);
        result.coreTypes.addAll(b.coreTypes);
        result.subtypes.addAll(a.subtypes);
        result.subtypes.addAll(b.subtypes);
        return result;
    }

    private static boolean isMultiwordType(final String type) {
        final String[] multiWordTypes = { "Serra's Realm", "Bolas's Meditation Realm" };
        // no need to loop for only 2 exceptions!
        if (multiWordTypes[0].startsWith(type) && !multiWordTypes[0].equals(type)) {
            return true;
        }
        if (multiWordTypes[1].startsWith(type) && !multiWordTypes[1].equals(type)) {
            return true;
        }
        return false;
    }

    public static class Constant {
        public static final Settable LOADED = new Settable();
        public static final List<String> BASIC_TYPES = new ArrayList<String>();
        public static final List<String> LAND_TYPES = new ArrayList<String>();
        public static final List<String> CREATURE_TYPES = new ArrayList<String>();
        public static final List<String> SPELL_TYPES = new ArrayList<String>();
        public static final List<String> ENCHANTMENT_TYPES = new ArrayList<String>();
        public static final List<String> ARTIFACT_TYPES = new ArrayList<String>();
        public static final List<String> WALKER_TYPES = new ArrayList<String>();
    }

    ///////// Utility methods
    public static boolean isACardType(final String cardType) {
        return getAllCardTypes().contains(cardType);
    }

    public static ImmutableList<String> getAllCardTypes() {
        return CoreType.allCoreTypeNames;
    }

    public static List<String> getBasicTypes() {
        return Collections.unmodifiableList(Constant.BASIC_TYPES);
    }

    public static List<String> getAllCreatureTypes() {
        return Collections.unmodifiableList(Constant.CREATURE_TYPES);
    }
    public static List<String> getAllLandTypes() {
        return ImmutableList.<String>builder()
                .addAll(getBasicTypes())
                .addAll(Constant.LAND_TYPES)
                .build();
    }

    public static boolean isASupertype(final String cardType) {
        return (Supertype.allSuperTypeNames.contains(cardType));
    }

    public static boolean isASubType(final String cardType) {
        return (!isASupertype(cardType) && !isACardType(cardType));
    }

    public static boolean isACreatureType(final String cardType) {
        return (Constant.CREATURE_TYPES.contains(cardType));
    }

    public static boolean isALandType(final String cardType) {
        return (Constant.LAND_TYPES.contains(cardType));
    }

    public static boolean isAPlaneswalkerType(final String cardType) {
        return (Constant.WALKER_TYPES.contains(cardType));
    }

    public static boolean isABasicLandType(final String cardType) {
        return (Constant.BASIC_TYPES.contains(cardType));
    }
}
