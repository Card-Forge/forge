package forge.net.client;

import java.util.List;
import java.util.concurrent.TimeoutException;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.match.NextGameDecision;
import forge.net.event.GuiGameEvent;
import forge.util.ITriggerEvent;

public class NetGameController implements IGameController {

    private final IToServer server;
    public NetGameController(final IToServer server) {
        this.server = server;
    }

    private String methodName() {
        boolean passedFirst = false;
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (ste.getClassName() == getClass().getName()) {
                if (passedFirst) {
                    return ste.getMethodName();
                }
                passedFirst = true;
            }
        }
        return null;
    }

    private void send(final String method, final Object... args) {
        server.send(new GuiGameEvent(method, args));
    }
    @SuppressWarnings("unchecked")
    private <T> T sendAndWait(final String method, final Object... args) {
        try {
            return (T) server.sendAndWait(new GuiGameEvent(method, args));
        } catch (final TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean useMana(final byte color) {
        return sendAndWait(methodName(), color);
    }

    @Override
    public boolean tryUndoLastAction() {
        return sendAndWait(methodName());
    }

    @Override
    public void selectPlayer(final PlayerView playerView, final ITriggerEvent triggerEvent) {
        send(methodName(), playerView, triggerEvent);
    }

    @Override
    public boolean selectCard(final CardView cardView, final List<CardView> otherCardViewsToSelect, final ITriggerEvent triggerEvent) {
        return sendAndWait(methodName(), cardView, otherCardViewsToSelect, triggerEvent);
    }

    @Override
    public void selectButtonOk() {
        send(methodName());
    }

    @Override
    public void selectButtonCancel() {
        send(methodName());
    }

    @Override
    public void selectAbility(final SpellAbilityView sa) {
        send(methodName(), sa);
    }

    @Override
    public boolean passPriorityUntilEndOfTurn() {
        return sendAndWait(methodName());
    }

    @Override
    public boolean passPriority() {
        return sendAndWait(methodName());
    }

    @Override
    public void nextGameDecision(final NextGameDecision decision) {
        send(methodName(), decision);
    }

    @Override
    public boolean mayLookAtAllCards() {
        // Don't do this over network
        return false;
    }

    public String getActivateDescription(final CardView card) {
        return sendAndWait(methodName(), card);
    }

    @Override
    public void concede() {
        send(methodName());
    }

    @Override
    public IDevModeCheats cheat() {
        return IDevModeCheats.NO_CHEAT;
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        return false;
    }

    @Override
    public void alphaStrike() {
        send(methodName());
    }

    @Override
    public void reorderHand(CardView card, int index) {
        send(methodName(), card, index);
    }
}
