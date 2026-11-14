/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.limited;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.Aggregates;

/**
 * Starts matches between a saved limited deck and the AI decks drafted or built alongside it,
 * either as a single match or as a gauntlet of rounds.
 */
public class LimitedMatch {
    private HostedMatch hostedMatch = null;
    private int rounds;
    private Deck humanDeck;
    private int currentRound;
    private int wins;
    private int losses;
    private boolean gauntlet;
    private GameType gameType;
    private final List<Deck> aiOpponents = new ArrayList<>();

    public LimitedMatch() {
        currentRound = 1;
        wins = 0;
        losses = 0;
    }

    /**
     * Resets the tournament.
     */
    public void resetCurrentRound() {
        wins = 0;
        losses = 0;
        currentRound = 1;
    }

    /**
     * Advances the tournament to the next round.
     */
    public void nextRound() {
        System.out.println("Moving from round " + currentRound + " to round " +  (currentRound + 1) + " of " + rounds);
        if (hostedMatch == null) {
            throw new IllegalStateException("Cannot advance round when no match has been hosted.");
        }

        if (currentRound >= rounds) {
            currentRound = rounds - 1;
            return;
        }

        currentRound++;
        hostedMatch.endCurrentGame();
        startRound();
    }

    /**
     * @param mode Draft, Sealed or Winston
     * @return the AI decks saved with {@code humanDeck0}, or an empty list for an event deck that has none
     */
    public List<Deck> getAiDecks(final GameType mode, final Deck humanDeck0) {
        final DeckGroup group = switch (mode) {
            case Draft -> FModel.getDecks().getDraft().get(humanDeck0.getName());
            case Sealed -> FModel.getDecks().getSealed().get(humanDeck0.getName());
            case Winston -> FModel.getDecks().getWinston().get(humanDeck0.getName());
            default -> throw new IllegalArgumentException("No saved AI decks for " + mode);
        };
        return group == null ? Collections.emptyList() : group.getAiDecks();
    }

    /**
     * @return the reason {@code deck} cannot be played in {@code mode}, or null when it can or legality is not enforced
     */
    public String getDeckProblem(final GameType mode, final Deck deck) {
        if (!FModel.getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            return null;
        }
        return resolveGameType(mode, deck).getDeckFormat().getDeckConformanceProblem(deck);
    }

    public void startMatch(final GameType mode, final Deck humanDeck0, final List<Deck> opponents) {
        humanDeck = humanDeck0;
        gameType = resolveGameType(mode, humanDeck0);
        gauntlet = false;
        host(opponents);
    }

    /**
     * Plays the AI decks saved with {@code humanDeck0} in order, one per round.
     * A single round is played against a random AI deck.
     */
    public void launchGauntlet(final GameType mode, final Deck humanDeck0, final int rounds0) {
        final List<Deck> aiDecks = getAiDecks(mode, humanDeck0);
        humanDeck = humanDeck0;
        gameType = resolveGameType(mode, humanDeck0);
        gauntlet = true;
        aiOpponents.clear();

        if (rounds0 == 1) {
            rounds = 1;
            aiOpponents.add(aiDecks.get(Aggregates.randomInt(0, aiDecks.size() - 1)));
        } else {
            rounds = Math.min(rounds0, aiDecks.size());
            aiOpponents.addAll(aiDecks.subList(0, rounds));
        }

        resetCurrentRound();
        startRound();
    }

    private void startRound() {
        host(Collections.singletonList(aiOpponents.get(currentRound - 1)));
    }

    private void host(final List<Deck> opponents) {
        final List<RegisteredPlayer> players = new ArrayList<>();
        final RegisteredPlayer human = register(humanDeck).setPlayer(GamePlayerUtil.getGuiPlayer());
        players.add(human);
        for (final Deck aiDeck : opponents) {
            players.add(register(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer()));
        }

        hostedMatch = GuiBase.getInterface().hostMatch();
        hostedMatch.startMatch(gameType, gameType == GameType.CommanderDraft ? EnumSet.of(GameType.Commander) : null,
                players, human, GuiBase.getInterface().getNewGuiGame());
    }

    private RegisteredPlayer register(final Deck deck) {
        final RegisteredPlayer player = gameType == GameType.CommanderDraft
                ? RegisteredPlayer.forCommander(deck) : new RegisteredPlayer(deck);
        player.assignConspiracies();
        return player;
    }

    private static GameType resolveGameType(final GameType mode, final Deck deck) {
        return mode == GameType.Draft && CommanderDraftUtil.isCommanderDraftDeck(deck) ? GameType.CommanderDraft : mode;
    }

    /**
     * Returns the total number of rounds in the tournament.
     * @return int, number of rounds in the tournament
     */
    public final int getRounds() {
        return rounds;
    }

    /**
     * Returns the number of the current round in the tournament.
     * @return int, number of rounds in the tournament
     */
    public final int getCurrentRound() {
        return currentRound;
    }

    /**
     * Adds a game win to the tournament statistics.
     */
    public void addWin() {
        wins++;
    }

    /**
     * Adds a game loss to the tournament statistics.
     */
    public void addLoss() {
        losses++;
    }

    /**
     * The total number of won games in this tournament.
     * @return int, number of wins
     */
    public final int getWins() {
        return wins;
    }

    /**
     * The total number of lost games in this tournament.
     * @return int, numer of losses
     */
    public final int getLosses() {
        return losses;
    }

    /**
     * @return true when {@code game} is a round of the gauntlet this class is running
     */
    public final boolean isGauntletRound(final GameView game) {
        return gauntlet && hostedMatch != null && hostedMatch.getGameView() == game;
    }

}
