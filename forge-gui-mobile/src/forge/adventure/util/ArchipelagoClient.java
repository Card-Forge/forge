package forge.adventure.util;

import forge.StaticData;
import forge.adventure.data.ArchipelagoData;
import forge.card.CardEdition;

import java.util.*;

public class ArchipelagoClient {
    private static ArchipelagoClient instance = null;

    public ArchipelagoClient() {
        instance = this;
    }

    public static ArchipelagoClient getInstance() {
        return instance == null ? instance = new ArchipelagoClient() : instance;
    }
}
