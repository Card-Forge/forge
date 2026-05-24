package forge.screens.online;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.deck.FDeckEditor;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.net.ChatMessage;
import forge.gamemodes.net.EventFormat;
import forge.gamemodes.net.IOnlineChatInterface;
import forge.gamemodes.net.IOnlineLobby;
import forge.gamemodes.net.NetConnectUtil;
import forge.gamemodes.net.EventParticipant;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.net.NetworkEventView;
import forge.gamemodes.net.OfflineLobby;
import forge.gamemodes.net.client.FGameClient;
import forge.gamemodes.net.event.DraftPickEvent;
import forge.gamemodes.net.server.FServerManager;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.gui.FThreads;
import forge.gui.interfaces.IDraftEventHandler;
import forge.gui.interfaces.ILobbyView;
import forge.gui.util.SGuiChoose;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.LoadingOverlay;
import forge.screens.constructed.LobbyScreen;
import forge.screens.constructed.PlayerPanel;
import forge.screens.limited.NetworkDraftingProcessScreen;
import forge.screens.online.OnlineMenu.OnlineScreen;
import forge.toolbox.FButton;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FTextField;
import forge.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class OnlineLobbyScreen extends LobbyScreen implements IOnlineLobby, IDraftEventHandler {

    private final FLabel lblTitle;
    private final FLabel lblWarning;
    private final FLabel lblGuideText;
    private final FLabel lblGuideLink;
    private final FButton btnHost;
    private final FButton btnJoin;
    private final FComboBox<String> cmbMode = new FComboBox<>();
    private final FButton btnSetUpEvent;
    private final FButton btnStartEvent;
    private final FButton btnStartMatch;
    private final FButton btnDismissEvent;
    private final FLabel lblEventPanel;
    private final FCheckBox cbDeckConformance;

    private boolean isHost;
    private boolean activeConformance = true;
    // "Constructed" or "Limited" — matches the localised string
    private String currentMode;
    private String activeEventId; // non-null after receiveEventPool; used for deck filter
    private NetworkEvent currentEvent;
    private NetworkEventView lastEventView;
    private boolean eventDrafted; // true after receiveEventPool marks draft done
    private NetworkDraftingProcessScreen activeDraftScreen;

    public OnlineLobbyScreen() {
        super(null, OnlineMenu.getMenu(), new OfflineLobby());

        lblTitle = new FLabel.Builder()
                .text("- = *  H E R E   B E   E L D R A Z I  * = -")
                .font(FSkinFont.get(18)).align(Align.center).build();
        add(lblTitle);

        lblWarning = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblOnlineWarning"))
                .font(FSkinFont.get(14)).align(Align.center).build();
        add(lblWarning);

        lblGuideText = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblOnlineGuideText"))
                .font(FSkinFont.get(14)).align(Align.center).build();
        add(lblGuideText);

        lblGuideLink = new FLabel.Builder()
                .text(Forge.getLocalizer().getMessage("lblNetworkPlayGuide"))
                .font(FSkinFont.get(14)).align(Align.center)
                .textColor(FSkinColor.get(FSkinColor.Colors.CLR_ACTIVE))
                .command(e -> Gdx.net.openURI(ForgeConstants.NETWORK_PLAY_WIKI_URL)).build();
        add(lblGuideLink);

        btnHost = new FButton(Forge.getLocalizer().getMessage("lblHostGame"));
        btnHost.setCommand(e -> activateHost());
        add(btnHost);

        btnJoin = new FButton(Forge.getLocalizer().getMessage("lblJoinGame"));
        btnJoin.setCommand(e -> activateJoin());
        add(btnJoin);

        cmbMode.addItem(Forge.getLocalizer().getMessage("lblConstructed"));
        cmbMode.addItem(Forge.getLocalizer().getMessage("lblLimited"));
        currentMode = Forge.getLocalizer().getMessage("lblConstructed");
        cmbMode.setFont(FSkinFont.get(12));
        cmbMode.setChangedHandler(e -> onModeChanged());
        add(cmbMode);

        btnSetUpEvent = new FButton(Forge.getLocalizer().getMessage("lblNetworkNewEventButton"));
        btnSetUpEvent.setCommand(e -> openSetUpEventDialog());
        add(btnSetUpEvent);

        btnStartEvent = new FButton("");
        btnStartEvent.setCommand(e -> startEvent());
        add(btnStartEvent);

        btnStartMatch = new FButton(Forge.getLocalizer().getMessage("lblNetworkStartMatch"));
        btnStartMatch.setCommand(e -> startMatch());
        add(btnStartMatch);

        btnDismissEvent = new FButton("X");
        btnDismissEvent.setCommand(e -> onDismissEvent());
        add(btnDismissEvent);

        lblEventPanel = new FLabel.Builder().align(Align.left).font(FSkinFont.get(12)).build();
        add(lblEventPanel);

        cbDeckConformance = new FCheckBox(Forge.getLocalizer().getMessage("lblNetworkDeckFilter"));
        cbDeckConformance.setFont(FSkinFont.get(12)); // match the event info panel
        cbDeckConformance.setSelected(true);
        cbDeckConformance.setCommand(e -> onConformanceChanged());
        add(cbDeckConformance);
    }

    private boolean isLimitedMode() {
        return Forge.getLocalizer().getMessage("lblLimited").equals(currentMode);
    }

    private void onModeChanged() {
        if (!isHost) return;
        String selected = cmbMode.getSelectedItem();
        if (selected == null || selected.equals(currentMode)) return;
        currentMode = selected;

        ServerGameLobby sgl = serverLobby();
        if (!isLimitedMode()) {
            if (sgl != null) sgl.clearCurrentEvent();
            currentEvent = null;
            activeEventId = null;
            eventDrafted = false;
            refreshEventPanel();
            updateDeckListFilter();
        }
        // Broadcast the mode so clients mirror it (their combo is read-only)
        if (sgl != null) sgl.setLimitedMode(isLimitedMode());

        updateModeVisibility();
        updateActionButtons();
        revalidate();
    }

    private void onDismissEvent() {
        ServerGameLobby sgl = serverLobby();
        if (sgl != null) sgl.clearCurrentEvent();
        currentEvent = null;
        activeEventId = null;
        broadcastEventSelection();
        refreshEventPanel();
        updateDeckListFilter();
        updateActionButtons();
        revalidate();
    }

    private void onConformanceChanged() {
        activeConformance = cbDeckConformance.isSelected();
        updateDeckListFilter();
        broadcastEventSelection();
    }

    private void broadcastEventSelection() {
        ServerGameLobby sgl = serverLobby();
        if (sgl != null) {
            sgl.selectEventForMatch(activeEventId, activeConformance);
        }
    }

    private void updateModeVisibility() {
        setVariantsVisible(!isLimitedMode());
    }

    private void updateActionButtons() {
        if (!isHost) return;
        boolean limited = isLimitedMode();
        if (limited) {
            boolean hasEvent = currentEvent != null;
            boolean hasPool  = eventDrafted;
            String startEventLabel = (hasEvent && currentEvent.getFormat() == EventFormat.SEALED)
                    ? Forge.getLocalizer().getMessage("lblNetworkGeneratePools")
                    : Forge.getLocalizer().getMessage("lblNetworkStartDraft");
            btnStartEvent.setText(startEventLabel);
            btnStartEvent.setEnabled(hasEvent && !hasPool);
            btnStartMatch.setEnabled(hasPool);
        }
    }

    private void openSetUpEventDialog() {
        if (serverLobby() == null) return;
        FThreads.invokeInBackgroundThread(() -> {
            ServerGameLobby sgl = serverLobby();
            if (sgl == null) return;

            String lblDraft  = Forge.getLocalizer().getMessage("lblDraft");
            String lblSealed = Forge.getLocalizer().getMessage("lblSealed");
            String chosen = SGuiChoose.oneOrNone(
                    Forge.getLocalizer().getMessage("lblNetworkChooseEventType"),
                    Arrays.asList(lblDraft, lblSealed));
            if (chosen == null) return;

            boolean isDraft = lblDraft.equals(chosen);
            EventFormat format = isDraft ? EventFormat.BOOSTER_DRAFT : EventFormat.SEALED;
            sgl.createEvent(format);

            LimitedPoolType poolType = SGuiChoose.oneOrNone(
                    Forge.getLocalizer().getMessage("lblNetworkChooseDraftFormat"),
                    Arrays.asList(LimitedPoolType.values(isDraft)));
            if (poolType == null) return;

            // Building the draft fires set/format sub-dialogs
            BoosterDraft draft = null;
            if (isDraft) {
                draft = BoosterDraft.createDraftForNetwork(poolType);
                if (draft == null) return;
            }

            NetworkEvent event = sgl.getCurrentEvent();
            if (event == null) return;
            int timerSeconds = event.getPickTimerSeconds();
            int graceSeconds = event.getDisconnectGraceSeconds();

            if (isDraft) {
                String timersTitle = Forge.getLocalizer().getMessage("lblNetworkDraftTimersTitle");
                String pickStr = SOptionPane.showInputDialog(
                        Forge.getLocalizer().getMessage("lblNetworkPickTimerPrompt"),
                        timersTitle, null, String.valueOf(timerSeconds), null, true);
                if (pickStr == null) return;
                String graceStr = SOptionPane.showInputDialog(
                        Forge.getLocalizer().getMessage("lblNetworkGraceTimerPromptLine1") + " "
                                + Forge.getLocalizer().getMessage("lblNetworkGraceTimerPromptLine2"),
                        timersTitle, null, String.valueOf(graceSeconds), null, true);
                if (graceStr == null) return;
                finishConfigureEvent(sgl, poolType, draft,
                        parseSecondsOrDefault(pickStr, timerSeconds),
                        parseSecondsOrDefault(graceStr, graceSeconds));
            } else {
                finishConfigureEvent(sgl, poolType, null, timerSeconds, graceSeconds);
            }
        });
    }

    private void finishConfigureEvent(ServerGameLobby sgl, LimitedPoolType poolType,
            BoosterDraft draft, int timerSeconds, int graceSeconds) {
        if (!sgl.configureEvent(poolType, draft, timerSeconds, graceSeconds)) return;
        FThreads.invokeInEdtLater(() -> {
            currentEvent = sgl.getCurrentEvent();
            eventDrafted = false;
            refreshEventPanel();
            updateActionButtons();
            revalidate();
        });
    }

    private static int parseSecondsOrDefault(String text, int fallback) {
        try {
            int n = Integer.parseInt(text.trim());
            return n >= 0 ? n : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void startEvent() {
        ServerGameLobby sgl = serverLobby();
        if (sgl == null || currentEvent == null) return;
        LobbySlot unready = sgl.findFirstUnreadySlot();
        if (unready != null) {
            FOptionPane.showMessageDialog(
                    Forge.getLocalizer().getMessage("lblPlayerIsNotReady", unready.getName()));
            return;
        }
        if (currentEvent.getFormat() == EventFormat.BOOSTER_DRAFT) {
            FThreads.invokeInBackgroundThread(() -> {
                ServerGameLobby.DraftStartResult result = sgl.startDraftEvent();
                if (result == null) {
                    FThreads.invokeInEdtLater(() ->
                            FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblNetworkFailedDraft")));
                }
            });
        } else {
            FThreads.invokeInBackgroundThread(sgl::startSealedEvent);
        }
    }

    private ServerGameLobby serverLobby() {
        GameLobby lobby = getLobby();
        return lobby instanceof ServerGameLobby sgl ? sgl : null;
    }

    private static GameLobby gameLobby;

    public static GameLobby getGameLobby() {
        return gameLobby;
    }

    public static void clearGameLobby() {
        gameLobby = null;
    }

    public static void setGameLobby(GameLobby gameLobby) {
        OnlineLobbyScreen.gameLobby = gameLobby;
    }

    private static FGameClient fGameClient;

    public static FGameClient getfGameClient() {
        return fGameClient;
    }

    public static void closeClient() {
        getfGameClient().close();
        fGameClient = null;
    }

    @Override
    public IDraftEventHandler getDraftHandler() {
        return this;
    }

    @Override
    public void draftPackArrived(int seatIndex, List<PaperCard> pack,
            int packNumber, int pickNumber, int timerDurationSeconds) {
        // Host routes picks directly; client sends via network
        Consumer<DraftPickEvent> pickSender;
        if (getLobby() instanceof ServerGameLobby sgl) {
            pickSender = ev -> sgl.handleDraftPick(ev, -1);
        } else {
            pickSender = ev -> {
                if (getfGameClient() != null) {
                    getfGameClient().send(ev);
                }
            };
        }
        final Consumer<DraftPickEvent> finalPickSender = pickSender;

        // This runs on a Netty thread; activeDraftScreen and the event fields are EDT-owned
        FThreads.invokeInEdtNowOrLater(() -> {
            if (activeDraftScreen == null) {
                // Host has the full NetworkEvent; a client only ever has the broadcast view
                List<EventParticipant> participants = currentEvent != null
                        ? currentEvent.getParticipants()
                        : (lastEventView != null ? lastEventView.getParticipants() : List.of());
                activeDraftScreen = new NetworkDraftingProcessScreen(
                        seatIndex, participants,
                        finalPickSender,
                        () -> {
                            activeDraftScreen = null;
                            closeConn("");
                        });
                Forge.openScreen(activeDraftScreen);
            }
            activeDraftScreen.onPackArrived(pack, packNumber, pickNumber, timerDurationSeconds);
        });
    }

    @Override
    public void draftSeatPicked(int seatIndex, int[] seatQueueDepths) {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (activeDraftScreen != null) {
                activeDraftScreen.onSeatPicked(seatIndex, seatQueueDepths);
            }
        });
    }

    @Override
    public void draftAutoPicked(int seatIndex, PaperCard card, int packNumber, int pickInPack) {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (activeDraftScreen != null) {
                activeDraftScreen.onAutoPicked(seatIndex, card, packNumber, pickInPack);
            }
        });
    }

    @Override
    public void receiveEventPool(String eventId, Deck pool) {
        FThreads.invokeInEdtNowOrLater(() -> {
            if (currentEvent != null && DeckProxy.getEventTag(pool, "eventId") == null) {
                NetworkEvent.setEventTags(pool, currentEvent);
            }
            FModel.getDecks().getNetworkEventDecks().add(pool);

            activeEventId = eventId;
            activeConformance = true;
            if (isHost) {
                broadcastEventSelection();
            }
            updateDeckListFilter();

            boolean draftCompletion = activeDraftScreen != null;
            if (draftCompletion) {
                activeDraftScreen.onDraftCompleted();
                activeDraftScreen = null;
                Forge.back();
            }
            eventDrafted = true;
            currentEvent = null;
            updateActionButtons();
            revalidate();

            Forge.openScreen(new FDeckEditor(FDeckEditor.EditorConfigNetworkEventPool, pool));
        });
    }

    @Override
    public void closeConn(String msg) {
        if (getfGameClient() != null) {
            getfGameClient().setDraftHandler(null);
        }
        if (FServerManager.getInstance() != null) {
            FServerManager.getInstance().setDraftHandler(null);
        }
        // A dropped connection invokes this on a Netty thread; the lobby/draft-screen state
        // is EDT-owned, so reset it and tear down the screen on the EDT.
        FThreads.invokeInEdtNowOrLater(() -> {
            NetworkDraftingProcessScreen draftScreen = activeDraftScreen;
            activeDraftScreen = null;
            currentEvent = null;
            activeEventId = null;
            activeConformance = true;
            eventDrafted = false;
            lastEventView = null;
            clearGameLobby();
            if (draftScreen != null) {
                draftScreen.onDraftCompleted(); // connection gone — close silently, like a finished draft
            }
            Forge.back();
        });
        if (msg.length() > 0) {
            FThreads.invokeInBackgroundThread(() -> {
                final boolean callBackAlwaysTrue = SOptionPane.showOptionDialog(msg, Forge.getLocalizer().getMessage("lblError"), FSkinProp.ICO_WARNING, List.of(Forge.getLocalizer().getMessage("lblOK")), 1) == 0;
                if (callBackAlwaysTrue) { //to activate online menu popup when player press play online
                    if (FServerManager.getInstance() != null)
                        FServerManager.getInstance().stopServer();
                    if (getfGameClient() != null)
                        closeClient();
                }
            });
        }
    }

    @Override
    public ILobbyView setLobby(GameLobby lobby0) {
        initLobby(lobby0);
        return this;
    }

    @Override
    public void setClient(FGameClient client) {
        fGameClient = client;
    }

    @Override
    public void update(boolean fullUpdate) {
        super.update(fullUpdate);
        // The base constructor calls update() before our fields initialize; skip until ready
        if (cmbMode == null) {
            return;
        }
        if (!isHost && getLobby() != null && getLobby().getData() != null) {
            GameLobby.GameLobbyData data = getLobby().getData();

            // The combo is read-only on a client; mirror the host's declared mode
            String hostMode = Forge.getLocalizer().getMessage(data.isLimitedMode() ? "lblLimited" : "lblConstructed");
            boolean modeChanged = !hostMode.equals(currentMode);
            if (modeChanged) {
                currentMode = hostMode;
                cmbMode.setSelectedItem(currentMode);
            }

            NetworkEventView view = data.getEventView();
            boolean viewChanged = view != lastEventView;
            if (viewChanged) {
                lastEventView = view;
                activeEventId = view != null ? view.getEventId() : null;
            }

            boolean newConformance = data.isActiveConformance();
            if (newConformance != activeConformance) {
                activeConformance = newConformance;
                updateDeckListFilter();
            }

            if (modeChanged || viewChanged) {
                updateModeVisibility();
                updateDeckListFilter();
                refreshEventPanel();
                revalidate();
            }
        }
    }

    @Override
    public void onActivate() {
        if (getGameLobby() == null) {
            revalidate();
        } else {
            super.onActivate();
        }
    }

    @Override
    public void setStartButtonAvailability() {
        // In Limited the action-button row replaces btnStart; keep it hidden so the base
        // host-visibility rule (called from update()) doesn't draw it over those buttons.
        if (isHost && isLimitedMode()) {
            btnStart.setVisible(false);
            return;
        }
        super.setStartButtonAvailability();
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        if (getGameLobby() == null) {
            btnStart.setVisible(false);
            setLobbyControlsVisible(false);
            cmbMode.setVisible(false);
            btnSetUpEvent.setVisible(false);
            btnStartEvent.setVisible(false);
            btnStartMatch.setVisible(false);
            btnDismissEvent.setVisible(false);
            lblEventPanel.setVisible(false);
            cbDeckConformance.setVisible(false);

            float padding = Utils.scale(10);
            float y = startY + height * 0.15f;

            float labelHeight = lblTitle.getAutoSizeBounds().height + padding;
            lblTitle.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblTitle.setVisible(true);
            y += labelHeight + padding * 2;

            labelHeight = lblWarning.getAutoSizeBounds().height + padding;
            lblWarning.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblWarning.setVisible(true);
            y += labelHeight + padding;

            labelHeight = lblGuideText.getAutoSizeBounds().height + padding;
            lblGuideText.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblGuideText.setVisible(true);
            y += labelHeight;

            labelHeight = lblGuideLink.getAutoSizeBounds().height + padding;
            lblGuideLink.setBounds(padding, y, width - 2 * padding, labelHeight);
            lblGuideLink.setVisible(true);
            y += labelHeight + padding * 4;

            float buttonGap = padding * 2;
            float buttonWidth = width * 0.35f;
            float totalButtonWidth = buttonWidth * 2 + buttonGap;
            float buttonX = (width - totalButtonWidth) / 2;
            float buttonHeight = Utils.AVG_FINGER_HEIGHT;
            btnHost.setBounds(buttonX, y, buttonWidth, buttonHeight);
            btnHost.setVisible(true);
            btnJoin.setBounds(buttonX + buttonWidth + buttonGap, y, buttonWidth, buttonHeight);
            btnJoin.setVisible(true);
        } else {
            lblTitle.setVisible(false);
            lblWarning.setVisible(false);
            lblGuideText.setVisible(false);
            lblGuideLink.setVisible(false);
            btnHost.setVisible(false);
            btnJoin.setVisible(false);
            setLobbyControlsVisible(true);
            // Variants are hidden in Limited mode (host and client alike)
            if (isLimitedMode()) {
                setVariantsVisible(false);
            }

            float padding = Utils.scale(10);

            boolean limited = isLimitedMode();
            float fieldH = FTextField.getDefaultHeight(FSkinFont.get(12));
            float comboW = Utils.AVG_FINGER_WIDTH * 3;
            // The mode combo shows for host and client alike; read-only (disabled) for a
            // client, whose selection is synced from the host in update().
            cmbMode.setVisible(true);
            cmbMode.setEnabled(isHost);
            cmbMode.setBounds(padding, startY + padding, comboW, fieldH);
            // Give the combo its own row in both modes so the player list starts at the
            // same height; in Limited the hidden variants row leaves that slot blank
            float superStartY = startY + padding + fieldH;

            if (isHost && limited) {
                btnStart.setVisible(false);
                updateActionButtons();
            } else {
                btnSetUpEvent.setVisible(false);
                btnStartEvent.setVisible(false);
                btnStartMatch.setVisible(false);
                if (isHost) {
                    btnStart.setVisible(true); // Constructed host; clients have no start button
                }
            }

            // Event info and the deck-filter toggle sit in a band at the bottom of the
            // lobby, just above the action buttons; shrink the player list to fit above it.
            boolean hasEvent = currentEvent != null || lastEventView != null;
            boolean showConformance = isLimitedMode() && (currentEvent != null || activeEventId != null);
            float panelH = hasEvent ? lblEventPanel.getAutoSizeBounds().height : 0;
            float checkH = showConformance ? Utils.AVG_FINGER_HEIGHT * 0.75f : 0;
            float bottomBandH = (hasEvent ? panelH + padding : 0) + (showConformance ? checkH + padding : 0);

            // In Limited the action buttons sit centred in btnStart's (taller) band, so
            // anchor the event band just above that row rather than to the band's top —
            // otherwise the centring slack leaves a gap below the conformance checkbox.
            float rowH = Utils.AVG_FINGER_HEIGHT;
            float actionRowTop = btnStart.getTop() + (btnStart.getHeight() - rowH) / 2f;
            float bandBottom = (isHost && limited) ? actionRowTop : height;
            float by = bandBottom - bottomBandH;

            super.doLayoutAboveBtnStart(superStartY, width, by);

            if (hasEvent) {
                float dismissW = Utils.AVG_FINGER_WIDTH;
                boolean showDismiss = isHost && currentEvent != null;
                float panelW = showDismiss ? width - dismissW - padding : width;
                lblEventPanel.setBounds(0, by, panelW, panelH);
                lblEventPanel.setVisible(true);
                if (showDismiss) {
                    btnDismissEvent.setBounds(panelW + padding, by, dismissW, panelH);
                    btnDismissEvent.setVisible(true);
                } else {
                    btnDismissEvent.setVisible(false);
                }
                by += panelH + padding;
            } else {
                lblEventPanel.setVisible(false);
                btnDismissEvent.setVisible(false);
            }

            if (showConformance) {
                cbDeckConformance.setBounds(0, by, width, checkH);
                cbDeckConformance.setEnabled(isHost);
                cbDeckConformance.setVisible(true);
            } else {
                cbDeckConformance.setVisible(false);
            }

            if (isHost && limited) {
                // The three Limited action buttons replace btnStart, laid across its band
                float btnW = width / 3 - padding;
                btnSetUpEvent.setBounds(0, actionRowTop, btnW, rowH);
                btnStartEvent.setBounds(btnW + padding, actionRowTop, btnW, rowH);
                btnStartMatch.setBounds((btnW + padding) * 2, actionRowTop, btnW, rowH);
                btnSetUpEvent.setVisible(true);
                btnStartEvent.setVisible(true);
                btnStartMatch.setVisible(true);
            }
        }
    }

    void refreshEventPanel() {
        if (currentEvent == null && lastEventView == null && activeEventId == null) {
            lblEventPanel.setText("");
            revalidate();
            return;
        }
        NetworkEvent.EventPanelText text = NetworkEvent.computeEventPanelText(
                isHost, activeEventId, currentEvent, lastEventView);
        StringBuilder sb = new StringBuilder();
        if (!text.formatText().isEmpty()) sb.append(text.formatText()).append('\n');
        if (!text.productText().isEmpty()) sb.append(text.productText()).append('\n');
        if (!text.timerText().isEmpty()) sb.append(text.timerText()).append('\n');
        if (!text.dateText().isEmpty()) sb.append(text.dateText()).append('\n');
        if (!text.statusText().isEmpty()) sb.append(text.statusText());
        String result = sb.toString();
        if (result.endsWith("\n")) result = result.substring(0, result.length() - 1);
        lblEventPanel.setText(result);
        revalidate();
    }

    private void updateDeckListFilter() {
        FModel.getDecks().reloadNetworkEventDecks();
        String filterEventId = activeEventId != null ? activeEventId
                : (currentEvent != null ? currentEvent.getEventId() : null);
        List<DeckProxy> pool = DeckProxy.getAllNetworkEventDecks();
        if (filterEventId != null && activeConformance) {
            pool.removeIf(dp -> dp.getDeck() == null
                    || !filterEventId.equals(DeckProxy.getEventTag(dp.getDeck(), "eventId")));
        }
        for (PlayerPanel panel : getPlayerPanels()) {
            FDeckChooser chooser = panel.getDeckChooser();
            if (chooser.getSelectedDeckType() != DeckType.NET_EVENT_DECK) {
                chooser.setSelectedDeckType(DeckType.NET_EVENT_DECK);
            }
            DeckProxy prev = chooser.getLstDecks().getSelectedItem();
            String prevName = (prev != null && prev.getDeck() != null) ? prev.getDeck().getName() : null;
            chooser.getLstDecks().setPool(pool);
            chooser.getLstDecks().setup(ItemManagerConfig.NET_EVENT_DECKS);
            if (prevName != null) {
                for (DeckProxy dp : pool) {
                    if (dp.getDeck() != null && prevName.equals(dp.getDeck().getName())) {
                        chooser.getLstDecks().setSelectedItem(dp);
                        break;
                    }
                }
            }
        }
    }

    private void activateHost() {
        isHost = true;
        setGameLobby(getLobby());
        revalidate();
        NetConnectUtil.ensurePlayerName();
        final String caption = Forge.getLocalizer().getMessage("lblStartingServer");
        LoadingOverlay.show(caption, true, () -> {
            final ChatMessage[] result = new ChatMessage[1];
            final IOnlineChatInterface chatInterface = (IOnlineChatInterface) OnlineScreen.Chat.getScreen();
            FThreads.invokeInBackgroundThread(() -> {
                result[0] = NetConnectUtil.host(OnlineLobbyScreen.this, chatInterface);
                chatInterface.addMessage(result[0]);
                NetConnectUtil.copyHostedServerUrl();
            });
            OnlineScreen.Lobby.update();
        });
    }

    private void activateJoin() {
        isHost = false;
        setGameLobby(getLobby());
        revalidate();
        FThreads.invokeInBackgroundThread(() -> {
            final String url = NetConnectUtil.getJoinServerUrl();
            FThreads.invokeInEdtLater(() -> {
                if (url == null) {
                    closeConn("");
                    return;
                }
                final String caption = Forge.getLocalizer().getMessage("lblConnectingToServer");
                LoadingOverlay.show(caption, true, () -> {
                    final ChatMessage[] result = new ChatMessage[1];
                    final IOnlineChatInterface chatInterface = (IOnlineChatInterface) OnlineScreen.Chat.getScreen();
                    result[0] = NetConnectUtil.join(url, OnlineLobbyScreen.this, chatInterface);
                    String message = result[0].getMessage();
                    if (ForgeConstants.CLOSE_CONN_COMMAND.equals(message)) {
                        closeConn(Forge.getLocalizer().getMessage("UnableConnectToServer", url));
                        return;
                    } else if (message != null && message.startsWith(ForgeConstants.CONN_ERROR_PREFIX)) {
                        String errorDetail = message.substring(ForgeConstants.CONN_ERROR_PREFIX.length());
                        closeConn(errorDetail);
                        return;
                    } else if (ForgeConstants.INVALID_HOST_COMMAND.equals(message)) {
                        closeConn(Forge.getLocalizer().getMessage("lblDetectedInvalidHostAddress", url));
                        return;
                    }
                    chatInterface.addMessage(result[0]);
                    OnlineScreen.Lobby.update();
                });
            });
        });
    }
}
