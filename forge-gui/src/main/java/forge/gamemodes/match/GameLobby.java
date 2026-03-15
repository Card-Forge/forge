package forge.gamemodes.match;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import forge.LobbyPlayer;
import forge.ai.AIOption;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.IHasGameType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.net.event.UpdateLobbyPlayerEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.gui.util.SOptionPane;
import forge.interfaces.IGameController;
import forge.interfaces.IUpdateable;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Localizer;
import forge.util.NameGenerator;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

public abstract class GameLobby implements IHasGameType {
    private final static int MAX_PLAYERS = 8;

    private GameLobbyData data = new GameLobbyData();
    private GameType currentGameType = GameType.Constructed;
    private int lastArchenemy = 0;

    private IUpdateable listener;

    private final boolean allowNetworking;
    private HostedMatch hostedMatch;
    private final HashMap<LobbySlot, IGameController> gameControllers = Maps.newHashMap();
    protected GameLobby(final boolean allowNetworking) {
        this.allowNetworking = allowNetworking;
    }

    public final boolean isAllowNetworking() {
        return allowNetworking;
    }

    public final boolean isMatchActive() {
        return hostedMatch != null && hostedMatch.isMatchOver() == false;
    }

    public HostedMatch getHostedMatch() {
        return hostedMatch;
    }

    public void setListener(final IUpdateable listener) {
        this.listener = listener;
    }

    public GameLobbyData getData() {
        return data;
    }
    public void setData(final GameLobbyData data) {
        this.data = data;
        updateView(true);
    }

    public GameType getGameType() {
        return currentGameType;
    }
    public void setGameType(final GameType type) {
        currentGameType = type;
    }

    public boolean hasAnyVariant() {
        return !data.appliedVariants.isEmpty();
    }

    public boolean hasVariant(final GameType variant) {
        return data.appliedVariants.contains(variant);
    }

    public int getNumberOfSlots() {
        return data.slots.size();
    }
    public LobbySlot getSlot(final int index) {
        if (index < 0 || index >= getNumberOfSlots()) {
            return null;
        }
        return data.slots.get(index);
    }
    public void applyToSlot(final int index, final UpdateLobbyPlayerEvent event) {
        final LobbySlot slot = getSlot(index);
        if (slot == null || event == null) {
            throw new NullPointerException();
        }

        final int nSlots = getNumberOfSlots();
        final boolean triesToChangeArchenemy = event.getArchenemy() != null;
        final boolean archenemyRemoved = triesToChangeArchenemy && !event.getArchenemy();
        final boolean hasArchenemyChanged = triesToChangeArchenemy && slot.isArchenemy() != event.getArchenemy();

        final boolean changed = slot.apply(event) || hasArchenemyChanged;

        // Change archenemy teams
        if (hasVariant(GameType.Archenemy) && hasArchenemyChanged) {
            final int newArchenemy = archenemyRemoved ? lastArchenemy : index;
            if (archenemyRemoved) {
                lastArchenemy = index;
            }
            for (int otherIndex = 0; otherIndex < nSlots; otherIndex++) {
                final LobbySlot otherSlot = getSlot(otherIndex);
                final boolean becomesArchenemy = otherIndex == newArchenemy;
                if (!archenemyRemoved && otherSlot.isArchenemy() && !becomesArchenemy) {
                    lastArchenemy = otherIndex;
                }
                otherSlot.setIsArchenemy(becomesArchenemy);
            }
        }

        if (event.getType() != null) {
            //refresh decklist for slot
            listener.update(index,event.getType());
        }

        if (changed) {
            updateView(false);
        }
    }

    public IGameController getController(final int index) {
        return gameControllers.get(getSlot(index));
    }
    public GameView getGameView() {
        return hostedMatch.getGameView();
    }

    public abstract boolean hasControl();
    public abstract boolean mayEdit(int index);
    public abstract boolean mayControl(int index);
    public abstract boolean mayRemove(int index);
    protected abstract IGuiGame getGui(int index);
    protected abstract void onGameStarted();

