package forge.quest.gui.bazaar;

import forge.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

public class QuestBankerStall extends QuestAbstractBazaarStall {

    private static final long serialVersionUID = 2409591658245091210L;

    public QuestBankerStall() {
        super("Banker", "CoinIconSmall.png", "A large book large enough to be seen from the outside rests on the Banker's desk.");
    }

    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        if (questData.getEstatesLevel() < 3){

            itemList.add(new QuestAbstractBazaarItem(
                    "Estate management training",
                    getEstatesDesc(),
                    getEstatePrice(),
                    GuiUtils.getIconFromFile("GoldIconLarge.png")) {
                @Override
                public void purchaseItem() {
                    questData.addEstatesLevel(1);
                }
            });
        }

        if (questData.getLuckyCoinLevel() < 1){
            itemList.add(new QuestAbstractBazaarItem(
                    "Lucky Coin",
                    "This coin is believed to give good luck to its owner.<br>"+
                            "Improves the chance of getting a random <br>rare after each match by <b>15%</b>.",
                    2000,
                    GuiUtils.getIconFromFile("CoinIcon.png")){
                @Override
                public void purchaseItem() {
                    questData.addLuckyCoinLevel(1);
                }
            });
        }

        return itemList;
    }

    private String getEstatesDesc() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        if (questData.getEstatesLevel() == 0) {
            sb.append("<u>Level 1 Estates</u><br>");
            sb.append("Gives a bonus of <b>10%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>1.0%</b>.");
        } else if (questData.getEstatesLevel() == 1) {
            sb.append("<u>Level 2 Estates</u><br>");
            sb.append("Gives a bonus of <b>15%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>1.75%</b>.");
        } else if (questData.getEstatesLevel() == 2) {
            sb.append("<u>Level 3 Estates</u><br>");
            sb.append("Gives a bonus of <b>20%</b> to match winnings.<br>");
            sb.append("Improves sell percentage by <b>2.5%</b>.");
        }

        sb.append("</html>");
        return sb.toString();
    }

    private int getEstatePrice() {
        int l = 0;
        if (questData.getEstatesLevel() == 0)
            l = 500;
        else if (questData.getEstatesLevel() == 1)
            l = 750;
        else if (questData.getEstatesLevel() == 2)
            l = 1000;
        return l;
    }
    
}
