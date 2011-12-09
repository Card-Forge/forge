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
    private final Map<String, QuestPetAbstract> pets = new HashMap<String, QuestPetAbstract>();

    /** The selected pet. */
    private QuestPetAbstract selectedPet;

    /** The plant. */
    private QuestPetAbstract plant;

    /** The use plant. */
    private boolean usePlant;

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
        return this.isUsePlant();
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

    /**
     * Checks if is use plant.
     * 
     * @return the usePlant
     */
    public boolean isUsePlant() {
        return this.usePlant;
    }

    /**
     * Sets the use plant.
     * 
     * @param usePlant0
     *            the usePlant to set
     */
    public void setUsePlant(final boolean usePlant0) {
        this.usePlant = usePlant0;
    }
}
