package forge.gamemodes.net.server;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.StackItemView;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages delta synchronization between server and clients.
 * Tracks changes to TrackableObjects and builds minimal delta packets.
 */
public class DeltaSyncManager {
    // How often to include a checksum for validation (every N packets)
    private static final int CHECKSUM_INTERVAL = 10;

    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private final Map<Integer, Long> clientAcknowledgedSeq = new ConcurrentHashMap<>();
    private final Set<Integer> trackedObjectIds = ConcurrentHashMap.newKeySet();
    private final Set<Integer> removedObjectIds = new HashSet<>();

    private long packetsSinceLastChecksum = 0;

    /**
     * Register a client for tracking.
     * @param clientIndex the client's player index
     */
    public void registerClient(int clientIndex) {
        clientAcknowledgedSeq.put(clientIndex, 0L);
    }

    /**
     * Unregister a client.
     * @param clientIndex the client's player index
     */
    public void unregisterClient(int clientIndex) {
        clientAcknowledgedSeq.remove(clientIndex);
    }

    /**
     * Process an acknowledgment from a client.
     * @param clientIndex the client's player index
     * @param acknowledgedSeq the sequence number being acknowledged
     */
    public void processAcknowledgment(int clientIndex, long acknowledgedSeq) {
        clientAcknowledgedSeq.compute(clientIndex, (k, v) -> {
            if (v == null) return acknowledgedSeq;
            return Math.max(v, acknowledgedSeq);
        });

        // Check if all clients have acknowledged - if so, we can clear changes
        long minAcked = getMinAcknowledgedSequence();
        if (minAcked > 0) {
            // All clients have caught up, changes for older sequences can be cleared
        }
    }

    /**
     * Get the minimum acknowledged sequence across all clients.
     * @return minimum acknowledged sequence number
     */
    public long getMinAcknowledgedSequence() {
        return clientAcknowledgedSeq.values().stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
    }

