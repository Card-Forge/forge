package forge;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.PrintSheet;
import forge.item.FatPack;
import forge.item.SealedProduct;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;


 /**
 * The class holding game invariants, such as cards, editions, game formats. All that data, which is not supposed to be changed by player
 * 
 * @author Max
 */
public class StaticData {
    private final CardDb commonCards;
    private final CardDb variantCards;
    private final CardEdition.Collection editions;
    private final IStorage<SealedProduct.Template> boosters;
    private final IStorage<SealedProduct.Template> specialBoosters;
    private final IStorage<SealedProduct.Template> tournaments;
    private final IStorage<FatPack.Template> fatPacks;
    private final IStorage<PrintSheet> printSheets;

    private static StaticData lastInstance = null;

    public StaticData(CardStorageReader reader, String editionFolder, String blockDataFolder) {
        this.editions = new CardEdition.Collection(new CardEdition.Reader(new File(editionFolder)));
        lastInstance = this;

        final Map<String, CardRules> regularCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);
        final Map<String, CardRules> variantsCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);

        List<CardRules> rules = reader.loadCards();
        for (CardRules card : rules) {
            if (null == card) continue;

            final String cardName = card.getName();
            if ( card.isVariant() ) {
                variantsCards.put(cardName, card);
            }
            else {
                regularCards.put(cardName, card);
            }
        }

        commonCards = new CardDb(regularCards, editions, false);
        variantCards = new CardDb(variantsCards, editions, false);

        this.boosters = new StorageBase<SealedProduct.Template>("Boosters", editions.getBoosterGenerator());
        this.specialBoosters = new StorageBase<SealedProduct.Template>("Special boosters", new SealedProduct.Template.Reader(new File(blockDataFolder, "boosters-special.txt")));
        this.tournaments = new StorageBase<SealedProduct.Template>("Starter sets", new SealedProduct.Template.Reader(new File(blockDataFolder, "starters.txt")));
        this.fatPacks = new StorageBase<FatPack.Template>("Fat packs", new FatPack.Template.Reader("res/blockdata/fatpacks.txt"));
        this.printSheets = new StorageBase<PrintSheet>("Special print runs", new PrintSheet.Reader(new File(blockDataFolder, "printsheets.txt")));
    }

    public final static StaticData instance() { 
        return lastInstance;
    }

    public final CardEdition.Collection getEditions() {
        return this.editions;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.item.FatPackTemplate}> */
    public IStorage<FatPack.Template> getFatPacks() {
        return fatPacks;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterTemplate}> */
    public final IStorage<SealedProduct.Template> getTournamentPacks() {
        return tournaments;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterTemplate}> */
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
}
