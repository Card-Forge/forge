package forge.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.model.FModel;

import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.util.ImageFetcher;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.item.PaperCard;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiGame;
import forge.gamemodes.match.HostedMatch;
import org.jupnp.UpnpServiceConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ForgeHeadless {

    public static void main(String[] args) {
        GuiBase.setInterface(new HeadlessGui());
        FModel.initialize(null, null);
        try {
            JsonObject initialState = getInitialGameState();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(initialState));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initialize() {
        // FModel.initialize() is called in main
    }

    public static JsonObject getInitialGameState() {
        Game game = runGameUntilStart();
        return extractGameState(game);
    }

    private static Game runGameUntilStart() {
        // Generate Decks
        Deck deck1 = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getStandard().getFilterPrinted(), true);
        Deck deck2 = DeckgenUtil.getRandomColorDeck(FModel.getFormats().getStandard().getFilterPrinted(), true);

        // Setup Players
        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer rp1 = new RegisteredPlayer(deck1).setPlayer(new HeadlessLobbyPlayer("Player 1"));
        RegisteredPlayer rp2 = new RegisteredPlayer(deck2).setPlayer(new HeadlessLobbyPlayer("Player 2"));
        players.add(rp1);
        players.add(rp2);

        // Setup Match
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Headless Match");
        Game game = match.createGame();

        // Run Game until start
        try {
            match.startGame(game, () -> {
                throw new StopGameException(game);
            });
        } catch (StopGameException e) {
            return e.game;
        }

        return game; // Should not be reached if exception is thrown
    }

    private static JsonObject extractGameState(Game game) {
        JsonObject state = new JsonObject();
        
        // General Game Info
        state.addProperty("turn", game.getPhaseHandler().getTurn());
        state.addProperty("phase", game.getPhaseHandler().getPhase().toString());
        state.addProperty("activePlayerId", game.getPhaseHandler().getPlayerTurn().getId());

        // Players
        JsonArray playersArray = new JsonArray();
        for (Player p : game.getPlayers()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("id", p.getId());
            playerObj.addProperty("name", p.getName());
            playerObj.addProperty("life", p.getLife());
            playerObj.addProperty("libraryCount", p.getCardsIn(ZoneType.Library).size());
            
            // Hand
            JsonArray handArray = new JsonArray();
            for (Card c : p.getCardsIn(ZoneType.Hand)) {
                JsonObject cardObj = new JsonObject();
                cardObj.addProperty("name", c.getName());
                cardObj.addProperty("id", c.getId());
                cardObj.addProperty("zone", "Hand");
                handArray.add(cardObj);
            }
            playerObj.add("hand", handArray);

            // Other Zones
            playerObj.add("graveyard", getZoneJson(p, ZoneType.Graveyard));
            playerObj.add("battlefield", getZoneJson(p, ZoneType.Battlefield));
            playerObj.add("exile", getZoneJson(p, ZoneType.Exile));

            playersArray.add(playerObj);
        }
        state.add("players", playersArray);

        return state;
    }

    private static JsonArray getZoneJson(Player p, ZoneType zone) {
        JsonArray zoneArray = new JsonArray();
        for (Card c : p.getCardsIn(zone)) {
            JsonObject cardObj = new JsonObject();
            cardObj.addProperty("name", c.getName());
            cardObj.addProperty("id", c.getId());
            cardObj.addProperty("zone", zone.toString());
            zoneArray.add(cardObj);
        }
        return zoneArray;
    }

    private static class StopGameException extends RuntimeException {
        public final Game game;
        public StopGameException(Game game) {
            this.game = game;
        }
    }

    private static class HeadlessLobbyPlayer extends forge.ai.LobbyPlayerAi {
        public HeadlessLobbyPlayer(String name) {
            super(name, null);
        }

        @Override
        public Player createIngamePlayer(Game game, final int id) {
            Player ai = new Player(getName(), game, id);
            ai.setFirstController(new HeadlessPlayerController(game, ai, this));
            return ai;
        }
    }

    private static class HeadlessPlayerController extends forge.ai.PlayerControllerAi {
        public HeadlessPlayerController(Game game, Player player, forge.ai.LobbyPlayerAi lobbyPlayer) {
            super(game, player, lobbyPlayer);
        }

        @Override
        public boolean mulliganKeepHand(Player player, int cardsToReturn) {
            return true; // Always keep hand
        }
    }

    private static class HeadlessGui implements IGuiBase {
        @Override public boolean isRunningOnDesktop() { return true; }
        @Override public boolean isLibgdxPort() { return false; }
        @Override public String getCurrentVersion() { return "Headless"; }
        @Override public String getAssetsDir() { return "./forge-gui/"; }
        @Override public ImageFetcher getImageFetcher() { return null; }
        @Override public void invokeInEdtNow(Runnable runnable) { runnable.run(); }
        @Override public void invokeInEdtLater(Runnable runnable) { runnable.run(); }
        @Override public void invokeInEdtAndWait(Runnable proc) { proc.run(); }
        @Override public boolean isGuiThread() { return true; }
        @Override public ISkinImage getSkinIcon(FSkinProp skinProp) { return null; }
        @Override public ISkinImage getUnskinnedIcon(String path) { return null; }
        @Override public ISkinImage getCardArt(PaperCard card) { return null; }
        @Override public ISkinImage getCardArt(PaperCard card, boolean backFace) { return null; }
        @Override public ISkinImage createLayeredImage(PaperCard card, FSkinProp background, String overlayFilename, float opacity) { return null; }
        @Override public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {}
        @Override public void showImageDialog(ISkinImage image, String message, String title) {}
        @Override public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) { return defaultOption; }
        @Override public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) { return initialInput; }
        @Override public <T> List<T> getChoices(String message, int min, int max, java.util.Collection<T> choices, java.util.Collection<T> selected, java.util.function.Function<T, String> display) { return new ArrayList<>(selected); }
        @Override public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices) { return destChoices; }
        @Override public String showFileDialog(String title, String defaultDir) { return null; }
        @Override public java.io.File getSaveFile(java.io.File defaultFile) { return defaultFile; }
        @Override public void download(GuiDownloadService service, java.util.function.Consumer<Boolean> callback) { callback.accept(false); }
        @Override public void refreshSkin() {}
        @Override public void showCardList(String title, String message, List<PaperCard> list) {}
        @Override public boolean showBoxedProduct(String title, String message, List<PaperCard> list) { return true; }
        @Override public PaperCard chooseCard(String title, String message, List<PaperCard> list) { return list.isEmpty() ? null : list.get(0); }
        @Override public int getAvatarCount() { return 0; }
        @Override public int getSleevesCount() { return 0; }
        @Override public void copyToClipboard(String text) {}
        @Override public void browseToUrl(String url) throws java.io.IOException, java.net.URISyntaxException {}
        @Override public IAudioClip createAudioClip(String filename) { return null; }
        @Override public IAudioMusic createAudioMusic(String filename) { return null; }
        @Override public void startAltSoundSystem(String filename, boolean isSynchronized) {}
        @Override public void clearImageCache() {}
        @Override public void showSpellShop() {}
        @Override public void showBazaar() {}
        @Override public IGuiGame getNewGuiGame() { return null; }
        @Override public HostedMatch hostMatch() { return null; }
        @Override public void runBackgroundTask(String message, Runnable task) { task.run(); }
        @Override public String encodeSymbols(String str, boolean formatReminderText) { return str; }
        @Override public void preventSystemSleep(boolean preventSleep) {}
        @Override public float getScreenScale() { return 1.0f; }
        @Override public UpnpServiceConfiguration getUpnpPlatformService() { return null; }
    }
}
