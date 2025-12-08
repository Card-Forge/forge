package forge.gamemodes.rogue;

import forge.StaticData;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.PaperCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Rogue Commander mode.
 * For MVP, Rogue Decks and Paths are hardcoded here.
 * Future: Move to JSON configuration files.
 */
public class RogueConfig {

    private static final StaticData db = StaticData.instance();

    /**
     * Load all available Rogue Decks.
     * For MVP, returns 2 hardcoded decks.
     */
    public static List<RogueDeckData> loadRogueDecks() {
        List<RogueDeckData> decks = new ArrayList<>();
        decks.add(createAegarDeck());
        decks.add(createMeriaDeck());
        return decks;
    }

    /**
     * Generate the default linear path for a run.
     * MVP: 5 nodes (3 Planes, 1 Sanctum, 1 Boss)
     */
    public static PathData getDefaultPath() {
        return PathData.createLinearPath(
            NodeData.createPlane("Dominaria", "Meria, Scholar of Antiquity", "rogue/planebounds/meria.dck"),
            NodeData.createSanctum(5, 3),
            NodeData.createPlane("Ixalan", "Zacama, Primal Calamity", "rogue/planebounds/zacama.dck"),
            NodeData.createPlane("Kamigawa", "Kaito Shizuki", "rogue/planebounds/kaito.dck"),
            NodeData.createBoss("Phyrexia", "Elesh Norn, Grand Cenobite", "rogue/planebounds/elesh_norn.dck")
        );
    }

    // ========== ROGUE DECK 1: AEGAR, THE FREEZING FLAME ==========
    private static RogueDeckData createAegarDeck() {
        RogueDeckData deck = new RogueDeckData();
        deck.setName("Aegar, the Freezing Flame");
        deck.setCommanderCardName("Aegar, the Freezing Flame");
        deck.setDescription("Control the battlefield with instants and sorceries that deal damage to creatures.");
        deck.setThemeDescription("Instants/Sorceries Matter - Giant tribal synergies");

        // Start Deck (40 cards)
        Deck startDeck = new Deck("Aegar Start Deck");

        // Commander
        startDeck.getOrCreate(DeckSection.Commander).add(getCard("Aegar, the Freezing Flame"), 1);

        // Lands (16)
        startDeck.getMain().add(getCard("Island"), 8);
        startDeck.getMain().add(getCard("Mountain"), 8);

        // Creatures (10)
        startDeck.getMain().add(getCard("Frost Titan"), 1);
        startDeck.getMain().add(getCard("Calamity Bearer"), 1);
        startDeck.getMain().add(getCard("Cyclone Summoner"), 1);
        startDeck.getMain().add(getCard("Rimescale Dragon"), 1);
        startDeck.getMain().add(getCard("Giant Killer"), 1);
        startDeck.getMain().add(getCard("Borderland Behemoth"), 1);
        startDeck.getMain().add(getCard("Inferno Titan"), 1);
        startDeck.getMain().add(getCard("Niv-Mizzet, Parun"), 1);
        startDeck.getMain().add(getCard("Bonecrusher Giant"), 1);
        startDeck.getMain().add(getCard("Stinkdrinker Daredevil"), 1);

        // Instants/Sorceries (10)
        startDeck.getMain().add(getCard("Lightning Bolt"), 1);
        startDeck.getMain().add(getCard("Counterspell"), 1);
        startDeck.getMain().add(getCard("Seismic Assault"), 1);
        startDeck.getMain().add(getCard("Chain Lightning"), 1);
        startDeck.getMain().add(getCard("Mana Leak"), 1);
        startDeck.getMain().add(getCard("Incinerate"), 1);
        startDeck.getMain().add(getCard("Burst Lightning"), 1);
        startDeck.getMain().add(getCard("Brainstorm"), 1);
        startDeck.getMain().add(getCard("Flame Slash"), 1);
        startDeck.getMain().add(getCard("Ponder"), 1);

        // Artifacts/Other (3)
        startDeck.getMain().add(getCard("Sol Ring"), 1);
        startDeck.getMain().add(getCard("Izzet Signet"), 1);
        startDeck.getMain().add(getCard("Arcane Signet"), 1);

        deck.setStartDeck(startDeck);

        // Reward Pool (~60 cards for MVP)
        CardPool rewardPool = new CardPool();

        // More instants/sorceries
        rewardPool.add(getCard("Mana Drain"), 1);
        rewardPool.add(getCard("Force of Will"), 1);
        rewardPool.add(getCard("Cyclonic Rift"), 1);
        rewardPool.add(getCard("Blasphemous Act"), 1);
        rewardPool.add(getCard("Chaos Warp"), 1);
        rewardPool.add(getCard("Rhystic Study"), 1);
        rewardPool.add(getCard("Fire // Ice"), 1);
        rewardPool.add(getCard("Electrolyze"), 1);
        rewardPool.add(getCard("Spell Pierce"), 1);
        rewardPool.add(getCard("Negate"), 1);
        rewardPool.add(getCard("Ral's Outburst"), 1);
        rewardPool.add(getCard("Fiery Fall"), 1);

        // More creatures
        rewardPool.add(getCard("Charmbreaker Devils"), 1);
        rewardPool.add(getCard("Spellheart Chimera"), 1);
        rewardPool.add(getCard("Young Pyromancer"), 1);
        rewardPool.add(getCard("Guttersnipe"), 1);
        rewardPool.add(getCard("Talrand, Sky Summoner"), 1);
        rewardPool.add(getCard("Volcanic Dragon"), 1);
        rewardPool.add(getCard("Truefire Captain"), 1);
        rewardPool.add(getCard("Izzet Chronarch"), 1);

        // Card draw/advantage
        rewardPool.add(getCard("Mystic Remora"), 1);
        rewardPool.add(getCard("Ponder"), 1);
        rewardPool.add(getCard("Preordain"), 1);
        rewardPool.add(getCard("Opt"), 1);
        rewardPool.add(getCard("Faithless Looting"), 1);
        rewardPool.add(getCard("Accumulated Knowledge"), 1);

        // Artifacts/rocks
        rewardPool.add(getCard("Thought Vessel"), 1);
        rewardPool.add(getCard("Mind Stone"), 1);
        rewardPool.add(getCard("Fire Diamond"), 1);
        rewardPool.add(getCard("Sky Diamond"), 1);
        rewardPool.add(getCard("Coldsteel Heart"), 1);
        rewardPool.add(getCard("Commander's Sphere"), 1);

        // Better lands
        rewardPool.add(getCard("Command Tower"), 1);
        rewardPool.add(getCard("Steam Vents"), 1);
        rewardPool.add(getCard("Sulfur Falls"), 1);
        rewardPool.add(getCard("Izzet Boilerworks"), 1);
        rewardPool.add(getCard("Shivan Reef"), 1);
        rewardPool.add(getCard("Temple of Epiphany"), 1);
        rewardPool.add(getCard("Myriad Landscape"), 1);
        rewardPool.add(getCard("Evolving Wilds"), 1);
        rewardPool.add(getCard("Terramorphic Expanse"), 1);
        rewardPool.add(getCard("Lonely Sandbar"), 1);
        rewardPool.add(getCard("Forgotten Cave"), 1);
        rewardPool.add(getCard("Swiftwater Cliffs"), 1);

        // More synergy cards
        rewardPool.add(getCard("Firebrand Archer"), 1);
        rewardPool.add(getCard("Thermo-Alchemist"), 1);
        rewardPool.add(getCard("Electrostatic Field"), 1);
        rewardPool.add(getCard("Crackling Drake"), 1);
        rewardPool.add(getCard("Nivix Cyclops"), 1);
        rewardPool.add(getCard("Stormchaser Mage"), 1);
        rewardPool.add(getCard("Sprite Dragon"), 1);
        rewardPool.add(getCard("Riddleform"), 1);
        rewardPool.add(getCard("Shark Typhoon"), 1);
        rewardPool.add(getCard("Metallurgic Summonings"), 1);

        deck.setRewardPool(rewardPool);
        return deck;
    }

