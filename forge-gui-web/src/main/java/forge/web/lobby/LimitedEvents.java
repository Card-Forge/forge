package forge.web.lobby;

import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.FormatChoices;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gamemodes.match.GameLobby;
import forge.gamemodes.match.GameLobby.GameLobbyData;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.net.EventFormat;
import forge.gamemodes.net.EventPhase;
import forge.gamemodes.net.NetworkEvent;
import forge.gamemodes.net.NetworkEventView;
import forge.gamemodes.net.server.ServerGameLobby;
import forge.model.FModel;
import forge.util.Localizer;
import forge.web.deck.DeckCatalog;
import forge.web.limited.OfflinePools;
import forge.web.match.NetplaySeat;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser.*;

import java.util.List;
import java.util.Objects;

/** The Limited half of a table: the draft and sealed events hosted through the lobby. */
public final class LimitedEvents {
    private final Lobby owner;
    private final NetplaySeat local;

    LimitedEvents(final Lobby owner, final NetplaySeat local) {
        this.owner = owner;
        this.local = local;
    }

    /** Whether the table's event has begun. Either its packs are dealt, or a past event's decks were chosen for the match. */
    boolean eventStarted() {
        final GameLobby lobby = owner.view();
        final GameLobbyData data = lobby == null ? null : lobby.getData();
        if (data == null) {
            return false;
        }
        final NetworkEventView event = data.getEventView();
        return data.getActiveEventId() != null || (event != null && event.getPhase() != EventPhase.LOBBY_GATHER);
    }

    /** Sets whether every seat's finder lists only the decks of the table's event, as desktop's conformance switch does. */
    public void setEventDecksOnly(final boolean on) {
        final ServerGameLobby lobby = owner.host();
        if (lobby != null && lobby.getData().getActiveEventId() != null) {
            lobby.selectEventForMatch(lobby.getData().getActiveEventId(), on);
        }
    }

    /** The Limited part of the table, read from the lobby data so a guest sees the same values. Null at a Constructed table. */
    LimitedTable limitedTable(final GameLobby lobby) {
        if (!Lobby.limited(lobby)) {
            return null;
        }
        final GameLobbyData data = lobby.getData();
        final NetworkEventView event = data.getEventView();
        final boolean draft = data.getLimitedType() != GameType.Sealed;
        // An event's product is blank until the event is set up
        String product = event == null || event.getProductDescription() == null || event.getProductDescription().isEmpty()
                ? null : event.getProductDescription();
        // A past event that is played again has no product of its own, so the product is read from its pools' tag
        if (product == null && data.getActiveEventId() != null) {
            product = playedAgain(data.getActiveEventId());
        }
        return new LimitedTable(draft ? EventKind.draft : EventKind.sealed, product,
                event == null ? 0 : event.getPodSize(),
                event == null || event.getDoublePick() == null ? null : event.getDoublePick().name(),
                event == null ? 0 : event.getPickTimerSeconds(), event == null ? null : event.getPhase().name(),
                data.getActiveEventId(), data.isActiveConformance(), eventStarted(),
                local.isHost() && data.getActiveEventId() == null && !eventStarted() ? pastEvents() : List.of());
    }

    /** The events whose pools the host has stored, newest first, in the same order as desktop's past events list. */
    private static List<PastEvent> pastEvents() {
        synchronized (DeckCatalog.DECKS) {
            return NetworkEvent.pastEventIds().stream().map(id -> pastEvent(id, NetworkEvent.findEventDeck(id))).toList();
        }
    }

    /** The product of a past event, read from its pools. A pool saved without a product is still named as an earlier event. */
    private static String playedAgain(final String eventId) {
        final Deck pool;
        synchronized (DeckCatalog.DECKS) {
            pool = NetworkEvent.findEventDeck(eventId);
        }
        final String product = pool == null ? null : DeckProxy.getEventTag(pool, "eventProduct");
        return product == null || product.isBlank() ? Localizer.getInstance().getMessage("lblWebEventAnEarlierEvent") : product;
    }

    /** A past event, as described by the tags on one of its pools. */
    private static PastEvent pastEvent(final String id, final Deck pool) {
        final boolean draft = EventFormat.BOOSTER_DRAFT.name().equals(DeckProxy.getEventTag(pool, "eventFormat"));
        return new PastEvent(id, draft ? EventKind.draft : EventKind.sealed, Objects.toString(DeckProxy.getEventTag(pool, "eventProduct"), ""),
                Objects.toString(DeckProxy.getEventTag(pool, "eventDate"), ""));
    }

