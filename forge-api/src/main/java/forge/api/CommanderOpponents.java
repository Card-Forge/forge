package forge.api;

import forge.StaticData;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;

/** Small, fixed singleton lists: the AI uses the same Commander rules as the human. */
final class CommanderOpponents {
    static Deck create(String key) {
        boolean green = switch (key) {
            case "green" -> true;
            case "red" -> false;
            default -> throw new IllegalArgumentException("Unknown opponent");
        };
        String commander = green ? "Goreclaw, Terror of Qal Sisma" : "Torbran, Thane of Red Fell";
        String spells = green ? """
                Llanowar Elves
                Elvish Mystic
                Fyndhorn Elves
                Arbor Elf
                Elvish Visionary
                Llanowar Visionary
                Leaf Gilder
                Paradise Druid
                Rampant Growth
                Cultivate
                Kodama's Reach
                Harrow
                Explosive Vegetation
                Migration Path
                Nature's Lore
                Sakura-Tribe Elder
                Wood Elves
                Farhaven Elf
                Springbloom Druid
                Grizzly Bears
                Runeclaw Bear
                Centaur Courser
                Nessian Courser
                Rumbling Baloth
                Colossal Dreadmaw
                Sentinel Spider
                Vastwood Gorger
                Craw Wurm
                Voracious Wurm
                Nessian Asp
                Terra Stomper
                Pelakka Wurm
                Thragtusk
                Ravenous Baloth
                Garruk's Packleader
                Garruk's Companion
                Beast Whisperer
                Reclamation Sage
                Acidic Slime
                Thrashing Brontodon
                Briarpack Alpha
                Silverback Shaman
                Greater Sandwurm
                Honey Mammoth
                Colossal Majesty
                Harmonize
                Hunter's Insight
                Return of the Wildspeaker
                Overrun
                Overwhelming Stampede
                Giant Growth
                Titanic Growth
                Blossoming Defense
                Ranger's Guile
                Beast Within
                Naturalize
                Return to Nature
                Sol Ring
                Swiftfoot Boots
                """ : """
                Monastery Swiftspear
                Ghitu Lavarunner
                Goblin Arsonist
                Borderland Marauder
                Viashino Pyromancer
                Goblin Instigator
                Krenko's Command
                Dragon Fodder
                Hordeling Outburst
                Mogg War Marshal
                Goblin Chieftain
                Goblin Warchief
                Goblin King
                Hobgoblin Bandit Lord
                Battle Cry Goblin
                Krenko, Tin Street Kingpin
                Beetleback Chief
                Siege-Gang Commander
                Goblin Trashmaster
                Goblin Cratermaker
                Goblin Chainwhirler
                Reckless Bushwhacker
                Goblin Bushwhacker
                Fanatical Firebrand
                Raging Goblin
                Frenzied Goblin
                Foundry Street Denizen
                Legion Warboss
                Chandra's Spitfire
                Guttersnipe
                Thermo-Alchemist
                Firebrand Archer
                Flames of the Firebrand
                Lightning Bolt
                Shock
                Lightning Strike
                Incinerate
                Searing Spear
                Magma Jet
                Abrade
                Flame Slash
                Roast
                Lava Coil
                Chandra's Pyrohelix
                Fireball
                Volcanic Geyser
                Flametongue Kavu
                Flametongue Yearling
                Inferno Titan
                Tormenting Voice
                Thrill of Possibility
                Cathartic Reunion
                Light Up the Stage
                Outpost Siege
                Sol Ring
                Mind Stone
                Fire Diamond
                Swiftfoot Boots
                Worn Powerstone
                """;
        Deck deck = new Deck(green ? "Verdant Commander" : "Cinder Commander");
        add(deck, DeckSection.Commander, commander, 1);
        add(deck, DeckSection.Main, green ? "Forest" : "Mountain", 40);
        spells.lines().filter(line -> !line.isBlank()).forEach(name -> add(deck, DeckSection.Main, name.strip(), 1));
        String problem = DeckFormat.Commander.getDeckConformanceProblem(deck);
        if (problem != null) throw new IllegalStateException("AI Commander deck " + deck.getName() + ": " + problem);
        return deck;
    }

    private static void add(Deck deck, DeckSection section, String name, int count) {
        var card = StaticData.instance().getCommonCards().getCard(name);
        if (card == null) throw new IllegalStateException("Missing opponent card: " + name);
        deck.getOrCreate(section).add(card, count);
    }
}
