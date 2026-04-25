package forge.game.phase;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventPlayerPriority;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.util.IHasForgeLog;
import forge.util.TextUtil;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;

/**
 * Standard implementation of {@link IPriorityManager}.
 *
 * <p>All priority-related state ({@code pPlayerPriority}, {@code pFirstPriority},
 * {@code givePriorityToPlayer}) has been extracted from
 * {@link PhaseHandler} into this class.  The full priority window + rotation
 * logic that previously lived in {@code PhaseHandler.mainLoopStep()} now lives
 * in {@link #conductStep()}.</p>
 *
 * <h3>How the different player types are handled</h3>
 * <ul>
 *   <li><b>Human</b> – {@link forge.game.player.PlayerController#chooseSpellAbilityToPlay()}
 *       blocks on a UI {@code InputPassPriority} prompt managed by
 *       {@code PlayerControllerHuman} and the {@code InputQueue}.</li>
 *   <li><b>AI</b> – returns synchronously from
 *       {@link forge.ai.PlayerControllerAi#chooseSpellAbilityToPlay()}.</li>
 *   <li><b>Network</b> – blocks until a remote response arrives via the
 *       net-play input pipeline.</li>
 * </ul>
 * <p>The rotation ring implemented here is identical for all player types;
 * only the mechanism by which a player makes a decision differs.</p>
 */
public class PlayerPriority implements IPriorityManager, IHasForgeLog {

    // -----------------------------------------------------------------------
    // Private state (previously fields of PhaseHandler)
    // -----------------------------------------------------------------------

    /** The player who currently holds priority. */
    private transient Player pPlayerPriority = null;

    /**
     * The player who <em>first</em> received priority in this passing round.
     * When priority rotates back to this player we know all players have
     * passed (CR 117.4).
     */
    private transient Player pFirstPriority = null;

    /**
     * Whether the engine should offer the current priority holder a chance to
     * act.  Set to {@code false} during phases that do not naturally grant
     * priority (e.g. Untap, Cleanup rule 514.3).
     */
    private boolean givePriorityToPlayer = false;

    // -----------------------------------------------------------------------
    // Back-references
    // -----------------------------------------------------------------------

    private final Game game;

    /**
     * Back-reference to the owning {@link PhaseHandler}.  Used to read
     * {@code playerTurn}, {@code phase}, {@code inCombat()}, and to call
     * {@code checkStateBasedEffects()} (package-private).
     */
    private final PhaseHandler phaseHandler;

    // -----------------------------------------------------------------------
    // Debug helpers (mirrored from PhaseHandler)
    // -----------------------------------------------------------------------

    private static final boolean DEBUG_PHASES = false;
    private final StopWatch sw = new StopWatch();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    PlayerPriority(final PhaseHandler phaseHandler) {
        this.phaseHandler = phaseHandler;
        this.game = phaseHandler.getGame();
    }

    // -----------------------------------------------------------------------
    // IPriorityManager implementation
    // -----------------------------------------------------------------------

    @Override
    public Player getPriorityPlayer() {
        return pPlayerPriority;
    }

    @Override
    public void setPriority(final Player player) {
        pFirstPriority  = player;
        pPlayerPriority = player;
    }

    @Override
    public void resetPriority() {
        setPriority(phaseHandler.getPlayerTurn());
    }

    @Override
    public boolean isGivingPriority() {
        return givePriorityToPlayer;
    }

    @Override
    public void setGivingPriority(final boolean give) {
        givePriorityToPlayer = give;
    }

    // -----------------------------------------------------------------------
    // Core priority step
    // -----------------------------------------------------------------------

