package forge.item.generation;

import java.util.List;

import forge.card.CardEdition;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.util.BagRandomizer;

public class ChaosBoosterSupplier implements IUnOpenedProduct {
    private BagRandomizer<CardEdition> randomizer;

    public ChaosBoosterSupplier(Iterable<CardEdition> sets) throws IllegalArgumentException {
        randomizer = new BagRandomizer<>(sets);
    }

    @Override
    public List<PaperCard> get() {
        final CardEdition set = randomizer.getNextItem();
        final BoosterPack pack = new BoosterPack(set.getCode(), set.getBoosterTemplate());
        return pack.getCards();
    }
}
