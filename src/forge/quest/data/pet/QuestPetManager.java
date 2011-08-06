package forge.quest.data.pet;

import java.util.*;

public class QuestPetManager{

    public Map<String, QuestPetAbstract> pets = new HashMap<String, QuestPetAbstract>();
    public QuestPetAbstract selectedPet;
    public QuestPetAbstract plant;
    public boolean usePlant;

    public QuestPetManager() {
        plant = new QuestPetPlant();
        for (QuestPetAbstract pet : getAllPets()) {
            addPet(pet);
        }
    }

    public void setSelectedPet(String pet) {
        selectedPet = (pet == null) ? null : getPet(pet);
    }

    public QuestPetAbstract getSelectedPet() {
        return selectedPet;
    }

    public QuestPetAbstract getPlant() {
        return plant;
    }

    public void addPlantLevel() {
        if (plant == null) {
            plant = new QuestPetPlant();
        }
        else {
            plant.incrementLevel();
        }
    }

    public QuestPetAbstract getPet(String petName) {

        return pets.get(petName);
    }

    public void addPet(QuestPetAbstract newPet) {
        pets.put(newPet.getName(), newPet);
    }

    public Set<String> getPetNames() {
        return pets.keySet();
    }

    public void addPetLevel(String s) {
        pets.get(s).incrementLevel();
    }

    public boolean shouldPlantBeUsed() {
        return usePlant;
    }

    public boolean shouldPetBeUsed() {
        return selectedPet != null;
    }

    private static Set<QuestPetAbstract> getAllPets() {
        SortedSet<QuestPetAbstract> set = new TreeSet<QuestPetAbstract>();

        set.add(new QuestPetBird());
        set.add(new QuestPetCrocodile());
        set.add(new QuestPetHound());
        set.add(new QuestPetWolf());

        return set;
    }


    public Set<String> getAvailablePetNames() {
        SortedSet<String> set = new TreeSet<String>();
        for (Map.Entry<String, QuestPetAbstract> pet : pets.entrySet()) {
            if (pet.getValue().getLevel() > 0){
                set.add(pet.getKey());
            }
        }
        return set;
    }


    public Collection<QuestPetAbstract> getPetsAndPlants() {
        Set <QuestPetAbstract> petsAndPlants = new HashSet<QuestPetAbstract>(pets.values());
        petsAndPlants.add(plant);

        return petsAndPlants;
    }

    //Magic to support added pet types when reading saves.
    private Object readResolve() {
        for (QuestPetAbstract pet : getAllPets()) {
            if (!pets.containsKey(pet.getName())) {
                addPet(pet);
            }
        }
        return this;
    }
}
