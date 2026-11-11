package forge.gamemodes.rogue;

import forge.gamemodes.rogue.effect.EventEffect;
import java.util.List;

/**
 * Catalog of all events in Rogue Commander mode.
 * Each event has a description and a list of choices with associated effects.
 */
public enum RogueEvent {

    AFTER_DUSK("After Dusk",
        "Rain drives you into an isolated mansion after midnight. Claw marks score the doors along the upper hall, and something heavy paces inside the walls.",
        List.of(
            new EventChoice("Turn insane", "By dawn, the house has filled your sleep with nightmares that follow you outside.",
                EventEffect.AFTER_DUSK_INSANE),
            new EventChoice("Explore mansion", "The mansion's traps draw blood before you reach the front door again.",
                EventEffect.AFTER_DUSK_EXPLORE),
            new EventChoice("Feed monsters", "You cast every scrap of white magic into the walls. Some ancient horrors, now feeling kind of sated, crawl free and follow you.",
                EventEffect.AFTER_DUSK_FEED)
        )),

    AMBUSH("Ambush!",
        "An Omenpath tears open across the Trail. Armed raiders pour through, and their leader levels a blade at you. \"Your Gold or your life, Commander. Choose.\"",
        List.of(
            new EventChoice("Fight", "You drive the ambushers back through the Omenpath.",
                EventEffect.AMBUSH_FIGHT),
            new EventChoice("Bribe", "The leader pockets your payment. \"Good choice, maggot.\" He orders the raiders back through the Omenpath.",
                EventEffect.AMBUSH_BRIBE)
        )),

    AMONG_MURDERERS("Among Murderers",
        "A noble lies dead beside an overturned goblet in a candlelit manor. The guests lock the doors while servants search the halls, and each accusation points to a different suspect.",
        List.of(
            new EventChoice("Investigate", "You find an object that ties one guest to the murder with no doubt and keep it as evidence.",
                EventEffect.AMONG_MURDERERS_INVESTIGATE),
            new EventChoice("Confess", "You give the guests the confession they want. Word of your suspected guilt follows you into every battle.",
                EventEffect.AMONG_MURDERERS_CONFESS),
            new EventChoice("Hire", "After a few discreet payments, the sharp-eyed detectives give their answers before you leave the manor.",
                EventEffect.AMONG_MURDERERS_HIRE)
        )),

    BENDING_DESTINY("Bending Destiny",
        "At a weathered roadside shrine, a traveler unrolls a set of lesson scrolls beside the fire. They offer to share the road or leave the scrolls in your care.",
        List.of(
            new EventChoice("Walk together", "The traveler shoulders their pack and joins you as an Ally.",
                EventEffect.BENDING_WALK),
            new EventChoice("Study", "You pack the lesson scrolls for the road ahead.",
                EventEffect.BENDING_STUDY)
        )),

    BREAKING_THE_OLD_LAWS("Breaking the Old Laws",
        "A horned demon waits beside a cracked lawstone, its surface scored where binding runes were cut away. \"All these laws, all these rules... for what?\" the demon asks. \"I think it's time for me to break these chains for you. Just for a little price.\"",
        List.of(
            new EventChoice("Duplicate", "The demon cuts your palm and presses the chosen card into the blood. Copies peel away from it.",
                EventEffect.BREAKING_OLD_LAWS_DUPLICATE),
            new EventChoice("Black Market", "The demon seals the hidden market again when your business is done.",
                EventEffect.BREAKING_OLD_LAWS_BLACK_MARKET),
            new EventChoice("Partner Up", "The pact scars you for life. But a legendary partner answers your command.",
                EventEffect.BREAKING_OLD_LAWS_PARTNER_UP)
        )),

    BURROWED_INTO_TROUBLE("Burrowed into Trouble",
        "A trader's caravan creaks beneath the weight of iron cages. Squirrels, otters, raccoons, and other small woodland creatures peer out with wide, mournful eyes while the trader smiles and watches you closely.",
        List.of(
            new EventChoice("Browse", "The trader bolts the cages after your bargaining ends, then sends the caravan rattling down the Trail.",
                EventEffect.BURROWED_BROWSE),
            new EventChoice("Free", "You smash the cages and scare the trader away, screaming and sobbing. With wounded pride, you let the cute Woodland leaders follow you.",
                EventEffect.BURROWED_FREE),
            new EventChoice("Sell", "The trader leads away the creatures you surrender eagerly, laughing a little, and presses a purse into your hand.",
                EventEffect.BURROWED_SELL)
        )),

