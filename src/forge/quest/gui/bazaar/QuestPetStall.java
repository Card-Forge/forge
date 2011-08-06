package forge.quest.gui.bazaar;

import forge.gui.GuiUtils;
import forge.quest.data.pet.QuestPetAbstract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QuestPetStall extends QuestAbstractBazaarStall {
	private static final long serialVersionUID = -599280030410495964L;
	
	public QuestPetStall() {
        super("Pet Shop", "FoxIconSmall.png", "This large stall echoes with a multitude of animal noises.");

    }

    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        Collection<QuestPetAbstract> pets = questData.getPetManager().getPets();

        for (final QuestPetAbstract pet : pets) {
            if (pet.getLevel() < pet.getMaxLevel()){
                itemList.add(new QuestAbstractBazaarItem(
                        pet.getName(),
                        formatDescription(pet),
                        pet.getUpgradePrice(),
                        GuiUtils.getIconFromFile(pet.getImageName())) {
                    @Override
                    public void purchaseItem() {
                        questData.getPetManager().addPetLevel(pet.getName());
                    }
                });
            }
        }

        return itemList;
    }

    private String formatDescription(QuestPetAbstract pet) {
        String description =
                "<em>"+pet.getDescription()+"</em><br>" + pet.getUpgradeDescription()+
                "<br><br><u>Current stats:</u> " + pet.getStats()+
                "<br><u>Upgraded stats:</u> " + pet.getUpgradedStats();
        

        return description;
    }


}
