package forge.match.input;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.interfaces.IGuiBase;
import forge.util.ITriggerEvent;
import forge.view.LocalGameView;
import forge.view.PlayerView;

public class InputNone implements Input {
    private final IGuiBase gui;
    private final PlayerView owner;

    public InputNone(LocalGameView gameView) {
        gui = gameView.getGui();
        owner = gameView.getLocalPlayerView();
    }

    @Override
    public IGuiBase getGui() {
        return gui;
    }

    @Override
    public PlayerView getOwner() {
        return owner;
    }

    @Override
    public void showMessageInitial() {
    }

    @Override
    public boolean selectCard(Card card, ITriggerEvent triggerEvent) {
        return false;
    }

    @Override
    public void selectAbility(SpellAbility ab) {
    }

    @Override
    public void selectPlayer(Player player, ITriggerEvent triggerEvent) {
    }

    @Override
    public void selectButtonOK() {
    }

    @Override
    public void selectButtonCancel() {
    }
}
