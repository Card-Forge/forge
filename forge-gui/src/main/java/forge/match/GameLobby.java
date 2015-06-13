package forge.match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import forge.AIOption;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IGameController;
import forge.interfaces.IGuiGame;
import forge.interfaces.IUpdateable;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.net.event.UpdateLobbyPlayerEvent;
import forge.player.GamePlayerUtil;
import forge.properties.ForgePreferences.FPref;
import forge.util.NameGenerator;
import forge.util.gui.SOptionPane;

public abstract class GameLobby {
    private final static int MAX_PLAYERS = 8;

    private GameLobbyData data = new GameLobbyData();
    private GameType currentGameType = GameType.Constructed;
    private int lastArchenemy = 0;

    private IUpdateable listener;

    private final boolean allowNetworking;
    private HostedMatch hostedMatch;
    private final Map<LobbySlot, IGameController> gameControllers = Maps.newHashMap();
    protected GameLobby(final boolean allowNetworking) {
        this.allowNetworking = allowNetworking;
    }

    public final boolean isAllowNetworking() {
        return allowNetworking;
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
        final boolean archenemyRemoved = triesToChangeArchenemy && !event.getArchenemy().booleanValue();
        final boolean hasArchenemyChanged = triesToChangeArchenemy && slot.isArchenemy() != event.getArchenemy().booleanValue();

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
        addSlot(new LobbySlot(type, null, newIndex, newIndex, false, !allowNetworking, Collections.<AIOption>emptySet()));
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
        updateView(false);
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
            result[i] = val == null ? -1 : val.intValue();
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
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.MomirBasic);
            break;
        case TinyLeaders:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.MomirBasic);
            break;
        case Vanguard:
            data.appliedVariants.remove(GameType.MomirBasic);
            break;
        case MomirBasic:
            data.appliedVariants.remove(GameType.Commander);
            data.appliedVariants.remove(GameType.TinyLeaders);
            data.appliedVariants.remove(GameType.Vanguard);
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

        if (data.appliedVariants.isEmpty()) {
            data.appliedVariants.add(GameType.Constructed);
        }
        if (variant == currentGameType) {
            if (hasVariant(GameType.Commander)) {
                currentGameType = GameType.Commander;
            } else if (hasVariant(GameType.TinyLeaders)) {
                currentGameType = GameType.TinyLeaders;
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
            SOptionPane.showMessageDialog("At least two players are required to start a game.");
            return null;
        }

        if (!isEnoughTeams()) {
            SOptionPane.showMessageDialog("There are not enough teams! Please adjust team allocations.");
            return null;
        }

        for (final LobbySlot slot : activeSlots) {
            if (!slot.isReady() && slot.getType() != LobbySlotType.OPEN) {
                SOptionPane.showMessageDialog(String.format("Player %s is not ready", slot.getName()));
                return null;
            }
            if (slot.getDeck() == null) {
                SOptionPane.showMessageDialog(String.format("Please specify a deck for %s", slot.getName()));
                return null;
            }
            if (hasVariant(GameType.Commander) || hasVariant(GameType.TinyLeaders)) {
                if (!slot.getDeck().has(DeckSection.Commander)) {
                    SOptionPane.showMessageDialog(String.format("%s doesn't have a commander", slot.getName()));
                    return null;
                }
            }
        }

        final Set<GameType> variantTypes = data.appliedVariants;

        GameType autoGenerateVariant = null;
        boolean isCommanderMatch = false;
        boolean isTinyLeadersMatch = false;
        if (!variantTypes.isEmpty()) {
            isTinyLeadersMatch = variantTypes.contains(GameType.TinyLeaders);
            isCommanderMatch = isTinyLeadersMatch || variantTypes.contains(GameType.Commander);
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
                    SOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Deck");
                    return null;
                }
            }
        }

        final List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        final Map<RegisteredPlayer, IGuiGame> guis = Maps.newHashMap();
        final Map<RegisteredPlayer, LobbySlot> playerToSlot = Maps.newHashMap();
        boolean hasNameBeenSet = false;
        for (final LobbySlot slot : activeSlots) {
            final IGuiGame gui = getGui(data.slots.indexOf(slot));
            final String name = slot.getName();
            final int avatar = slot.getAvatarIndex();
            final int team = slot.getTeam();
            final boolean isArchenemy = slot.isArchenemy();
            final Set<AIOption> aiOptions = slot.getAiOptions();

            final boolean isAI = slot.getType() == LobbySlotType.AI;
            final LobbyPlayer lobbyPlayer;
            if (isAI) {
                lobbyPlayer = GamePlayerUtil.createAiPlayer(name, avatar, aiOptions);
            } else {
                boolean setNameNow = false;
                if (!hasNameBeenSet && slot.getType() == LobbySlotType.LOCAL) {
                    setNameNow = true;
                    hasNameBeenSet = true;
                }
                lobbyPlayer = GamePlayerUtil.getGuiPlayer(name, avatar, setNameNow);
            }

            Deck deck = slot.getDeck();
            RegisteredPlayer rp = new RegisteredPlayer(deck);

            if (variantTypes.isEmpty()) {
                rp.setTeamNumber(team);
                players.add(rp.setPlayer(lobbyPlayer));
            } else {
                PaperCard vanguardAvatar = null;
                if (isCommanderMatch) {
                    final GameType commanderGameType = isTinyLeadersMatch ? GameType.TinyLeaders : GameType.Commander;
                    if (checkLegality) {
                        final String errMsg = commanderGameType.getDeckFormat().getDeckConformanceProblem(deck);
                        if (null != errMsg) {
                            SOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid " + commanderGameType + " Deck");
                            return null;
                        }
                    }
                } else if (autoGenerateVariant != null) {
                    deck = autoGenerateVariant.autoGenerateDeck(rp);
                }

                // Initialise variables for other variants
                deck = deck == null ? rp.getDeck() : deck;

                final CardPool avatarPool = deck.get(DeckSection.Avatar);
                if (avatarPool != null) {
                    vanguardAvatar = avatarPool.get(0);
                }

                Iterable<PaperCard> schemes = null;
                Iterable<PaperCard> planes = null;

                //Archenemy
                if (variantTypes.contains(GameType.ArchenemyRumble)
                        || (variantTypes.contains(GameType.Archenemy) && isArchenemy)) {
                    final CardPool schemePool = deck.get(DeckSection.Schemes);
                    if (checkLegality) {
                        final String errMsg = DeckFormat.getSchemeSectionConformanceProblem(schemePool);
                        if (null != errMsg) {
                            SOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Scheme Deck");
                            return null;
                        }
                    }
                    schemes = schemePool == null ? Collections.<PaperCard>emptyList() : schemePool.toFlatList();
                }

                //Planechase
                if (variantTypes.contains(GameType.Planechase)) {
                    final CardPool planePool = deck.get(DeckSection.Planes);
                    if (checkLegality) {
                        final String errMsg = DeckFormat.getPlaneSectionConformanceProblem(planePool);
                        if (null != errMsg) {
                            SOptionPane.showErrorDialog(name + "'s deck " + errMsg, "Invalid Planar Deck");
                            return null;
                        }
                    }
                    planes = planePool == null ? Collections.<PaperCard>emptyList() : planePool.toFlatList();
                }

                //Vanguard
                if (variantTypes.contains(GameType.Vanguard)) {
                    if (vanguardAvatar == null) { //ERROR! null if avatar deselected on list
                        SOptionPane.showMessageDialog("No Vanguard avatar selected for " + name
                                + ". Please choose one or disable the Vanguard variant");
                        return null;
                    }
                }

                rp = RegisteredPlayer.forVariants(activeSlots.size(), variantTypes, deck, schemes, isArchenemy, planes, vanguardAvatar);
                rp.setTeamNumber(team);
                players.add(rp.setPlayer(lobbyPlayer));
            }

            if (!isAI) {
                guis.put(rp, gui);
            }
            playerToSlot.put(rp, slot);
        }

        //if above checks succeed, return runnable that can be used to finish starting game
        return new Runnable() {
            @Override
            public void run() {
                hostedMatch = GuiBase.getInterface().hostMatch();
                hostedMatch.startMatch(GameType.Constructed, variantTypes, players, guis);

                for (final Player p : hostedMatch.getGame().getPlayers()) {
                    final LobbySlot slot = playerToSlot.get(p.getRegisteredPlayer());
                    if (p.getController() instanceof IGameController) {
                        gameControllers.put(slot, (IGameController) p.getController());
                    }
                }

                onGameStarted();
            }
        };
    }

    public final static class GameLobbyData implements Serializable {
        private static final long serialVersionUID = 9184758307999646864L;

        private final Set<GameType> appliedVariants = EnumSet.of(GameType.Constructed);
        private final List<LobbySlot> slots = Lists.newArrayList();

        public GameLobbyData() {
        }
    }
}