    // ========== ROGUE DECK 2: MERIA, SCHOLAR OF ANTIQUITY ==========
    private static RogueDeckData createMeriaDeck() {
        RogueDeckData deck = new RogueDeckData();
        deck.setName("Meria, Scholar of Antiquity");
        deck.setCommanderCardName("Meria, Scholar of Antiquity");
        deck.setDescription("Ramp with artifacts and overwhelming power with big creatures.");
        deck.setThemeDescription("Artifacts Matter - Green ramp and red aggression");

        // Start Deck (40 cards)
        Deck startDeck = new Deck("Meria Start Deck");

        // Commander
        startDeck.getOrCreate(DeckSection.Commander).add(getCard("Meria, Scholar of Antiquity"), 1);

        // Lands (16)
        startDeck.getMain().add(getCard("Forest"), 8);
        startDeck.getMain().add(getCard("Mountain"), 8);

        // Creatures (10)
        startDeck.getMain().add(getCard("Solemn Simulacrum"), 1);
        startDeck.getMain().add(getCard("Steel Hellkite"), 1);
        startDeck.getMain().add(getCard("Meteor Golem"), 1);
        startDeck.getMain().add(getCard("Traxos, Scourge of Kroog"), 1);
        startDeck.getMain().add(getCard("Jhoira's Familiar"), 1);
        startDeck.getMain().add(getCard("Burnished Hart"), 1);
        startDeck.getMain().add(getCard("Rampaging Brontodon"), 1);
        startDeck.getMain().add(getCard("Atog"), 1);
        startDeck.getMain().add(getCard("Galvanic Juggernaut"), 1);
        startDeck.getMain().add(getCard("Metalwork Colossus"), 1);

        // Artifacts (11)
        startDeck.getMain().add(getCard("Sol Ring"), 1);
        startDeck.getMain().add(getCard("Mind Stone"), 1);
        startDeck.getMain().add(getCard("Gruul Signet"), 1);
        startDeck.getMain().add(getCard("Arcane Signet"), 1);
        startDeck.getMain().add(getCard("Hedron Archive"), 1);
        startDeck.getMain().add(getCard("Worn Powerstone"), 1);
        startDeck.getMain().add(getCard("Thran Dynamo"), 1);
        startDeck.getMain().add(getCard("Spine of Ish Sah"), 1);
        startDeck.getMain().add(getCard("Thought Vessel"), 1);
        startDeck.getMain().add(getCard("Liquimetal Torque"), 1);
        startDeck.getMain().add(getCard("Wayfarer's Bauble"), 1);

        // Other spells (2)
        startDeck.getMain().add(getCard("Hull Breach"), 1);
        startDeck.getMain().add(getCard("Vandalblast"), 1);

        deck.setStartDeck(startDeck);

        // Reward Pool (~60 cards for MVP)
        CardPool rewardPool = new CardPool();

        // More artifact creatures
        rewardPool.add(getCard("Myr Battlesphere"), 1);
        rewardPool.add(getCard("Wurmcoil Engine"), 1);
        rewardPool.add(getCard("Duplicant"), 1);
        rewardPool.add(getCard("Platinum Angel"), 1);
        rewardPool.add(getCard("Darksteel Colossus"), 1);
        rewardPool.add(getCard("Phyrexian Metamorph"), 1);
        rewardPool.add(getCard("Treasure Keeper"), 1);
        rewardPool.add(getCard("Foundry Inspector"), 1);
        rewardPool.add(getCard("Scrap Trawler"), 1);
        rewardPool.add(getCard("Crystalline Giant"), 1);

        // Green creatures
        rewardPool.add(getCard("Eternal Witness"), 1);
        rewardPool.add(getCard("Wood Elves"), 1);
        rewardPool.add(getCard("Farhaven Elf"), 1);
        rewardPool.add(getCard("Sakura-Tribe Elder"), 1);
        rewardPool.add(getCard("Reclamation Sage"), 1);
        rewardPool.add(getCard("Acidic Slime"), 1);
        rewardPool.add(getCard("Kodama's Reach"), 1);
        rewardPool.add(getCard("Cultivate"), 1);
        rewardPool.add(getCard("Rampant Growth"), 1);
        rewardPool.add(getCard("Explosive Vegetation"), 1);

        // More artifacts
        rewardPool.add(getCard("Lightning Greaves"), 1);
        rewardPool.add(getCard("Swiftfoot Boots"), 1);
        rewardPool.add(getCard("Darksteel Ingot"), 1);
        rewardPool.add(getCard("Chromatic Lantern"), 1);
        rewardPool.add(getCard("Gilded Lotus"), 1);
        rewardPool.add(getCard("Sword of the Animist"), 1);
        rewardPool.add(getCard("Mind's Eye"), 1);
        rewardPool.add(getCard("Ichor Wellspring"), 1);
        rewardPool.add(getCard("Prophetic Prism"), 1);
        rewardPool.add(getCard("Scrabbling Claws"), 1);

        // Artifact synergies
        rewardPool.add(getCard("Goblin Welder"), 1);
        rewardPool.add(getCard("Goblin Engineer"), 1);
        rewardPool.add(getCard("Daretti, Scrap Savant"), 1);
        rewardPool.add(getCard("Trash for Treasure"), 1);
        rewardPool.add(getCard("Scrap Mastery"), 1);
        rewardPool.add(getCard("Open the Vaults"), 1);
        rewardPool.add(getCard("Metalwork Colossus"), 1);

        // Better lands
        rewardPool.add(getCard("Command Tower"), 1);
        rewardPool.add(getCard("Stomping Ground"), 1);
        rewardPool.add(getCard("Rootbound Crag"), 1);
        rewardPool.add(getCard("Cinder Glade"), 1);
        rewardPool.add(getCard("Karplusan Forest"), 1);
        rewardPool.add(getCard("Temple of Abandon"), 1);
        rewardPool.add(getCard("Myriad Landscape"), 1);
        rewardPool.add(getCard("Evolving Wilds"), 1);
        rewardPool.add(getCard("Terramorphic Expanse"), 1);
        rewardPool.add(getCard("Tranquil Thicket"), 1);
        rewardPool.add(getCard("Forgotten Cave"), 1);
        rewardPool.add(getCard("Rugged Highlands"), 1);

        // Red spells
        rewardPool.add(getCard("Chaos Warp"), 1);
        rewardPool.add(getCard("By Force"), 1);
        rewardPool.add(getCard("Shattering Spree"), 1);
        rewardPool.add(getCard("Decimate"), 1);
        rewardPool.add(getCard("Return to Dust"), 1);

        deck.setRewardPool(rewardPool);
        return deck;
    }

    // Helper method to get cards from the database
    private static PaperCard getCard(String cardName) {
        PaperCard card = db.getCommonCards().getCard(cardName);
        if (card == null) {
            System.err.println("Warning: Card not found: " + cardName);
            // Return a basic land as fallback
            return db.getCommonCards().getCard("Forest");
        }
        return card;
    }
}
