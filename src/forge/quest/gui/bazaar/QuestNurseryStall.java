package forge.quest.gui.bazaar;

import forge.gui.GuiUtils;
import forge.quest.data.pet.QuestPetAbstract;

import java.util.ArrayList;
import java.util.List;

public class QuestNurseryStall extends QuestAbstractBazaarStall {
    private static final long serialVersionUID = 9217496944324343390L;

    public QuestNurseryStall() {
        super("Nursery",
                "LeafIconSmall.png",
                "The smells of the one hundred and one different plants forms a unique fragrance.");
    }


    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        QuestPetAbstract plant = questData.getPetManager().getPlant();

        if (plant.getLevel() >= 6) {
            return new ArrayList<QuestAbstractBazaarItem>();
        }

        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();


        itemList.add(new QuestAbstractBazaarItem(plant.getName(),
                plant.getUpgradeDescription(),
                plant.getUpgradePrice(),
                GuiUtils.getIconFromFile(plant.getImageName())) {
            @Override
            public void purchaseItem() {
                questData.getPetManager().getPlant().incrementLevel();
            }
        });

        return itemList;
    }


}
