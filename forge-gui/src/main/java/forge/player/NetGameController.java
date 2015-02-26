package forge.player;

import java.util.Collections;
import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.interfaces.IDevModeCheats;
import forge.interfaces.IGameController;
import forge.match.NextGameDecision;
import forge.net.game.client.IToServer;
import forge.trackable.TrackableObject;
import forge.util.ITriggerEvent;

public class NetGameController implements IGameController {

    private final IToServer server;
    public NetGameController(final IToServer server) {
        this.server = server;
    }

    private void send(final String method) {
        send(method, Collections.<TrackableObject>emptySet());
    }
    private void send(final String method, final TrackableObject object) {
        send(method, Collections.singleton(object));
    }
    private void send(final String method, final Iterable<? extends TrackableObject> objects) {
        //server.send(new (method, objects));
    }

    @Override
    public boolean mayLookAtAllCards() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canPlayUnlimitedLands() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void concede() {
        send("concede");
    }

    @Override
    public void alphaStrike() {
        send("alphaStrike");
    }

    @Override
    public boolean useMana(byte color) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void selectButtonOk() {
        // TODO Auto-generated method stub

    }

    @Override
    public void selectButtonCancel() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean passPriority() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean passPriorityUntilEndOfTurn() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void selectPlayer(PlayerView playerView, ITriggerEvent triggerEvent) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean selectCard(CardView cardView,
            List<CardView> otherCardViewsToSelect, ITriggerEvent triggerEvent) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void selectAbility(SpellAbility sa) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean tryUndoLastAction() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IDevModeCheats cheat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void nextGameDecision(NextGameDecision decision) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getActivateDescription(CardView card) {
        // TODO Auto-generated method stub
        return null;
    }

}
