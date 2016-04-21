package forge;

import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.PrintSheet;
import forge.item.BoosterBox;
import forge.item.FatPack;
import forge.item.SealedProduct;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


 /**
 * The class holding game invariants, such as cards, editions, game formats. All that data, which is not supposed to be changed by player
 *
 * @author Max
 */
public class StaticData {
    private final CardDb commonCards;
    private final CardDb variantCards;
    private final CardDb allCards;
    private final CardEdition.Collection editions;
    private final IStorage<SealedProduct.Template> boosters;
    private final IStorage<SealedProduct.Template> specialBoosters;
    private final IStorage<SealedProduct.Template> tournaments;
    private final IStorage<FatPack.Template> fatPacks;
    private final IStorage<BoosterBox.Template> boosterBoxes;
    private final IStorage<PrintSheet> printSheets;

    private static StaticData lastInstance = null;

    public StaticData(CardStorageReader reader, String editionFolder, String blockDataFolder) {
        this.editions = new CardEdition.Collection(new CardEdition.Reader(new File(editionFolder)));
        lastInstance = this;

        final Map<String, CardRules> regularCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, CardRules> variantsCards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, CardRules> fullCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);

        for (CardRules card : reader.loadCards()) {
            if (null == card) continue;

            final String cardName = card.getName();
            if ( card.isVariant() ) {
                variantsCards.put(cardName, card);
            }
            else {
                regularCards.put(cardName, card);
            }
            fullCards.put(cardName, card);
        }

        commonCards = new CardDb(regularCards, editions);
        variantCards = new CardDb(variantsCards, editions);
        allCards = new CardDb(fullCards, editions);

        //muse initialize after establish field values for the sake of card image logic
        commonCards.initialize(false, false);
        variantCards.initialize(false, false);
        allCards.initialize( false, false);

        this.boosters = new StorageBase<>("Boosters", editions.getBoosterGenerator());
        this.specialBoosters = new StorageBase<>("Special boosters", new SealedProduct.Template.Reader(new File(blockDataFolder, "boosters-special.txt")));
        this.tournaments = new StorageBase<>("Starter sets", new SealedProduct.Template.Reader(new File(blockDataFolder, "starters.txt")));
        this.fatPacks = new StorageBase<>("Fat packs", new FatPack.Template.Reader(blockDataFolder + "fatpacks.txt"));
        this.boosterBoxes = new StorageBase<>("Booster boxes", new BoosterBox.Template.Reader(blockDataFolder + "boosterboxes.txt"));
        this.printSheets = new StorageBase<>("Special print runs", new PrintSheet.Reader(new File(blockDataFolder, "printsheets.txt")));
    }

    public static StaticData instance() {
        return lastInstance;
    }

    public final CardEdition.Collection getEditions() {
        return this.editions;
    }

    private List<CardEdition> sortedEditions;
    public final List<CardEdition> getSortedEditions() {
        if (sortedEditions == null) {
            sortedEditions = new ArrayList<CardEdition>();
            for (CardEdition set : editions) {
                sortedEditions.add(set);
            }
            Collections.sort(sortedEditions);
            Collections.reverse(sortedEditions); //put newer sets at the top
        }
        return sortedEditions;
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public IStorage<FatPack.Template> getFatPacks() {
        return fatPacks;
    }

    public IStorage<BoosterBox.Template> getBoosterBoxes() {
        return boosterBoxes;
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public final IStorage<SealedProduct.Template> getTournamentPacks() {
        return tournaments;
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.item.SealedProduct.Template}> */
    public final IStorage<SealedProduct.Template> getBoosters() {
        return boosters;
    }

    public final IStorage<SealedProduct.Template> getSpecialBoosters() {
        return specialBoosters;
    }

    public IStorage<PrintSheet> getPrintSheets() {
        return printSheets;
    }

    public CardDb getCommonCards() {
        return commonCards;
    }

    public CardDb getVariantCards() {
        return variantCards;
    }

    public CardDb getAllCards() {
         return allCards;
     }

}
