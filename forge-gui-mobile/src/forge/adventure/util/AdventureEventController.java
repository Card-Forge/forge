package forge.adventure.util;

import com.badlogic.gdx.utils.Array;
import forge.StaticData;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.item.generation.BoosterGenerator;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

public class AdventureEventController implements Serializable {

    public void finalizeEvent(AdventureEventData completedEvent) {
        Current.player().getStatistic().setResult(completedEvent);
        Current.player().removeEvent(completedEvent);

    }

    public enum EventFormat{
        Draft,
        Sealed,
        Constructed
    }

    public enum EventStyle{
        Bracket,
        RoundRobin,
        Swiss
    }

    public enum EventStatus{
        Available, //New event
        Entered, //Entry fee paid, deck not locked in
        Ready,   //Deck is registered but can still be edited
        Started, //Matches available
        Completed, //All matches complete, rewards pending
        Awarded, //Rewards distributed
        Abandoned //Ended without completing all matches
    }

    private static AdventureEventController object;

    public static AdventureEventController instance() {
        if (object == null) {
            object = new AdventureEventController();
        }
        return object;
    }

    private AdventureEventController(){

    }

    private transient Array<AdventureEventData> allEvents = new Array<>();
    private Map<String, Long> nextEventDate = new HashMap<>();

    public AdventureEventController(AdventureEventController other){
        if (object == null) {
            object = this;
        }
        else{
            System.out.println("Could not initialize AdventureEventController. An instance already exists and cannot be merged.");
        }
    }

    public static void clear(){
        object = null;
    }

    public AdventureEventData createEvent(EventFormat format, EventStyle style, String pointID, int eventOrigin, PointOfInterestChanges changes)
    {
        if (nextEventDate.containsKey(pointID) && nextEventDate.get(pointID) >= LocalDate.now().toEpochDay()){
            //No event currently available here
            return null;
        }

        long eventSeed;
        long timeSeed = LocalDate.now().toEpochDay();
        long placeSeed =  Long.parseLong(pointID.replaceAll("[^0-9]",""));
        long room = Long.MAX_VALUE - placeSeed;
        if (timeSeed > room){
            //ensuring we don't ever hit an overflow
            eventSeed = Long.MIN_VALUE + timeSeed - room;
        }
        else
        {
            eventSeed = timeSeed + placeSeed;
        }

        if (new Random(eventSeed).nextInt(10) <=6){
            //60% chance of new event being available in a given location on a day by day basis.
            //This is intentionally cumulative with the potential nextEventDate lockout to encourage moving around map
            return null;
        }

        AdventureEventData e = new AdventureEventData(eventSeed);
        if (e.cardBlock == null){
            //covers cases where (somehow) metaset only editions (like jumpstart) have been picked up
            return null;
        }
        e.sourceID = pointID;
        e.eventOrigin = eventOrigin;
        e.format = format;
        e.eventRules = new AdventureEventData.AdventureEventRules();
        e.eventRules.acceptsChallengeCoin = true;
        e.eventRules.goldToEnter = (int)((changes.getTownPriceModifier() == -1.0f? 1: changes.getTownPriceModifier())* 3000);
        e.eventRules.shardsToEnter = (int)((changes.getTownPriceModifier() == -1.0f? 1: changes.getTownPriceModifier())* 50);
        e.style = style;

        switch (style){
            case Swiss:
            case Bracket:
                e.rounds = (e.participants.length / 2) - 1;
                break;
            case RoundRobin:
                e.rounds = e.participants.length - 1 ;
                break;
        }

        AdventurePlayer.current().addEvent(e);
        nextEventDate.put(pointID, LocalDate.now().toEpochDay() + new Random().nextInt(2)); //next local event availability date
        return e;
    }

    public Deck generateBooster(String setCode) {
        List<PaperCard> cards = BoosterGenerator.getBoosterPack(StaticData.instance().getBoosters().get(setCode));
        Deck output = new Deck();
        output.getMain().add(cards);
        output.setName("Booster Pack: " + setCode);
        output.setComment(setCode);
        return output;
    }
}
