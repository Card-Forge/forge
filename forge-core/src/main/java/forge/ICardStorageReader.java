package forge;

import java.util.List;

import forge.card.CardScriptInfo;

public interface ICardStorageReader{
    List<CardScriptInfo> loadCards();
}