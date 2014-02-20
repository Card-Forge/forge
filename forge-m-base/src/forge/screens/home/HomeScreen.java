package forge.screens.home;

import java.util.ArrayList;

import forge.FScreen;
import forge.toolbox.FButton;

public class HomeScreen extends FScreen {
    private final ArrayList<FButton> buttons = new ArrayList<FButton>();
    
    public HomeScreen() {
        addButton("Constructed");
        addButton("Draft");
        addButton("Sealed");
        addButton("Quest");
        addButton("Guantlet");
        addButton("Settings");
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = width / 20;
        float y = height / 2;
        float dy = height / 100;
        float buttonWidth = width - 2 * x;
        float buttonHeight = (height - y) / buttons.size() - dy;
        dy += buttonHeight;

        for (FButton button : buttons) {
            button.setBounds(x, y, buttonWidth, buttonHeight);
            y += dy;
        }
    }

    private void addButton(String caption) {
        buttons.add(this.add(new FButton(caption)));
    }
}
