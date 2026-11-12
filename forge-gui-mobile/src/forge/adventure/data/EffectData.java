package forge.adventure.data;

import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.model.FModel;
import forge.util.FileSection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EffectData implements Serializable {
    //Saves store items with their EffectData. Keep this value so saves made before a field was added still load.
    private static final long serialVersionUID = -1350202758899129612L;
    public String name = null;           //Effect name. Can be checked for.
    public String description;           //Shown in place of the generic intro line in effect dialogs and on the Statistics screen.

    //Duel effects.
    public int lifeModifier = 0;         //Amount to add to starting Life.
    public int changeStartCards = 0;     //Amount to add to starting hand size.
    public String[] startBattleWithCard; //Cards that start in the Battlefield.
    public String[] startBattleWithCardInCommandZone; //Cards that start in the Command Zone of the Battlefield.
    public String[] startBattleScript; //Card-script lines run after mulligans. "SVar:Name:Value" lines set SVars the others can use.
    public boolean allowIllegalCommander = false; //Keeps a commander that is not legal: any creature or planeswalker.

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
        description=effect.description;
        lifeModifier=effect.lifeModifier;
        changeStartCards=effect.changeStartCards;
        startBattleWithCard=effect.startBattleWithCard;
        startBattleScript=effect.startBattleScript;
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

    public String getIntro(String fallback) {
        return description != null ? description : fallback;
    }

    public String getDescription() {
        return getDescription(null, "Gives Opponent:");
    }

    /** Describes an effect whose top level is the player's and whose opponent block goes to every enemy. */
    public String getPlayerSideDescription() {
        return getDescription(Forge.getLocalizer().getMessage("lblEffectDataHeaderPlayer"), Forge.getLocalizer().getMessage("lblEffectDataHeader"));
    }

    /** Describes an effect whose top level goes to every enemy and whose opponent block is the player's. */
    public String getEnemySideDescription() {
        return getDescription(Forge.getLocalizer().getMessage("lblEffectDataHeader"), Forge.getLocalizer().getMessage("lblEffectDataHeaderPlayer"));
    }

    /** @param ownHeader shown above the top level only when it has text; null for none */
    private String getDescription(String ownHeader, String opponentHeader) {
        String description = "";
        if(colorView) description += "Manasight.\n";
        if(lifeModifier != 0)
            description += "[+Life] " + ((lifeModifier > 0) ? "+" : "") + lifeModifier + "\n";
        if(startBattleWithCard != null && startBattleWithCard.length != 0)
            description+="Battlefield:" + itemize(startBattleWithCards()) + "\n";
        if(startBattleWithCardInCommandZone != null && startBattleWithCardInCommandZone.length != 0)
            description+="Command:" + itemize(startBattleWithCardsInCommandZone()) + "\n";
        if(startBattleScript != null) {
            for (String line : startBattleScript) {
                String spellDescription = line.startsWith("SVar:") ? null
                        : FileSection.parseToMap(line, FileSection.DOLLAR_SIGN_KV_SEPARATOR).get("SpellDescription");
                if (spellDescription != null)
                    description += spellDescription + "\n";
            }
        }
        if(changeStartCards != 0)
            description+="Starting hand: " + changeStartCards + "\n";
        if(allowIllegalCommander)
            description+="Any creature or planeswalker can be commander\n";
        if(moveSpeed!=0 && moveSpeed != 1)
            description+="[+MovementSpeed] " + ((moveSpeed > 0) ? "+" : "") + Math.round((moveSpeed-1.f)*100) + "%\n";
        if(goldModifier > 0.0f)
            description+="Shop discount: x" + (goldModifier) + "\n";
        if(cardRewardBonus > 0)
            description += Forge.getLocalizer().getMessage("advBonusEnemyDeckRewards", cardRewardBonus);
        if(ownHeader != null && !description.isEmpty())
            description = ownHeader + "\n" + description;
        if(this.opponent != null) {
            String oppEffect = this.opponent.getDescription();
            description += opponentHeader + "\n";
            if(!oppEffect.isEmpty()) {
                description += oppEffect;
            }
        }
        return description;
    }

}