    /**
     * Collect all changes from the GameView hierarchy and build a delta packet.
     * @param gameView the game view to collect changes from
     * @return a DeltaPacket containing all changes, or null if no changes
     */
    public DeltaPacket collectDeltas(GameView gameView) {
        if (gameView == null) {
            return null;
        }

        Map<Integer, byte[]> objectDeltas = new HashMap<>();
        Set<Integer> currentObjectIds = new HashSet<>();

        // Collect changes from GameView itself
        collectObjectDelta(gameView, objectDeltas, currentObjectIds);

        // Collect changes from all players
        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                collectPlayerDeltas(player, objectDeltas, currentObjectIds);
            }
        }

        // Collect changes from the stack
        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                collectObjectDelta(stackItem, objectDeltas, currentObjectIds);
            }
        }

        // Collect changes from combat
        if (gameView.getCombat() != null) {
            collectObjectDelta(gameView.getCombat(), objectDeltas, currentObjectIds);
        }

        // Detect removed objects
        Set<Integer> nowRemoved = new HashSet<>(trackedObjectIds);
        nowRemoved.removeAll(currentObjectIds);
        removedObjectIds.addAll(nowRemoved);

        // Update tracked objects
        trackedObjectIds.clear();
        trackedObjectIds.addAll(currentObjectIds);

        // Build the packet
        long seq = sequenceNumber.incrementAndGet();
        packetsSinceLastChecksum++;

        // Include checksum periodically
        int checksum = 0;
        if (packetsSinceLastChecksum >= CHECKSUM_INTERVAL) {
            checksum = computeStateChecksum(gameView);
            packetsSinceLastChecksum = 0;
        }

        DeltaPacket packet = new DeltaPacket(seq, objectDeltas, new HashSet<>(removedObjectIds), checksum);

        // Clear removedObjectIds after including in packet
        removedObjectIds.clear();

        return packet;
    }

    /**
     * Collect delta for a single TrackableObject.
     */
    private void collectObjectDelta(TrackableObject obj, Map<Integer, byte[]> objectDeltas,
                                    Set<Integer> currentObjectIds) {
        if (obj == null) {
            return;
        }

        currentObjectIds.add(obj.getId());

        if (obj.hasChanges()) {
            byte[] serialized = serializeChanges(obj);
            if (serialized != null && serialized.length > 0) {
                objectDeltas.put(obj.getId(), serialized);
            }
        }
    }

    /**
     * Collect deltas from a PlayerView and all its associated objects.
     */
    private void collectPlayerDeltas(PlayerView player, Map<Integer, byte[]> objectDeltas,
                                     Set<Integer> currentObjectIds) {
        if (player == null) {
            return;
        }

        collectObjectDelta(player, objectDeltas, currentObjectIds);

        // Collect from player's cards in various zones
        collectCardsFromZone(player.getHand(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getGraveyard(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getLibrary(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getExile(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getFlashback(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getCommanders(), objectDeltas, currentObjectIds);
        collectCardsFromZone(player.getAnte(), objectDeltas, currentObjectIds);

        // Collect battlefield cards
        if (player.getBattlefield() != null) {
            for (CardView card : player.getBattlefield()) {
                collectCardDelta(card, objectDeltas, currentObjectIds);
            }
        }
    }

    /**
     * Collect deltas from a zone's cards.
     */
    private void collectCardsFromZone(Iterable<CardView> cards, Map<Integer, byte[]> objectDeltas,
                                      Set<Integer> currentObjectIds) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            collectCardDelta(card, objectDeltas, currentObjectIds);
        }
    }

    /**
     * Collect delta from a card and its associated objects.
     */
    private void collectCardDelta(CardView card, Map<Integer, byte[]> objectDeltas,
                                  Set<Integer> currentObjectIds) {
        if (card == null) {
            return;
        }

        collectObjectDelta(card, objectDeltas, currentObjectIds);

        // Collect from attached cards
        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                collectCardDelta(attached, objectDeltas, currentObjectIds);
            }
        }
    }

    /**
     * Serialize the changed properties of an object.
     * Uses Java serialization to create a byte array containing:
     * - The number of changed properties
     * - For each changed property: the property ordinal and its value
     */
    private byte[] serializeChanges(TrackableObject obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            Set<TrackableProperty> changedProps = obj.getChangedProps();
            oos.writeInt(changedProps.size());

            // Get the props map to read values
            Map<TrackableProperty, Object> props = obj.getProps();

            for (TrackableProperty prop : changedProps) {
                oos.writeInt(prop.ordinal());
                Object value = props.get(prop);
                // Write the value - it should be Serializable
                oos.writeObject(value);
            }

            oos.flush();
            oos.close();
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Error serializing delta for object " + obj.getId() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Compute a checksum for the current game state.
     */
    private int computeStateChecksum(GameView gameView) {
        int hash = 17;
        hash = 31 * hash + gameView.getId();
        hash = 31 * hash + gameView.getTurn();
        if (gameView.getPhase() != null) {
            hash = 31 * hash + gameView.getPhase().hashCode();
        }
        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                hash = 31 * hash + player.getId();
                hash = 31 * hash + player.getLife();
            }
        }
        return hash;
    }

    /**
     * Create a full state packet for initial sync or reconnection.
     * @param gameView the complete game view
     * @return a FullStatePacket
     */
    public FullStatePacket createFullStatePacket(GameView gameView) {
        long seq = sequenceNumber.get();
        return new FullStatePacket(seq, gameView);
    }

    /**
     * Create a full state packet for reconnection with session info.
     * @param gameView the complete game view
     * @param sessionId the session identifier
     * @param sessionToken the session token
     * @return a FullStatePacket
     */
    public FullStatePacket createFullStatePacketForReconnect(GameView gameView, String sessionId, String sessionToken) {
        long seq = sequenceNumber.get();
        return new FullStatePacket(seq, gameView, sessionId, sessionToken);
    }

    /**
     * Clear all tracked changes for all objects.
     * Should be called after all clients have acknowledged.
     * @param gameView the game view to clear changes from
     */
    public void clearAllChanges(GameView gameView) {
        if (gameView == null) {
            return;
        }

        gameView.clearChanges();

        if (gameView.getPlayers() != null) {
            for (PlayerView player : gameView.getPlayers()) {
                clearPlayerChanges(player);
            }
        }

        if (gameView.getStack() != null) {
            for (StackItemView stackItem : gameView.getStack()) {
                stackItem.clearChanges();
            }
        }

        if (gameView.getCombat() != null) {
            gameView.getCombat().clearChanges();
        }
    }

    /**
     * Clear changes for a player and all associated objects.
     */
    private void clearPlayerChanges(PlayerView player) {
        if (player == null) {
            return;
        }

        player.clearChanges();

        clearCardsChanges(player.getHand());
        clearCardsChanges(player.getGraveyard());
        clearCardsChanges(player.getLibrary());
        clearCardsChanges(player.getExile());
        clearCardsChanges(player.getFlashback());
        clearCardsChanges(player.getCommanders());
        clearCardsChanges(player.getAnte());

        if (player.getBattlefield() != null) {
            for (CardView card : player.getBattlefield()) {
                clearCardChanges(card);
            }
        }
    }

    /**
     * Clear changes for cards in a zone.
     */
    private void clearCardsChanges(Iterable<CardView> cards) {
        if (cards == null) {
            return;
        }
        for (CardView card : cards) {
            clearCardChanges(card);
        }
    }

    /**
     * Clear changes for a card and its attached cards.
     */
    private void clearCardChanges(CardView card) {
        if (card == null) {
            return;
        }

        card.clearChanges();

        if (card.getAttachedCards() != null) {
            for (CardView attached : card.getAttachedCards()) {
                clearCardChanges(attached);
            }
        }
    }

    /**
     * Get the current sequence number.
     * @return current sequence number
     */
    public long getCurrentSequence() {
        return sequenceNumber.get();
    }

    /**
     * Check if a client needs a full resync.
     * @param clientIndex the client's player index
     * @return true if the client has fallen too far behind
     */
    public boolean needsFullResync(int clientIndex) {
        Long acked = clientAcknowledgedSeq.get(clientIndex);
        if (acked == null) {
            return true;
        }
        // If client is more than 100 packets behind, force full resync
        return (sequenceNumber.get() - acked) > 100;
    }
}
