package forge.card;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import forge.card.CardType.CoreType;
import forge.card.CardType.Supertype;

//Interface to expose only the desired functions of CardType without allowing modification
public interface CardTypeView extends Iterable<String>, Serializable {
    boolean isEmpty();
    Iterable<CoreType> getCoreTypes();
    Iterable<Supertype> getSupertypes();
    Iterable<String> getSubtypes();
    Set<String> getCreatureTypes();
    Set<String> getLandTypes();
    boolean hasStringType(String t);
    boolean hasType(CoreType type);
    boolean hasSupertype(Supertype supertype);
    boolean hasSubtype(String subtype);
    boolean hasCreatureType(String creatureType);
    boolean isPermanent();
    boolean isCreature();
    boolean isPlaneswalker();
    boolean isLand();
    boolean isArtifact();
    boolean isInstant();
    boolean isSorcery();
    boolean isConspiracy();
    boolean isVanguard();
    boolean isScheme();
    boolean isEnchantment();
    boolean isBasic();
    boolean isLegendary();
    boolean isSnow();
    boolean isBasicLand();
    boolean isPlane();
    boolean isPhenomenon();
    boolean isEmblem();
    boolean isTribal();
    CardTypeView getTypeWithChanges(Map<Long, CardChangedType> changedCardTypes);
}
