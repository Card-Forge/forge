package forge.web.match;

import forge.LobbyPlayer;
import forge.game.event.GameEvent;
import forge.game.event.GameEventFlipCoin;
import forge.game.player.PlayerView;
import forge.sound.EventVisualizer;
import forge.sound.SoundEffectType;
import forge.web.protocol.ToBrowser.Sound;

import java.util.function.Supplier;

/** Picks the sound a game event makes. Only the sound's name is sent, and the browser plays it. Two of the sounds depend on whose view this is, and the opening coin toss sends none. */
final class BrowserSounds {
    private final EventVisualizer sounds;

    BrowserSounds(final Supplier<PlayerView> currentPlayer) {
        // The visualizer finds the viewer by lobby name. The viewer is read on each event, because the seat is known only once the game opens
        final LobbyPlayer viewer = new LobbyPlayer("") {
            @Override
            public String getName() {
                final PlayerView local = currentPlayer.get();
                return local == null ? "" : local.getLobbyPlayerName();
            }

            @Override
            public void hear(final LobbyPlayer player, final String message) {
            }
        };
        sounds = new EventVisualizer(viewer) {
            // The page plays the opening toss's sound itself, in time with the coin (coin.ts)
            @Override
            public SoundEffectType visit(final GameEventFlipCoin event) {
                return event.startingToss() ? null : super.visit(event);
            }
        };
    }

    /** The sound this event makes, or null if it makes none. */
    Sound soundFor(final GameEvent event) {
        final SoundEffectType effect = event.visit(sounds);
        if (effect == null) {
            return null;
        }
        final String name = effect == SoundEffectType.ScriptedEffect
                ? sounds.getScriptedSoundEffectName(event) : effect.getResourceFileName();
        return name == null || name.isEmpty() ? null : new Sound(name, effect.isSynced());
    }
}
