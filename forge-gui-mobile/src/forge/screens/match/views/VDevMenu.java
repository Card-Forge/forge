package forge.screens.match.views;

import forge.game.player.Player;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.model.FModel;
import forge.net.FServer;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.FControl;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.GuiDisplayUtil;

public class VDevMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        addItem(new FMenuItem("Generate Mana", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeGenerateMana();
            }
        }));
        addItem(new FMenuItem("Tutor for Card", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeTutor();
            }
        }));
        addItem(new FMenuItem("Add card to hand", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeCardToHand();
            }
        }));
        addItem(new FMenuItem("Add card to play", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeCardToBattlefield();
            }
        }));
        addItem(new FMenuItem("Set Player Life", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeSetLife();
            }
        }));
        addItem(new FMenuItem("Setup Game State", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devSetupGameState();
            }
        }));

        final ForgePreferences prefs = FModel.getPreferences();
        addItem(new FMenuItem("Play Unlimited Lands", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                boolean unlimitedLands = !prefs.getPrefBoolean(FPref.DEV_UNLIMITED_LAND);

                for (Player p : FControl.getGame().getPlayers()) {
                    if (p.getLobbyPlayer() == FServer.getLobby().getGuiPlayer() ) {
                        p.canCheatPlayUnlimitedLands = unlimitedLands;
                    }
                }
                // probably will need to call a synchronized method to have the game thread see changed value of the variable

                prefs.setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(unlimitedLands));
                prefs.save();
            }
        }));
        addItem(new FMenuItem("Add Counter to Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeAddCounter();
            }
        }));
        addItem(new FMenuItem("Tap Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeTapPerm();
            }
        }));
        addItem(new FMenuItem("Untap Permanent", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeUntapPerm();
            }
        }));
        addItem(new FMenuItem("Rigged planar roll", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModeRiggedPlanarRoll();
            }
        }));
        addItem(new FMenuItem("Planeswalk to", new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiDisplayUtil.devModePlaneswalkTo();
            }
        }));
    }
}
