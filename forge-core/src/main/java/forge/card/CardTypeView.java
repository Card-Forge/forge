package forge.card;

import forge.card.CardType.CoreType;
import forge.card.CardType.Supertype;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

//Interface to expose only the desired functions of CardType without allowing modification
public interface CardTypeView extends Serializable {
    boolean isEmpty();
    Collection<CoreType> getCoreTypes();
    Collection<Supertype> getSupertypes();
    Collection<String> getSubtypes();
    Iterable<String> getExcludedCreatureSubTypes();

    Set<String> getCreatureTypes();
    Set<String> getLandTypes();
    Set<String> getBattleTypes();

    boolean hasStringType(String t);
    boolean hasType(CoreType type);
    boolean hasSupertype(Supertype supertype);
    boolean hasSubtype(String subtype);
    boolean hasCreatureType(String creatureType);
    boolean hasAllCreatureTypes();
    boolean hasABasicLandType();
    boolean hasANonBasicLandType();

    public boolean sharesCreaturetypeWith(final CardTypeView ctOther);
    public boolean sharesLandTypeWith(final CardTypeView ctOther);
    public boolean sharesPermanentTypeWith(final CardTypeView ctOther);
    public boolean sharesCardTypeWith(final CardTypeView ctOther);
    public boolean sharesAllCardTypesWith(final CardTypeView ctOther);

    boolean isPermanent();
    boolean isCreature();
    boolean isPlaneswalker();
    boolean isBattle();
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
    boolean isKindred();
    boolean isDungeon();

    boolean isAttachment();
    boolean isAura();
    boolean isEquipment();
    boolean isFortification();
    boolean isAttraction();
    boolean isContraption();

    boolean isSaga();
    boolean isHistoric();
    boolean isOutlaw();
    boolean isParty();

    CardTypeView getTypeWithChanges(Iterable<ICardChangedType> changedCardTypes);
}
