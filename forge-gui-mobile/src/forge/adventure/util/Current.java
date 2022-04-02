package forge.adventure.util;

import forge.adventure.player.AdventurePlayer;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
/**
 * Shortcut class to handle global access, may need some redesign
 */
public class Current {
    public static AdventurePlayer player()
    {
        return WorldSave.getCurrentSave().getPlayer();
    }

    static Deck deck;
    public static Deck latestDeck() {
        return deck;
    }
    public static void setLatestDeck(Deck generateDeck) {
        deck=generateDeck;
    }

    static boolean debug=false;
    public static boolean isInDebug()
    {
        return debug;
    }
    public static void setDebug(boolean b) {
        debug=b;
    }
}
