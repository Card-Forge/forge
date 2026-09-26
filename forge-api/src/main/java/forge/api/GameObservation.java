package forge.api;

import com.google.common.eventbus.Subscribe;
import forge.game.Game;
import forge.game.event.GameEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pollable invalidation hook. It never exports raw events (which can contain hidden cards),
 * nor calls client code on the rules thread. Use a stable UI boundary to capture a snapshot.
 */
public final class GameObservation implements AutoCloseable {
    private final Game game;
    private final AtomicLong revision = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    public GameObservation(Game game) {
        this.game = Objects.requireNonNull(game);
        game.subscribeToEvents(this);
    }

    public long revision() { return revision.get(); }

    @Subscribe
    public void onGameEvent(GameEvent event) {
        if (!closed.get()) { revision.incrementAndGet(); }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) { game.unsubscribeFromEvents(this); }
    }
}