    CROOKED_COUNSEL("Crooked Counsel",
        "An old wizard, once a cherished friend, blocks the road beneath a black stone tower. \"The shadow rises in the east,\" he murmurs. \"Stand against it if you must... but it wouldn't be wise, my friend.\"",
        List.of(
            new EventChoice("Rally the Free Peoples", "You reject the wizard's offer and send word to those still willing to resist.",
                EventEffect.CROOKED_COUNSEL_FELLOWSHIP),
            new EventChoice("Join with the Dark Lord", "Dark riders close around you. Allies that once followed your banner abandon the road.",
                EventEffect.CROOKED_COUNSEL_NAZGUL),
            new EventChoice("Keep to your own path", "The ring burns cold against your palm, yours, your precious, as you leave the wizard behind without even looking back.",
                EventEffect.CROOKED_COUNSEL_RING)
        )),

    DISTORTION("Distortion",
        "The Trail folds back on itself. Loose stones hang in the air while the Omenpath ahead opens sideways across the ground.",
        List.of(
            new EventChoice("Embrace", "The distortion races down the Trail. Side paths collapse, leaving chests where their entrances stood, and a curse marks the Planebounds ahead.",
                EventEffect.DISTORTION_EMBRACE),
            new EventChoice("Endure", "You force the Trail straight. A trace of the distortion remains, leaving less to claim from the battles ahead.",
                EventEffect.DISTORTION_ENDURE)
        )),

    DRIFTED_AWAY("Drifted Away",
        "Smoke rises from a fresh crash site beside the Trail. The pilot lies unconscious near the wreck, blood soaking through a torn flight suit, while the vehicle's engine still hums.",
        List.of(
            new EventChoice("Rescue", "Pulling the pilot clear leaves you bleeding. They wake before sunset and choose to follow you.",
                EventEffect.DRIFTED_RESCUE),
            new EventChoice("Steal", "You coax the damaged vehicle back to life and take it with you, leaving the pilot helpless on the road.",
                EventEffect.DRIFTED_STEAL)
        )),

    ETERNAL_CRUSADE("Eternal Crusade",
        "Armored giants march wordless through a shattered outpost beneath failing lumen lights. Something claws from inside a sealed chamber while soldiers kneel before a throne wired into the wall.",
        List.of(
            new EventChoice("Secure Specimen", "You open the containment chamber. The Tyranid inside is obedient enough (for now) to lower its claws and follows you.",
                EventEffect.ETERNAL_CRUSADE_SECURE_SPECIMEN),
            new EventChoice("Join Space Marines", "You kneel with the crusade. It accepts your oath and drills you in the Codex Astartes.",
                EventEffect.ETERNAL_CRUSADE_JOIN_SPACE_MARINES),
            new EventChoice("Offer Sacrifice", "The throne literally consumes the creatures you chose. Its ancient machinery starts with a low, steady hum.",
                EventEffect.ETERNAL_CRUSADE_OFFER_SACRIFICE)
        )),

    FINAL_PREPARATIONS("Final Preparations",
        "At the edge of the world, crystal light spills across the city, still glowing as if nothing could ever end. Mercenaries laugh over cheap drinks and weary travelers barter for one last advantage. Every smile carries the quiet knowledge that by morning, you and half these folks may be gone.",
        List.of(
            new EventChoice("Visit Smith", "When you step away, the smith banks the forge and closes the stall.",
                EventEffect.FINAL_PREPARATIONS_VISIT_SMITH),
            new EventChoice("Learn Summoning", "You complete the summoning circle. A summoned creature steps across its boundary and waits for your order.",
                EventEffect.FINAL_PREPARATIONS_LEARN_SUMMONING),
            new EventChoice("Level Up", "The mercenaries drill you until your legs shake. After a full night's rest, you wake stronger.",
                EventEffect.FINAL_PREPARATIONS_LEVEL_UP)
        )),

