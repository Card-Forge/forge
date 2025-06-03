package forge.adventure.character;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.MapDialog;

public class QuestActor extends DialogActor {
    String POI_ID;
    PointOfInterestChanges changes;
    String questOrigin;

    public QuestActor(String POI_ID, PointOfInterestChanges changes, String questOrigin, MapStage stage, int id) {
        super(null, stage, id);
        this.POI_ID = POI_ID;
        this.changes = changes;
        this.questOrigin = questOrigin;

    }

    @Override
    public void onPlayerCollide() {
        questData = AdventureQuestController.instance().getQuestNPCResponse(POI_ID, changes, questOrigin);

        dialog = new MapDialog(questData.offerDialog, stage, objectId, questData);

        ChangeListener finished = new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                removeFromMap();
                dialog = null;
            }
        };
        dialog.addDialogCompleteListener(finished);

        if (dialog != null) {
            if (dialog.activate()){
                stage.resetPosition();
                stage.showDialog();
            }
        }
    }
}
