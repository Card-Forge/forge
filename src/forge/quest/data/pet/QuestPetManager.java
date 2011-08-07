package forge.quest.data.pet;

import java.util.*;

/**
 * <p>QuestPetManager class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestPetManager {

    public Map<String, QuestPetAbstract> pets = new HashMap<String, QuestPetAbstract>();
    public QuestPetAbstract selectedPet;
    public QuestPetAbstract plant;
    public boolean usePlant;

    /**
     * <p>Constructor for QuestPetManager.</p>
     */
    public QuestPetManager() {
        plant = new QuestPetPlant();
        for (QuestPetAbstract pet : getAllPets()) {
            addPet(pet);
        }
    }

    /**
     * <p>Setter for the field <code>selectedPet</code>.</p>
     *
     * @param pet a {@link java.lang.String} object.
     */
    public void setSelectedPet(String pet) {
        selectedPet = (pet == null) ? null : getPet(pet);
    }

    /**
     * <p>Getter for the field <code>selectedPet</code>.</p>
     *
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public QuestPetAbstract getSelectedPet() {
        return selectedPet;
    }

    /**
     * <p>Getter for the field <code>plant</code>.</p>
     *
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public QuestPetAbstract getPlant() {
        return plant;
    }

    /**
     * <p>addPlantLevel.</p>
     */
    public void addPlantLevel() {
        if (plant == null) {
            plant = new QuestPetPlant();
        } else {
            plant.incrementLevel();
        }
    }

    /**
     * <p>getPet.</p>
     *
     * @param petName a {@link java.lang.String} object.
     * @return a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public QuestPetAbstract getPet(String petName) {

        return pets.get(petName);
    }

    /**
     * <p>addPet.</p>
     *
     * @param newPet a {@link forge.quest.data.pet.QuestPetAbstract} object.
     */
    public void addPet(QuestPetAbstract newPet) {
        pets.put(newPet.getName(), newPet);
    }

    /**
     * <p>getPetNames.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<String> getPetNames() {
        return pets.keySet();
    }

    /**
     * <p>addPetLevel.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addPetLevel(String s) {
        pets.get(s).incrementLevel();
    }

    /**
     * <p>shouldPlantBeUsed.</p>
     *
     * @return a boolean.
     */
    public boolean shouldPlantBeUsed() {
        return usePlant;
    }

    /**
     * <p>shouldPetBeUsed.</p>
     *
     * @return a boolean.
     */
    public boolean shouldPetBeUsed() {
        return selectedPet != null;
    }

    /**
     * <p>getAllPets.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    private static Set<QuestPetAbstract> getAllPets() {
        SortedSet<QuestPetAbstract> set = new TreeSet<QuestPetAbstract>();

        set.add(new QuestPetBird());
        set.add(new QuestPetCrocodile());
        set.add(new QuestPetHound());
        set.add(new QuestPetWolf());

        return set;
    }


    /**
     * <p>getAvailablePetNames.</p>
     *
     * @return a {@link java.util.Set} object.
     */
    public Set<String> getAvailablePetNames() {
        SortedSet<String> set = new TreeSet<String>();
        for (Map.Entry<String, QuestPetAbstract> pet : pets.entrySet()) {
            if (pet.getValue().getLevel() > 0) {
                set.add(pet.getKey());
            }
        }
        return set;
    }


    /**
     * <p>getPetsAndPlants.</p>
     *
     * @return a {@link java.util.Collection} object.
     */
    public Collection<QuestPetAbstract> getPetsAndPlants() {
        Set<QuestPetAbstract> petsAndPlants = new HashSet<QuestPetAbstract>(pets.values());
        petsAndPlants.add(plant);

        return petsAndPlants;
    }

    //Magic to support added pet types when reading saves.
    /**
     * <p>readResolve.</p>
     *
     * @return a {@link java.lang.Object} object.
     */
    private Object readResolve() {
        for (QuestPetAbstract pet : getAllPets()) {
            if (!pets.containsKey(pet.getName())) {
                addPet(pet);
            }
        }
        return this;
    }
}
