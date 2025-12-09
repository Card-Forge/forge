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
        decks.add(createAnimDeck());
        decks.add(createOmoDeck());
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

    // ========== ROGUE DECK 1: ANIM PAKAL, THOUSANDTH MOON ==========
    private static RogueDeckData createAnimDeck() {
        RogueDeckData deck = new RogueDeckData();
        deck.setName("Anim Pakal, Thousandth Moon");
        deck.setCommanderCardName("Anim Pakal, Thousandth Moon");
        deck.setDescription("Go wide with Artifact Tokens and burn down your opponents with spells that care about entering creatures.");
        deck.setThemeDescription("Tokens and Artifacts Matter - Boros Aggro / Go Wide");

        // Start Deck (~40 cards)
        Deck startDeck = new Deck("Anim Start Deck");

        // Commander
        startDeck.getOrCreate(DeckSection.Commander).add(getCard("Anim Pakal, Thousandth Moon", "LCI"), 1);

        startDeck.getMain().add(getCard("Ancient Den", "C21"), 1);
        startDeck.getMain().add(getCard("Baird, Argivian Recruiter", "DMU"), 1);
        startDeck.getMain().add(getCard("Basri's Solidarity", "J25"), 1);
        startDeck.getMain().add(getCard("Battlefield Forge", "C21"), 1);
        startDeck.getMain().add(getCard("Boros Garrison", "ONC"), 1);
        startDeck.getMain().add(getCard("Buried Ruin", "ONC"), 1);
        startDeck.getMain().add(getCard("Command Tower", "ONC"), 1);
        startDeck.getMain().add(getCard("Demand Answers", "MKM"), 1);
        startDeck.getMain().add(getCard("Dispatch", "C21"), 1);
        startDeck.getMain().add(getCard("Dispeller's Capsule", "C21"), 1);
        startDeck.getMain().add(getCard("Furycalm Snarl", "ONC"), 1);
        startDeck.getMain().add(getCard("Goldnight Commander", "ONC"), 1);
        startDeck.getMain().add(getCard("Great Furnace", "C21"), 1);
        startDeck.getMain().add(getCard("Ichor Wellspring", "C21"), 1);
        startDeck.getMain().add(getCard("Impact Tremors", "DTK"), 1);
        startDeck.getMain().add(getCard("Ingenious Artillerist", "CLB"), 1);
        startDeck.getMain().add(getCard("Intangible Virtue", "ONC"), 1);
        startDeck.getMain().add(getCard("Keleth, Sunmane Familiar", "CMM"), 1);
        startDeck.getMain().add(getCard("Legion Warboss", "ONC"), 1);
        startDeck.getMain().add(getCard("Lorehold Campus", "STX"), 1);
        startDeck.getMain().add(getCard("Loyal Apprentice", "ONC"), 1);
        startDeck.getMain().add(getCard("Luminarch Aspirant", "PZNR"), 1);
        startDeck.getMain().add(getCard("Molten Gatekeeper", "MH3"), 1);
        startDeck.getMain().add(getCard("Mountain", "ONE"), 5);
        startDeck.getMain().add(getCard("Ornithopter of Paradise", "PLST"), 1);
        startDeck.getMain().add(getCard("Orzhov Advokist", "MIC"), 1);
        startDeck.getMain().add(getCard("Path to Exile", "ONC"), 1);
        startDeck.getMain().add(getCard("Phantom General", "ONC"), 1);
        startDeck.getMain().add(getCard("Phoenix Chick", "DMU"), 1);
        startDeck.getMain().add(getCard("Plains", "ONE"), 5);
        startDeck.getMain().add(getCard("Prava of the Steel Legion", "ONC"), 1);
        startDeck.getMain().add(getCard("Reckless Fireweaver", "KLD"), 1);
        startDeck.getMain().add(getCard("Rip Apart", "ONC"), 1);
        startDeck.getMain().add(getCard("Witty Roastmaster", "SNC"), 1);


        deck.setStartDeck(startDeck);

        // Reward Pool (~100 cards)
        CardPool rewardPool = new CardPool();

        rewardPool.add(getCard("Abzan Falconer", "MOC"), 1);
        rewardPool.add(getCard("Adeline, Resplendent Cathar", "MOC"), 1);
        rewardPool.add(getCard("Andúril, Narsil Reforged", "LTC"), 1);
        rewardPool.add(getCard("Angel of the Ruins", "C21"), 1);
        rewardPool.add(getCard("Angelfire Ignition", "INR"), 1);
        rewardPool.add(getCard("Angelic Intervention", "MOM"), 1);
        rewardPool.add(getCard("Angrath's Marauders", "LCC"), 1);
        rewardPool.add(getCard("Arabella, Abandoned Doll", "DSK"), 1);
        rewardPool.add(getCard("Archaeomancer's Map", "C21"), 1);
        rewardPool.add(getCard("Assemble the Legion", "ONC"), 1);
        rewardPool.add(getCard("Audacious Reshapers", "C21"), 1);
        rewardPool.add(getCard("Aurelia, the Law Above", "PMKM"), 1);
        rewardPool.add(getCard("Blade Historian", "PSTX"), 1);
        rewardPool.add(getCard("Boros Charm", "C21"), 1);
        rewardPool.add(getCard("Bronze Guardian", "C21"), 1);
        rewardPool.add(getCard("Castle Ardenvale", "ONC"), 1);
        rewardPool.add(getCard("Castle Embereth", "ONC"), 1);
        rewardPool.add(getCard("Cathars' Crusade", "INR"), 1);
        rewardPool.add(getCard("Chain Reaction", "ONC"), 1);
        rewardPool.add(getCard("Citadel Siege", "MIC"), 1);
        rewardPool.add(getCard("Cleansing Nova", "C21"), 1);
        rewardPool.add(getCard("Clever Concealment", "ONC"), 1);
        rewardPool.add(getCard("Collective Effort", "ONC"), 1);
        rewardPool.add(getCard("Cursed Mirror", "C21"), 1);
        rewardPool.add(getCard("Darksteel Citadel", "C21"), 1);
        rewardPool.add(getCard("Devilish Valet", "BLC"), 1);
        rewardPool.add(getCard("Digsite Engineer", "C21"), 1);
        rewardPool.add(getCard("Duty Beyond Death", "TDM"), 1);
        rewardPool.add(getCard("Enduring Innocence", "PDSK"), 1);
        rewardPool.add(getCard("Feat of Resistance", "KTK"), 1);
        rewardPool.add(getCard("Felidar Retreat", "ONC"), 1);
        rewardPool.add(getCard("Flawless Maneuver", "ONC"), 1);
        rewardPool.add(getCard("Geist-Honored Monk", "ISD"), 1);
        rewardPool.add(getCard("Generous Gift", "ONC"), 1);
        rewardPool.add(getCard("Glimmer Lens", "ONC"), 1);
        rewardPool.add(getCard("Goblin Bombardment", "MH2"), 1);
        rewardPool.add(getCard("Goldwardens' Gambit", "ONC"), 1);
        rewardPool.add(getCard("Guide of Souls", "MH3"), 1);
        rewardPool.add(getCard("Hedron Archive", "C21"), 1);
        rewardPool.add(getCard("Hellkite Igniter", "C21"), 1);
        rewardPool.add(getCard("Heroic Reinforcements", "ONC"), 1);
        rewardPool.add(getCard("Hexplate Wallbreaker", "ONC"), 1);
        rewardPool.add(getCard("Hoard-Smelter Dragon", "C21"), 1);
        rewardPool.add(getCard("Hour of Reckoning", "ONC"), 1);
        rewardPool.add(getCard("Idol of Oblivion", "ONC"), 1);
        rewardPool.add(getCard("Inti, Seneschal of the Sun", "PLCI"), 1);
        rewardPool.add(getCard("Jor Kadeen, the Prevailer", "C21"), 1);
        rewardPool.add(getCard("Kemba's Banner", "ONC"), 1);
        rewardPool.add(getCard("Kher Keep", "ONC"), 1);
        rewardPool.add(getCard("Lae'zel, Vlaakith's Champion", "CLB"), 1);
        rewardPool.add(getCard("Lightning Greaves", "M3C"), 1);
        rewardPool.add(getCard("Losheel, Clockwork Scholar", "C21"), 1);
        rewardPool.add(getCard("Mace of the Valiant", "ONC"), 1);
        rewardPool.add(getCard("Martial Coup", "ONC"), 1);
        rewardPool.add(getCard("Mentor of the Meek", "ONC"), 1);
        rewardPool.add(getCard("Myr Battlesphere", "ONC"), 1);
        rewardPool.add(getCard("Myriad Landscape", "C21"), 1);
        rewardPool.add(getCard("Path of Ancestry", "ONC"), 1);
        rewardPool.add(getCard("Phyrexia's Core", "C21"), 1);
        rewardPool.add(getCard("Pia and Kiran Nalaar", "PLST"), 1);
        rewardPool.add(getCard("Pia Nalaar", "C21"), 1);
        rewardPool.add(getCard("Queen Kayla bin-Kroog", "BRO"), 1);
        rewardPool.add(getCard("Raid Bombardment", "UMA"), 1);
        rewardPool.add(getCard("Reconstruct History", "STX"), 1);
        rewardPool.add(getCard("Requisition Raid", "OTJ"), 1);
        rewardPool.add(getCard("Return to Dust", "C21"), 1);
        rewardPool.add(getCard("Ring of Thune", "CMR"), 1);
        rewardPool.add(getCard("Ring of Valkas", "CMR"), 1);
        rewardPool.add(getCard("Roar of Resistance", "ONC"), 1);
        rewardPool.add(getCard("Rogue's Passage", "C21"), 1);
        rewardPool.add(getCard("Rosie Cotton of South Lane", "LTR"), 1);
        rewardPool.add(getCard("Rout", "C21"), 1);
        rewardPool.add(getCard("Rumor Gatherer", "SNC"), 1);
        rewardPool.add(getCard("Sculpting Steel", "C21"), 1);
        rewardPool.add(getCard("Sentinel Sarah Lyons", "PIP"), 1);
        rewardPool.add(getCard("Shared Animosity", "LCC"), 1);
        rewardPool.add(getCard("Siege Veteran", "PBRO"), 1);
        rewardPool.add(getCard("Siege-Gang Commander", "ONC"), 1);
        rewardPool.add(getCard("Silverwing Squadron", "ONC"), 1);
        rewardPool.add(getCard("Slayers' Stronghold", "C21"), 1);
        rewardPool.add(getCard("Sol Ring", "C21"), 1);
        rewardPool.add(getCard("Staff of the Storyteller", "ONC"), 1);
        rewardPool.add(getCard("Steel Hellkite", "C21"), 1);
        rewardPool.add(getCard("Steel Overseer", "C21"), 1);
        rewardPool.add(getCard("Strionic Resonator", "C19"), 1);
        rewardPool.add(getCard("Sunhome, Fortress of the Legion", "C21"), 1);
        rewardPool.add(getCard("Take Up the Shield", "J25"), 1);
        rewardPool.add(getCard("Tarrian's Soulcleaver", "PLCI"), 1);
        rewardPool.add(getCard("Tempered Steel", "2XM"), 1);
        rewardPool.add(getCard("Temple of Triumph", "C21"), 1);
        rewardPool.add(getCard("Thousand Moons Smithy / Barracks of the Thousand", "LCI"), 1);
        rewardPool.add(getCard("Throne of the God-Pharaoh", "AKH"), 1);
        rewardPool.add(getCard("Triplicate Titan", "C21"), 1);
        rewardPool.add(getCard("Unbreakable Formation", "MOC"), 1);
        rewardPool.add(getCard("Vulshok Factory", "ONC"), 1);
        rewardPool.add(getCard("Wake the Past", "C21"), 1);
        rewardPool.add(getCard("Weftstalker Ardent", "EOE"), 1);
        rewardPool.add(getCard("Welcoming Vampire", "TDC"), 1);
        rewardPool.add(getCard("Windbrisk Heights", "ONC"), 1);
        rewardPool.add(getCard("Windcrag Siege", "TDM"), 1);


        deck.setRewardPool(rewardPool);
        return deck;
    }

    // ========== ROGUE DECK 2: OMO, QUEEN OF VESUVIA ==========
    private static RogueDeckData createOmoDeck() {
        RogueDeckData deck = new RogueDeckData();
        deck.setName("Omo, Queen of Vesuva");
        deck.setCommanderCardName("Omo, Queen of Vesuva");
        deck.setDescription("Ramp with powerful lands and smash your opponents with mighty tribal lords.");
        deck.setThemeDescription("Land Types and Creature Types Matter - Simic Ramp / Tribal");

        // Start Deck (40 cards)
        Deck startDeck = new Deck("Omo Start Deck");

        // Commander
        startDeck.getOrCreate(DeckSection.Commander).add(getCard("Omo, Queen of Vesuva", "M3C"), 1);

        startDeck.getMain().add(getCard("Aven Courier", "NCC"), 1);
        startDeck.getMain().add(getCard("Basilisk Gate", "M3C"), 1);
        startDeck.getMain().add(getCard("Beast Within", "M3C"), 1);
        startDeck.getMain().add(getCard("Cloudpost", "M3C"), 1);
        startDeck.getMain().add(getCard("Command Tower", "M3C"), 1);
        startDeck.getMain().add(getCard("Elvish Archdruid", "KHC"), 1);
        startDeck.getMain().add(getCard("Elvish Champion", "8ED"), 1);
        startDeck.getMain().add(getCard("Elvish Rejuvenator", "M3C"), 1);
        startDeck.getMain().add(getCard("Forest", "MH3"), 2);
        startDeck.getMain().add(getCard("Forest", "LCI"), 2);
        startDeck.getMain().add(getCard("Galerider Sliver", "CMM"), 1);
        startDeck.getMain().add(getCard("Growth Spiral", "LCC"), 1);
        startDeck.getMain().add(getCard("Harmonize", "M3C"), 1);
        startDeck.getMain().add(getCard("Hashep Oasis", "M3C"), 1);
        startDeck.getMain().add(getCard("Heap Gate", "CLB"), 1);
        startDeck.getMain().add(getCard("Herald of Hoofbeats", "MOC"), 1);
        startDeck.getMain().add(getCard("Hinterland Harbor", "LCC"), 1);
        startDeck.getMain().add(getCard("Island", "MH3"), 1);
        startDeck.getMain().add(getCard("Island", "LCI"), 3);
        startDeck.getMain().add(getCard("Llanowar Scout", "DOM"), 1);
        startDeck.getMain().add(getCard("Lumbering Falls", "M3C"), 1);
        startDeck.getMain().add(getCard("Magus of the Candelabra", "M3C"), 1);
        startDeck.getMain().add(getCard("Master of the Pearl Trident", "LCC"), 1);
        startDeck.getMain().add(getCard("Merfolk Sovereign", "LCC"), 1);
        startDeck.getMain().add(getCard("Pheres-Band Warchief", "JOU"), 1);
        startDeck.getMain().add(getCard("Pongify", "M3C"), 1);
        startDeck.getMain().add(getCard("Priest of Titania", "MH3"), 1);
        startDeck.getMain().add(getCard("Quandrix Campus", "M3C"), 1);
        startDeck.getMain().add(getCard("Rapid Hybridization", "LCC"), 1);
        startDeck.getMain().add(getCard("Ravenform", "LCC"), 1);
        startDeck.getMain().add(getCard("Sage of the Maze", "M3C"), 1);
        startDeck.getMain().add(getCard("Seafloor Oracle", "LCC"), 1);
        startDeck.getMain().add(getCard("Seer's Sundial", "M3C"), 1);
        startDeck.getMain().add(getCard("Simic Growth Chamber", "M3C"), 1);
        startDeck.getMain().add(getCard("Tatyova, Benthic Druid", "M3C"), 1);
        startDeck.getMain().add(getCard("Urban Evolution", "M3C"), 1);
        startDeck.getMain().add(getCard("Urza's Mine", "M3C"), 1);


        deck.setStartDeck(startDeck);

        // Reward Pool (~60 cards for MVP)
        CardPool rewardPool = new CardPool();

        rewardPool.add(getCard("Aggressive Biomancy", "M3C"), 1);
        rewardPool.add(getCard("Alchemist's Refuge", "LCC"), 1);
        rewardPool.add(getCard("Arcane Denial", "M3C"), 1);
        rewardPool.add(getCard("Azami, Lady of Scrolls", "CMM"), 1);
        rewardPool.add(getCard("Azusa, Lost but Seeking", "CMM"), 1);
        rewardPool.add(getCard("Baldur's Gate", "CLB"), 1);
        rewardPool.add(getCard("Baru, Wurmspeaker", "DMC"), 1);
        rewardPool.add(getCard("Blast Zone", "M3C"), 1);
        rewardPool.add(getCard("Call to the Kindred", "DKA"), 1);
        rewardPool.add(getCard("Case of the Locked Hothouse", "PMKM"), 1);
        rewardPool.add(getCard("Chromatic Lantern", "M3C"), 1);
        rewardPool.add(getCard("Cold-Eyed Selkie", "LCC"), 1);
        rewardPool.add(getCard("Copy Land", "M3C"), 1);
        rewardPool.add(getCard("Crop Rotation", "2XM"), 1);
        rewardPool.add(getCard("Curse of the Swine", "M3C"), 1);
        rewardPool.add(getCard("Cyclone Summoner", "PKHM"), 1);
        rewardPool.add(getCard("Dazzling Denial", "BLB"), 1);
        rewardPool.add(getCard("Deeproot Pilgrimage", "LCI"), 1);
        rewardPool.add(getCard("Descendants' Path", "LCC"), 1);
        rewardPool.add(getCard("Desert Warfare", "M3C"), 1);
        rewardPool.add(getCard("Deserted Temple", "MH3"), 1);
        rewardPool.add(getCard("Diffusion Sliver", "M15"), 1);
        rewardPool.add(getCard("Dreamroot Cascade", "M3C"), 1);
        rewardPool.add(getCard("Drown in Dreams", "M3C"), 1);
        rewardPool.add(getCard("Dryad of the Ilysian Grove", "M3C"), 1);
        rewardPool.add(getCard("Endless Evil", "CLB"), 1);
        rewardPool.add(getCard("Eureka Moment", "M3C"), 1);
        rewardPool.add(getCard("Evacuation", "M3C"), 1);
        rewardPool.add(getCard("Evasive Action", "DDE"), 1);
        rewardPool.add(getCard("Expedition Map", "M3C"), 1);
        rewardPool.add(getCard("Feline Sovereign", "PM21"), 1);
        rewardPool.add(getCard("Floriferous Vinewall", "M3C"), 1);
        rewardPool.add(getCard("Full Moon's Rise", "ISD"), 1);
        rewardPool.add(getCard("Gemhide Sliver", "PLST"), 1);
        rewardPool.add(getCard("Genesis Wave", "FDN"), 1);
        rewardPool.add(getCard("Glimmerpost", "M3C"), 1);
        rewardPool.add(getCard("Harabaz Druid", "WWK"), 1);
        rewardPool.add(getCard("Harmonize", "M3C"), 1);
        rewardPool.add(getCard("Hidden Cataract", "M3C"), 1);
        rewardPool.add(getCard("Hidden Nursery", "M3C"), 1);
        rewardPool.add(getCard("Horizon of Progress", "M3C"), 1);
        rewardPool.add(getCard("Hour of Promise", "M3C"), 1);
        rewardPool.add(getCard("Hydra Broodmaster", "M3C"), 1);
        rewardPool.add(getCard("Hydroid Krasis", "M3C"), 1);
        rewardPool.add(getCard("Kiora's Follower", "LCC"), 1);
        rewardPool.add(getCard("Kopala, Warden of Waves", "LCC"), 1);
        rewardPool.add(getCard("Lair of the Hydra", "M3C"), 1);
        rewardPool.add(getCard("Lazotep Quarry", "M3C"), 1);
        rewardPool.add(getCard("Lord of the Unreal", "J25"), 1);
        rewardPool.add(getCard("Mana Reflection", "M3C"), 1);
        rewardPool.add(getCard("Manascape Refractor", "C20"), 1);
        rewardPool.add(getCard("March from Velis Vel", "M3C"), 1);
        rewardPool.add(getCard("Maskwood Nexus", "M3C"), 1);
        rewardPool.add(getCard("Mass Appeal", "AVR"), 1);
        rewardPool.add(getCard("Mirage Mirror", "M3C"), 1);
        rewardPool.add(getCard("Monument to Perfection", "ONE"), 1);
        rewardPool.add(getCard("Mosswort Bridge", "LCC"), 1);
        rewardPool.add(getCard("Multiclass Baldric", "CLB"), 1);
        rewardPool.add(getCard("Myriad Landscape", "LCC"), 1);
        rewardPool.add(getCard("Nesting Grounds", "MH3"), 1);
        rewardPool.add(getCard("Oracle of Mul Daya", "EOC"), 1);
        rewardPool.add(getCard("Path of Ancestry", "LCC"), 1);
        rewardPool.add(getCard("Planar Nexus", "M3C"), 1);
        rewardPool.add(getCard("Raise the Palisade", "LTC"), 1);
        rewardPool.add(getCard("Ramunap Excavator", "M3C"), 1);
        rewardPool.add(getCard("Reliquary Tower", "LCC"), 1);
        rewardPool.add(getCard("Replication Technique", "M3C"), 1);
        rewardPool.add(getCard("Satyr Wayfinder", "M3C"), 1);
        rewardPool.add(getCard("Scion of Oona", "LRW"), 1);
        rewardPool.add(getCard("Scute Swarm", "M3C"), 1);
        rewardPool.add(getCard("Sea Gate Loremaster", "ZEN"), 1);
        rewardPool.add(getCard("Seshiro the Anointed", "PLST"), 1);
        rewardPool.add(getCard("Shifting Sliver", "CMM"), 1);
        rewardPool.add(getCard("Sol Ring", "LCC"), 1);
        rewardPool.add(getCard("Spelunking", "LCI"), 1);
        rewardPool.add(getCard("Summary Dismissal", "M3C"), 1);
        rewardPool.add(getCard("Summon: Leviathan", "FIN"), 1);
        rewardPool.add(getCard("Sunken Palace", "M3C"), 1);
        rewardPool.add(getCard("Swarmyard", "BLC"), 1);
        rewardPool.add(getCard("Swiftfoot Boots", "LCC"), 1);
        rewardPool.add(getCard("Sylvan Scrying", "M3C"), 1);
        rewardPool.add(getCard("Talon Gates of Madara", "M3C"), 1);
        rewardPool.add(getCard("Temple of Mystery", "M3C"), 1);
        rewardPool.add(getCard("Terastodon", "M3C"), 1);
        rewardPool.add(getCard("Thespian's Stage", "M3C"), 1);
        rewardPool.add(getCard("Thrumming Hivepool", "EOE"), 1);
        rewardPool.add(getCard("Timber Protector", "LRW"), 1);
        rewardPool.add(getCard("Treasure Cruise", "M3C"), 1);
        rewardPool.add(getCard("Trenchpost", "M3C"), 1);
        rewardPool.add(getCard("Urza's Power Plant", "M3C"), 1);
        rewardPool.add(getCard("Urza's Tower", "M3C"), 1);
        rewardPool.add(getCard("Vesuva", "M3C"), 1);
        rewardPool.add(getCard("Volatile Fault", "M3C"), 1);
        rewardPool.add(getCard("Whelming Wave", "CMR"), 1);
        rewardPool.add(getCard("Wonderscape Sage", "M3C"), 1);
        rewardPool.add(getCard("Yavimaya, Cradle of Growth", "M3C"), 1);


        deck.setRewardPool(rewardPool);
        return deck;
    }

    // Helper method to get cards from the database
    private static PaperCard getCard(String cardName, String setCode) {
        PaperCard card = db.getCommonCards().getCard(cardName, setCode);
        if (card == null) {
            System.err.println("Warning: Card not found: " + cardName);
            // Return a basic land as fallback
            return db.getCommonCards().getCard("Plain");
        }
        return card;
    }
}
