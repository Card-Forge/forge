package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.model.FModel;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EffectData implements Serializable {
    //Saves store items with their EffectData. Keep this value so saves made before a field was added still load.
    private static final long serialVersionUID = -1350202758899129612L;
    public String name = null;           //Effect name. Can be checked for.

    //Duel effects.
    public int lifeModifier = 0;         //Amount to add to starting Life.
    public int changeStartCards = 0;     //Amount to add to starting hand size.
    public String[] startBattleWithCard; //Cards that start in the Battlefield.
    public String[] startBattleWithCardInCommandZone; //Cards that start in the Command Zone of the Battlefield.
    public String[] startBattleWithCardInHand; //Cards added to the hand after mulligans.
    public int maxHandSizeModifier = 0;  //Amount to add to maximum hand size, without changing starting hand size.
    public SuspendedCard[] startBattleWithCardSuspended; //Cards that start suspended in Exile.

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
        return cardsFromNames(startBattleWithCard);
    }

    public Array<IPaperCard> startBattleWithCardsInCommandZone() {
        return cardsFromNames(startBattleWithCardInCommandZone);
    }

    public Array<IPaperCard> startBattleWithCardsInHand() {
        return cardsFromNames(startBattleWithCardInHand);
    }

    public Array<Pair<IPaperCard, Integer>> startBattleWithCardsSuspended() {
        Array<Pair<IPaperCard, Integer>> cards = new Array<>();
        if (startBattleWithCardSuspended == null) {
            return cards;
        }
        for (SuspendedCard suspended : startBattleWithCardSuspended) {
            if (suspended.turns < 1) {
                System.err.print("Suspended card \"" + suspended.card + "\" needs at least 1 turn\n");
                continue;
            }
            IPaperCard card = cardFromName(suspended.card);
            if (card != null)
                cards.add(Pair.of(card, suspended.turns));
        }
        return cards;
    }

    private static Array<IPaperCard> cardsFromNames(String[] names) {
        Array<IPaperCard> cards = new Array<>(IPaperCard.class);
        if (names == null) {
            return cards;
        }
        for (String name : names) {
            IPaperCard card = cardFromName(name);
            if (card != null)
                cards.add(card);
        }
        return cards;
    }

    private static IPaperCard cardFromName(String name) {
        PaperCard C = FModel.getMagicDb().getCommonCards().getCard(name);
        if (C != null)
            return C;
        try {
            PaperToken T = FModel.getMagicDb().getAllTokens().getToken(name);
            if (T != null) return T;
            System.err.print("Can not find card/token \"" + name + "\"\n");
        } catch (Exception e) {
            //if it's not found probably the item is using funny cards and the users setting disabled non legal cards
            System.err.print("Can not find card/token \"" + name + "\"\n");
        }
        return null;
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
        if(startBattleWithCardInHand != null && startBattleWithCardInHand.length != 0)
            description+="Hand:" + itemize(startBattleWithCardsInHand()) + "\n";
        if(startBattleWithCardSuspended != null && startBattleWithCardSuspended.length != 0) {
            description+="Suspended:";
            for (Pair<IPaperCard, Integer> suspended : startBattleWithCardsSuspended())
                description+="\n" + suspended.getLeft() + " (" + suspended.getRight() + ")";
            description+="\n";
        }
        if(changeStartCards != 0)
            description+="Starting hand: " + changeStartCards + "\n";
        if(maxHandSizeModifier != 0)
            description+="Max hand size: " + maxHandSizeModifier + "\n";
        if(moveSpeed!=0 && moveSpeed != 1)
            description+="[+MovementSpeed] " + ((moveSpeed > 0) ? "+" : "") + Math.round((moveSpeed-1.f)*100) + "%\n";
        if(goldModifier > 0.0f)
            description+="Shop discount: x" + (goldModifier) + "\n";
        if(cardRewardBonus > 0)
            description += Forge.getLocalizer().getMessage("advBonusEnemyDeckRewards", cardRewardBonus);
        if(this.opponent != null) {
            String oppEffect = this.opponent.getDescription();
            description += "Gives Opponent:\n";
            if(!oppEffect.isEmpty()) {
                description += oppEffect;
            }
        }
        return description;
    }


    public static class SuspendedCard implements Serializable {
        public String card;
        public int turns;
    }
}
