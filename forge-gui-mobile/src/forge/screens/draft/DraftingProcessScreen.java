package forge.screens.draft;

import forge.limited.BoosterDraft;
import forge.screens.TabPageScreen;
import forge.toolbox.FOptionPane;
import forge.util.Callback;

public class DraftingProcessScreen extends TabPageScreen {
    public DraftingProcessScreen(BoosterDraft draft0) {
        super(new TabPage[] {
                new CurrentPackPage(draft0),
                new DraftDeckPage(draft0),
                new DraftSideboardPage(draft0)
        });
    }

    @Override
    public void onClose(Callback<Boolean> canCloseCallback) {
        if (canCloseCallback == null) { return; }
        FOptionPane.showConfirmDialog("This will end the current draft and you will not be able to resume.\n\n" +
                "Leave anyway?", "Leave Draft?", "Leave", "Cancel", false, canCloseCallback);
    }
}