    GAMECHANGER("Gamechanger",
        "A suspicious figure in a patched coat spreads your deck across a roadside table. \"Worthless\" they sneer. \"Let me show you the cards that change games.\"",
        List.of(
            new EventChoice("Trust blindly", "The cardsharp sweeps your discarded cards from the table and hands over your replacements.",
                EventEffect.GAMECHANGER_TRUST),
            new EventChoice("Choose wisely", "The figure folds the roadside table and slips into the next Omenpath.",
                EventEffect.GAMECHANGER_CHOOSE)
        )),

    GROUND_ZERO("Ground Zero",
        "A rusted vault door juts from a crater beside the Trail. Inside, bobbleheads rattle in a cracked display case beside a humming workbench, while fresh tracks lead back into the irradiated wasteland.",
        List.of(
            new EventChoice("Loot something S.P.E.C.I.A.L.", "The bobbleheads knock together as you carry them out of the vault.",
                EventEffect.GROUND_ZERO_SPECIAL),
            new EventChoice("Use Workbench", "The workbench sparks to life. Repaired robots climb down and follow you into the wasteland.",
                EventEffect.GROUND_ZERO_REPAIR),
            new EventChoice("Explore Wasteland", "Radioactive dust settles over the tracks behind you. Anything that followed you into the wasteland returns changed.",
                EventEffect.GROUND_ZERO_MUTATE)
        )),

    INFAMOUS_JUNCTION("Once Upon a Time at an Infamous Junction",
        "Wanted posters cover the walls of a badlands town. The sheriff watches the bank from an empty street while a rancher offers payment for cattle lost in the scrub.",
        List.of(
            new EventChoice("Raise a Gang", "By sundown your crew prepared for life as outlaws, ready to take what's rightfully theirs.",
                EventEffect.INFAMOUS_JUNCTION_RAISE_GANG),
            new EventChoice("Rob the Local Bank", "You ride out under gunfire with a torn coat and the bank's strongbox strapped behind your saddle.",
                EventEffect.INFAMOUS_JUNCTION_ROB_BANK),
            new EventChoice("Rope the Lost Cattle", "A sure-footed animal follows your rope back to town, then refuses to leave your side.",
                EventEffect.INFAMOUS_JUNCTION_ROPE_CATTLE)
        )),

    LOST_NOT_FORGOTTEN("Lost, But not Forgotten",
        "Fresh boot prints cross the dust ahead of your torch in the dungeon. Voices carry from a side passage, while a narrow stairway descends beyond the edge of the light.",
        List.of(
            new EventChoice("Stumble Into Party", "An adventuring party steps from the side passage. After a wary exchange, they agree to travel with you.",
                EventEffect.LOST_NOT_FORGOTTEN_PARTY),
            new EventChoice("Level Up", "A veteran adventurer drills you beside the dying torch. When the lesson ends, they pocket their fee and leave.",
                EventEffect.LOST_NOT_FORGOTTEN_LEVEL_UP),
            new EventChoice("Venture deeper", "You descend until the last torchlit step disappears overhead.",
                EventEffect.LOST_NOT_FORGOTTEN_VENTURE_DEEPER)
        )),

    LOST_CONNECTION("Lost",
        "The Omenpath tears at your body. Your hands turn transparent, and each step leaves a fading copy behind you.",
        List.of(
            new EventChoice("Depart", "You let the distortion pull you from the Trail. Your forces continue without you until you return.",
                EventEffect.LOST_DEPART),
            new EventChoice("Persist", "You force yourself to remain. The distortion leaves a weakness that does not fade.",
                EventEffect.LOST_PERSIST),
            new EventChoice("Replace", "A new legend answers the call, to continue what you couldn't.",
                EventEffect.LOST_REPLACE)
        )),

    MERCHANT_CARAVAN("Merchant Caravan",
        "A line of painted wagons pulls off the Trail and unfolds into a roadside market. Merchants raise canvas awnings over crates from distant planes.",
        List.of(
            new EventChoice("Browse Their Wares", "The merchants fold their awnings when you step away, and the caravan returns to the Trail.",
                EventEffect.CARAVAN_BROWSE),
            new EventChoice("Rob Them", "The merchants fight back. When the wagons retreat, their strongbox is in your hands and blood runs down your sleeve.",
                EventEffect.CARAVAN_ROB)
        )),

