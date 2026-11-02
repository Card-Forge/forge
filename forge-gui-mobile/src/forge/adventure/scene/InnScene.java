package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.model.CardBlock;

/**
 * Scene for the Inn in towns
 */
public class InnScene extends UIScene {
    private static InnScene object;
    private static int localObjectId;
    private static String localPointOfInterestId;
    private static AdventureEventData localEvent;
    Scene lastGameScene;
    public static InnScene instance() {
        return instance(null, "", null, -1);
    }

    public static InnScene instance(Scene lastGameScene, String pointOfInterestId, PointOfInterestChanges localChanges, int objectId){
        if (object == null || object.isLandscape != Forge.isLandscapeMode()) {
            clearInstance();
            object = new InnScene();
        }

        changes = localChanges;
        localPointOfInterestId = pointOfInterestId;
        localObjectId = objectId;
        if (lastGameScene != null)
            object.lastGameScene=lastGameScene;
        //Only pick up an event that already exists. Generating a new one (boosters, opponents) is slow, so it
        //happens after the inn is on screen, see refreshStatus().
        localEvent = findExistingLocalEvent();

        return object;
    }

    @Override
    public void onOrientationChanged() {
        Scene last = this.lastGameScene;
        clearInstance();
        Forge.replaceCurrentScene(instance(last, localPointOfInterestId, changes, localObjectId));
    }

    public static void clearInstance() {
        if (object != null) {
            object.dispose();
            object = null;
        }
    }


    TextraButton tempHitPointCost, sell, leave, event;
    Image healIcon, sellIcon, leaveIcon;
    private TextraLabel playerGold,playerShards,eventDescription;

    private InnScene() {

        super(Forge.isLandscapeMode() ? "ui/inn.json" : "ui/inn_portrait.json");
        tempHitPointCost = ui.findActor("tempHitPointCost");
        ui.onButtonPress("done", InnScene.this::done);
        ui.onButtonPress("tempHitPointCost", InnScene.this::potionOfFalseLife);
        ui.onButtonPress("sell", InnScene.this::sell);
        leave = ui.findActor("done");
        sell = ui.findActor("sell");
        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"),true);

        leaveIcon = ui.findActor("leaveIcon");
        healIcon = ui.findActor("healIcon");
        sellIcon = ui.findActor("sellIcon");

        event = ui.findActor("event");
        eventDescription = ui.findActor("eventDescription");

        ui.onButtonPress("event", InnScene.this::startEvent);
    }



    public void done() {
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchScene(lastGameScene==null?GameScene.instance():lastGameScene);
    }

    public void potionOfFalseLife() {
        if (Current.player().potionOfFalseLife()){
            refreshStatus();
        }
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }


    @Override
    public void render() {
        super.render();
    }

    int tempHealthCost = 0;
    static PointOfInterestChanges changes;

    @Override
    public void enter() {
        super.enter();
        refreshStatus();
        GameHUD.getInstance().updateBGM();
    }

    private void refreshStatus(){
        float townPriceModifier = changes == null ? 1f : changes.getTownPriceModifier();
        tempHealthCost = Math.round(Current.player().falseLifeCost() * townPriceModifier);
        boolean purchaseable = Current.player().getMaxLife() == Current.player().getLife() &&
                tempHealthCost <= Current.player().getGold();

        tempHitPointCost.setDisabled(!purchaseable);
        tempHitPointCost.setText("[+GoldCoin] " + tempHealthCost);

        localEvent = findExistingLocalEvent();
        if (localEvent == null && !AdventureEventController.instance().isEventOnCooldown(localPointOfInterestId)) {
            //A new event has to be generated. Show the inn right away and fill the event in once it's ready.
            eventDescription.setText("[GREY]Loading events...");
            event.setDisabled(true);
            scheduleEventCreation();
        } else {
            updateEventStatus();
        }
    }

    private void scheduleEventCreation() {
        if (eventCreationPending)
            return;
        eventCreationPending = true;
        //Posted twice so at least one frame with the loading text is drawn before the blocking generation starts.
        Gdx.app.postRunnable(() -> Gdx.app.postRunnable(() -> {
            eventCreationPending = false;
            if (Forge.getCurrentScene() != this)
                return; //Left the inn in the meantime, the event is created on the next visit.
            initLocalEvent();
            updateEventStatus();
        }));
    }

    private void updateEventStatus() {
        if (localEvent == null){
            eventDescription.setText("[GREY]No events at this time");
            event.setDisabled(true);
        }
        else{
            event.setDisabled(false);
            switch (localEvent.eventStatus){
                case Available:
                    eventDescription.setText(localEvent.format.toString() + " available");
                    break;
                case Entered:
                    eventDescription.setText(localEvent.format.toString() + " [GREEN]entered");
                    break;
                case Ready:
                    eventDescription.setText(localEvent.format.toString() + " [GREEN]ready");
                    break;
                case Started:
                    eventDescription.setText(localEvent.format.toString() + " [GREEN]in progress");
                    break;
                case Completed:
                    eventDescription.setText(localEvent.format.toString() + " [GREEN]rewards available");
                    break;
                case Awarded:
                    eventDescription.setText(localEvent.format.toString() + " complete");
                    break;
                case Abandoned:
                    eventDescription.setText(localEvent.format.toString() + " [RED]abandoned");
                    event.setDisabled(true);
                    break;
            }
        }
    }

    private void sell() {
        ShopScene scene = ShopScene.instance(getUIBackground());
        scene.loadChanges(changes);
        Forge.switchScene(scene);
    }

    private boolean eventCreationPending = false;

    private static AdventureEventData findExistingLocalEvent() {
        for (AdventureEventData data :  AdventurePlayer.current().getEvents()){
            if (data.sourceID.equals(localPointOfInterestId) && data.eventOrigin == localObjectId){
                return data;
            }
        }
        return null;
    }

    private static void initLocalEvent() {
        localEvent = findExistingLocalEvent();
        if (localEvent != null)
            return;
        AdventureEventController controller = AdventureEventController.instance();
        localEvent = controller.createEvent(localPointOfInterestId);
        if(localEvent != null)
            controller.initializeEvent(localEvent, localPointOfInterestId, localObjectId, changes);
    }

    public static void replaceLocalEvent(AdventureEventController.EventFormat format, CardBlock cardBlock) {
        AdventurePlayer.current().getEvents().removeIf((data) -> data.sourceID.equals(localPointOfInterestId) && data.eventOrigin == localObjectId);
        AdventureEventController controller = AdventureEventController.instance();
        localEvent = controller.createEvent(format, cardBlock, localPointOfInterestId);
        if(localEvent != null)
            controller.initializeEvent(localEvent, localPointOfInterestId, localObjectId, changes);
    }

    private void startEvent(){

        Forge.switchScene(EventScene.instance(this, localEvent, changes), true);

    }

}
