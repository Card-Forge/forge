package forge.quest.bazaar;

import java.util.ArrayList;
import java.util.List;

public class QuestBookStall extends QuestAbstractBazaarStall{
	private static final long serialVersionUID = -2045024031362910371L;

	public QuestBookStall() {
        super("Bookstore", "BookIconSmall.png", "Tomes of different sizes are stacked in man-high towers.");
    }
    
    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        if (questData.getSleightOfHandLevel() == 0){
            itemList.add(new QuestAbstractBazaarItem(
                    "Sleight of Hand Vol. I",
                    "These volumes explain how to perform the most difficult of sleights.<br>"+
                            "<u>Your first mulligan is <b>free</b></u>.",
                    2000,
                    getIcon("BookIcon.png")) {
                @Override
                public void purchaseItem() {
                    questData.addSleightOfHandLevel(1);
                }
            });
        }

        return itemList;
    }
}