    NEON_LID("Neon-Lid",
        "Rain runs down the neon signs of Towashi. Steel rings in an alley below the rooftops, and incense drifts from a shrine wedged between chrome towers.",
        List.of(
            new EventChoice("Path of the Samurai", "At dawn, the dojo master closes the doors and bows you back onto the street.",
                EventEffect.NEON_LID_SAMURAI),
            new EventChoice("Path of the Ninja", "The rooftop trials leave scars. The clan teaches you how to strike unseen.",
                EventEffect.NEON_LID_NINJA),
            new EventChoice("Path of Inner Peace", "You let the shrine's wisdom settle over you as you step back into the rain.",
                EventEffect.NEON_LID_SHRINE)
        )),

    ON_THE_EDGE("On the Edge",
        "A city-sized spacecraft drifts beyond the Omenpath, its running lights still blinking across a scarred hull. An open docking bay passes close enough to reach before the ship slides back into the dark.",
        List.of(
            new EventChoice("Hijack", "You catch the drifting craft before it shears away and make it your own.",
                EventEffect.ON_THE_EDGE_HIJACK),
            new EventChoice("Board", "You invoke the ship's still functioning hyperdrive and come back out somewhere else on the route entirely.",
                EventEffect.ON_THE_EDGE_BOARD),
            new EventChoice("Scavenge", "Sharp hull plates cut through your gear before you make it back to the airlock.",
                EventEffect.ON_THE_EDGE_SCAVENGE)
        )),

    PLANAR_EXCHANGE("Planar Exchange",
        "A circular portal opens beside the Trail. Beyond it, unfamiliar relics and sealed scrolls cover a stone counter beside an empty offering tray.",
        List.of(
            new EventChoice("Make the Exchange", "The portal takes the cards you chose and drops unfamiliar replacements at your feet.",
                EventEffect.PLANAR_EXCHANGE_EXCHANGE),
            new EventChoice("Stay Put", "You decide not to risk it.",
                EventEffect.NOTHING)
        )),

    PLANAR_RIFT("Planar Rift",
        "A jagged rift splits the air beside the Trail. Heat rolls from its center while glassy residue hardens along the edges.",
        List.of(
            new EventChoice("Enter the Rift", "You step through. Rift energy settles into your muscles and remains, even after the tear closes.",
                EventEffect.PLANAR_RIFT_BOOST),
            new EventChoice("Harvest the Energy", "You scrape the hardened residue into a pouch and sell it before it cools.",
                EventEffect.PLANAR_RIFT_ENERGY)
        )),

    PLANAR_TRIBUTE("Planar Tribute",
        "An altar of fused stone blocks the Trail. Its empty slots flare whenever you draw near. It seems to demand something before letting you pass.",
        List.of(
            new EventChoice("Sacrifice", "The altar swallows the cards you chose and sinks back into the road.",
                EventEffect.PLANAR_TRIBUTE_REMOVE),
            new EventChoice("Give and take", "After the exchange, unfamiliar cards lie where the altar stood.",
                EventEffect.PLANAR_TRIBUTE_REPLACE)
        )),

    SATCHEL("Satchel",
        "A leather satchel lies beneath a fallen pillar, its clasp stamped with a Planeswalker symbol. Dust covers everything except the handle.",
        List.of(
            new EventChoice("Open the Satchel", "You return to the Trail after opening the hidden chest.",
                EventEffect.SATCHEL_OPEN)
        )),

    STREET_OF_CONCEALMENT("Street of Concealment",
        "Rain beads on the brass doors of a shuttered club while brass horns mutter somewhere. Capenna....again... In the cellar of the club, a devil in a tailored coat slides a new contract across the table. \"Missed me?\" he says with a grin.",
        List.of(
            new EventChoice("Accept", "The devil signs beneath your name. Your forces slip through every battle line and lose the power to hold one.",
                EventEffect.STREET_OF_CONCEALMENT_ACCEPT),
            new EventChoice("Decline", "You leave the contract unsigned and climb back to the rain-soaked avenue.",
                EventEffect.NOTHING)
        )),

