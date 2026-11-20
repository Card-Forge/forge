package forge.web;

import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import forge.web.deck.DeckWorkspace;
import forge.web.limited.OfflinePools;
import forge.web.limited.OfflineLimited;
import forge.web.lobby.Lobby;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.Commands;
import forge.web.protocol.Commands.Handler;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.ToBrowser.DeckDetailsMessage;
import forge.web.protocol.ToBrowser.ExtraChoices;

import static forge.web.WebSession.error;

/** The commands that {@link Lobby} answers: changes to the table's seats, format, variants and event, and the deck finder's questions. */
public final class TableCommands {
    public static final Commands<TableCommands> COMMANDS = new Commands<TableCommands>()
            .on(Plain.decks, (t, ch, c) -> t.lobby.queueDecks(ch, t.decks::inTurn))
            .on(Ready.class, atSetup((t, ch, r) -> t.lobby.setReady(r.ready())))
            .on(SeatCommand.class, atSetup(TableCommands::seatCommand))
            .on(SetFormat.class, atSetup((t, ch, f) -> {
                t.lobby.setFormat(f.format());
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(SetCardPool.class, atSetup((t, ch, p) -> {
                t.lobby.setCardPool(p.cardPool());
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(SetVariant.class, atSetup((t, ch, v) -> {
                t.lobby.setVariant(v.variant(), v.on(), WebSession.invited(t.session.stage()));
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(SetArchenemy.class, atSetup((t, ch, a) -> t.lobby.setArchenemy(a.index())))
            .on(SetTeams.class, atSetup((t, ch, c) -> t.lobby.setTeams(c.count())))
            .on(SetTeam.class, atSetup((t, ch, c) -> t.lobby.setTeam(c.index(), c.team())))
            .on(SetSeatExtra.class, atSetup((t, ch, e) -> t.lobby.setSeatExtra(e.index(), e.section(), e.choice())))
            .on(SetPlayerCount.class, atSetup((t, ch, p) -> t.lobby.setPlayerCount(p.count(), WebSession.invited(t.session.stage()))))
            .on(SetMatchLength.class, atSetup((t, ch, m) -> {
                if (t.lobby.setMatchLength(m.games())) {
                    t.sessions.lobbyChanged();
                }
            }))
            .on(SetMaxBracket.class, atSetup((t, ch, b) -> {
                if (t.lobby.setMaxBracket(b.bracket())) {
                    t.sessions.lobbyChanged();
                }
            }))
            .on(SetSeat.class, atSetup((t, ch, s) -> t.session.applySeat(ch, s)))
            .on(SleeveArt.class, atSetup((t, ch, a) -> t.lobby.setSleeveArt(a.index(), a.key(), a.offset())))
            // The saved sleeves belong to the host's own Forge profile, and their list is sent in the hello
            .on(ForgetSleeveArt.class, atSetup(host((t, ch, f) -> {
                forge.util.SleeveArt.forget(f.key());
                ch.send(t.session.hello());
            })))
            .on(EventDecksOnly.class, atEvent((t, ch, d) -> {
                t.lobby.events().setEventDecksOnly(d.on());
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(EventHostAgain.class, atEvent((t, ch, e) -> {
                reportProblem(ch, t.lobby.events().hostAgain(e.eventId()));
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(EventForget.class, atEvent((t, ch, e) -> {
                reportProblem(ch, t.lobby.events().forgetEvent(e.eventId()));
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(Plain.eventNew, atEvent((t, ch, c) -> {
                final String problem = t.lobby.events().newEvent();
                if (problem != null) {
                    ch.send(error(problem));
                } else {
                    t.sendEventOptions(ch);
                }
                t.lobby.relistDecks(ch, t.decks::inTurn);
            }))
            .on(SetLimited.class, atEvent((t, ch, l) -> t.setLimited(ch, l.kind())))
            .on(SitOut.class, atEvent((t, ch, o) -> t.lobby.events().sitOut(o.index(), o.sittingOut())))
            // Building a product can wait for a web site, so it does not run on the host UI thread
            .on(EventSetup.class, atEvent((t, ch, e) -> t.ui.runBackgroundTask("Event", () -> {
                reportProblem(ch, t.lobby.events().setUpEvent(e));
                t.lobby.sendState(ch);
            })))
            .on(Plain.eventStart, atEvent((t, ch, c) -> {
                if (t.limited.draft() != null) {
                    ch.send(error(Localizer.getInstance().getMessage("lblWebSessionFinishDraft")));
                } else {
                    reportProblem(ch, t.lobby.events().startEvent());
                }
            }))
            .on(AskExtraChoices.class, (t, ch, a) -> {
                final ExtraChoices choices = t.lobby.extraChoices(a.index(), a.section());
                if (choices != null) {
                    ch.send(choices);
                }
            })
            .on(DeckQuery.class, (t, ch, q) -> t.decks.inTurn(() -> ch.send(t.lobby.deckMatches(q.kind(), q.value()))))
            .on(AskDeckDetails.class, (t, ch, a) -> t.decks.inTurn(() -> {
                final DeckDetailsMessage details = t.lobby.deckDetails(a.key());
                if (details != null) {
                    ch.send(details);
                }
            }))
            // Core asks for the category through a host question and blocks until it is answered, so this must not run on the socket thread
            .on(Plain.netDecks, host((t, ch, c) -> t.ui.runBackgroundTask("Net decks", () -> t.lobby.loadNetDecks(ch))))
            // The host's preferences are written on the host UI thread
            .on(SetDeckGen.class, host((t, ch, gen) -> t.ui.invokeInEdtLater(() -> {
                final var prefs = FModel.getPreferences();
                prefs.setPref(FPref.DECKGEN_SINGLETONS, gen.singletons());
                prefs.setPref(FPref.DECKGEN_ARTIFACTS, gen.noArtifacts());
                prefs.save();
                t.lobby.queueDecks(ch, t.decks::inTurn);
            })));

    private final WebSession session;
    private final Lobby lobby;
    private final DeckWorkspace decks;
    private final OfflineLimited limited;
    private final WebGuiBase ui;
    private final WebHub sessions;

    TableCommands(final WebSession session, final Lobby lobby, final DeckWorkspace decks, final OfflineLimited limited, final WebGuiBase ui,
            final WebHub sessions) {
        this.session = session;
        this.lobby = lobby;
        this.decks = decks;
        this.limited = limited;
        this.ui = ui;
        this.sessions = sessions;
    }

    /**
     * The table can be changed only during setup: not while it is being built, and not once its match is played.
     * Each change starts on the host UI thread, which also opens, starts and leaves the table.
     */
    private static <T extends Record> Handler<TableCommands, T> atSetup(final Handler<TableCommands, T> change) {
        return (t, ch, c) -> t.ui.invokeInEdtLater(() -> {
            if (t.session.stage() instanceof WebSession.Setup) {
                change.run(t, ch, c);
                t.lobby.sendState(ch);
            }
        });
    }

    /** A change to the table's draft or sealed event. Only the host may make it. */
    private static <T extends Record> Handler<TableCommands, T> atEvent(final Handler<TableCommands, T> change) {
        return atSetup(host(change));
    }

    private static <T extends Record> Handler<TableCommands, T> host(final Handler<TableCommands, T> handler) {
        return (t, ch, c) -> {
            if (t.session.hosting()) {
                handler.run(t, ch, c);
            }
        };
    }

    private void seatCommand(final BrowserChannel channel, final SeatCommand seat) {
        // A guest's seat is emptied by sending the guest away, and the seat itself is removed only once it is empty
        if (!session.hosting() || seat.t() == SeatAction.aiSeat || !sessions.releaseGuestAt(seat.index())) {
            switch (seat.t()) {
                case openSeat -> lobby.openSeat(seat.index());
                case aiSeat -> lobby.aiSeat(seat.index());
                case removeSeat -> lobby.removeSeat(seat.index());
            }
        }
    }

    /** Switches the table to a kind of event, or back to Constructed when kind is null. */
    void setLimited(final BrowserChannel channel, final EventKind kind) {
        final String problem = lobby.events().setLimited(kind);
        if (problem != null) {
            channel.send(error(problem));
        } else if (kind != null) {
            sendEventOptions(channel);
        }
        lobby.relistDecks(channel, decks::inTurn);
    }

    private static void reportProblem(final BrowserChannel channel, final String problem) {
        if (problem != null) {
            channel.send(error(problem));
        }
    }

    /** The event setup form offers the same options as the offline one. Reading the lists reads files, so it runs on a background thread. */
    void sendEventOptions(final BrowserChannel channel) {
        ui.runBackgroundTask("Limited", () -> channel.send(OfflinePools.options()));
    }
}
