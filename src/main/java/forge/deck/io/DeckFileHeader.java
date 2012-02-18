package forge.deck.io;

import java.util.Map;

import forge.PlayerType;
import forge.game.GameType;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckFileHeader {

    public static final String NAME = "Name";
    private static final String DECK_TYPE = "Deck Type";
    public static final String COMMENT = "Comment";
    private static final String PLAYER = "Player";
    private static final String CSTM_POOL = "Custom Pool";
    private static final String PLAYER_TYPE = "PlayerType";

    private final GameType deckType;
    private final PlayerType playerType;
    private final boolean customPool;

    private final String name;
    private final String comment;


    /**
     * TODO: Write javadoc for Constructor.
     * @param parseKvPairs
     */
    public DeckFileHeader(Map<String, String> kvPairs) {
        name = kvPairs.get(NAME);
        comment = kvPairs.get(COMMENT);
        deckType = GameType.smartValueOf(kvPairs.get(DECK_TYPE), GameType.Constructed);
        customPool = "true".equalsIgnoreCase(kvPairs.get(CSTM_POOL));
        playerType = "computer".equalsIgnoreCase(kvPairs.get(PLAYER)) || "ai".equalsIgnoreCase(kvPairs.get(PLAYER_TYPE)) ? PlayerType.COMPUTER : PlayerType.HUMAN;
    }


    public  final PlayerType getPlayerType() {
        return playerType;
    }
    public  final boolean isCustomPool() {
        return customPool;
    }
    public  final String getName() {
        return name;
    }
    public  final String getComment() {
        return comment;
    }


    public  final GameType getDeckType() {
        return deckType;
    }



}
