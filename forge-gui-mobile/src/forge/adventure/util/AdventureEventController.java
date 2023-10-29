package forge.adventure.util;

import com.badlogic.gdx.utils.Array;
import forge.StaticData;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.item.SealedProduct;
import forge.item.generation.BoosterGenerator;
import forge.item.generation.UnOpenedProduct;
import forge.model.CardBlock;
import forge.util.Aggregates;

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
        Jumpstart,
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

    public AdventureEventData createEvent(EventStyle style, String pointID, int eventOrigin, PointOfInterestChanges changes)
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

        Random random = new Random(eventSeed);

        AdventureEventData e ;

        if (random.nextInt(10) <=4){
            e = new AdventureEventData(eventSeed, EventFormat.Jumpstart);
        }
        else{
            e = new AdventureEventData(eventSeed, EventFormat.Draft);
        }

        if (e.cardBlock == null){
            //covers cases where (somehow) editions that do not match the event style have been picked up
            return null;
        }
        e.sourceID = pointID;
        e.eventOrigin = eventOrigin;
        e.eventRules = new AdventureEventData.AdventureEventRules(e.format, changes.getTownPriceModifier());
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

    public List<Deck> getJumpstartBoosters(CardBlock block, int count){
        //Get all candidates then remove at random until no more than count are included
        //This will prevent duplicate choices within a round of a Jumpstart draft
        List<Deck> packsAsDecks = new ArrayList<>();
        for(SealedProduct.Template template : StaticData.instance().getSpecialBoosters())
        {
            if (!template.getEdition().contains(block.getLandSet().getCode()))
                continue;
            UnOpenedProduct toOpen = new UnOpenedProduct(template);

            Deck contents = new Deck();
            contents.getMain().add(toOpen.get());

            int size = contents.getMain().toFlatList().size();

            if ( size < 18 || size > 25)
                continue;

            contents.setName(template.getEdition());

            int black = 0;
            int blue = 0;
            int green = 0;
            int red = 0;
            int white = 0;
            int multi = 0;
            int colorless = 0;

            for (PaperCard card: contents.getMain().toFlatList()) {
                int colors = 0;
                if (card.getRules().getColorIdentity().hasBlack()) {
                    black++;
                    colors++;
                }
                if (card.getRules().getColorIdentity().hasBlue()) {
                    blue++;
                    colors++;
                }
                if (card.getRules().getColorIdentity().hasGreen()) {
                    green++;
                    colors++;
                }
                if (card.getRules().getColorIdentity().hasRed()) {
                    red++;
                    colors++;
                }
                if (card.getRules().getColorIdentity().hasWhite()) {
                    white++;
                    colors++;
                }
                if (colors == 0 && !card.getRules().getType().isLand()) {
                    colorless++;
                }
                else if (colors > 1) {
                    multi++;
                }
            }

            if (multi > 3)
                contents.getTags().add("multicolor");
            if (colorless > 3)
                contents.getTags().add("colorless");
            if (black > 3)
                contents.getTags().add("black");
            if (blue > 3)
                contents.getTags().add("blue");
            if (green > 3)
                contents.getTags().add("green");
            if (red > 3)
                contents.getTags().add("red");
            if (white > 3)
                contents.getTags().add("white");

            packsAsDecks.add(contents);
        }

        while (packsAsDecks.size() > count){
            Aggregates.removeRandom(packsAsDecks);
        }

        return packsAsDecks;
    }
}
