package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.item.IPaperCard;
import forge.model.FModel;

public class EffectData {
    public int lifeModifier = 0; //Amount to add to starting Life.
    public int changeStartCards = 0; //Amount to add to starting hand size.
    public float moveSpeed = 1.0f; //Change of movement speed in the map.
    public String[] startBattleWithCard; //Cards that start in the Battlefield.
    public EffectData opponent; //Repeat effects to be applied to the opponent's side.

    public Array<IPaperCard> startBattleWithCards() {
        Array<IPaperCard> startCards=new Array<>();
        if(startBattleWithCard != null) {
            for (String name:startBattleWithCard) {
                if(FModel.getMagicDb().getCommonCards().contains(name))
                    startCards.add(FModel.getMagicDb().getCommonCards().getCard(name));
                else if (FModel.getMagicDb().getAllTokens().containsRule(name))
                    startCards.add(FModel.getMagicDb().getAllTokens().getToken(name));
                else {
                    System.err.print("Can not find card "+name+"\n");
                }
            }
        }
        return startCards;
    }

    public String cardNames() {
        String ret = "";
        Array<IPaperCard> array=startBattleWithCards();
        for(int i =0;i<array.size;i++) {
            ret+=array.get(i).toString();
            if(i!=array.size-1) ret+=" , ";
        }
        return ret;
    }

    public String getDescription() {
        String description = "";
        if(this.lifeModifier != 0)
            description += "Life: " + ((this.lifeModifier > 0) ? "+" : "") + this.lifeModifier + "\n";
        if(this.startBattleWithCard != null && this.startBattleWithCard.length != 0)
            description+="Cards on battlefield: \n" + this.cardNames() + "\n";
        if(this.moveSpeed!=0 && this.moveSpeed != 1)
            description+="Movement speed: " + ((this.lifeModifier > 0) ? "+" : "") + Math.round((this.moveSpeed-1.f)*100) + "%\n";
        if(this.changeStartCards != 0)
            description+="Starting hand: " + this.changeStartCards + "\n";
        if(this.opponent != null) {
            String oppEffect=this.opponent.getDescription();
            if(oppEffect != "") {
                description += "Gives Opponent:\n";
                description += oppEffect;
            }
        }
        return description;
    }

}


