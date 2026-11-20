package forge.web;

import forge.game.Game;
import forge.game.GameState;
import forge.game.player.Player;
import forge.gamemodes.net.server.RemoteClientGuiGame;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IDevModeCheats;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.PlayerControllerHuman;
import forge.util.Localizer;
import forge.web.match.NetplaySeat;
import forge.web.match.WebGuiGame;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.FromBrowser.Dev;
import forge.web.protocol.ToBrowser.DevDump;
import forge.web.protocol.ToBrowser.DevState;
import forge.web.protocol.ToBrowser.Notice;

import java.util.Arrays;

/** Runs the host's dev mode actions on the running game, such as making mana or setting up a board. Forge gives a netplay client no cheats. The host shares its process with the server, so only the host's own seat can cheat. */
final class DevMode {
    private DevMode() {
    }

    /** Why the host cannot cheat in the game now, or null when it can. */
    static String refusal(final NetplaySeat local) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            return Localizer.getInstance().getMessage("lblWebDevOff");
        }
        return local.ownPlayer() == null ? Localizer.getInstance().getMessage("lblWebDevNeedsOwnGame") : null;
    }

    /** Runs one cheat. Do not call it on a socket thread or the host UI thread, because a cheat waits for the browser's answers. */
    static void run(final NetplaySeat local, final WebGuiGame gui, final Dev dev, final BrowserChannel channel) {
        final String refused = refusal(local);
        if (refused != null) {
            channel.send(new Notice(Localizer.getInstance().getMessage("lblWebDevNoCheating"), refused, false));
            return;
        }
        final Player own = local.ownPlayer();
        final PlayerControllerHuman controller = (PlayerControllerHuman) own.getController();
        final IDevModeCheats cheat = controller.cheat();
        switch (dev.action()) {
            case state -> { }
            case unlimitedLands -> cheat.setCanPlayUnlimitedLands(!controller.canPlayUnlimitedLands());
            case viewAll -> {
                final boolean on = !controller.mayLookAtAllCards();
                gui.seeAll(on);
                cheat.setViewAllCards(on);
                gui.resendState();
            }
            case generateMana -> cheat.generateMana();
            case tutorForCard -> cheat.tutorForCard();
            case addCardToHand -> cheat.addCardToHand();
            case addCardToBattlefield -> cheat.addCardToBattlefield();
            case addTokenToBattlefield -> cheat.addTokenToBattlefield();
            case addCardToLibrary -> cheat.addCardToLibrary();
            case addCardToGraveyard -> cheat.addCardToGraveyard();
            case addCardToExile -> cheat.addCardToExile();
            case repeatLastAddition -> cheat.repeatLastAddition();
            case castASpell -> cheat.castASpell();
            case exileCardsFromHand -> cheat.exileCardsFromHand();
            case exileCardsFromBattlefield -> cheat.exileCardsFromBattlefield();
            case removeCardsFromGame -> cheat.removeCardsFromGame();
            case addCountersToPermanent -> cheat.addCountersToPermanent();
            case removeCountersFromPermanent -> cheat.removeCountersFromPermanent();
            case tapPermanents -> cheat.tapPermanents();
            case untapPermanents -> cheat.untapPermanents();
            case setPlayerLife -> cheat.setPlayerLife();
            case winGame -> cheat.winGame();
            case riggedPlanarRoll -> cheat.riggedPlanarRoll();
            case planeswalkTo -> cheat.planeswalkTo();
            case setupGameState -> setUp(own.getGame(), controller.getGui(), cheat, dev.text());
            case dumpGameState -> dump(own.getGame(), channel);
        }
        channel.send(new DevState(controller.canPlayUnlimitedLands(), controller.mayLookAtAllCards()));
    }

    /**
     * Placing cards fires no game event, so the whole board is sent here. It is sent from the game thread task that applies
     * the state, because another task could run at the same time and send the old board.
     */
    private static void setUp(final Game game, final IGuiGame gui, final IDevModeCheats cheat, final String text) {
        game.getAction().invoke(() -> {
            cheat.setupGameState(Arrays.asList((text == null ? "" : text).split("\\R")));
            if (gui instanceof RemoteClientGuiGame remote) {
                remote.updateGameView();
            }
        });
    }

    private static void dump(final Game game, final BrowserChannel channel) {
        final GameState state = new GameState();
        state.initFromGame(game);
        channel.send(new DevDump(state.toString()));
    }
}
