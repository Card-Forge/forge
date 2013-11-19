package forge;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.EditionCollection;
import forge.card.FatPackTemplate;
import forge.card.FormatCollection;
import forge.card.PrintSheet;
import forge.card.SealedProductTemplate;
import forge.game.GameFormat;
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
  private final EditionCollection editions;
  private final FormatCollection formats;
  private final IStorage<SealedProductTemplate> boosters;
  private final IStorage<SealedProductTemplate> specialBoosters;
  private final IStorage<SealedProductTemplate> tournaments;
  private final IStorage<FatPackTemplate> fatPacks;
  private final IStorage<PrintSheet> printSheets;

  private static StaticData lastInstance = null;


  
  public StaticData(ICardStorageReader reader, String editionFolder, String blockDataFolder) {
      this.editions = new EditionCollection(new CardEdition.Reader(new File(editionFolder)));
      lastInstance = this;

      final Map<String, CardRules> regularCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);
      final Map<String, CardRules> variantsCards = new TreeMap<String, CardRules>(String.CASE_INSENSITIVE_ORDER);


      List<CardRules> rules = reader.loadCards();
      for (CardRules card : rules) {
          if (null == card) continue;
          
          final String cardName = card.getName();
          if ( card.isVariant() )
              variantsCards.put(cardName, card);
          else
              regularCards.put(cardName, card);
      }
      
      commonCards = new CardDb(regularCards, editions, false);
      variantCards = new CardDb(variantsCards, editions, false);



      this.formats = new FormatCollection(new GameFormat.Reader(new File(blockDataFolder, "formats.txt")));
      this.boosters = new StorageBase<SealedProductTemplate>("Boosters", editions.getBoosterGenerator());
      this.specialBoosters = new StorageBase<SealedProductTemplate>("Special boosters", new SealedProductTemplate.Reader(new File(blockDataFolder, "boosters-special.txt")));
      this.tournaments = new StorageBase<SealedProductTemplate>("Starter sets", new SealedProductTemplate.Reader(new File(blockDataFolder, "starters.txt")));
      this.fatPacks = new StorageBase<FatPackTemplate>("Fat packs", new FatPackTemplate.Reader("res/blockdata/fatpacks.txt"));
      this.printSheets = new StorageBase<PrintSheet>("Special print runs", new PrintSheet.Reader(new File(blockDataFolder, "printsheets.txt")));
  }
  
  public final static StaticData instance() { 
      return lastInstance;
  }
  
  
  public final EditionCollection getEditions() {
      return this.editions;
  }

  public final FormatCollection getFormats() {
      return this.formats;
  }

  /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.FatPackTemplate}> */
  public IStorage<FatPackTemplate> getFatPacks() {
      return fatPacks;
  }

  /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterTemplate}> */
  public final IStorage<SealedProductTemplate> getTournamentPacks() {
      return tournaments;
  }

  /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterTemplate}> */
  public final IStorage<SealedProductTemplate> getBoosters() {
      return boosters;
  }

  public final IStorage<SealedProductTemplate> getSpecialBoosters() {
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
