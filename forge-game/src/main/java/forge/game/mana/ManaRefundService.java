package forge.game.mana;

import com.google.common.collect.Lists;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventZone;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Collections;
import java.util.List;

public class ManaRefundService {

    private final SpellAbility sa;
    private final Player activator;
    public ManaRefundService(SpellAbility sa) {
        this.sa = sa;
        this.activator = sa.getActivatingPlayer();
    }

    public void refundManaPaid() {
        List<Player> payers = Lists.newArrayList();

        // move non-undoable paying mana back to floating
        for (Mana mana : sa.getPayingMana()) {
            Player pl = mana.getManaAbility().getSourceSA().getActivatingPlayer();

            pl.getManaPool().addMana(mana);
            if (!payers.contains(pl)) {
                payers.add(pl);
            }
        }

        sa.getPayingMana().clear();

        List<SpellAbility> payingAbilities = sa.getPayingManaAbilities();

        // start with the most recent
        Collections.reverse(payingAbilities);

        for (final SpellAbility am : payingAbilities) {
            // What if am is owned by a different player?
            am.undo();
        }

        for (final SpellAbility am : payingAbilities) {
            // Recursively refund abilities that were used.
            ManaRefundService refundService = new ManaRefundService(am);
            refundService.refundManaPaid();
            activator.getGame().getStack().clearUndoStack(am);
        }

        payingAbilities.clear();

        // update battlefield of all activating players - to redraw cards used to pay mana as untapped
        for (Player p : payers) {
            p.getGame().fireEvent(new GameEventZone(ZoneType.Battlefield, p, EventValueChangeType.ComplexUpdate, null));
        }
    }
}
