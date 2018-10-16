package forge.item.generation;

import forge.card.CardEdition;
import forge.item.BoosterPack;
import forge.item.PaperCard;

import java.util.List;

public class ChaosBoosterSupplier implements IUnOpenedProduct {
    private List<CardEdition> sets;

    public ChaosBoosterSupplier(List<CardEdition> sets) {
        this.sets = sets;
    }

    @Override
    public List<PaperCard> get() {
        if (sets.size() == 0) {
            System.out.println("No chaos boosters left to supply.");
            return null;
        }
        final CardEdition set = sets.remove(0);
        final BoosterPack pack = new BoosterPack(set.getCode(), set.getBoosterTemplate());
        return pack.getCards();
    }
}
