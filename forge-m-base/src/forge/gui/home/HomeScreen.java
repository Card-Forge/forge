package forge.gui.home;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import forge.ForgeScreen;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.LayoutHelper;

public class HomeScreen extends ForgeScreen {
    private final FButton btnConstructed = new FButton("Constructed");
    private final FButton btnDraft = new FButton("Draft");
    private final FButton btnSealed = new FButton("Sealed");
    private final FButton btnQuest = new FButton("Quest");
    private final FButton btnGuantlet = new FButton("Guantlet");
    private final FButton btnSettings = new FButton("Settings");

    public HomeScreen() {
        final Table table = new Table();
        table.setFillParent(true);

        addButton(table, btnConstructed);
        addButton(table, btnDraft);
        addButton(table, btnSealed);
        addButton(table, btnQuest);
        addButton(table, btnGuantlet);
        addButton(table, btnSettings);

        this.addActor(table);
    }

    private void addButton(Table table, final FButton button) {
        button.setWidth(60);
        button.setHeight(60);
        table.add(button).expandX().center();
        table.row();
    }

    @Override
    protected void doLayout(float width, float height) {
        /*LayoutHelper helper = new LayoutHelper(width, height);
        float buttonHeight = height / this.getActors().size - helper.getGapY();
        for (Actor actor : this.getActors()) {
            helper.fillLine(actor, buttonHeight);
        }*/
    }
}
