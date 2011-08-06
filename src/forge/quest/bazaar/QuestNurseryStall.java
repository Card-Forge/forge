package forge.quest.bazaar;

import java.util.ArrayList;
import java.util.List;

public class QuestNurseryStall extends QuestAbstractBazaarStall{
	private static final long serialVersionUID = 9217496944324343390L;

	public QuestNurseryStall() {
        super("Nursery", "LeafIconSmall.png", "The smells of the one hundred and one different plants forms a unique fragrance.");
    }


    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        if (questData.getPlantLevel()>=6){
            return null;
        }

        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        itemList.add(new QuestAbstractBazaarItem("Wall of plants",
                getDesc(),
                getPrice(),
                getIcon(getImageString())) {
            @Override
            public void purchaseItem() {
                questData.addPlantLevel();
            }
        });

        return itemList;
    }

    private String getDesc()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<html>");
    	if (questData.getPlantLevel() == 0)
    	{
    		sb.append("<br>Start each of your battles with this lush, verdant plant on your side.<br>");
    		sb.append("Excellent at blocking the nastiest of critters!<br><br>");
    		sb.append("<u><b>Current Level</b></u>: <em>Not purchased</em><br>");
    		sb.append("<u><b>Level 1</b></u>: 0/1<br>");
    		sb.append("<u><b>Level 2</b></u>: 0/2<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 1)
    	{
    		sb.append("Improve the toughness of your plant.<br>");
            sb.append("<u><b>Current Level</b></u>: 0/1 <br>");
    		sb.append("<u><b>Level 2</b></u>: 0/2<br>");
    		sb.append("<u><b>Level 3</b></u>: 0/3<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 2)
    	{
    		sb.append("Improve the toughness of your plant.<br>");
            sb.append("<u><b>Current Level</b></u>: 0/2 <br>");
    		sb.append("<u><b>Level 3</b></u>: 0/3<br>");
    		sb.append("<u><b>Level 4</b></u>: 1/3<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 3)
    	{
    		sb.append("Improve the power of your plant.<br>");
            sb.append("<u><b>Current Level</b></u>: 0/3 <br>");
    		sb.append("<u><b>Level 4</b></u>: 1/3<br>");
    		sb.append("<u><b>Level 5</b></u>: Deathtouch<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 4)
    	{
    		sb.append("Grow venomous thorns on your plant.<br>");
            sb.append("<u><b>Current Level</b></u>: 1/3 <br>");
    		sb.append("<u><b>Level 5</b></u>: 1/3, Deathtouch<br>");
    		sb.append("<u><b>Level 6</b></u>: 1/4, Deathtouch, Tap: you gain 1 life.<br>");
    	}
    	else if (questData.getPlantLevel() == 5)
    	{
    		sb.append("As well as gaining more toughness,<br>");
    		sb.append("your plant will have healing properties.<br>");
            sb.append("<u><b>Current Level</b></u>: 1/3, Deathtouch<br>");
    		sb.append("<u><b>Level 6</b></u>: 1/4, Deathtouch, Tap: you gain 1 life.");
    	}
    	else
    	{
    		sb.append("Plant Level Maxed out.");
    	}
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private int getPrice()
    {
    	int l = 0;
    	if (questData.getPlantLevel() == 0)
    		l = 100;
    	else if (questData.getPlantLevel() == 1)
    		l = 150;
    	else if (questData.getPlantLevel() == 2)
    		l = 200;
    	else if (questData.getPlantLevel() == 3)
    		l = 300;
    	else if (questData.getPlantLevel() == 4)
    		l = 750;
    	else if (questData.getPlantLevel() == 5)
    		l = 1000;
    	return l;
    }
    
    private String getImageString()
    {
    	String s = "";
    	if (questData.getPlantLevel() == 0)
    		s = "g_0_1_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 1)
    		s = "g_0_2_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 2)
    		s = "g_0_3_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 3)
    		s = "g_1_3_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 4)
    		s = "g_1_3_plant_wall_deathtouch_small.jpg";
    	else if (questData.getPlantLevel() == 5)
    		s = "g_1_4_plant_wall_small.jpg";
    	
    	return s;
    }
}
