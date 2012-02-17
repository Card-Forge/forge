package forge.deck.io;

import java.util.Map;

import forge.item.IHasName;


public interface IDeckReader<T extends IHasName> {
    Map<String, T> readAll();
    //T read(File file);
}

