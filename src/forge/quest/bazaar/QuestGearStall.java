package forge.quest.bazaar;

import forge.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class QuestGearStall extends QuestAbstractBazaarStall{
	private static final long serialVersionUID = -3809151981859475562L;

	public QuestGearStall() {
        super("Adventuring Gear","GearIconSmall.png","This adventurer's market has a tool for every need ... or so the plaque on the wall claims.");
    }

    @Override
    protected java.util.List<QuestAbstractBazaarItem> populateItems() {
        if (questData.getGearLevel()>2){
            return null;
        }

        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        if(questData.getGearLevel() == 0){
            itemList.add(new QuestAbstractBazaarItem("Adventurer's Map",
                    "These ancient charts should facilitate navigation during your travels significantly.<br>"+
                            "<u>Quest assignments become available more frequently</u>.",
                    2000,
                    GuiUtils.getIconFromFile("MapIconLarge.png")) {
                @Override
                public void purchaseItem() {questData.addGearLevel(1);
                }

            });
        }
        if(questData.getGearLevel() == 1){
            itemList.add(new QuestAbstractBazaarItem("Zeppelin",
                    "This extremely comfortable airship allows for more efficient and safe travel<br>to faraway destinations. <br>"+
                            "<u>Quest assignments become available more frequently<br>Adds +3 to max life during quest games</u>.",
                    5000,
                    GuiUtils.getIconFromFile("ZeppelinIcon.png")) {
                @Override
                public void purchaseItem() {questData.addGearLevel(1);
                }

            });
        }


        return itemList;
    }
}