    /** Plays an event's decks at this table. type is Draft or Sealed. Returns the reason it cannot, or null. */
    public String playEvent(final String eventId, final GameType type, final boolean decksOnly) {
        final ServerGameLobby lobby = owner.host();
        if (lobby == null || eventId == null) {
            return null;
        }
        if (Lobby.drafting(lobby)) {
            return Localizer.getInstance().getMessage("lblWebLobbyDraftStillOn");
        }
        lobby.clearCurrentEvent();
        if (!Lobby.limited(lobby)) {
            withoutFormat(lobby);
        }
        lobby.setLimitedType(type);
        lobby.selectEventForMatch(eventId, decksOnly);
        return null;
    }

    /** Hosts a past event again. Its kind is read from its pools, as on desktop. */
    public String hostAgain(final String eventId) {
        final Deck pool;
        synchronized (DeckCatalog.DECKS) {
            pool = eventId == null ? null : NetworkEvent.findEventDeck(eventId);
        }
        if (pool == null) {
            return Localizer.getInstance().getMessage("lblWebLobbyNoEventDecks");
        }
        final boolean sealed = EventFormat.SEALED.name().equals(DeckProxy.getEventTag(pool, "eventFormat"));
        return playEvent(eventId, sealed ? GameType.Sealed : GameType.Draft, true);
    }