    STREET_OF_GREED("Street of Greed",
        "Coins cover a roulette table in the back room of a New Capenna casino. Not this again... A devil taps the contract beside them. \"Well if it isn't my favourite Teferi puppet.\" they say. \"Only the best deals for you, my friend.\"",
        List.of(
            new EventChoice("Accept", "You put your signature. From then on, every attempt to restore your strength fails.",
                EventEffect.STREET_OF_GREED_ACCEPT),
            new EventChoice("Decline", "You push the contract back across the table and leave the casino for good.",
                EventEffect.NOTHING)
        )),

    STREET_OF_FORCEFULNESS("Street of Forcefulness",
        "How...how do you always end up here?? Behind a New Capenna fight club, a devil lays a contract across a scarred desk. \"Surprise. It's your favourite devil offering your favourite deals.\"",
        List.of(
            new EventChoice("Accept", "The devil stamps the contract, and your spells come more easily.",
                EventEffect.STREET_OF_FORCEFULNESS_ACCEPT),
            new EventChoice("Decline", "You leave the contract on the desk and walk back through the club.",
                EventEffect.NOTHING)
        )),

    SHRINE("Shrine",
        "A crumbling shrine pulses faintly beside the Trail. Near its base, warm light escapes through symbols cut too low to read while standing.",
        List.of(
            new EventChoice("Kneel", "You leave the hidden Sanctum as the shrine wall seals behind you.",
                EventEffect.SHRINE_KNEEL)
        )),

    TRAPPED_IN_THE_LAIR("Trapped in the Lair",
        "You stumble into a dark tunnel as black flesh closes across the passage behind you. Ahead, a massive beast prowls among clusters of living tissue rooted in the floor.",
        List.of(
            new EventChoice("Slay", "The beast's claws tear into you before it falls. You butcher the carcass and carry its meat back to the Trail.",
                EventEffect.TRAPPED_IN_THE_LAIR_SLAY),
            new EventChoice("Tame", "The beast wounds you as you approach. You hold your ground until it lets you rest a hand against its muzzle, then it follows you from the lair.",
                EventEffect.TRAPPED_IN_THE_LAIR_TAME),
            new EventChoice("Examine", "You press a sample from the tunnel floor against your arm. It burrows beneath your skin, and the creatures you summon can merge with other bodies.",
                EventEffect.TRAPPED_IN_THE_LAIR_EXAMINE)
        )),

    WANDERING_HEALER("Wandering Healer",
        "A healer has pitched a canvas tent beside the Trail. Bottles clink inside a wooden case as they inspect your injuries and name their price.",
        List.of(
            new EventChoice("Receive Healing Potion", "The potion tastes disgustingly of rust and bitter roots. Warmth returns to your limbs.",
                EventEffect.HEALER_POTION),
            new EventChoice("Treat Wounds", "The healer cleans each wound and binds it with silver-threaded cloth.",
                EventEffect.HEALER_TREAT_WOUNDS),
            new EventChoice("Strengthen", "The healer traces a ward across your skin. Once the glow fades, you shoulder your gear without its usual strain.",
                EventEffect.HEALER_STRENGTHEN),
            new EventChoice("Decline", "You wave the healer away.",
                EventEffect.NOTHING)
        ));

    public record EventChoice(String label, String resultText, EventEffect effect) {}

    private final String displayName;
    private final String description;
    private final List<EventChoice> choices;

    RogueEvent(String displayName, String description, List<EventChoice> choices) {
        this.displayName = displayName;
        this.description = description;
        this.choices = choices;
    }

    public String getDisplayName() { return displayName; }
    public String getRawDescription() { return description; }
    public String getDescription() { return TextHelper.stripPreviewMarkers(getRawDescription()); }
    public List<PreviewReference> getPreviewReferences() { return TextHelper.extractPreviewReferences(getRawDescription()); }
    public List<EventChoice> getChoices() { return choices; }

    /** Whether this event should appear in the event pool. Override for one-time events. */
    public boolean isAvailable() { return true; }

    @Override
    public String toString() { return displayName; }
}
