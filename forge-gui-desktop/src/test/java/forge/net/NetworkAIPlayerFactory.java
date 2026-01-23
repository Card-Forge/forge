package forge.net;

import forge.deck.Deck;
import forge.gamemodes.match.LobbySlot;
import forge.gamemodes.match.LobbySlotType;

/**
 * Convenience helper for configuring AI players in network test scenarios.
 *
 * Phase 3 of the Automated Network Testing Plan.
 */
public class NetworkAIPlayerFactory {

    /**
     * AI behavior profiles for testing.
     * Currently all profiles use random precons, but the infrastructure
     * allows for profile-specific deck selection in the future.
     */
    public enum AIProfile {
        DEFAULT,
        AGGRESSIVE,
        CONTROL,
        COMBO
    }

    /**
     * Configure a lobby slot as an AI player with the specified name and profile.
     *
     * @param slot The lobby slot to configure
     * @param name The player name
     * @param profile The AI profile (currently for categorization, affects deck selection in future)
     */
    public static void configureAIPlayer(LobbySlot slot, String name, AIProfile profile) {
        slot.setType(LobbySlotType.AI);
        slot.setName(name);
        slot.setDeck(selectDeckForProfile(profile));
        slot.setIsReady(true);
    }

    /**
     * Configure a lobby slot as an AI player with default profile.
     *
     * @param slot The lobby slot to configure
     * @param name The player name
     */
    public static void configureAIPlayer(LobbySlot slot, String name) {
        configureAIPlayer(slot, name, AIProfile.DEFAULT);
    }

    /**
     * Configure a lobby slot as an AI player with a specific deck.
     *
     * @param slot The lobby slot to configure
     * @param name The player name
     * @param deck The deck to use
     */
    public static void configureAIPlayer(LobbySlot slot, String name, Deck deck) {
        slot.setType(LobbySlotType.AI);
        slot.setName(name);
        slot.setDeck(deck);
        slot.setIsReady(true);
    }

    /**
     * Select a deck based on the AI profile.
     * Currently uses random precons for all profiles.
     * Future: map profiles to specific deck archetypes.
     *
     * @param profile The AI profile
     * @return A deck appropriate for the profile
     */
    private static Deck selectDeckForProfile(AIProfile profile) {
        // For now, use random precons for all profiles
        // Future enhancement: filter precons by archetype based on profile
        // - AGGRESSIVE: fast aggro decks
        // - CONTROL: blue-based control decks
        // - COMBO: combo-oriented decks
        return TestDeckLoader.getRandomPrecon();
    }

    /**
     * Get a descriptive string for an AI profile.
     *
     * @param profile The AI profile
     * @return Description string
     */
    public static String getProfileDescription(AIProfile profile) {
        switch (profile) {
            case AGGRESSIVE:
                return "Aggressive AI - prioritizes attacking and damage";
            case CONTROL:
                return "Control AI - prioritizes card advantage and answers";
            case COMBO:
                return "Combo AI - attempts combo wins when possible";
            case DEFAULT:
            default:
                return "Default AI - balanced decision making";
        }
    }
}
