package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.model.FModel;

import java.io.Serializable;

public class EffectData implements Serializable {
    public String name = null;           //Effect name. Can be checked for.
    //Duel effects.
    public int lifeModifier = 0;         //Amount to add to starting Life.
    public int changeStartCards = 0;     //Amount to add to starting hand size.
    public String[] startBattleWithCard; //Cards that start in the Battlefield.
    //Map only effects.
    public boolean colorView = false;    //Allows to display enemy colors on the map (TODO)
    public float moveSpeed = 1.0f;       //Change of movement speed. Map only.
    //Opponent field.
    public EffectData opponent;          //Effects to be applied to the opponent's side.

    public EffectData()
    {

    }
    public EffectData(EffectData effect) {

        name=effect.name;
        lifeModifier=effect.lifeModifier;
        changeStartCards=effect.changeStartCards;
        startBattleWithCard=effect.startBattleWithCard;
        colorView=effect.colorView;
        opponent=opponent==null?null:new EffectData(effect.opponent);
    }

    public Array<IPaperCard> startBattleWithCards() {
        Array<IPaperCard> startCards=new Array<>();
        if(startBattleWithCard != null) {
            for (String name:startBattleWithCard) {
                PaperCard C = FModel.getMagicDb().getCommonCards().getCard(name);
                if(C != null)
                    startCards.add(C);
                else {
                    PaperToken T = FModel.getMagicDb().getAllTokens().getToken(name);
                    if (T != null) startCards.add(T);
                    else System.err.print("Can not find card \"" + name + "\"\n");
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
        if(this.name != null && !this.name.isEmpty())
            description += this.name + "\n";
        if(this.colorView)
            description += "Manasight.\n";
        if(this.lifeModifier != 0)
            description += "Life: " + ((this.lifeModifier > 0) ? "+" : "") + this.lifeModifier + "\n";
        if(this.startBattleWithCard != null && this.startBattleWithCard.length != 0)
            description+="Cards on battlefield: \n" + this.cardNames() + "\n";
        if(this.moveSpeed!=0 && this.moveSpeed != 1)
            description+="Movement speed: " + ((this.lifeModifier > 0) ? "+" : "") + Math.round((this.moveSpeed-1.f)*100) + "%\n";
        if(this.changeStartCards != 0)
            description+="Starting hand: " + this.changeStartCards + "\n";
        if(this.opponent != null) {
            String oppEffect = this.opponent.getDescription();
            description += "Gives Opponent:\n";
            if(!oppEffect.isEmpty()) {
                description += oppEffect;
            }
        }
        return description;
    }

}


