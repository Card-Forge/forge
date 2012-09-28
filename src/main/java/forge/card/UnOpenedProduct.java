package forge.card;

import java.util.List;

import com.google.common.base.Function;

import forge.item.CardPrinted;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UnOpenedProduct {

    private final Function<BoosterGenerator, List<CardPrinted>> openBooster;
    private final BoosterGenerator generator;
    private final BoosterData booster;

    public UnOpenedProduct(Function<BoosterGenerator, List<CardPrinted>> identityPick, BoosterGenerator bpFull) {
        openBooster = identityPick;
        generator = bpFull;
        booster = null;
    }

    /**
     * TODO: Write javadoc for Constructor.
     * @param boosterData
     */
    public UnOpenedProduct(BoosterData boosterData) {
        booster = boosterData;
        openBooster = null;
        generator = new BoosterGenerator(boosterData.getEditionFilter());
    }

    public List<CardPrinted> open() {
        return openBooster != null ? openBooster.apply(generator) : generator.getBoosterPack(booster);
    }

    /**
     * Like open, can define whether is human or not.
     * @param isHuman
     *      boolean, is human player?
     * @param partialities
     *      known partialities for the AI.
     * @return List, list of cards.
     */
    public List<CardPrinted> open(final boolean isHuman, List<String> partialities) {
        return open();
    }

}
