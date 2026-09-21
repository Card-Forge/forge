package forge.adventure.character;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.MapDialog;

/**
 * QuestActor
 * Specialized actor to handle Quest interactions
 */
public class QuestActor extends DialogActor {
    private final String POI_ID;
    private final PointOfInterestChanges changes;
    private final String questOrigin;
    private final ChangeListener dialogFinishedListener;

    public QuestActor(String POI_ID, PointOfInterestChanges changes, String questOrigin, MapStage stage, int id) {
        super(null, stage, id);
        this.POI_ID = POI_ID;
        this.changes = changes;
        this.questOrigin = questOrigin;

        this.dialogFinishedListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                removeFromMap();
                dialog = null;
            }
        };
    }

    @Override
    public void onPlayerCollide() {
        questData = AdventureQuestController.instance().getQuestNPCResponse(POI_ID, changes, questOrigin);

        dialog = new MapDialog(questData.offerDialog, stage, objectId, questData);

        dialog.addDialogCompleteListener(dialogFinishedListener);

        if (dialog != null) {
            if (dialog.activate()){
                stage.resetPosition();
                stage.showDialog();
            }
        }
    }
}
