package forge.achievement;

import forge.game.Game;
import forge.game.player.GameLossReason;
import forge.game.player.Player;

public class Blackjack extends Achievement {
    private static final int THRESHOLD = 21;

    public Blackjack(int silver0, int gold0) {
        super("Blackjack",
            String.format("Win a game by dealing %d combat damage with your commander.", THRESHOLD), THRESHOLD,
            String.format("Win a game by dealing %d combat damage with your commander.", silver0), silver0,
            String.format("Win a game by dealing %d combat damage with your commander.", gold0), gold0);
    }

    @Override
    protected int evaluate(Player player, Game game) {
        if (player.getOutcome().hasWon()) {
            for (Player p : game.getRegisteredPlayers()) {
                if (p.isOpponentOf(player) && p.getOutcome().lossState == GameLossReason.CommanderDamage) {
                    Integer damage = p.getCommanderDamage().get(player.getCommander());
                    if (damage != null && damage >= THRESHOLD) {
                        return damage;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public String getSubTitle() {
        if (best > 0) {
            return "Best: " + best + " Damage";
        }
        return null;
    }
}
