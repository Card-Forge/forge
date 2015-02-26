package forge.net;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.player.RegisteredPlayer;
import forge.interfaces.IGuiGame;
import forge.match.HostedMatch;
import forge.match.NetGuiGame;
import forge.net.game.server.RemoteClient;
import forge.player.LobbyPlayerHuman;

public final class NetGame {

    private final Map<RemoteClient, NetPlayer> clients = Maps.newHashMap();
    private final GameRules rules;
    private final HostedMatch match = new HostedMatch();
    public NetGame(final GameRules rules) {
        this.rules = rules;
    }

    public void addClient(final RemoteClient client) {
        clients.put(client, new NetPlayer(client, new NetGuiGame(client)));
    }

    public void startMatch() {
        final List<RegisteredPlayer> registeredPlayers = Lists.newArrayListWithCapacity(clients.size());
        final Map<RegisteredPlayer, IGuiGame> guis = Maps.newHashMap();
        for (final NetPlayer np : clients.values()) {
            if (np.player == null) {
                System.err.println("No deck registered for player " + np.client.getUsername());
                return;
            }
            registeredPlayers.add(np.player);
            guis.put(np.player, np.gui);
        }
        
        // DEBUG
        if (registeredPlayers.size() == 1) {
            RegisteredPlayer r = new RegisteredPlayer(new Deck());
            registeredPlayers.add(r);
            r.setPlayer(new LobbyPlayerAi("AI", new HashMap<String, String>()));
        }
        match.startMatch(rules, null, registeredPlayers, guis);
    }

    public void registerDeck(final RemoteClient client, final Deck deck) {
        final RegisteredPlayer r = new RegisteredPlayer(deck);
        clients.get(client).player = r;
        r.setPlayer(new LobbyPlayerHuman(client.getUsername()));
    }

    private static final class NetPlayer {
        private final RemoteClient client;
        private RegisteredPlayer player = null;
        private final IGuiGame gui;
        private NetPlayer(final RemoteClient client, final IGuiGame gui) {
            this.client = client;
            this.gui = gui;
        }
    }
}
