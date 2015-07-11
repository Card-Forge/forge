package forge.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import forge.card.CardStateName;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.util.collect.FCollectionView;

public abstract class GameState {
    private static final Map<ZoneType, String> ZONES = new HashMap<ZoneType, String>();
    static {
        ZONES.put(ZoneType.Battlefield, "play");
        ZONES.put(ZoneType.Hand, "hand");
        ZONES.put(ZoneType.Graveyard, "graveyard");
        ZONES.put(ZoneType.Library, "library");
        ZONES.put(ZoneType.Exile, "exile");
        ZONES.put(ZoneType.Command, "command");
    }

    private int humanLife = -1;
    private int computerLife = -1;
    private final Map<ZoneType, String> humanCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
    private final Map<ZoneType, String> aiCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
    private String tChangePlayer = "NONE";
    private String tChangePhase = "NONE";
    
    public GameState() {
    }

    public abstract IPaperCard getPaperCard(String cardName);

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("humanlife=%d\n", humanLife));
        sb.append(String.format("ailife=%d\n", computerLife));
        sb.append(String.format("activeplayer=%s\n", tChangePlayer));
        sb.append(String.format("activephase=%s\n", tChangePhase));
        appendCards(humanCardTexts, "human", sb);
        appendCards(aiCardTexts, "ai", sb);
        return sb.toString();
    }

    private void appendCards(Map<ZoneType, String> cardTexts, String categoryPrefix, StringBuilder sb) {
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            sb.append(String.format("%scardsin%s=%s\n", categoryPrefix, ZONES.get(kv.getKey()), kv.getValue()));
        }
    }

    public void initFromGame(Game game) throws Exception {
        FCollectionView<Player> players = game.getPlayers();
        // Can only serialized a two player game with one AI and one human.
        if (players.size() != 2) {
            throw new Exception("Game not supported");
        }
        final Player human = game.getPlayers().get(0);
        final Player ai = game.getPlayers().get(1);
        if (!human.getController().isGuiPlayer() || !ai.getController().isAI()) {
            throw new Exception("Game not supported");
        }
        humanLife = human.getLife();
        computerLife = ai.getLife();
        tChangePlayer = game.getPhaseHandler().getPlayerTurn() == ai ? "ai" : "human";
        tChangePhase = game.getPhaseHandler().getPhase().toString();
        aiCardTexts.clear();
        humanCardTexts.clear();
        for (ZoneType zone : ZONES.keySet()) {
            // Init texts to empty, so that restoring will clear the state
            // if the zone had no cards in it (e.g. empty hand).
            aiCardTexts.put(zone, "");
            humanCardTexts.put(zone, "");
            for (Card card : game.getCardsIn(zone)) {
                addCard(zone, card.getOwner() == ai ? aiCardTexts : humanCardTexts, card);
            }
        }
    }

    private void addCard(ZoneType zoneType, Map<ZoneType, String> cardTexts, Card c) {
        StringBuilder newText = new StringBuilder(cardTexts.get(zoneType));
        if (newText.length() > 0) {
            newText.append(";");
        }
        if (c.isToken()) {
            newText.append("t:" + new CardFactory.TokenInfo(c).toString());
        } else {
            newText.append(c.getPaperCard().getName());
        }
        if (zoneType == ZoneType.Battlefield) {
            if (c.isTapped()) {
                newText.append("|Tapped:True");
            }
            if (c.isSick()) {
                newText.append("|SummonSick:True");
            }
            if (c.isFaceDown()) {
                newText.append("|FaceDown:True");
            }
            Map<CounterType, Integer> counters = c.getCounters();
            if (!counters.isEmpty()) {
                newText.append("|Counters:");
                boolean start = true;
                for(Entry<CounterType, Integer> kv : counters.entrySet()) {
                    String str = kv.getKey().toString();
                    int count = kv.getValue();
                    for (int i = 0; i < count; i++) {
                        if (!start) {
                            newText.append(",");
                        }
                        newText.append(str);
                        start = false;
                    }
                }
            }
        }
        cardTexts.put(zoneType, newText.toString());
    }

    private String[] parseLine(String line) {
        if (line.charAt(0) == '#') {
            return null;
        }
        final String[] tempData = line.split("=");
        if (tempData.length >= 2) {
            return tempData;
        }
        if (tempData.length == 1 && line.endsWith("=")) {
            // Empty value.
            return new String[] {tempData[0], ""};
        }
        return null;
    }

    public void parse(InputStream in) throws Exception {
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = br.readLine()) != null) {
            String[] keyValue = parseLine(line);
            if (keyValue == null) {
                continue;
            }
            final String categoryName = keyValue[0].toLowerCase();
            final String categoryValue = keyValue[1];

            if (categoryName.equals("humanlife"))                   humanLife = Integer.parseInt(categoryValue);
            else if (categoryName.equals("ailife"))                 computerLife = Integer.parseInt(categoryValue);

            else if (categoryName.equals("activeplayer"))           tChangePlayer = categoryValue.trim().toLowerCase();
            else if (categoryName.equals("activephase"))            tChangePhase = categoryValue;

            else if (categoryName.equals("humancardsinplay"))       humanCardTexts.put(ZoneType.Battlefield, categoryValue);
            else if (categoryName.equals("aicardsinplay"))          aiCardTexts.put(ZoneType.Battlefield, categoryValue);
            else if (categoryName.equals("humancardsinhand"))       humanCardTexts.put(ZoneType.Hand, categoryValue);
            else if (categoryName.equals("aicardsinhand"))          aiCardTexts.put(ZoneType.Hand, categoryValue);
            else if (categoryName.equals("humancardsingraveyard"))  humanCardTexts.put(ZoneType.Graveyard, categoryValue);
            else if (categoryName.equals("aicardsingraveyard"))     aiCardTexts.put(ZoneType.Graveyard, categoryValue);
            else if (categoryName.equals("humancardsinlibrary"))    humanCardTexts.put(ZoneType.Library, categoryValue);
            else if (categoryName.equals("aicardsinlibrary"))       aiCardTexts.put(ZoneType.Library, categoryValue);
            else if (categoryName.equals("humancardsinexile"))      humanCardTexts.put(ZoneType.Exile, categoryValue);
            else if (categoryName.equals("aicardsinexile"))         aiCardTexts.put(ZoneType.Exile, categoryValue);
            else if (categoryName.equals("humancardsincommand"))    humanCardTexts.put(ZoneType.Command, categoryValue);
            else if (categoryName.equals("aicardsincommand"))       aiCardTexts.put(ZoneType.Command, categoryValue);
            else System.out.println("Unknown key: " + categoryName);
        }
    }

    public void applyToGame(final Game game) {
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final Player human = game.getPlayers().get(0);
                final Player ai = game.getPlayers().get(1);

                Player newPlayerTurn = tChangePlayer.equals("human") ? newPlayerTurn = human : tChangePlayer.equals("ai") ? newPlayerTurn = ai : null;
                PhaseType newPhase = tChangePhase.trim().equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);

                game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn);

                game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);

                setupPlayerState(humanLife, humanCardTexts, human);
                setupPlayerState(computerLife, aiCardTexts, ai);

                game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                game.getAction().checkStateEffects(true); //ensure state based effects and triggers are updated
            }
        });
    }

    private void setupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p) {
        Map<ZoneType, CardCollectionView> playerCards = new EnumMap<ZoneType, CardCollectionView>(ZoneType.class);
        for(Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            String value = kv.getValue();
            playerCards.put(kv.getKey(), processCardsForZone(value.isEmpty() ? new String[0] : value.split(";"), p));
        }

        if (life >= 0) p.setLife(life, null);
        for (Entry<ZoneType, CardCollectionView> kv : playerCards.entrySet()) {
            if (kv.getKey() == ZoneType.Battlefield) {
                List<Card> cards = new ArrayList<Card>();
                for (final Card c : kv.getValue()) {
                    if (c.isToken()) {
                        cards.add(c);
                    }
                }
                p.getZone(kv.getKey()).setCards(cards);
                for (final Card c : kv.getValue()) {
                    if (c.isToken()) {
                        continue;
                    }
                    boolean tapped = c.isTapped();
                    boolean sickness = c.hasSickness();
                    p.getZone(ZoneType.Hand).add(c);
                    p.getGame().getAction().moveToPlay(c);
                    c.setTapped(tapped);
                    c.setSickness(sickness);
                }
            } else {
                p.getZone(kv.getKey()).setCards(kv.getValue());
            }
        }
    }

    /**
     * <p>
     * processCardsForZone.
     * </p>
     * 
     * @param data
     *            an array of {@link java.lang.String} objects.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.CardList} object.
     */
    private CardCollectionView processCardsForZone(final String[] data, final Player player) {
        final CardCollection cl = new CardCollection();
        for (final String element : data) {
            final String[] cardinfo = element.trim().split("\\|");

            Card c;
            if (cardinfo[0].startsWith("t:")) {
                String tokenStr = cardinfo[0].substring(2);
                // TODO: Use a version of the API that doesn't return a list (i.e. these shouldn't be affected
                // by doubling season, etc).
                c = CardFactory.makeToken(CardFactory.TokenInfo.fromString(tokenStr), player).get(0);
            } else {
                c = Card.fromPaperCard(getPaperCard(cardinfo[0]), player);
            }
            c.setSickness(false);

            boolean hasSetCurSet = false;
            for (final String info : cardinfo) {
                if (info.startsWith("Set:")) {
                    c.setSetCode(info.substring(info.indexOf(':') + 1));
                    hasSetCurSet = true;
                } else if (info.equalsIgnoreCase("Tapped:True")) {
                    c.tap();
                } else if (info.startsWith("Counters:")) {
                    final String[] counterStrings = info.substring(info.indexOf(':') + 1).split(",");
                    for (final String counter : counterStrings) {
                        c.addCounter(CounterType.valueOf(counter), 1, true);
                    }
                } else if (info.equalsIgnoreCase("SummonSick:True")) {
                    c.setSickness(true);
                } else if (info.equalsIgnoreCase("FaceDown:True")) {
                    c.setState(CardStateName.FaceDown, true);
                }
            }

            if (!hasSetCurSet && !c.isToken()) {
                c.setSetCode(c.getMostRecentSet());
            }

            cl.add(c);
        }
        return cl;
    }
}
