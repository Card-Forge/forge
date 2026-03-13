package forge.adventure.util;

import forge.StaticData;
import forge.adventure.data.AdventureEventData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.deck.Deck;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.BoosterGenerator;
import forge.item.generation.UnOpenedProduct;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.Aggregates;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

public class AdventureEventController implements Serializable {
    public void finalizeEvent(AdventureEventData completedEvent) {
        Current.player().getStatistic().setResult(completedEvent);
        Current.player().removeEvent(completedEvent);
    }

    public enum EventFormat {
        Draft,
        Sealed,
        Jumpstart,
        Constructed;

        public static EventFormat smartValueOf(String name) {
            return Arrays.stream(EventFormat.values())
                    .filter(e -> e.name().equalsIgnoreCase(name))
                    .findFirst().orElse(null);
        }
    }

    public enum EventStyle {
        Bracket,
        RoundRobin,
        Swiss
    }

    public enum EventStatus {
        Available, // New event
        Entered,   // Entry fee paid, deck not locked in
        Ready,     // Deck is registered but can still be edited
        Started,   // Matches available
        Completed, // All matches complete, rewards pending
        Awarded,   // Rewards distributed
        Abandoned  // Ended without completing all matches
    }

    private static AdventureEventController object;

    public static AdventureEventController instance() {
        if (object == null) {
            object = new AdventureEventController();
        }
        return object;
    }

    private AdventureEventController() {

    }

    private final Map<String, Long> nextEventDate = new HashMap<>();

    public static void clear() {
        object = null;
    }

    public AdventureEventData createEvent(String pointID) {
        if (nextEventDate.containsKey(pointID) && nextEventDate.get(pointID) >= LocalDate.now().toEpochDay()) {
            // No event currently available here
            return null;
        }

        long eventSeed = getEventSeed(pointID);
        Random random = new Random(eventSeed);

        AdventureEventData e;
        // After a certain number of wins, stop offering Jumpstart events
        if (Current.player().getStatistic().totalWins() < 10 &&
                random.nextInt(10) <= 2) {
            e = new AdventureEventData(eventSeed, EventFormat.Jumpstart);
        } else {
            e = new AdventureEventData(eventSeed, EventFormat.Draft);
        }

        if (e.cardBlock == null) {
            //covers cases where (somehow) editions that do not match the event style have been picked up
            return null;
        }
        return e;
    }

    public AdventureEventData createEvent(EventFormat format, CardBlock cardBlock, String pointID) {
        long eventSeed = getEventSeed(pointID);
        AdventureEventData e = new AdventureEventData(eventSeed, format, cardBlock);
        if(e.cardBlock == null)
             return null;
        return e;
    }

    private static long getEventSeed(String pointID) {
        long eventSeed;
        long timeSeed = LocalDate.now().toEpochDay();
        long placeSeed = Long.parseLong(pointID.replaceAll("[^0-9]", ""));
        long room = Long.MAX_VALUE - placeSeed;
        if (timeSeed > room) {
            //ensuring we don't ever hit an overflow
            eventSeed = Long.MIN_VALUE + timeSeed - room;
        } else {
            eventSeed = timeSeed + placeSeed;
        }
        return eventSeed;
    }

    public void initializeEvent(AdventureEventData e, String pointID, int eventOrigin, PointOfInterestChanges changes) {
        e.sourceID = pointID;
        e.eventOrigin = eventOrigin;

        AdventureEventData.PairingStyle pairingStyle;
        if (e.style == EventStyle.RoundRobin) {
            pairingStyle = AdventureEventData.PairingStyle.RoundRobin;
        } else {
            pairingStyle = AdventureEventData.PairingStyle.SingleElimination;
        }

        e.eventRules = new AdventureEventData.AdventureEventRules(e.format, pairingStyle, changes == null ? 1f : changes.getTownPriceModifier());

        e.generateParticipants();

        AdventurePlayer.current().addEvent(e);
        nextEventDate.put(pointID, LocalDate.now().toEpochDay() + new Random().nextInt(2)); //next local event availability date
    }

    public Deck generateBooster(String setCode) {
        List<PaperCard> cards = BoosterGenerator.getBoosterPack(StaticData.instance().getBoosters().get(setCode));
        Deck output = new Deck();
        output.getMain().add(cards);
        String editionName = FModel.getMagicDb().getEditions().get(setCode).getName();
        output.setName(editionName + " Booster");
        output.setComment(setCode);
        return output;
    }
    public Deck generateBoosterByColor(String color) {
        List<PaperCard> cards = BoosterPack.fromColor(color).getCards();
        Deck output = new Deck();
        output.getMain().add(cards);
        String editionName = color + " Booster Pack";
        output.setName(editionName);
        output.setComment(color);
        return output;
    }

    public List<Deck> getJumpstartBoosters(CardBlock block, int count) {
        // Get all candidates, then remove at random until no more than count are included
        // This will prevent duplicate choices within a round of a Jumpstart draft
        List<Deck> packsAsDecks = new ArrayList<>();
        for (SealedTemplate template : StaticData.instance().getSpecialBoosters()) {
            if (!template.getEdition().contains(block.getLandSet().getCode()))
                continue;
            UnOpenedProduct toOpen = new UnOpenedProduct(template);

            Deck contents = new Deck();
            contents.getMain().add(toOpen.get());

            int size = contents.getMain().toFlatList().size();

            if (size < 18 || size > 25)
                continue;

            contents.setName(template.getEdition());

            int black = 0;
            int blue = 0;
            int green = 0;
            int red = 0;
            int white = 0;
            int multi = 0;
            int colorless = 0;

            for (PaperCard card : contents.getMain().toFlatList()) {
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
                } else if (colors > 1) {
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

        while (packsAsDecks.size() > count) {
            Aggregates.removeRandom(packsAsDecks);
        }

        return packsAsDecks;
    }
}
