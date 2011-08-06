package forge.quest.bazaar;

import java.util.ArrayList;
import java.util.List;

public class QuestPetStall extends QuestAbstractBazaarStall {
	private static final long serialVersionUID = -599280030410495964L;
	
	public QuestPetStall() {
        super("Pet Shop", "FoxIconSmall.png", "This large stall echoes with a multitude of animal noises.");

    }

    @Override
    protected List<QuestAbstractBazaarItem> populateItems() {
        List<QuestAbstractBazaarItem> itemList = new ArrayList<QuestAbstractBazaarItem>();

        if (questData.getWolfPetLevel()<=3){
            itemList.add(new QuestAbstractBazaarItem("Wolf Pet",
                    getDesc(WOLF),
                    getPrice(WOLF),
                    getIcon(getImageString(WOLF))) {
                @Override
                public void purchaseItem() {
                    questData.addWolfPetLevel();
                }
            });
        }
        if (questData.getCrocPetLevel()<=3){
            itemList.add(new QuestAbstractBazaarItem("Crocodile Pet",
                    getDesc(CROC),
                    getPrice(CROC),
                    getIcon(getImageString(CROC))) {
                @Override
                public void purchaseItem() {
                    questData.addCrocPetLevel();
                }
            });
        }
        if (questData.getBirdPetLevel()<=3){
            itemList.add(new QuestAbstractBazaarItem("Bird Pet",
                    getDesc(BIRD),
                    getPrice(BIRD),
                    getIcon(getImageString(BIRD))) {
                @Override
                public void purchaseItem() {
                    questData.addBirdPetLevel();
                }
            });
        }
        if (questData.getHoundPetLevel()<=3){
            itemList.add(new QuestAbstractBazaarItem("Hound Pet",
                    getDesc(HOUND),
                    getPrice(HOUND),
                    getIcon(getImageString(HOUND))) {
                @Override
                public void purchaseItem() {
                    questData.addHoundPetLevel();
                }
            });
        }
        return itemList;
    }

    private String getDesc(int petType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><br>");

        switch (petType) {
            case WOLF:
                if (questData.getWolfPetLevel() == 0) {
                    sb.append("This ferocious animal may have been raised<br> in captivity, but it has been trained to kill.<br>");
                    sb.append("Eats goblins for breakfast.<br><br>");
                    sb.append("<u><b>Level 1</b></u>: 1/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 1/2<br>");
                    sb.append("<u><b>Can learn</b></u>: Flanking");
                } else if (questData.getWolfPetLevel() == 1) {
                    sb.append("Improve the toughness of your wolf.<br>");
                    sb.append("<u><b>Level 2</b></u>: 1/2<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/2<br>");
                    sb.append("<u><b>Can learn</b></u>: Flanking");
                } else if (questData.getWolfPetLevel() == 2) {
                    sb.append("Improve the attack power of your wolf.<br>");
                    sb.append("<u><b>Level 3</b></u>: 2/2<br>");
                    sb.append("<u><b>Next Level</b></u>: Flanking<br>");
                    sb.append("<u><b>Can learn</b></u>: Flanking");
                } else if (questData.getWolfPetLevel() == 3) {
                    sb.append("Gives Flanking to your wolf.<br>");
                    sb.append("<u><b>Level 4</b></u>: 2/2 Flanking<br>");
                }
                break;
            case CROC:
                if (questData.getCrocPetLevel() == 0) {
                    sb.append("With its razor sharp teeth, this swamp-dwelling monster is extremely dangerous.");
                    sb.append("Crikey mate!<br><br>");
                    sb.append("<u><b>Level 1</b></u>: 1/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/1<br>");
                    sb.append("<u><b>Can learn</b></u>: Swampwalk");
                } else if (questData.getCrocPetLevel() == 1) {
                    sb.append("Improve the attack power of your croc.<br>");
                    sb.append("<u><b>Level 2</b></u>: 2/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 3/1<br>");
                    sb.append("<u><b>Can learn</b></u>: Swampwalk");
                } else if (questData.getCrocPetLevel() == 2) {
                    sb.append("Improve the attack power of your croc.<br>");
                    sb.append("<u><b>Level 3</b></u>: 3/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 3/1 Swampwalk<br>");
                } else if (questData.getCrocPetLevel() == 3) {
                    sb.append("Gives Swampwalk to your croc.<br>");
                    sb.append("<u><b>Level 4</b></u>: 3/1 Swampwalk<br>");
                }
                break;
            case BIRD:
                if (questData.getBirdPetLevel() == 0) {
                    sb.append("Unmatched in speed, agility and awareness,<br>");
                    sb.append("this trained hawk makes a fantastic hunter.<br><br>");
                    sb.append("<u><b>Level 1</b></u>: 0/1 Flying<br>");
                    sb.append("<u><b>Next Level</b></u>: 1/1<br>");
                    sb.append("<u><b>Can learn</b></u>: First strike");
                } else if (questData.getBirdPetLevel() == 1) {
                    sb.append("Improve the attack power of your bird.<br>");
                    sb.append("<u><b>Level 2</b></u>: 1/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/1 <br>");
                    sb.append("<u><b>Can learn</b></u>: First strike");
                } else if (questData.getBirdPetLevel() == 2) {
                    sb.append("Improve the attack power of your bird.<br>");
                    sb.append("<u><b>Level 3</b></u>: 2/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/1 First strike<br>");
                } else if (questData.getBirdPetLevel() == 3) {
                    sb.append("Gives First strike to your bird.<br>");
                    sb.append("<u><b>Level 4</b></u>: 2/1 First strike<br>");
                }
                break;
            case HOUND:
                if (questData.getHoundPetLevel() == 0) {
                    sb.append("Dogs are said to be man's best friend.<br>");
                    sb.append("Definitely not this one.<br><br>");
                    sb.append("<u><b>Level 1</b></u>: 1/1<br>");
                    sb.append("<u><b>Next Level</b></u>: 1/1 Haste<br>");
                    sb.append("<u><b>Can learn</b></u>: Whenever this creature attacks alone,<br> it gets +2/+0 until end of turn.");
                } else if (questData.getHoundPetLevel() == 1) {
                    sb.append("Gives haste to your hound.<br>");
                    sb.append("<u><b>Level 2</b></u>: 1/1 Haste<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/1 Haste<br>");
                    sb.append("<u><b>Can learn</b></u>: Whenever this creature attacks alone,<br> it gets +2/+0 until end of turn.");
                } else if (questData.getHoundPetLevel() == 2) {
                    sb.append("Improve the attack power of your hound.<br>");
                    sb.append("<u><b>Level 3</b></u>: 2/1 Haste<br>");
                    sb.append("<u><b>Next Level</b></u>: 2/1 Whenever this creature attacks<br> alone, it gets +2/+0 until end of turn.<br>");
                } else if (questData.getHoundPetLevel() == 3) {
                    sb.append("Greatly improves your hound's attack power if it<br> attacks alone.<br>");
                    sb.append("<u><b>Level 4</b></u>: 2/1 Haste, whenever this creature attacks alone, it gets +2/+0 until end of turn.<br>");
                }
                break;
        }

        sb.append("</html>");
        return sb.toString();
    }

