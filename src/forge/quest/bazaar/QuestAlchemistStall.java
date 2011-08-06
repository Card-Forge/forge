package forge.quest.bazaar;

import forge.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class QuestAlchemistStall extends QuestAbstractBazaarStall {
	private static final long serialVersionUID = 4168443923065232858L;


	protected QuestAlchemistStall() {
        super("Alchemist",
                "BottlesIconSmall.png",
                "The walls of this alchemist's stall are covered with shelves with potions, oils, powders, poultices and elixirs, each meticulously labeled.");

    }


    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        if (questData.getLife()>30)
        {
            return null;
        }

        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();
        
        int price=0;

        if (questData.getLife() < 20)
            price = 250;
        else if (questData.getLife() < 25)
            price = 500;
        else
            price = 750;

        itemList.add(new QuestAbstractBazaarItem(
                "Elixir of Health",
                "Gives +1 to max Life.<br><u>Current life:</u> " +
                        questData.getLife(),
                price,
                GuiUtils.getIconFromFile("ElixirIcon.png")) {
            @Override
            public void purchaseItem() {
                questData.addLife(1);
            }
        });

        return itemList;
    }
}
