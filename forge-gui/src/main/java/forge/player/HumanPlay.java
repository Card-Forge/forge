package forge.player;

import forge.game.card.*;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.gui.FThreads;


public class HumanPlay {

    private HumanPlay() {
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final PlayerControllerHuman controller, SpellAbility sa, boolean mayChooseNewTargets) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getHostCard();

        source.setSplitStateToPlayAbility(sa);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        req.playAbility(mayChooseNewTargets, true, false);
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final static boolean playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa) {
        return playSpellAbilityNoStack(controller, player, sa, false);
    }
    public final static boolean playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        return req.playAbility(!useOldTargets, false, true);
    }

}
