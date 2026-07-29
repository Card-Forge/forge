package forge.gamemodes.limited;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.deck.DeckFormat;
import forge.deck.generation.DeckGenPool;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;

/**
 * Created by maustin on 28/02/2018.
 */
public class CardThemedCommanderDeckBuilder extends CardThemedDeckBuilder {

    public CardThemedCommanderDeckBuilder(PaperCard commanderCard0, PaperCard partner0, final List<PaperCard> dList, boolean isForAI, DeckFormat format) {
        super(new DeckGenPool(FModel.getMagicDb().getCommonCards().getUniqueCards()), format);
        this.availableList = new ArrayList<>(dList);
        keyCard = commanderCard0;
        secondKeyCard = partner0;
        // remove Unplayables
        if(isForAI) {
            this.aiPlayables = availableList.stream()
                    .filter(PaperCardPredicates.fromRules(CardRulesPredicates.IS_KEPT_IN_AI_DECKS))
                    .collect(Collectors.toList());
        }else{
            this.aiPlayables = Lists.newArrayList(availableList);
        }
        this.availableList.removeAll(aiPlayables);
        this.aiPlayables = uniqueCardNamesForSingletonDeck(aiPlayables);
        this.availableList = uniqueCardNamesForSingletonDeck(availableList);
        targetSize=format.getMainRange().getMinimum();
        colors = keyCard.getRules().getColorIdentity();
        if (secondKeyCard != null && !format.equals(DeckFormat.Oathbreaker)) {
            colors = ColorSet.combine(colors, secondKeyCard.getRules().getColorIdentity());
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
    protected void extendPlaysets(int numSpellsNeeded) {
        // Commander-family formats are singleton except for basic lands and cards
        // with explicit deckbuilding exceptions. Do not fill gaps by duplicating
        // cards already selected for the main deck.
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

    private List<PaperCard> uniqueCardNamesForSingletonDeck(final List<PaperCard> cards) {
        final List<PaperCard> result = new ArrayList<>();
        final Map<String, Integer> countsByName = new HashMap<>();
        countsByName.put(keyCard.getName(), 1);
        if (secondKeyCard != null) {
            countsByName.put(secondKeyCard.getName(), 1);
        }

        for (final PaperCard card : cards) {
            final int maxCopies = format.getMaxCardCopies(card);
            final String name = card.getName();
            final int currentCount = countsByName.getOrDefault(name, 0);
            if (currentCount < maxCopies) {
                result.add(card);
                countsByName.put(name, currentCount + 1);
            }
        }

        return result;
    }

}
