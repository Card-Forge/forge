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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Immutable Card type. Can be built only from parsing a string.
 * </p>
 * 
 * @author Forge
 * @version $Id: java 9708 2011-08-09 19:34:12Z jendave $
 */
public final class CardType implements Comparable<CardType>, CardTypeView {
    public enum CoreType {
        Artifact(true),
        Conspiracy(false),
        Creature(true),
        Emblem(false),
        Enchantment(true),
        Instant(false),
        Land(true),
        Plane(false),
        Planeswalker(true),
        Scheme(false),
        Sorcery(false),
        Tribal(false),
        Vanguard(false),
        Phenomenon(false);

        public final boolean isPermanent;

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
        World
    }

    // This will be useful for faster parses
    private static HashMap<String, CoreType> stringToCoreType = new HashMap<String, CoreType>();
    private static HashMap<String, Supertype> stringToSupertype = new HashMap<String, Supertype>();
    static {
        for (final Supertype st : Supertype.values()) {
            stringToSupertype.put(st.name(), st);
        }
        for (final CoreType ct : CoreType.values()) {
            stringToCoreType.put(ct.name(), ct);
        }
    }

    private final EnumSet<CoreType> coreTypes = EnumSet.noneOf(CoreType.class);
    private final EnumSet<Supertype> supertypes = EnumSet.noneOf(Supertype.class);
    private final HashSet<String> subtypes = new HashSet<String>();
    private String calculatedType = null;

    public CardType() {
    }
    public CardType(Iterable<String> from0) {
        addAll(from0);
    }
    public CardType(CardType from0) {
        addAll(from0);
    }
    public CardType(CardTypeView from0) {
        addAll(from0);
    }