    public void addSlot() {
        final int newIndex = getNumberOfSlots();
        final LobbySlotType type = allowNetworking ? LobbySlotType.OPEN : LobbySlotType.AI;
        addSlot(new LobbySlot(type, null, newIndex, newIndex, newIndex, false, !allowNetworking, Collections.emptySet()));
    }
    protected final void addSlot(final LobbySlot slot) {
        if (slot == null) {
            throw new NullPointerException();
        }
        if (data.slots.size() >= MAX_PLAYERS) {
            return;
        }

        data.slots.add(slot);
        if (StringUtils.isEmpty(slot.getName()) && slot.getType() != LobbySlotType.OPEN) {
            slot.setName(randomName());
        }
        if (data.slots.size() == 1) {
            // If first slot, make archenemy
            slot.setIsArchenemy(true);
            lastArchenemy = 0;
        }
        updateView(true);
    }
    private String randomName() {
        final List<String> names = Lists.newArrayListWithCapacity(MAX_PLAYERS);
        for (final LobbySlot slot : data.slots) {
            names.add(slot.getName());
        }
        return NameGenerator.getRandomName("Any", "Any", names);
    }
    protected final static String localName() {
        return FModel.getPreferences().getPref(FPref.PLAYER_NAME);
    }
    protected final static int[] localAvatarIndices() {
        final String[] sAvatars = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");
        final int[] result = new int[sAvatars.length];
        for (int i = 0; i < sAvatars.length; i++) {
            final Integer val = Ints.tryParse(sAvatars[i]);
            result[i] = val == null ? -1 : val;
        }
        return result;
    }
    protected final static int[] localSleeveIndices() {
        final String[] sSleeves = FModel.getPreferences().getPref(FPref.UI_SLEEVES).split(",");
        final int[] result = new int[sSleeves.length];
        for (int i = 0; i < sSleeves.length; i++) {
            final Integer val = Ints.tryParse(sSleeves[i]);
            result[i] = val == null ? -1 : val;
        }
        return result;
    }

    public void removeSlot(final int index) {
        if (index < 0 || index >= data.slots.size()) {
            return;
        }

        if (getSlot(index).isArchenemy()) {
            getSlot(lastArchenemy).setIsArchenemy(true);
            // Should actually be a stack here, but that's rather involved for
            // such a nonimportant feature
            lastArchenemy = 0;
        } else if (lastArchenemy == index) {
            lastArchenemy = 0;
        } else {
            lastArchenemy--;
        }
        data.slots.remove(index);
        updateView(false);
    }

