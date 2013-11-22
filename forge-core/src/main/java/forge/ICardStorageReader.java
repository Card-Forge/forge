package forge;

import java.io.File;
import java.util.List;

import forge.card.CardRules;

public interface ICardStorageReader {
    List<CardRules> loadCards();
    
    public interface Observer {
        public void cardLoaded(CardRules rules, List<String> lines, File fileOnDisk);
    }
}