    /** Starts a new event of the same kind. The old event's pools are kept so it can be played again. Returns the reason it cannot, or null. */
    public String newEvent() {
        final ServerGameLobby lobby = owner.host();
        if (lobby == null || !Lobby.limited(lobby)) {
            return null;
        }
        if (Lobby.drafting(lobby)) {
            return Localizer.getInstance().getMessage("lblWebLobbyDraftStillOn");
        }
        final EventKind kind = lobby.getData().getLimitedType() == GameType.Sealed ? EventKind.sealed : EventKind.draft;
        lobby.clearCurrentEvent();
        lobby.selectEventForMatch(null, false);
        for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
            lobby.getSlot(i).setSittingOut(false);
        }
        return setLimited(kind);
    }

    /** Deletes a past event's pools from the host's event decks. The event the table is playing is kept. Returns the reason it cannot, or null. */
    public String forgetEvent(final String eventId) {
        final ServerGameLobby lobby = owner.host();
        if (lobby == null || eventId == null) {
            return null;
        }
        if (eventId.equals(lobby.getData().getActiveEventId())) {
            return Localizer.getInstance().getMessage("lblWebLobbyEventInPlay");
        }
        synchronized (DeckCatalog.DECKS) {
            final var stored = FModel.getDecks().getNetworkEventDecks();
            for (final Deck d : stored.stream().toList()) {
                if (eventId.equals(DeckProxy.getEventTag(d, "eventId"))) {
                    stored.delete(d.getName());
                }
            }
        }
        return null;
    }

    /** Switches the table to a kind of event, or to Constructed when kind is null. Returns the reason it cannot, or null. */
    public String setLimited(final EventKind kind) {
        final ServerGameLobby lobby = owner.host();
        if (lobby == null) {
            return null;
        }
        if (Lobby.drafting(lobby)) {
            return Localizer.getInstance().getMessage("lblWebLobbyDraftStillOn");
        }
        if (kind == null) {
            lobby.clearCurrentEvent();
            lobby.selectEventForMatch(null, false);
            // Sitting out applies only to events. A Constructed table neither shows it nor clears it, so it is cleared here
            for (int i = 0; i < lobby.getNumberOfSlots(); i++) {
                lobby.getSlot(i).setSittingOut(false);
            }
            lobby.setLimitedType(null);
            return null;
        }
        if (eventStarted()) {
            return Localizer.getInstance().getMessage("lblWebLobbyEventStartedSwitch");
        }
        final boolean sealed = kind == EventKind.sealed;
        withoutFormat(lobby);
        lobby.setLimitedType(sealed ? GameType.Sealed : GameType.Draft);
        lobby.createEvent(sealed ? EventFormat.SEALED : EventFormat.BOOSTER_DRAFT);
        return null;
    }

    /** An event is played without a format or casual variant, as desktop's Limited mode hides them. */
    private void withoutFormat(final ServerGameLobby lobby) {
        owner.applyFormat(GameType.Constructed);
        for (final GameType v : FormatChoices.VARIANTS) {
            lobby.removeVariant(v);
        }
    }

    /** Sets up the table's event from the host's form, and returns the reason it cannot, or null. It runs off the host UI thread because building the product can wait for a web site, and it changes the table on the host UI thread. */
    public String setUpEvent(final EventSetup s) {
        final ServerGameLobby lobby = owner.host();
        if (lobby == null || !Lobby.limited(lobby)) {
            return Localizer.getInstance().getMessage("lblWebLobbyChooseLimited");
        }
        if (eventStarted()) {
            return Localizer.getInstance().getMessage("lblWebLobbyEventStartedFixed");
        }
        try {
            if (lobby.getData().getLimitedType() == GameType.Sealed) {
                final SealedCardPoolGenerator gen = OfflinePools.generator(new SealedCreate(s.product(), s.block(), s.combo(),
                        s.edition(), s.template(), s.cubeId(), s.packs(), "", false));
                return owner.onTable(() -> {
                    lobby.createEvent(EventFormat.SEALED);
                    return lobby.configureEvent(LimitedPoolType.valueOf(s.product()), gen, 0, 0) ? null : Localizer.getInstance().getMessage("lblWebLobbySealedSetUpFailed");
                });
            }
            final BoosterDraft draft = OfflinePools.draft(new DraftStart(s.product(), s.block(), s.combo(), s.cube(), s.theme(),
                    s.cubeId(), s.pickRule())).get();
            if (draft == null) {
                return Localizer.getInstance().getMessage("lblWebLobbyDraftSetUpFailed");
            }
            // Pod size as desktop chooses it: never fewer than the seats at the table, and never more than a full pod
            final int wanted = s.podSize() > 0 ? s.podSize() : draft.getPodSize();
            draft.setPodSize(Math.min(BoosterDraft.N_PLAYERS, Math.max(Math.max(2, lobby.getNumberOfSlots()), wanted)));
            return owner.onTable(() -> {
                lobby.createEvent(EventFormat.BOOSTER_DRAFT);
                return lobby.configureEvent(LimitedPoolType.valueOf(s.product()), draft, Math.max(0, s.timer()), Math.max(0, s.grace()))
                        ? null : Localizer.getInstance().getMessage("lblWebLobbyDraftSetUpFailed");
            });
        } catch (final RuntimeException e) {
            return e.getMessage() == null ? Localizer.getInstance().getMessage("lblWebLobbyEventSetUpFailed") : e.getMessage();
        }
    }

    /** Deals the packs or pools once every seat is ready, as desktop's Start Event does. Returns the reason it cannot, or null. Runs on the host UI thread, which stays busy while every pool is opened. */
    public String startEvent() {
        final ServerGameLobby lobby = owner.host();
        final NetworkEvent event = lobby == null ? null : lobby.getCurrentEvent();
        if (event == null || (event.getDraft() == null && event.getSealedGenerator() == null)) {
            return Localizer.getInstance().getMessage("lblWebLobbySetUpFirst");
        }
        if (eventStarted()) {
            return Localizer.getInstance().getMessage("lblWebLobbyEventStarted");
        }
        final LobbySlot unready = lobby.findFirstUnreadySlot();
        if (unready != null) {
            return Localizer.getInstance().getMessage("lblPlayerIsNotReady", String.valueOf(unready.getName()));
        }
        if (event.getFormat() == EventFormat.SEALED) {
            lobby.startSealedEvent();
            return null;
        }
        return lobby.startDraftEvent() == null ? Localizer.getInstance().getMessage("lblWebLobbyDraftStartFailed") : null;
    }

    /** Whether the host's setup form is open: a Limited table whose event has not begun. */
    public boolean settingUpEvent() {
        final GameLobby lobby = owner.view();
        return lobby != null && Lobby.limited(lobby) && !eventStarted();
    }

    /** Sits a seat out of the next match, or brings it back. Only the host can do this, and not while the pod is drafting. */
    public void sitOut(final int index, final boolean sittingOut) {
        final ServerGameLobby lobby = owner.host();
        if (lobby != null && Lobby.limited(lobby) && !Lobby.drafting(lobby) && index >= 0 && index < lobby.getNumberOfSlots()) {
            lobby.getSlot(index).setSittingOut(sittingOut);
            local.pushLobby();
        }
    }
}
