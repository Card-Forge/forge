package forge.view;

import forge.game.Game;
import forge.game.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Multiplicity-aware semantic outcome for simulator reporting. */
public final class SimulationOutcomeClassification {
    public enum Kind {
        DRAW,
        SINGLE_WINNER,
        MULTIPLE_WINNERS,
        NO_WINNER,
        INVALID_MULTIPLE_WINNERS,
        INVALID_NO_WINNER
    }

    private final Kind kind;
    private final List<Integer> winnerSeats;
    private final String winnerName;

    private SimulationOutcomeClassification(final Kind kind, final List<Integer> winnerSeats,
            final String winnerName) {
        this.kind = kind;
        this.winnerSeats = List.copyOf(winnerSeats);
        this.winnerName = winnerName;
    }

    public static SimulationOutcomeClassification classify(final Game game) {
        if (game.getOutcome().isDraw()) {
            return new SimulationOutcomeClassification(Kind.DRAW, List.of(), null);
        }
        final List<Player> players = new ArrayList<>(game.getRegisteredPlayers());
        final List<Player> winners = players.stream()
                .filter(player -> player.getOutcome() != null && player.getOutcome().hasWon())
                .sorted(Comparator.comparingInt(Player::getId)).toList();
        if (winners.size() == 1) {
            final Player winner = winners.get(0);
            return new SimulationOutcomeClassification(Kind.SINGLE_WINNER,
                    List.of(winner.getId()), winner.getName());
        }
        final boolean ordinaryTwoPlayerNonTeam = players.size() == 2
                && players.get(0).getTeam() != players.get(1).getTeam();
        if (winners.isEmpty()) {
            return new SimulationOutcomeClassification(ordinaryTwoPlayerNonTeam
                    ? Kind.INVALID_NO_WINNER : Kind.NO_WINNER, List.of(), null);
        }
        final List<Integer> winnerSeats = winners.stream().map(Player::getId).toList();
        return new SimulationOutcomeClassification(ordinaryTwoPlayerNonTeam
                ? Kind.INVALID_MULTIPLE_WINNERS : Kind.MULTIPLE_WINNERS, winnerSeats, null);
    }

    public Kind getKind() {
        return kind;
    }

    public List<Integer> getWinnerSeats() {
        return winnerSeats;
    }

    public Optional<String> getWinnerName() {
        return Optional.ofNullable(winnerName);
    }

    public String canonical() {
        final String seats = winnerSeats.toString().replace(" ", "");
        return switch (kind) {
        case DRAW -> "DRAW";
        case SINGLE_WINNER -> "WINNER " + winnerName;
        case MULTIPLE_WINNERS -> "MULTIPLE_WINNERS " + seats;
        case NO_WINNER -> "NO_WINNER";
        case INVALID_MULTIPLE_WINNERS -> "INVALID_OUTCOME MULTIPLE_WINNERS " + seats;
        case INVALID_NO_WINNER -> "INVALID_OUTCOME NO_WINNER";
        };
    }
}
