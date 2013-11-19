package forge;

import java.util.List;

import forge.card.CardRules;

public interface ICardStorageReader{
    List<CardRules> loadCards();
}