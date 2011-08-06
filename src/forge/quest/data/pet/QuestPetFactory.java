package forge.quest.data.pet;

import java.util.ArrayList;
import java.util.List;

public class QuestPetFactory {
    static List<QuestPetAbstract> getAllPets(){
        List<QuestPetAbstract> pets = new ArrayList<QuestPetAbstract>();

        pets.add(new QuestPetWolf());
        pets.add(new QuestPetCrocodile());
        pets.add(new QuestPetBird());
        pets.add(new QuestPetHound());

        return pets;
    }
}
