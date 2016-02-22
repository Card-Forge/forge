package forge.screens.planarconquest;

import forge.model.FModel;
import forge.planarconquest.ConquestData;
import forge.screens.FScreen;

public class ConquestPlaneswalkScreen extends FScreen {
    private ConquestPlaneSelector planeSelector = add(new ConquestPlaneSelector());

    public ConquestPlaneswalkScreen() {
        super("", ConquestMenu.getMenu());
    }

    @Override
    public void onActivate() {
        ConquestData model = FModel.getConquest().getModel();
        setHeaderCaption(model.getName());
        planeSelector.setSelectedPlane(model.getCurrentPlane());
        planeSelector.activate();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        planeSelector.setBounds(0, startY, width, height - startY);
    }
}
