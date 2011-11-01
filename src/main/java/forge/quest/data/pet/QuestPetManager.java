package forge.quest.data.pet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * QuestPetManager class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPetManager {

    /** The pets. */
    public Map<String, QuestPetAbstract> pets = new HashMap<String, QuestPetAbstract>();

    /** The selected pet. */
    public QuestPetAbstract selectedPet;

    /** The plant. */
    public QuestPetAbstract plant;

    /** The use plant. */
    public boolean usePlant;

    /**
     * <p>
     * Constructor for QuestPetManager.
     * </p>
     */
    public QuestPetManager() {
        this.plant = new QuestPetPlant();
        for (final QuestPetAbstract pet : QuestPetManager.getAllPets()) {
            this.addPet(pet);
        }
    }

    /**
     * <p>
     * Setter for the field <code>selectedPet</code>.
     * </p>
     * 
     * @param pet
     *            a {@link java.lang.String} object.
     */
    public final void setSelectedPet(final String pet) {
        this.selectedPet = (pet == null) ? null : this.getPet(pet);
    }

    /**
     * <p>
     * Getter for the field <code>selectedPet</code>.
     * </p>
     * 
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public final QuestPetAbstract getSelectedPet() {
        return this.selectedPet;
    }

    /**
     * <p>
     * Getter for the field <code>plant</code>.
     * </p>
     * 
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public final QuestPetAbstract getPlant() {
        return this.plant;
    }

    /**
     * <p>
     * addPlantLevel.
     * </p>
     */
    public final void addPlantLevel() {
        if (this.plant == null) {
            this.plant = new QuestPetPlant();
        } else {
            this.plant.incrementLevel();
        }
    }

    /**
     * <p>
     * getPet.
     * </p>
     * 
     * @param petName
     *            a {@link java.lang.String} object.
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public final QuestPetAbstract getPet(final String petName) {

        return this.pets.get(petName);
    }

    /**
     * <p>
     * addPet.
     * </p>
     * 
     * @param newPet
     *            a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public final void addPet(final QuestPetAbstract newPet) {
        this.pets.put(newPet.getName(), newPet);
    }

    /**
     * <p>
     * getPetNames.
     * </p>
     * 
     * @return a {@link java.util.Set} object.
     */
    public final Set<String> getPetNames() {
        return this.pets.keySet();
    }

    /**
     * <p>
     * addPetLevel.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addPetLevel(final String s) {
        this.pets.get(s).incrementLevel();
    }

    /**
     * <p>
     * shouldPlantBeUsed.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean shouldPlantBeUsed() {
        return this.usePlant;
    }

    /**
     * <p>
     * shouldPetBeUsed.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean shouldPetBeUsed() {
        return this.selectedPet != null;
    }

    /**
     * <p>
     * getAllPets.
     * </p>
     * 
     * @return a {@link java.util.Set} object.
     */
    private static Set<QuestPetAbstract> getAllPets() {
        final SortedSet<QuestPetAbstract> set = new TreeSet<QuestPetAbstract>();

        set.add(new QuestPetBird());
        set.add(new QuestPetCrocodile());
        set.add(new QuestPetHound());
        set.add(new QuestPetWolf());

        return set;
    }

    /**
     * <p>
     * getAvailablePetNames.
     * </p>
     * 
     * @return a {@link java.util.Set} object.
     */
    public final Set<String> getAvailablePetNames() {
        final SortedSet<String> set = new TreeSet<String>();
        for (final Map.Entry<String, QuestPetAbstract> pet : this.pets.entrySet()) {
            if (pet.getValue().getLevel() > 0) {
                set.add(pet.getKey());
            }
        }
        return set;
    }

    /**
     * <p>
     * getPetsAndPlants.
     * </p>
     * 
     * @return a {@link java.util.Collection} object.
     */
    public final Collection<QuestPetAbstract> getPetsAndPlants() {
        final Set<QuestPetAbstract> petsAndPlants = new HashSet<QuestPetAbstract>(this.pets.values());
        petsAndPlants.add(this.plant);

        return petsAndPlants;
    }

    // Magic to support added pet types when reading saves.
    /**
     * <p>
     * readResolve.
     * </p>
     * 
     * @return a {@link java.lang.Object} object.
     */
    private Object readResolve() {
        for (final QuestPetAbstract pet : QuestPetManager.getAllPets()) {
            if (!this.pets.containsKey(pet.getName())) {
                this.addPet(pet);
            }
        }
        return this;
    }
}
