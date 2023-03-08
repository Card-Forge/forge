package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.model.FModel;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EffectData implements Serializable {
    public String name = null;           //Effect name. Can be checked for.
    //Duel effects.
    public int lifeModifier = 0;         //Amount to add to starting Life.
    public int changeStartCards = 0;     //Amount to add to starting hand size.
    public String[] startBattleWithCard; //Cards that start in the Battlefield.
    public String[] startBattleWithCardInCommandZone; //Cards that start in the Command Zone of the Battlefield.
    //Map only effects.
    public boolean colorView = false;    //Allows to display enemy colors on the map.
    public float moveSpeed = 1.0f;       //Change of movement speed. Map only.
    public float goldModifier = -1.0f;   //Modifier for shop discounts.
    public int cardRewardBonus = 0;    //Bonus "DeckCard" drops. Max 3.
    public int extraManaShards = 0; //Mana Shard tokens available to spend in battle

    //Opponent field.
    public EffectData opponent;          //Effects to be applied to the opponent's side.

    public EffectData() {}
    public EffectData(EffectData effect) {
        name=effect.name;
        lifeModifier=effect.lifeModifier;
        changeStartCards=effect.changeStartCards;
        startBattleWithCard=effect.startBattleWithCard;
        colorView=effect.colorView;
        opponent = (effect.opponent == null) ? null : new EffectData(effect.opponent);
        extraManaShards = effect.extraManaShards;
    }

    public Array<IPaperCard> startBattleWithCards() {
        Array<IPaperCard> startCards=new Array<>(IPaperCard.class);
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

    public Array<IPaperCard> startBattleWithCardsInCommandZone(){
        Array<IPaperCard> startCardsInCommandZone=new Array<>(IPaperCard.class);
        if(startBattleWithCardInCommandZone != null) {
            for (String name:startBattleWithCardInCommandZone) {
                PaperCard C = FModel.getMagicDb().getCommonCards().getCard(name);
                if(C != null)
                    startCardsInCommandZone.add(C);
                else {
                    PaperToken T = FModel.getMagicDb().getAllTokens().getToken(name);
                    if (T != null) startCardsInCommandZone.add(T);
                    else System.err.print("Can not find card \"" + name + "\"\n");
                }
            }
        }
        return startCardsInCommandZone;
    }

    public String itemize(Array<IPaperCard> paperCards) {
        StringBuilder ret = new StringBuilder();
        Map<IPaperCard, Integer> duplicateCountMap = Arrays.stream(paperCards.toArray()).collect(Collectors.toMap(Function.identity(), cards -> 1, Math::addExact));
        duplicateCountMap.forEach((key, value) -> ret.append("\n").append(value).append("x ").append(key));
        return ret.toString();
    }

    public String getDescription() {
        String description = "";
        if(colorView) description += "Manasight.\n";
        if(lifeModifier != 0)
            description += "[+Life] " + ((lifeModifier > 0) ? "+" : "") + lifeModifier + "\n";
        if(startBattleWithCard != null && startBattleWithCard.length != 0)
            description+="Battlefield:" + itemize(startBattleWithCards()) + "\n";
        if(startBattleWithCardInCommandZone != null && startBattleWithCardInCommandZone.length != 0)
            description+="Command:" + itemize(startBattleWithCardsInCommandZone()) + "\n";
        if(changeStartCards != 0)
            description+="Starting hand: " + changeStartCards + "\n";
        if(moveSpeed!=0 && moveSpeed != 1)
            description+="[+MovementSpeed] " + ((moveSpeed > 0) ? "+" : "") + Math.round((moveSpeed-1.f)*100) + "%\n";
        if(goldModifier > 0.0f)
            description+="Shop discount: x" + (goldModifier) + "\n";
        if(cardRewardBonus > 0)
            description += "Bonus enemy deck rewards: +" + (cardRewardBonus) + "\n";
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