    private int getPrice(int petType) {
        int l = 0;
        switch (petType) {
            case WOLF:
                if (questData.getWolfPetLevel() == 0)
                    l = 250;
                else if (questData.getWolfPetLevel() == 1)
                    l = 250;
                else if (questData.getWolfPetLevel() == 2)
                    l = 500;
                else if (questData.getWolfPetLevel() == 3)
                    l = 550;
                break;
            case CROC:
                if (questData.getCrocPetLevel() == 0)
                    l = 250;
                else if (questData.getCrocPetLevel() == 1)
                    l = 300;
                else if (questData.getCrocPetLevel() == 2)
                    l = 450;
                else if (questData.getCrocPetLevel() == 3)
                    l = 600;
                break;
            case BIRD:
                if (questData.getBirdPetLevel() == 0)
                    l = 200;
                else if (questData.getBirdPetLevel() == 1)
                    l = 300;
                else if (questData.getBirdPetLevel() == 2)
                    l = 450;
                else if (questData.getBirdPetLevel() == 3)
                    l = 400;
                break;
            case HOUND:
                if (questData.getHoundPetLevel() == 0)
                    l = 200;
                else if (questData.getHoundPetLevel() == 1)
                    l = 350;
                else if (questData.getHoundPetLevel() == 2)
                    l = 450;
                else if (questData.getHoundPetLevel() == 3)
                    l = 750;
                break;
        }
        return l;
    }


    private String getImageString(int petType) {
        String s = null;

        switch (petType) {
            case WOLF:
                if (questData.getWolfPetLevel() == 0)
                    s = "g_1_1_wolf_pet_small.jpg";
                else if (questData.getWolfPetLevel() == 1)
                    s = "g_1_2_wolf_pet_small.jpg";
                else if (questData.getWolfPetLevel() == 2)
                    s = "g_2_2_wolf_pet_small.jpg";
                else if (questData.getWolfPetLevel() == 3)
                    s = "g_2_2_wolf_pet_flanking_small.jpg";
                break;
            case CROC:
                if (questData.getCrocPetLevel() == 0)
                    s = "b_1_1_crocodile_pet_small.jpg";
                else if (questData.getCrocPetLevel() == 1)
                    s = "b_2_1_crocodile_pet_small.jpg";
                else if (questData.getCrocPetLevel() == 2)
                    s = "b_3_1_crocodile_pet_small.jpg";
                else if (questData.getCrocPetLevel() == 3)
                    s = "b_3_1_crocodile_pet_swampwalk_small.jpg";
                break;
            case BIRD:
                if (questData.getBirdPetLevel() == 0)
                    s = "w_0_1_bird_pet_small.jpg";
                else if (questData.getBirdPetLevel() == 1)
                    s = "w_1_1_bird_pet_small.jpg";
                else if (questData.getBirdPetLevel() == 2)
                    s = "w_2_1_bird_pet_small.jpg";
                else if (questData.getBirdPetLevel() == 3)
                    s = "w_2_1_bird_pet_first_strike_small.jpg";
            case HOUND:
                if (questData.getHoundPetLevel() == 0)
                    s = "r_1_1_hound_pet_small.jpg";
                else if (questData.getHoundPetLevel() == 1)
                    s = "r_1_1_hound_pet_haste_small.jpg";
                else if (questData.getHoundPetLevel() == 2)
                    s = "r_2_1_hound_pet_small.jpg";
                else if (questData.getHoundPetLevel() == 3)
                    s = "r_2_1_hound_pet_alone_small.jpg";
                break;
        }
        return s;
    }

    public static final int WOLF = 1;
    public static final int CROC = 2;
    public static final int HOUND = 3;
    public static final int BIRD = 4;
}
