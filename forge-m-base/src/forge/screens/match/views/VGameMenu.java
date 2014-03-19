package forge.screens.match.views;

import java.util.ArrayList;

import forge.Forge;
import forge.screens.match.FControl;
import forge.screens.match.views.VHeader.HeaderDropDown;
import forge.toolbox.FButton;

public class VGameMenu extends HeaderDropDown {
    private static final float INSETS_FACTOR = 0.025f;
    private static final float GAP_Y_FACTOR = 0.01f;

    private final ArrayList<FButton> buttons = new ArrayList<FButton>();

    public VGameMenu() {
        addButton("Concede Game", new Runnable() {
            @Override
            public void run() {
                hide();
                FControl.concede();
            }
        });
        addButton("End Turn", new Runnable() {
            @Override
            public void run() {
                hide();
                FControl.endCurrentTurn();
            }
        });
        addButton("Alpha Strike", new Runnable() {
            @Override
            public void run() {
                hide();
                FControl.alphaStrike();
            }
        });
        addButton("View Deck List", new Runnable() {
            @Override
            public void run() {
                hide();
            }
        });
        addButton("Settings", new Runnable() {
            @Override
            public void run() {
                hide();
                Forge.openScreen(new forge.screens.settings.SettingsScreen());
            }
        });
    }

    private void addButton(String caption, Runnable command) {
        buttons.add(add(new FButton(caption, command)));
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = width * INSETS_FACTOR;
        float y = x;
        float dy = height * GAP_Y_FACTOR;
        float buttonWidth = width - 2 * x;
        float buttonHeight = (height - y - x) / buttons.size() - dy;
        dy += buttonHeight;

        for (FButton button : buttons) {
            button.setBounds(x, y, buttonWidth, buttonHeight);
            y += dy;
        }
    }

    @Override
    public int getCount() {
        return -1;
    }

    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }
}