    public boolean add(final String t) {
        boolean changed;
        CoreType ct = stringToCoreType.get(t);
        if (ct != null) {
            changed = coreTypes.add(ct);
        }
        else {
            Supertype st = stringToSupertype.get(t);
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
    public boolean addAll(Iterable<String> types) {
        boolean changed = false;
        for (String t : types) {
            if (add(t)) {
                changed = true;
            }
        }
        return changed;
    }
    public boolean addAll(CardType type) {
        boolean changed = false;
        if (coreTypes.addAll(type.coreTypes)) { changed = true; }
        if (supertypes.addAll(type.supertypes)) { changed = true; }
        if (subtypes.addAll(type.subtypes)) { changed = true; }
        return changed;
    }
    public boolean addAll(CardTypeView type) {
        boolean changed = false;
        if (Iterables.addAll(coreTypes, type.getCoreTypes())) { changed = true; }
        if (Iterables.addAll(supertypes, type.getSupertypes())) { changed = true; }
        if (Iterables.addAll(subtypes, type.getSubtypes())) { changed = true; }
        return changed;
    }

    public boolean removeAll(CardType type) {
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

    public boolean isEmpty() {
        return coreTypes.isEmpty() && supertypes.isEmpty() && subtypes.isEmpty();
    }

    public Iterable<CoreType> getCoreTypes() {
        return coreTypes;
    }
    public Iterable<Supertype> getSupertypes() {
        return supertypes;
    }
    public Iterable<String> getSubtypes() {
        return subtypes;
    }
    public Set<String> getCreatureTypes() {
        Set<String> creatureTypes = new HashSet<String>();
        if (isCreature() || isTribal()) {
            for (String t : subtypes) {
                if (isACreatureType(t) || t.equals("AllCreatureTypes")) {
                    creatureTypes.add(t);
                }
            }
        }
        return creatureTypes;
    }

    public boolean hasStringType(String t) {
        if (subtypes.contains(t)) {
            return true;
        }
        CoreType type = stringToCoreType.get(t);
        if (type != null) {
            return hasType(type);
        }
        Supertype supertype = stringToSupertype.get(t);
        if (supertype != null) {
            return hasSupertype(supertype);
        }
        return false;
    }
    public boolean hasType(CoreType type) {
        return coreTypes.contains(type);
    }
    public boolean hasSupertype(Supertype supertype) {
        return supertypes.contains(supertype);
    }
    public boolean hasSubtype(String subtype) {
        return subtypes.contains(subtype);
    }
    public boolean hasCreatureType(String creatureType) {
        if (subtypes.isEmpty()) { return false; }
        if (!isCreature() && !isTribal()) { return false; }

        creatureType = toMixedCase(creatureType);
        if (!isACreatureType(creatureType)) { return false; }

        return subtypes.contains(creatureType) || subtypes.contains("AllCreatureTypes");
    }
    private String toMixedCase(final String s) {
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

    public boolean isPermanent() {
        for (CoreType type : coreTypes) {
            if (type.isPermanent) {
                return true;
            }
        }
        return false;
    }

    public boolean isCreature() {
        return coreTypes.contains(CoreType.Creature);
    }

    public boolean isPlaneswalker() {
        return coreTypes.contains(CoreType.Planeswalker);
    }

    public boolean isLand() {
        return coreTypes.contains(CoreType.Land);
    }

    public boolean isArtifact() {
        return coreTypes.contains(CoreType.Artifact);
    }

    public boolean isInstant() {
        return coreTypes.contains(CoreType.Instant);
    }

    public boolean isSorcery() {
        return coreTypes.contains(CoreType.Sorcery);
    }

    public boolean isConspiracy() {
        return coreTypes.contains(CoreType.Conspiracy);
    }

    public boolean isVanguard() {
        return coreTypes.contains(CoreType.Vanguard);
    }

    public boolean isScheme() {
        return coreTypes.contains(CoreType.Scheme);
    }

    public boolean isEnchantment() {
        return coreTypes.contains(CoreType.Enchantment);
    }

    public boolean isBasic() {
        return supertypes.contains(Supertype.Basic);
    }

    public boolean isLegendary() {
        return supertypes.contains(Supertype.Legendary);
    }

    public boolean isSnow() {
        return supertypes.contains(Supertype.Snow);
    }

    public boolean isBasicLand() {
        return isBasic() && isLand();
    }

    public boolean isPlane() {
        return coreTypes.contains(CoreType.Plane);
    }

    public boolean isPhenomenon() {
        return coreTypes.contains(CoreType.Phenomenon);
    }

    public boolean isEmblem() {
        return coreTypes.contains(CoreType.Emblem);
    }

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

    public LinkedHashSet<String> getTypesBeforeDash() {
        final LinkedHashSet<String> types = new LinkedHashSet<String>();
        for (final Supertype st : supertypes) {
            types.add(st.name());
        }
        for (final CoreType ct : coreTypes) {
            types.add(ct.name());
        }
        return types;
    }

    public CardTypeView getTypeWithChanges(Map<Long, CardChangedType> changedCardTypes) {
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
                Iterator<String> subtypes = newType.subtypes.iterator();
                while (subtypes.hasNext()) {
                    String t = subtypes.next();
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

    public boolean sharesSubtypeWith(CardType ctOther) {
        for (String t : ctOther.getSubtypes()) {
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
        CardType result = new CardType();
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
        public static final boolean[] LOADED = { false };
        public static final List<String> CARD_TYPES = new ArrayList<String>();
        public static final List<String> SUPER_TYPES = new ArrayList<String>();
        public static final List<String> BASIC_TYPES = new ArrayList<String>();
        public static final List<String> LAND_TYPES = new ArrayList<String>();
        public static final List<String> CREATURE_TYPES = new ArrayList<String>();
        public static final List<String> INSTANT_TYPES = new ArrayList<String>();
        public static final List<String> SORCERY_TYPES = new ArrayList<String>();
        public static final List<String> ENCHANTMENT_TYPES = new ArrayList<String>();
        public static final List<String> ARTIFACT_TYPES = new ArrayList<String>();
        public static final List<String> WALKER_TYPES = new ArrayList<String>();
    }

    ///////// Utility methods
    public static boolean isACardType(final String cardType) {
        return getAllCardTypes().contains(cardType);
    }

    public static ArrayList<String> getAllCardTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        // types.addAll(getCardTypes());
        types.addAll(Constant.CARD_TYPES);
    
        // Variant card types (I don't understand these lines, shouldn't core types be enough?)
        types.add("Plane");
        types.add("Scheme");
        types.add("Vanguard");
        types.add("Conspiracy");
    
        return types;
    }

    public static ArrayList<String> getBasicTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.BASIC_TYPES);
    
        return types;
    }

    public static ArrayList<String> getLandTypes() {
        final ArrayList<String> types = new ArrayList<String>();
    
        types.addAll(Constant.BASIC_TYPES);
        types.addAll(Constant.LAND_TYPES);
    
        return types;
    }

    public static boolean isASupertype(final String cardType) {
        return (Constant.SUPER_TYPES.contains(cardType));
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
