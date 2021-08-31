package forge.adventure.util;

import com.google.common.base.Predicate;
import forge.adventure.data.RewardData;
import forge.adventure.world.WorldSave;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.MagicColor;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardUtil {
    private static final class CardPredicate implements Predicate<PaperCard> {
        enum ColorType
        {
            Any,
            Colorless,
            MultiColor,
            MonoColor
        }
        private final List<CardRarity> rarities=new ArrayList<>();
        private final List<String> editions=new ArrayList<>();
        private final List<String> subType=new ArrayList<>();
        private final List<CardType.CoreType> type=new ArrayList<>();
        private final List<CardType.Supertype> superType=new ArrayList<>();
        private  int colors;
        private final ColorType colorType;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final PaperCard card) {
            if(!this.rarities.isEmpty()&&!this.rarities.contains(card.getRarity()))
                return !this.shouldBeEqual;
            if(!this.editions.isEmpty()&&!this.editions.contains(card.getEdition()))
                return !this.shouldBeEqual;
            if(!this.subType.isEmpty())
            {
                boolean found=false;
                for(String subtype:card.getRules().getType().getSubtypes())
                {
                    if(this.subType.contains(subtype))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(!this.type.isEmpty())
            {
                boolean found=false;
                for(CardType.CoreType type:card.getRules().getType().getCoreTypes())
                {
                    if(this.type.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(!this.superType.isEmpty())
            {
                boolean found=false;
                for(CardType.Supertype type:card.getRules().getType().getSupertypes())
                {
                    if(this.superType.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(this.colors!= MagicColor.ALL_COLORS)
            {
                if(!card.getRules().getColor().hasNoColorsExcept(this.colors)||card.getRules().getColor().isColorless())
                    return !this.shouldBeEqual;
            }
            if(colorType!=ColorType.Any)
            {
                switch (colorType)
                {
                    case Colorless:
                        if(!card.getRules().getColor().isColorless())
                            return !this.shouldBeEqual;
                        break;
                    case MonoColor:
                        if(!card.getRules().getColor().isMonoColor())
                            return !this.shouldBeEqual;
                        break;
                    case MultiColor:
                        if(!card.getRules().getColor().isMulticolor())
                            return !this.shouldBeEqual;
                        break;
                }
            }

            return this.shouldBeEqual;
        }

        private CardPredicate(final RewardData type, final boolean wantEqual) {
            this.shouldBeEqual = wantEqual;
            if(type.colors==null||type.colors.size==0)
            {
                this.colors=MagicColor.ALL_COLORS;
            }
            else
            {
                this.colors=0;
                for(String color:type.colors)
                {
                    colors|=MagicColor.fromName(color.toLowerCase());
                }
            }
            if(type.rarity!=null&&type.rarity.size!=0)
            {
                for(String rarity:type.rarity)
                {
                    rarities.add(CardRarity.smartValueOf(rarity));
                }
            }

            if(type.subTypes!=null&&type.subTypes.size!=0)
            {
                subType.addAll(Arrays.asList(type.subTypes.toArray()));
            }
            if(type.editions!=null&&type.editions.size!=0)
            {
                editions.addAll(Arrays.asList(type.editions.toArray()));
            }
            if(type.superTypes!=null&&type.superTypes.size!=0)
            {
                for(String string:type.superTypes)
                    superType.add(CardType.Supertype.getEnum(string));
            }
            if(type.cardTypes!=null&&type.cardTypes.size!=0)
            {
                for(String string:type.cardTypes)
                    this.type.add(CardType.CoreType.getEnum(string));
            }
            if(type.colorType!=null&&!type.colorType.isEmpty())
            {
                this.colorType=ColorType.valueOf(type.colorType);
            }
            else
            {
                this.colorType=ColorType.Any;
            }
        }
    }

    public static List<PaperCard> generateCards(Iterable<PaperCard> cards,final RewardData data, final int count)
    {

        final List<PaperCard> result = new ArrayList<>();


        for (int i=0;i<count;i++) {

            CardPredicate pre=new CardPredicate(data, true);
            PaperCard card = null;
            int lowest = Integer.MAX_VALUE;
            for (final PaperCard item : cards)
            {
                if(!pre.apply(item))
                    continue;
                int next = WorldSave.getCurrentSave().getWorld().getRandom().nextInt();
                if(next < lowest) {
                    lowest = next;
                    card = item;
                }
            }
            if (card != null )
                result.add(card);
        }

        return result;
    }
    public static int getCardPrice(PaperCard card)
    {
        switch (card.getRarity())
        {
            case BasicLand:
                return 20;
            case Common:
                return 50;
            case Uncommon:
                return 150;
            case Rare:
                return 300;
            case MythicRare:
                return 500;
            default:
                return 90000;
        }
    }
}
