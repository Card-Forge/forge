package forge.gamemodes.limited;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.deck.DeckFormat;
import forge.deck.generation.DeckGenPool;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;

import java.util.List;

/**
 * Created by maustin on 28/02/2018.
 */
public class CardThemedConquestDeckBuilder extends CardThemedDeckBuilder {

    public CardThemedConquestDeckBuilder(PaperCard commanderCard0, final List<PaperCard> dList, GameFormat gameFormat, boolean isForAI, DeckFormat format) {
        super(new DeckGenPool(
                Iterables.filter(FModel.getMagicDb().getCommonCards().getUniqueCards(),
                        gameFormat.getFilterPrinted())
        ), format);
        this.availableList = dList;
        keyCard = commanderCard0;
        secondKeyCard = null;
        // remove Unplayables
        if(isForAI) {
            final Iterable<PaperCard> playables = Iterables.filter(availableList,
                    Predicates.compose(CardRulesPredicates.IS_KEPT_IN_AI_DECKS, PaperCard.FN_GET_RULES));
            this.aiPlayables = Lists.newArrayList(playables);
        }else{
            this.aiPlayables = Lists.newArrayList(availableList);
        }
        this.availableList.removeAll(aiPlayables);
        targetSize=format.getMainRange().getMinimum();
        colors = keyCard.getRules().getColorIdentity();
        colors = ColorSet.fromMask(colors.getColor() | keyCard.getRules().getColorIdentity().getColor());
        if(secondKeyCard!=null) {
            colors = ColorSet.fromMask(colors.getColor() | secondKeyCard.getRules().getColorIdentity().getColor());
            targetSize--;
        }
        numSpellsNeeded = ((Double)Math.floor(targetSize*(getCreaturePercentage()+getSpellPercentage()))).intValue();
        numCreaturesToStart = ((Double)Math.ceil(targetSize*(getCreaturePercentage()))).intValue();
        landsNeeded = ((Double)Math.ceil(targetSize*(getLandPercentage()))).intValue();
        if (logColorsToConsole) {
            System.out.println(keyCard.getName());
            System.out.println("Pre Colors: " + colors.toEnumSet().toString());
        }
        findBasicLandSets();
    }

    @Override
    protected void addKeyCards(){
        //do nothing as keycards are commander/partner and are added by the DeckGenUtils
    }

    @Override
    protected void addLandKeyCards(){
        //do nothing as keycards are commander/partner and are added by the DeckGenUtils
    }

    @Override
    protected void addThirdColorCards(int num) {
        //do nothing as we cannot add extra colours beyond commanders
    }

    @Override
    protected void updateColors(){
        //do nothing as we cannot deviate from commander colours
    }
    /**
     * Generate a descriptive name.
     *
     * @return name
     */
    @Override
    protected String generateName() {
        return keyCard.getName() +" based commander deck";
    }

}