    /**
     * Runs one complete priority window followed by rotation.
     *
     * <ol>
     *   <li>If {@link #isGivingPriority()} is {@code true}: fire the priority
     *       event, then loop asking the current priority holder to act until
     *       they pass or the game ends.</li>
     *   <li>Rotate priority to the next player in turn order.</li>
     *   <li>If the next player is the round anchor ({@code pFirstPriority})
     *       all players have passed → return {@link PriorityResult#ALL_PASSED}
     *       so the caller can end the phase or resolve the stack.</li>
     * </ol>
     *
     * @return a {@link PriorityResult} the caller uses to drive phase/stack logic
     */
    @Override
    public PriorityResult conductStep() {

        // ── Priority window ──────────────────────────────────────────────
        if (givePriorityToPlayer) {
            if (DEBUG_PHASES) {
                sw.start();
            }

            game.fireEvent(new GameEventPlayerPriority(
                    PlayerView.get(phaseHandler.getPlayerTurn()),
                    phaseHandler.getPhase(),
                    PlayerView.get(pPlayerPriority)));

            List<SpellAbility> lastChosenSa = null;
            int loopCount = 0;

            do {
                // CR 704: check state-based effects before each priority offer
                if (phaseHandler.checkStateBasedEffects()) {
                    return PriorityResult.GAME_OVER;
                }
                game.stashGameState();

                final List<SpellAbility> chosenSa =
                        pPlayerPriority.getController().chooseSpellAbilityToPlay();
                lastChosenSa = chosenSa;

                // If the active player has conceded while holding priority,
                // reassign both priority pointers to the next living player.
                final Player playerTurn = phaseHandler.getPlayerTurn();
                if (playerTurn.hasLost()
                        && pPlayerPriority.equals(playerTurn)
                        && pFirstPriority.equals(playerTurn)) {
                    System.out.println("Active player is no longer in the game...");
                    pPlayerPriority = game.getNextPlayerAfter(pPlayerPriority);
                    pFirstPriority  = pPlayerPriority;
                }

                if (chosenSa == null) {
                    break; // player passed
                }

                if (DEBUG_PHASES) {
                    System.out.print("... " + pPlayerPriority + " plays " + chosenSa);
                }

                boolean rollback = false;
                for (final SpellAbility sa : chosenSa) {
                    final Card saHost = sa.getHostCard();
                    final Zone originZone = saHost.getZone();
                    final CardZoneTable triggerList = new CardZoneTable(
                            game.getLastStateBattlefield(),
                            game.getLastStateGraveyard());

                    if (pPlayerPriority.getController().playChosenSpellAbility(sa)) {
                        // CR 117.3c – after playing, the acting player gets
                        // priority again; opponents must all pass before stack resolves.
                        pFirstPriority = pPlayerPriority;
                    } else if (game.EXPERIMENTAL_RESTORE_SNAPSHOT) {
                        rollback = true;
                    }

                    final Card saHostNow = game.getCardState(saHost);
                    final Zone currentZone = saHostNow.getZone();

                    // Fire zone-change triggers if the card moved during casting.
                    if (currentZone != null && originZone != null
                            && !currentZone.equals(originZone)
                            && (sa.isSpell() || sa.isLandAbility())) {
                        triggerList.put(originZone.getZoneType(),
                                currentZone.getZoneType(), saHostNow);
                        triggerList.triggerChangesZoneAll(game, sa);
                    }
                }

                if (!rollback) {
                    game.copyLastState();
                }
                loopCount++;
            } while (loopCount < 999 || !pPlayerPriority.getController().isAI());

            if (loopCount >= 999 && pPlayerPriority.getController().isAI()) {
                aiLog.warn("AI looped too much with: {}", lastChosenSa);
            }

            if (DEBUG_PHASES) {
                sw.stop();
                System.out.printf("... passed in %.3f s%n", sw.getTime() / 1000f);
                System.out.println("\t\tStack: " + game.getStack());
                sw.reset();
            }

        } else if (DEBUG_PHASES) {
            System.out.print(" >> (no priority given to " + pPlayerPriority + ")\n");
        }

        // ── Priority rotation ─────────────────────────────────────────────
        final Player nextPlayer = game.getNextPlayerAfter(pPlayerPriority);

        if (game.isGameOver() || nextPlayer == null) {
            return PriorityResult.GAME_OVER;
        }

        if (DEBUG_PHASES) {
            final Player playerTurn = phaseHandler.getPlayerTurn();
            System.out.println(TextUtil.concatWithSpace(
                    playerTurn.toString(),
                    TextUtil.addSuffix(phaseHandler.getPhase().toString(), ":"),
                    pPlayerPriority.toString(),
                    "is active, previous was",
                    nextPlayer.toString()));
        }

        if (pFirstPriority == nextPlayer) {
            // ── Round complete: all players have passed ───────────────────
            // Reset priority back to the active turn player (or the next
            // living player if the active player has lost).
            if (phaseHandler.getPlayerTurn().hasLost()) {
                setPriority(game.getNextPlayerAfter(phaseHandler.getPlayerTurn()));
            } else {
                setPriority(phaseHandler.getPlayerTurn());
            }
            givePriorityToPlayer = true;

            updatePriorityView();
            return PriorityResult.ALL_PASSED;
        } else {
            // Pass priority to the next player and continue the loop.
            pPlayerPriority = nextPlayer;
            updatePriorityView();
            return PriorityResult.ACTION_TAKEN;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Pushes the current priority holder to each player's view. */
    private void updatePriorityView() {
        for (final Player p : game.getPlayers()) {
            p.setHasPriority(pPlayerPriority == p);
        }
    }
}

