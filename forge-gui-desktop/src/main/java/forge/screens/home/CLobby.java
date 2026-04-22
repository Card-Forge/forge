package forge.screens.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.net.EventFormat;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.net.NetworkEventView;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.FDraftOverlay;
import forge.gui.GuiChoose;
import forge.gui.framework.FScreen;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.controllers.CEditorLimited;
import forge.screens.deckeditor.controllers.CEditorNetworkDraft;
import forge.screens.deckeditor.controllers.NetworkDraftLog;
import forge.screens.deckeditor.views.VEditorLog;
import forge.screens.home.online.VSubmenuOnlineLobby;
import forge.toolbox.FLabel;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public class CLobby {

    public enum LobbyMode { CONSTRUCTED, LIMITED }

    /** Desktop event-panel render contract: shared text content + desktop widget visibility. */
    public record EventPanelContents(
            String formatText,
            String productText,
            String timerText,
            String dateText,
            String statusText,
            boolean showDismissX,
            boolean showConformance,
            boolean conformanceEnabled) { }

    private final VLobby view;
    private LobbyMode currentMode = LobbyMode.CONSTRUCTED;
    private boolean suppressModeListener;

    // Event state (network lobby only)
    private EventFormat configuredFormat;
    private String activeEventId;
    private boolean activeConformance = true;
    private List<String> eventIdsByDropdownIndex = new ArrayList<>();
    private NetworkEventView lastEventView;
    private int mySeatIndex;
    private int lastPackNumber;
    private CEditorNetworkDraft networkDraftEditor;

    public CLobby(final VLobby view) {
        this.view = view;
        view.setController(this);
    }

    public boolean isLimitedMode() { return currentMode == LobbyMode.LIMITED; }
    public EventFormat getConfiguredFormat() { return configuredFormat; }
    public String getActiveEventId() { return activeEventId; }
    public boolean isActiveConformance() { return activeConformance; }

    /** Client: sync the combo selection to the host's mode without re-firing onModeChanged. */
    void syncModeFromHost() {
        if (!view.getLobby().isAllowNetworking() || view.getLobby().hasControl()) return;
        if (view.getLobby().getData() == null) return;
        boolean hostIsLimited = view.getLobby().getData().isLimitedMode();
        int desiredIndex = hostIsLimited ? 1 : 0;
        if (view.getCurrentModeIndex() != desiredIndex) {
            suppressModeListener = true;
            try {
                view.setCurrentModeIndex(desiredIndex);
                currentMode = hostIsLimited ? LobbyMode.LIMITED : LobbyMode.CONSTRUCTED;
                view.setVariantsVisible(!hostIsLimited);
            } finally {
                suppressModeListener = false;
            }
        }
    }

    void onModeChanged() {
        if (suppressModeListener) return;

        // Client: mode is host-controlled. If a user click diverges from the synced value,
        // revert via setCurrentModeIndex (which re-fires this listener).
        if (view.getLobby().isAllowNetworking() && !view.getLobby().hasControl()) {
            boolean hostIsLimited = view.getLobby().getData() != null && view.getLobby().getData().isLimitedMode();
            int desiredIndex = hostIsLimited ? 1 : 0;
            if (view.getCurrentModeIndex() != desiredIndex) {
                view.setCurrentModeIndex(desiredIndex);
                return;
            }
        }

        final String selected = view.getCurrentModeSelection();
        if (Localizer.getInstance().getMessage("lblNetworkModeLimited").equals(selected)) {
            currentMode = LobbyMode.LIMITED;
        } else {
            currentMode = LobbyMode.CONSTRUCTED;
        }
        final boolean isLimited = (currentMode == LobbyMode.LIMITED);

        // Clear event when switching away from Limited, and broadcast the new mode.
        if (view.getLobby().hasControl() && view.getLobby() instanceof ServerGameLobby serverLobby) {
            if (!isLimited) {
                configuredFormat = null;
                serverLobby.clearCurrentEvent();
            }
            serverLobby.setLimitedMode(isLimited);
        }
        view.updateEventPanelState();
        view.setVariantsVisible(!isLimited);
        view.updateRightPanelForMode();
        view.updateActionButtons();
        view.refreshConstructedFrame();
    }

    private void addDecks(final Iterable<DeckProxy> commanderDecks, FList<Object> deckList, String... initialItems) {
        Vector<Object> listData = new Vector<>(Arrays.asList(initialItems));
        listData.add("Generate");
        if (!Iterables.isEmpty(commanderDecks)) {
            listData.add("Random");
            for (DeckProxy comDeck : commanderDecks) {
                listData.add(comDeck.getDeck());
            }
        }
        Object val = deckList.getSelectedValue();
        deckList.setListData(listData);
        if (null != val) {
            deckList.setSelectedValue(val, true);
        }
        if (-1 == deckList.getSelectedIndex()) {
            deckList.setSelectedIndex(0);
        }
    }
    
    public void update() {
        SwingUtilities.invokeLater(() -> {
            final Iterable<DeckProxy> schemeDecks = DeckProxy.getAllSchemeDecks();
            final Iterable<DeckProxy> planarDecks = DeckProxy.getAllPlanarDecks();

            for (int i = 0; i < VLobby.MAX_PLAYERS; i++) {
                addDecks(schemeDecks, view.getSchemeDeckLists().get(i),
                        "Use deck's scheme section (random if unavailable)");
                addDecks(planarDecks, view.getPlanarDeckLists().get(i),
                        "Use deck's planes section (random if unavailable)");
                view.updateVanguardList(i);
            }

            // General updates when switching back to this view
            view.getBtnStart().requestFocusInWindow();
        });
        view.getGamesInMatchBinder().load();
    }

    /** React to a lobby-data change: detect event-state transitions and refresh the panel. */
    public void onLobbyDataChanged() {
        GameLobby.GameLobbyData data = view.getLobby().getData();
        if (data == null) return;

        boolean eventPanelNeedsUpdate = false;
        NetworkEventView newView = data.getEventView();
        if (newView != lastEventView) {
            lastEventView = newView;
            eventPanelNeedsUpdate = true;
        }
        String newEventId = data.getActiveEventId();
        boolean newConformance = data.isActiveConformance();
        if (!java.util.Objects.equals(newEventId, activeEventId) || newConformance != activeConformance) {
            activeEventId = newEventId;
            activeConformance = newConformance;
            if (!view.getLobby().hasControl()) {
                view.setConformanceSelected(newConformance);
                view.updateDeckListFilter();
            }
            eventPanelNeedsUpdate = true;
        }
        if (eventPanelNeedsUpdate) {
            refreshEventPanel();
        }
    }

    /** Compute the event panel contents and push to the view. */
    public void refreshEventPanel() {
        if (!view.getLobby().isAllowNetworking()) return;
        view.setEventPanelContents(buildEventPanelContents());
    }

    private EventPanelContents buildEventPanelContents() {
        final boolean isHost = view.getLobby().hasControl();
        final NetworkEvent currentEvent = (view.getLobby() instanceof ServerGameLobby sgl) ? sgl.getCurrentEvent() : null;
        final boolean inState2 = activeEventId != null;
        final boolean inState1 = !inState2 && (isHost ? currentEvent != null : lastEventView != null);

        NetworkEvent.EventPanelText text = NetworkEvent.computeEventPanelText(
                isHost, activeEventId, currentEvent, lastEventView);

        return new EventPanelContents(
                text.formatText(), text.productText(), text.timerText(),
                text.dateText(), text.statusText(),
                isHost && (inState1 || inState2),
                inState2,
                isHost && !inState1);
    }

    void onDismissEvent() {
        if (view.getLobby() instanceof ServerGameLobby serverLobby) {
            configuredFormat = null;
            serverLobby.clearCurrentEvent();
        }
        activeEventId = null;
        broadcastEventSelection();
        view.updateEventPanelState();
        view.updateActionButtons();
        view.updateDeckListFilter();
    }

    void onConformanceChanged() {
        activeConformance = view.getConformanceSelected();
        view.updateDeckListFilter();
        broadcastEventSelection();
    }

    void broadcastEventSelection() {
        if (view.getLobby().hasControl() && view.getLobby() instanceof ServerGameLobby serverLobby) {
            serverLobby.selectEventForMatch(activeEventId, activeConformance);
        }
    }

    void scanAvailableEvents() {
        LinkedHashSet<String> eventIds = new LinkedHashSet<>();
        for (Deck d : FModel.getDecks().getNetworkEventDecks()) {
            String eventId = DeckProxy.getEventTag(d, "eventId");
            if (eventId != null) eventIds.add(eventId);
        }
        eventIdsByDropdownIndex = new ArrayList<>(eventIds);
    }

    void openEventConfigDialog() {
        if (!(view.getLobby() instanceof ServerGameLobby serverLobby)) return;
        Localizer localizer = Localizer.getInstance();

        // Step 0: If past events exist, offer a choice between creating new and loading one
        if (!eventIdsByDropdownIndex.isEmpty()) {
            String[] setupOptions = {
                    localizer.getMessage("lblNetworkSetUpEventCreate"),
                    localizer.getMessage("lblNetworkSetUpEventLoadPast")
            };
            String setupChoice = GuiChoose.oneOrNone(
                    localizer.getMessage("lblNetworkSetUpEventPrompt"), setupOptions);
            if (setupChoice == null) return;
            if (setupChoice.equals(setupOptions[1])) {
                openLoadPastEventDialog();
                return;
            }
        }

        // Step 1: Choose event type (Draft / Sealed)
        String[] formatNames = { localizer.getMessage("lblNetworkModeDraft"), localizer.getMessage("lblNetworkModeSealed") };
        String chosenFormatName = GuiChoose.oneOrNone(
                localizer.getMessage("lblNetworkChooseEventType"), formatNames);
        if (chosenFormatName == null) return;
        EventFormat chosenFormat = formatNames[0].equals(chosenFormatName)
                ? EventFormat.BOOSTER_DRAFT : EventFormat.SEALED;

        serverLobby.createEvent(chosenFormat);
        configuredFormat = chosenFormat;

        // Step 2: Choose pool type
        boolean isDraft = (chosenFormat == EventFormat.BOOSTER_DRAFT);
        LimitedPoolType[] poolTypes = LimitedPoolType.values(isDraft);
        LimitedPoolType chosen = GuiChoose.oneOrNone(
                localizer.getMessage("lblNetworkChooseDraftFormat"), poolTypes);
        if (chosen == null) return;

        // Step 3: For draft, build the BoosterDraft now so block/set/cube/theme sub-dialogs
        // pop before the timer prompt — matches the offline CSubmenuDraft flow.
        BoosterDraft draft = null;
        if (isDraft) {
            draft = BoosterDraft.createDraftForNetwork(chosen);
            if (draft == null) return;
        }

        NetworkEvent event = serverLobby.getCurrentEvent();
        if (event == null) return;

        // Step 4: Pick timer + disconnect grace period (draft only, combined prompt)
        int timerSeconds = event.getPickTimerSeconds();
        int graceSeconds = event.getDisconnectGraceSeconds();
        if (isDraft) {
            FTextField pickField = new FTextField.Builder().text(String.valueOf(timerSeconds)).build();
            FTextField graceField = new FTextField.Builder().text(String.valueOf(graceSeconds)).build();
            FLabel pickLbl = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkPickTimerPrompt")).build();
            FLabel graceLbl1 = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkGraceTimerPromptLine1")).build();
            FLabel graceLbl2 = new FLabel.Builder().fontSize(12).text(localizer.getMessage("lblNetworkGraceTimerPromptLine2")).build();

            JPanel panel = new JPanel(new MigLayout("insets 4, gap 2 4, wrap 1"));
            panel.setOpaque(false);
            panel.add(pickLbl);
            panel.add(pickField, "w 80!");
            panel.add(graceLbl1, "gaptop 10");
            panel.add(graceLbl2);
            panel.add(graceField, "w 80!");

            int result = FOptionPane.showOptionDialog(
                    null,
                    localizer.getMessage("lblNetworkDraftTimersTitle"),
                    null,
                    panel,
                    Arrays.asList(
                            localizer.getMessage("lblOK"),
                            localizer.getMessage("lblCancel")));
            if (result == 0) {
                try {
                    int parsed = Integer.parseInt(pickField.getText().trim());
                    if (parsed >= 0) timerSeconds = parsed;
                } catch (NumberFormatException ignored) { }
                try {
                    int parsed = Integer.parseInt(graceField.getText().trim());
                    if (parsed >= 0) graceSeconds = parsed;
                } catch (NumberFormatException ignored) { }
            }
        }

        if (!serverLobby.configureEvent(chosen, draft, timerSeconds, graceSeconds)) {
            return;
        }
        view.updateEventPanelState();
        view.updateActionButtons();
    }

    void openLoadPastEventDialog() {
        if (eventIdsByDropdownIndex.isEmpty()) return;
        List<NetworkEvent.EventChoice> choices = new ArrayList<>(eventIdsByDropdownIndex.size());
        for (String id : eventIdsByDropdownIndex) {
            choices.add(new NetworkEvent.EventChoice(id, NetworkEvent.getEventDisplayLabel(id)));
        }
        NetworkEvent.EventChoice chosen = GuiChoose.oneOrNone(
                Localizer.getInstance().getMessage("lblNetworkLoadPastEventPrompt"), choices);
        if (chosen == null) return;
        activeEventId = chosen.id();
        view.updateEventPanelState();
        view.updateActionButtons();
        view.updateDeckListFilter();
        broadcastEventSelection();
    }

    void startEvent() {
        if (!(view.getLobby() instanceof ServerGameLobby serverLobby)) return;
        Localizer localizer = Localizer.getInstance();
        NetworkEvent event = serverLobby.getCurrentEvent();
        if (event == null) {
            FOptionPane.showErrorDialog(localizer.getMessage("lblNetworkNoEventConfigured"));
            return;
        }

        LobbySlot unready = view.getLobby().findFirstUnreadySlot();
        if (unready != null) {
            SOptionPane.showMessageDialog(localizer.getMessage("lblPlayerIsNotReady", unready.getName()));
            return;
        }

        if (event.getFormat() == EventFormat.SEALED) {
            serverLobby.startSealedEvent();
        } else if (event.getFormat() == EventFormat.BOOSTER_DRAFT) {
            ServerGameLobby.DraftStartResult result = serverLobby.startDraftEvent();
            if (result == null) {
                FOptionPane.showErrorDialog(localizer.getMessage("lblNetworkFailedDraft"));
                return;
            }
            mySeatIndex = result.hostSeatIndex();
            FDraftOverlay.SINGLETON_INSTANCE.initDraft(
                    mySeatIndex, result.names(), result.aiFlags(), result.totalPacks());
            NetworkDraftLog.logDraftStart(
                    event.getParticipants(), result.totalPacks(),
                    event.getProductDescription(), mySeatIndex);
            lastPackNumber = 0;
        }
    }

    void onDraftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) {
        SwingUtilities.invokeLater(() -> {
            if (networkDraftEditor == null) {
                initDraftEditor(seatIndex);
            }

            FDraftOverlay.SINGLETON_INSTANCE.onPackArrived(packNumber, pickNumber, pack.size(), timerDurationSeconds);

            if (packNumber != lastPackNumber) {
                lastPackNumber = packNumber;
                boolean passingRight = (packNumber % 2 == 1);
                NetworkDraftLog.logPackHeader(packNumber, passingRight);
            }

            networkDraftEditor.showPack(pack, packNumber, pickNumber);
        });
    }

    private void initDraftEditor(int seatIndex) {
        mySeatIndex = seatIndex;

        // Prefer the latest state's eventView over the cached copy — the first lobby
        // broadcast during configureEvent() carries an empty participants list.
        if (view.getLobby().getData() != null && view.getLobby().getData().getEventView() != null) {
            lastEventView = view.getLobby().getData().getEventView();
        }
        if (lastEventView != null) {
            List<EventParticipant> participants = lastEventView.getParticipants();
            int totalPacks = lastEventView.getNumRounds();
            String[] names = new String[participants.size()];
            boolean[] aiFlags = new boolean[participants.size()];
            for (EventParticipant p : participants) {
                int seat = p.getSeatIndex();
                if (seat >= 0 && seat < names.length) {
                    names[seat] = p.getName();
                    aiFlags[seat] = p.isAI();
                }
            }
            FDraftOverlay.SINGLETON_INSTANCE.initDraft(mySeatIndex, names, aiFlags, totalPacks);
            NetworkDraftLog.logDraftStart(
                    participants, totalPacks,
                    lastEventView.getProductDescription(), mySeatIndex);
        }

        Consumer<DraftPickEvent> pickSender;
        if (view.getLobby() instanceof ServerGameLobby serverLobby) {
            pickSender = ev -> serverLobby.handleDraftPick(ev, -1);
        } else {
            FGameClient gameClient = VSubmenuOnlineLobby.SINGLETON_INSTANCE.getClient();
            if (gameClient == null) return;
            pickSender = gameClient::send;
        }

        networkDraftEditor = new CEditorNetworkDraft(
                mySeatIndex, pickSender, this::cancelActiveDraft,
                CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
        VEditorLog.SINGLETON_INSTANCE.resetNewDraft();

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(networkDraftEditor);
    }

    private String resolveParticipantName(int seatIndex) {
        List<EventParticipant> viewParticipants = lastEventView != null ? lastEventView.getParticipants() : null;
        List<EventParticipant> currentParticipants = (view.getLobby() instanceof ServerGameLobby sgl
                && sgl.getCurrentEvent() != null) ? sgl.getCurrentEvent().getParticipants() : null;
        return EventParticipant.resolveName(seatIndex, viewParticipants, currentParticipants);
    }

    void onDraftSeatPicked(int seatIndex, int[] seatQueueDepths) {
        SwingUtilities.invokeLater(() -> {
            FDraftOverlay.SINGLETON_INSTANCE.onSeatPicked(seatQueueDepths);

            int depth = (seatIndex >= 0 && seatIndex < seatQueueDepths.length) ? seatQueueDepths[seatIndex] : 0;
            if (seatIndex == mySeatIndex) {
                if (networkDraftEditor != null) {
                    networkDraftEditor.flushSelfPickLog(depth);
                }
            } else {
                NetworkDraftLog.logOtherPick(resolveParticipantName(seatIndex), depth);
            }
        });
    }

    void onDraftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack) {
        SwingUtilities.invokeLater(() -> {
            if (networkDraftEditor != null) {
                networkDraftEditor.addAutoPickedCard(card, packNumber, pickInPack);
            }
        });
    }

    /** Release draft editor and overlay. Safe to call when no draft is active. */
    public void cancelActiveDraft() {
        networkDraftEditor = null;
        FDraftOverlay.SINGLETON_INSTANCE.reset();
    }

    void onReceiveEventPool(String eventId, Deck pool) {
        SwingUtilities.invokeLater(() -> {
            if (networkDraftEditor != null) {
                networkDraftEditor.completeDraft(pool);
                networkDraftEditor = null;
            } else {
                FModel.getDecks().getNetworkEventDecks().add(pool);
                CEditorLimited<Deck> editor = new CEditorLimited<>(
                        FModel.getDecks().getNetworkEventDecks(), Deck::new,
                        FScreen.DECK_EDITOR_SEALED, CDeckEditorUI.SINGLETON_INSTANCE.getCDetailPicture());
                Singletons.getControl().setCurrentScreen(FScreen.DECK_EDITOR_SEALED);
                CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(editor);
                editor.getDeckController().load(null, pool.getName());
            }
            lastPackNumber = 0;

            activeEventId = eventId;
            activeConformance = true;
            if (view.getLobby().hasControl()) {
                scanAvailableEvents();
                broadcastEventSelection();
            }
            view.updateRightPanelForMode();
            view.updateEventPanelState();
            view.updateActionButtons();
        });
    }

    public void initialize() {
        final ForgePreferences prefs = FModel.getPreferences();
        // Checkbox event handling
        view.getCbSingletons().addActionListener(arg0 -> {
            prefs.setPref(FPref.DECKGEN_SINGLETONS, String.valueOf(view.getCbSingletons().isSelected()));
            prefs.save();
        });

        view.getCbArtifacts().addActionListener(arg0 -> {
            prefs.setPref(FPref.DECKGEN_ARTIFACTS, String.valueOf(view.getCbArtifacts().isSelected()));
            prefs.save();
        });

        // Pre-select checkboxes
        view.getCbSingletons().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_SINGLETONS));
        view.getCbArtifacts().setSelected(prefs.getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
    }
}