    public void applyVariant(final GameType variant) {
        setGameType(variant);
        data.appliedVariants.add(variant);

        //ensure other necessary variants are unchecked
        switch (variant) {
        case Archenemy:
            data.appliedVariants.remove(GameType.ArchenemyRumble);
            break;
        case ArchenemyRumble:
            data.appliedVariants.remove(GameType.Archenemy);
            break;
        case Commander:
            data.appliedVariants.remove(GameType.Oathbreaker);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.Brawl);
            data.appliedVariants.remove(GameType.MomirBasic);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case Oathbreaker:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.Brawl);
            data.appliedVariants.remove(GameType.MomirBasic);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case TinyLeaders:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.Oathbreaker);
            data.appliedVariants.remove(GameType.Brawl);
            data.appliedVariants.remove(GameType.MomirBasic);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case Brawl:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.Oathbreaker);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.MomirBasic);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case Vanguard:
            data.appliedVariants.remove(GameType.MomirBasic);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case MomirBasic:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.Oathbreaker);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.Brawl);
            data.appliedVariants.remove(GameType.Vanguard);
            data.appliedVariants.remove(GameType.MoJhoSto);
            break;
        case MoJhoSto:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.Oathbreaker);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.Brawl);
            data.appliedVariants.remove(GameType.Vanguard);
            data.appliedVariants.remove(GameType.MomirBasic);
            break;
        default:
            break;
        }
        updateView(true);
    }

    public void removeVariant(final GameType variant) {
        final boolean changed = data.appliedVariants.remove(variant);
        if (!changed) {
            return;
        }

        if (variant == currentGameType) {
            if (hasVariant(GameType.Commander)) {
                currentGameType = GameType.Commander;
            } else if (hasVariant(GameType.Oathbreaker)) {
                currentGameType = GameType.Oathbreaker;
            } else if (hasVariant(GameType.TinyLeaders)) {
                currentGameType = GameType.TinyLeaders;
            } else if (hasVariant(GameType.Brawl)) {
                currentGameType = GameType.Brawl;
            } else {
                currentGameType = GameType.Constructed;
            }
        }
        updateView(true);
    }

    public void clearVariants() {
        data.appliedVariants.clear();
    }

    public Iterable<GameType> getAppliedVariants() {
        return data.appliedVariants;
    }

    private boolean isEnoughTeams() {
        int lastTeam = -1;
        final boolean useArchenemyTeams = data.appliedVariants.contains(GameType.Archenemy);
        for (final LobbySlot slot : data.slots) {
            final int team = useArchenemyTeams ? (slot.isArchenemy() ? 0 : 1) : slot.getTeam();
            if (lastTeam == -1) {
                lastTeam = team;
            } else if (lastTeam != team) {
                return true;
            }
        }
        return false;
    }

    protected final void updateView(final boolean fullUpdate) {
        if (listener != null) {
            listener.update(fullUpdate);
        }
    }

    /** Returns a runnable to start a match with the applied variants if allowed. */
    public Runnable startGame() {
        final List<LobbySlot> activeSlots = Lists.newArrayListWithCapacity(getNumberOfSlots());
        for (final LobbySlot slot : data.slots) {
            if (slot.getType() != LobbySlotType.OPEN) {
                activeSlots.add(slot);
            }
        }

        if (activeSlots.size() < 2) {
            SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblRequiredLeastTwoPlayerStartGame"));
            return null;
        }

        if (!isEnoughTeams()) {
            SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNotEnoughTeams"));
            return null;
        }

        for (final LobbySlot slot : activeSlots) {
            if (!slot.isReady()) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPlayerIsNotReady", slot.getName()));
                return null;
            }
            if (slot.getDeck() == null) {
                SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPleaseSpecifyPlayerDeck", slot.getName()));
                return null;
            }
            if (hasVariant(GameType.Commander) || hasVariant(GameType.Oathbreaker) || hasVariant(GameType.TinyLeaders) || hasVariant(GameType.Brawl)) {
                if (!slot.getDeck().has(DeckSection.Commander)) {
                    SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPlayerDoesntHaveCommander", slot.getName()));
                    return null;
                }
            }
        }

        final Set<GameType> variantTypes = data.appliedVariants;

        GameType autoGenerateVariant = null;
        boolean isCommanderMatch = false;
        boolean isOathbreakerMatch = false;
        boolean isTinyLeadersMatch = false;
        boolean isBrawlMatch = false;
        if (!variantTypes.isEmpty()) {
            isOathbreakerMatch = variantTypes.contains(GameType.Oathbreaker);
            isTinyLeadersMatch = variantTypes.contains(GameType.TinyLeaders);
            isBrawlMatch = variantTypes.contains(GameType.Brawl);
            isCommanderMatch = isBrawlMatch || isTinyLeadersMatch || isOathbreakerMatch || variantTypes.contains(GameType.Commander);
            if (!isCommanderMatch) {
                for (final GameType variant : variantTypes) {
                    if (variant.isAutoGenerated()) {
                        autoGenerateVariant = variant;
                        break;
                    }
                }
            }
        }

        final boolean checkLegality = FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY);

        //Auto-generated decks don't need to be checked here
        //Commander deck replaces regular deck and is checked later
        if (checkLegality && autoGenerateVariant == null && !isCommanderMatch) {
            for (final LobbySlot slot : activeSlots) {
                final String name = slot.getName();
                final String errMsg = GameType.Constructed.getDeckFormat().getDeckConformanceProblem(slot.getDeck());
                if (null != errMsg) {
                    SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblPlayerDeckError", name, errMsg), Localizer.getInstance().getMessage("lblInvalidDeck"));
                    return null;
                }
            }
        }

        final List<RegisteredPlayer> players = new ArrayList<>();
        final Map<RegisteredPlayer, IGuiGame> guis = Maps.newHashMap();
        final Map<RegisteredPlayer, LobbySlot> playerToSlot = Maps.newHashMap();
        boolean hasNameBeenSet = false;
        for (final LobbySlot slot : activeSlots) {
            final IGuiGame gui = getGui(data.slots.indexOf(slot));
            final String name = slot.getName();
            final int avatar = slot.getAvatarIndex();
            final int sleeve = slot.getSleeveIndex();
            final boolean isArchenemy = slot.isArchenemy();
            final int team = GameType.Archenemy.equals(currentGameType) && !isArchenemy ? 1 : slot.getTeam();
            final Set<AIOption> aiOptions = slot.getAiOptions(); // TODO: could AiOptions carry the choice of which AI is selected to play against?

            final boolean isAI = slot.getType() == LobbySlotType.AI;
            final LobbyPlayer lobbyPlayer;
            if (isAI) {
                String aiProfileOverride = slot.getAiProfile();
                lobbyPlayer = GamePlayerUtil.createAiPlayer(name, avatar, sleeve, aiOptions, aiProfileOverride);
            }
            else {
                boolean setNameNow = false;
                if (!hasNameBeenSet && slot.getType() == LobbySlotType.LOCAL) {
                    setNameNow = true;
                    hasNameBeenSet = true;
                }
                lobbyPlayer = GamePlayerUtil.getGuiPlayer(name, avatar, sleeve, setNameNow);
            }

            Deck deck = slot.getDeck();
            RegisteredPlayer rp = new RegisteredPlayer(deck);

            if (!variantTypes.isEmpty()) {
                if (isCommanderMatch) {
                    final GameType commanderGameType =
                            isOathbreakerMatch ? GameType.Oathbreaker :
                                isTinyLeadersMatch ? GameType.TinyLeaders :
                                    isBrawlMatch ? GameType.Brawl :
                                        GameType.Commander;
                    if (checkLegality) {
                        final String errMsg = commanderGameType.getDeckFormat().getDeckConformanceProblem(deck);
                        if (errMsg != null) {
                            SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblPlayerDeckError", name, errMsg), Localizer.getInstance().getMessage("lblInvalidCommanderGameTypeDeck", commanderGameType));
                            return null;
                        }
                    }
                }
                else if (autoGenerateVariant != null) {
                    Deck autoDeck = autoGenerateVariant.autoGenerateDeck(rp);
                    deck = new Deck();
                    for (DeckSection d : DeckSection.values()) {
                        if (autoDeck.has(d)) {
                            deck.getOrCreate(d).clear();
                            deck.get(d).addAll(autoDeck.get(d));
                        }
                    }
                }

                // Initialise variables for other variants
                deck = deck == null ? rp.getDeck() : deck;

                final CardPool avatarPool = deck.get(DeckSection.Avatar);

                Iterable<PaperCard> schemes = null;
                Iterable<PaperCard> planes = null;

                if (variantTypes.contains(GameType.ArchenemyRumble)
                        || (variantTypes.contains(GameType.Archenemy) && isArchenemy)) {
                    final CardPool schemePool = deck.get(DeckSection.Schemes);
                    if (checkLegality) {
                        final String errMsg = DeckFormat.getSchemeSectionConformanceProblem(schemePool);
                        if (null != errMsg) {
                            SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblPlayerDeckError", name, errMsg), Localizer.getInstance().getMessage("lblInvalidSchemeDeck"));
                            return null;
                        }
                    }
                    schemes = schemePool == null ? Collections.emptyList() : schemePool.toFlatList();
                }

                if (variantTypes.contains(GameType.Planechase)) {
                    final CardPool planePool = deck.get(DeckSection.Planes);
                    if (checkLegality) {
                        final String errMsg = DeckFormat.getPlaneSectionConformanceProblem(planePool);
                        if (null != errMsg) {
                            SOptionPane.showErrorDialog(Localizer.getInstance().getMessage("lblPlayerDeckError", name, errMsg), Localizer.getInstance().getMessage("lblInvalidPlanarDeck"));
                            return null;
                        }
                    }
                    planes = planePool == null ? Collections.emptyList() : planePool.toFlatList();
                }

                if (variantTypes.contains(GameType.Vanguard)) {
                    if (avatarPool == null || avatarPool.countAll() == 0) { //ERROR! null if avatar deselected on list
                        SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblNoSelectedVanguardAvatarForPlayer", name));
                        return null;
                    }
                }

                rp = RegisteredPlayer.forVariants(activeSlots.size(), variantTypes, deck, schemes, isArchenemy, planes, avatarPool);
            }

            rp.setTeamNumber(team);
            players.add(rp.setPlayer(lobbyPlayer));

            if (!isAI) {
                guis.put(rp, gui);
            }
            //override starting life for 1v1 Brawl
            if (hasVariant(GameType.Brawl) && activeSlots.size() == 2){
                for (RegisteredPlayer player : players){
                    player.setStartingLife(25);
                }
            }
            playerToSlot.put(rp, slot);
        }

        //if above checks succeed, return runnable that can be used to finish starting game
        return () -> {
            hostedMatch = GuiBase.getInterface().hostMatch();
            hostedMatch.startMatch(GameType.Constructed, variantTypes, players, guis);

            for (final Player p : hostedMatch.getGame().getPlayers()) {
                final LobbySlot slot = playerToSlot.get(p.getRegisteredPlayer());
                if (p.getController() instanceof IGameController) {
                    gameControllers.put(slot, (IGameController) p.getController());
                }
            }

            hostedMatch.gameControllers = gameControllers;

            onGameStarted();
        };
    }

    public final static class GameLobbyData implements Serializable {
        private static final long serialVersionUID = 9184758307999646864L;

        private final Set<GameType> appliedVariants = EnumSet.noneOf(GameType.class);
        private final List<LobbySlot> slots = Lists.newArrayList();

        public GameLobbyData() {
        }
    }
}
