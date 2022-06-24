package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import forge.Forge;
import forge.StaticData;
import forge.adventure.data.RewardData;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.adventure.util.RewardActor;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.item.PaperCard;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class SpellSmithScene extends UIScene {
    private List<PaperCard> cardPool = new ArrayList<>();
    private Label goldLabel;
    private TextButton pullButton;
    private ScrollPane rewardDummy;
    private RewardActor rewardActor;
    SelectBox<CardEdition> editionList;
    //Button containers.
    final private HashMap<String, TextButton> rarityButtons = new HashMap<>();
    final private HashMap<String, TextButton> costButtons   = new HashMap<>();
    final private HashMap<String, TextButton> colorButtons  = new HashMap<>();
    //Filter variables.
    private String edition = "";
    private String rarity  = "";
    private int cost_low   = -1;
    private int cost_high  = 9999;
    //Other
    private float basePrice  = 125f;
    private int currentPrice = 0;

    public SpellSmithScene() { super(Forge.isLandscapeMode() ? "ui/spellsmith.json" : "ui/spellsmith_portrait.json"); }

    public boolean done() {
        if(rewardActor != null) rewardActor.remove();
        cardPool.clear(); //Get rid of cardPool, filtering is fast enough to justify keeping it cached.
        Forge.switchToLast();
        return true;
    }

    private boolean selectRarity(String what){
        for(Map.Entry<String, TextButton> B : rarityButtons.entrySet())
            B.getValue().setColor(Color.WHITE);
        switch(what){
            case "BCommon":
                if(rarity.equals("C")) { rarity = ""; return false; }
                rarity = "C"; break;
            case "BUncommon":
                if(rarity.equals("U")) { rarity = ""; return false; }
                rarity = "U"; break;
            case "BRare":
                if(rarity.equals("R")) { rarity = ""; return false; }
                rarity = "R"; break;
            case "BMythic":
                if(rarity.equals("M")) { rarity = ""; return false; }
                rarity = "M"; break;
            default:
                rarity = ""; break;
        }
        return true;
    }

    private void selectColor(String what){
        TextButton B = colorButtons.get(what);
        switch(what){
            case "BColorless":
                if(B.getColor().equals(Color.RED)) B.setColor(Color.WHITE); else {
                    for (Map.Entry<String, TextButton> BT : colorButtons.entrySet())
                        BT.getValue().setColor(Color.WHITE);
                    B.setColor(Color.RED);
                }
                break;
            case "BBlack":
            case "BBlue":
            case "BGreen":
            case "BRed":
            case "BWhite":
                if(B.getColor().equals(Color.RED)) B.setColor(Color.WHITE); else B.setColor(Color.RED);
                break;

        }
    }

    private boolean selectCost(String what){
        for(Map.Entry<String, TextButton> B : costButtons.entrySet())
            B.getValue().setColor(Color.WHITE);
        switch(what){
            case "B02":
                if(cost_low == 0 && cost_high == 2) { cost_low = -1; cost_high = 9999; return false; }
                cost_low = 0; cost_high = 2; break;
            case "B35":
                if(cost_low == 3 && cost_high == 5) { cost_low = -1; cost_high = 9999; return false; }
                cost_low = 3; cost_high = 5; break;
            case "B68":
                if(cost_low == 6 && cost_high == 8) { cost_low = -1; cost_high = 9999; return false; }
                cost_low = 6; cost_high = 8; break;
            case "B9X":
                if(cost_low == 9 && cost_high == 9999) { cost_low = -1; cost_high = 9999; return false; }
                cost_low = 9; cost_high = 9999; break;
            default:
                cost_low = -1; break;
        }
        return true;
    }

    @Override
    public void enter(){
        edition = "";
        cost_low = -1; cost_high = 9999;
        rarity = "";
        currentPrice = (int)basePrice;

        for(Map.Entry<String, TextButton> B : colorButtons.entrySet())  B.getValue().setColor(Color.WHITE);
        for(Map.Entry<String, TextButton> B : costButtons.entrySet())   B.getValue().setColor(Color.WHITE);
        for(Map.Entry<String, TextButton> B : rarityButtons.entrySet()) B.getValue().setColor(Color.WHITE);
        editionList.setColor(Color.WHITE);
        filterResults();
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        List<CardEdition> editions = StaticData.instance().getSortedEditions();
        editions = editions.stream().filter(input -> {
           if(input == null) return false;
           return(!Config.instance().getConfigData().restrictedEditions.contains(input.getCode()));
        }).collect(Collectors.toList());
        editionList = ui.findActor("BSelectPlane");
        rewardDummy = ui.findActor("RewardDummy");
        rewardDummy.setVisible(false);
        editionList.clearItems();
        editionList.showScrollPane();
        editionList.setItems(editions.toArray(new CardEdition[editions.size()]));
        editionList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor){
                CardEdition E = editionList.getSelected();
                edition = E.getCode();
                editionList.setColor(Color.RED);
                filterResults();
            }
        });

        goldLabel  = ui.findActor("gold");
        pullButton = ui.findActor("pull");
        pullButton.setDisabled(true);
        goldLabel.setText("Gold: "+ Current.player().getGold());
        for(String i : new String[]{"BBlack", "BBlue", "BGreen", "BRed", "BWhite", "BColorless"} ){
            TextButton button = ui.findActor(i);
            if(button != null){
                colorButtons.put(i, button);
                button.addListener(new ClickListener() {
                   @Override
                   public void clicked(InputEvent event, float x, float y){
                       selectColor(i);
                       filterResults();
                   }
                });
            }
        }
        for(String i : new String[]{"BCommon", "BUncommon", "BRare", "BMythic"} ){
            TextButton button = ui.findActor(i);
            if(button != null) {
                rarityButtons.put(i, button);
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if(selectRarity(i)) button.setColor(Color.RED);
                        filterResults();
                    }
                });
            }
        }
        for(String i : new String[]{"B02", "B35", "B68", "B9X"} ){
            TextButton button = ui.findActor(i);
            if(button != null) {
                costButtons.put(i, button);
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if(selectCost(i)) button.setColor(Color.RED);
                        filterResults();
                    }
                });
            }
        }

        ui.onButtonPress("done", new Runnable() {
            @Override
            public void run() {
                SpellSmithScene.this.done();
            }
        });
        ui.onButtonPress("pull", new Runnable() {
            @Override
            public void run() { SpellSmithScene.this.pullCard(); }
        });
        ui.onButtonPress("BResetEdition", new Runnable() {
            @Override
            public void run() {
                editionList.setColor(Color.WHITE);
                edition = "";
                filterResults();
            }
        });

    }


    public void filterResults() {
        RewardData R = new RewardData();
        Iterable<PaperCard> P = R.getAllCards();
        float totalCost = basePrice * Current.player().goldModifier();
        final List<String> colorFilter = new ArrayList<>();
        for(Map.Entry<String, TextButton> B : colorButtons.entrySet())
            switch (B.getKey()){
                case "BColorless":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("Colorless");
                    continue;
                case "BBlack":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("Black");
                    break;
                case "BBlue":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("Blue");
                    break;
                case "BGreen":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("Green");
                    break;
                case "BRed":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("Red");
                    break;
                case "BWhite":
                    if(B.getValue().getColor().equals(Color.RED)) colorFilter.add("White");
                    break;
            }
        P = StreamSupport.stream(P.spliterator(), false).filter(input -> {
            //L|Basic Land, C|Common, U|Uncommon, R|Rare, M|Mythic Rare, S|Special, N|None
            if (input == null) return false;
            if(!edition.isEmpty()) if (!input.getEdition().equals(edition)) return false;
            if(colorFilter.size() > 0) if(input.getRules().getColor() != ColorSet.fromNames(colorFilter)) return false;
            if(!rarity.isEmpty()) if (!input.getRarity().toString().equals(rarity)) return false;
            if(cost_low > -1) {
                if (!(input.getRules().getManaCost().getCMC() >= cost_low && input.getRules().getManaCost().getCMC() <= cost_high))
                    return false;
            }
            return true;
        }).collect(Collectors.toList());
        //Stream method is very fast, might not be necessary to precache anything.
        if(!edition.isEmpty()) totalCost *= 4.0f; //Edition select cost multiplier. This is a huge factor, so it's most expensive.
        if(colorFilter.size() > 0) totalCost *= Math.min(colorFilter.size() * 2.5f, 6.0f); //Color filter cost multiplier.
        if(!rarity.isEmpty()){ //Rarity cost multiplier.
            switch(rarity){
                case "C": totalCost *= 1.5f; break;
                case "U": totalCost *= 2.5f; break;
                case "R": totalCost *= 4.0f; break;
                case "M": totalCost *= 5.5f; break;
                default: break;
            }
        }
        if(cost_low > -1) totalCost *= 2.5f; //And CMC cost multiplier.
        cardPool = StreamSupport.stream(P.spliterator(), false).collect(Collectors.toList());
        pullButton.setText("Pull (" + cardPool.size() + ") " + totalCost + "G");
        currentPrice = (int)totalCost;
        pullButton.setDisabled(false);
        if(!(cardPool.size() > 0) || Current.player().getGold() < totalCost)
            pullButton.setDisabled(true);
    }

    public void pullCard() {
        PaperCard P = cardPool.get(MyRandom.getRandom().nextInt(cardPool.size())); //Don't use the standard RNG.
        Reward R = new Reward(P);
        Current.player().addReward(R);
        Current.player().takeGold(currentPrice);
        if(Current.player().getGold() < currentPrice) pullButton.setDisabled(true);
        if(rewardActor != null) rewardActor.remove();
        rewardActor = new RewardActor(R, true);
        rewardActor.flip(); //Make it flip so it draws visual attention, why not.
        rewardActor.setBounds(rewardDummy.getX(), rewardDummy.getY(), rewardDummy.getWidth(), rewardDummy.getHeight());
        stage.addActor(rewardActor);
    }
}
