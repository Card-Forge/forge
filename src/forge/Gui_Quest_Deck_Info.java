package forge;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.util.TreeMap;

public class Gui_Quest_Deck_Info implements NewConstants {

    static TreeMap<String, DeckInfo> nameDeckMap = new TreeMap<String, DeckInfo>();

    static {
        buildDeckList();
    }

    private static void buildDeckList() {
        //TODO: Build this list dynamically from the deck files.

        addToDeckList("Abraham Lincoln 3", "hard", "WUR flying creatures with Flamebreak and Earthquake");
        addToDeckList("Albert Einstein 2", "medium", "Garruk Wildspeaker, W+G creatures with Needle Storm and Retribution of the Meek");
        addToDeckList("Albert Einstein 3", "hard", "Garruk Wildspeaker, W+G creatures with Needle Storm and Retribution of the Meek");
        addToDeckList("Aragorn 2", "medium", "WBRG Landfall deck");
        addToDeckList("Bamm Bamm Rubble 1", "easy", "WUBRG domain deck, creatures and spells with the Domain ability");
        addToDeckList("Barney Rubble 1", "easy", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Barney Rubble 2", "medium", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Barney Rubble 3", "hard", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Bart Simpson 1", "easy", "BUG creatures that will tap your creatures and will use auras to keep them tapped");
        addToDeckList("Bart Simpson 2", "medium", "WUG creatures that will tap your creatures and will use auras to keep them tapped");
        addToDeckList("Bart Simpson 3", "hard", "WUG creatures that will tap your creatures and will use auras to keep them tapped");
        addToDeckList("Batman 3", "hard", "Creatures with Exalted and Unblockable abilities, WoG and Armageddon");
        addToDeckList("Bela Lugosi 3", "hard", "Rares' Vampire deck, B creatures, little to no spells");
        addToDeckList("Betty Rubble 3", "hard", "Indicatie's Summer Bloom deck with mods, features Plant + Eldrazi Spawn tokens");
        addToDeckList("Blackbeard 3", "hard", "W Soldiers with Preeminent Captain, Captain of the Watch and Daru Warchief");
        addToDeckList("Boba Fett 3", "hard", "Dragons, Chandra Nalaar, Crucible of Fire and Dragon Roost");
        addToDeckList("Boris Karloff 3", "hard", "LokiUndergod's Boros Aggro (RW) deck with mods, Kors, levelers and threat removal");
        addToDeckList("Boromir 2", "medium", "Elvish Piper and Quicksilver Amulet with huge creatures");
        addToDeckList("Boromir 3", "hard", "Elvish Piper and Quicksilver Amulet with huge creatures");
        addToDeckList("Buffy 1", "easy", "Vampires and creatures with wither + Sorceress Queen");
        addToDeckList("Buffy 2", "medium", "Vampires and creatures with wither + Sorceress Queen");
        addToDeckList("Buffy 3", "hard", "Vampires and creatures with wither + Sorceress Queen");
        addToDeckList("C3PO 1", "easy", "BR Goblins, Goblin Ringleader, Mad Auntie and Sensation Gorger");
        addToDeckList("C3PO 2", "medium", "BR Goblins, Goblin Ringleader, Kiki-Jiki, Mad Auntie and Sensation Gorger");
        addToDeckList("C3PO 3", "hard", "BR Goblins, Goblin Ringleader, Kiki-Jiki, Mad Auntie and Sensation Gorger");
        addToDeckList("Catwoman 1", "easy", "Cat creatures G+W");
        addToDeckList("Catwoman 2", "medium", "Cats creatures G+W+R with Lightning Helix");
        addToDeckList("Comic Book Guy 3", "hard", "Roc and Rukh Eggs, Flamebrake, Earthquake, Auriok Champion, Kor Firewalker");
        addToDeckList("Crocodile Dundee 1", "easy", "Mono red deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Crocodile Dundee 2", "medium", "Mono red deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Crocodile Dundee 3", "hard", "Mono red deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Cyclops 3", "hard", "Slivers mainly, some spells");
        addToDeckList("Da Vinci 1", "easy", "Mono black deck, Ashenmoor Cohort + Badmoon + some Fear");
        addToDeckList("Da Vinci 2", "medium", "Mono black deck, Korlash, Heir to Blackblade's + Badmoon + threat removal");
        addToDeckList("Da Vinci 3", "hard", "Mono black deck, Korlash, Heir to Blackblade's + Badmoon + threat removal");
        addToDeckList("Darrin Stephens 1", "easy", "U Affinity deck, Affinity for artifacts and Modular cards");
        addToDeckList("Darrin Stephens 2", "medium", "U Affinity deck, Affinity for artifacts and Modular cards");
        addToDeckList("Darrin Stephens 3", "hard", "U Affinity deck, Affinity for artifacts and Modular cards");
        addToDeckList("Darth Vader 3", "hard", "UW Battle of Wits style alternate win type deck, WoG");
        addToDeckList("Data 3", "hard", "Korlash, Heir to Blackblade, Liliana Vess");
        addToDeckList("Dino 2", "medium", "Mono brown affinity deck, Affinity for artifacts and Modular cards");
        addToDeckList("Dino 3", "hard", "Mono brown affinity deck, Affinity for artifacts and Modular cards");
        addToDeckList("Doc Holiday 1", "easy", "Morph + Regenerate GWU creatures");
        addToDeckList("Doc Holiday 2", "medium", "Morph + Regenerate GWU creatures");
        addToDeckList("Doc Holiday 3", "hard", "Morph + Regenerate GWU creatures");
        addToDeckList("Doran 3", "hard", "WBG Doran, the Siege Tower deck with high toughness creatures");
        addToDeckList("Dr No 3", "hard", "The Rack, Balance, Propaganda, discard spells");
        addToDeckList("Electro 3", "hard", "Resonantg's Stormfront deck with mods, Arashi, the Sky Asunder + Ball Lightning");
        addToDeckList("Elrond 2", "medium", "Aura Gnarlid, Rabid Wombat and Uril with lots of auras");
        addToDeckList("Endora 2", "medium", "Enchantress deck, enchantments + cards with enchantment effects");
        addToDeckList("Endora 3", "hard", "Enchantress deck, enchantments + cards with enchantment effects");
        addToDeckList("Fat Albert 1", "easy", "Winter Orb, Keldon Warlord, mana Elves/Slivers + several 4/4 creatures");
        addToDeckList("Fat Albert 2", "medium", "Winter Orb, Keldon Warlord, mana Elves/Slivers + several 5/5 creatures");
        addToDeckList("Fat Albert 3", "hard", "Winter Orb, Keldon Warlord, mana Elves/Slivers + several 6/6 creatures");
        addToDeckList("Fred Flintstone 3", "hard", "Reasontg's Predator's Garden deck with mods, featuring Lorescale Coatl");
        addToDeckList("Frodo 1", "easy", "New, Apthaven's AI Zoo Easy, some creature removal");
        addToDeckList("Frodo 2", "medium", "New, Apthaven's AI Zoo Medium, some creature removal + Glorious Anthem");
        addToDeckList("Frodo 3", "hard", "New, Apthaven's AI Zoo Hard, more creature removal + Glorious Anthems");
        addToDeckList("Galadriel 2", "medium", "Amulet of Vigor, green mana ramp, time vault and Howl of the Night Pack");
        addToDeckList("Galahad 1", "easy", "Apthaven's knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Galahad 2", "medium", "Apthaven's knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Galahad 3", "hard", "Apthaven's knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Genghis Khan 1", "easy", "Mana Elves + Birds + Armageddon, Llanowar Behemoth");
        addToDeckList("Genghis Khan 2", "medium", "Mana Elves + Birds + Armageddon, Llanowar Behemoth");
        addToDeckList("Genghis Khan 3", "hard", "Mana Elves + Birds + Armageddon, Llanowar Behemoth + Elspeth, Knight-Errant");
        addToDeckList("George of the Jungle 1", "easy", "Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + a few RW spells");
        addToDeckList("George of the Jungle 2", "medium", "Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + some RW spells");
        addToDeckList("George of the Jungle 3", "hard", "Belligerent Hatchling, Battlegate Mimic, Ajani Vengeant + many RW spells");
        addToDeckList("Gimli 2", "medium", "Indestructible permanents with lots of mass removal");
        addToDeckList("Gold Finger 3", "hard", "Rares' U control deck, various counter spells and Serra Sphinx + Memnarch");
        addToDeckList("Grampa Simpson 1", "easy", "WR double strike deck, various equipments and auras");
        addToDeckList("Grampa Simpson 2", "medium", "WR double strike deck, various equipments and auras");
        addToDeckList("Grampa Simpson 3", "hard", "WR double strike deck, various equipments and auras");
        addToDeckList("Green Lantern 3", "hard", "Nicol Bolas, Planeswalker + threat removal and several creatures");
        addToDeckList("Han Solo 3", "hard", "WG enchantments deck with Sigil of the Empty Throne");
        addToDeckList("Harry Potter 1", "easy", "Sloth' deck, easier version of Beached As' mill and counter spell deck");
        addToDeckList("Harry Potter 2", "medium", "Sloth' deck, easier version of Beached As' mill and counter spell deck");
        addToDeckList("Harry Potter 3", "hard", "Beached As' deck, various milling cards, some speed up and counter spells");
        addToDeckList("Hellboy 3", "hard", "A BR direct damage deck");
        addToDeckList("Higgins 3", "hard", "Corwin72's Grixis Control deck, lots of threat removal and some creatures");
        addToDeckList("Homer Simpson 1", "easy", "Morph + Regenerate BRU creatures, + Raise Dead");
        addToDeckList("Homer Simpson 2", "medium", "Morph + Regenerate BRU creatures, + Raise Dead");
        addToDeckList("Homer Simpson 3", "hard", "Morph + Regenerate BRU creatures, + card draw and creature buff");
        addToDeckList("Iceman 3", "hard", "BU Bounce and Control style deck");
        addToDeckList("Indiana Jones 1", "easy", "Sol'kanar + buff");
        addToDeckList("Indiana Jones 2", "medium", "Sol'kanar + buff + Raise Dead");
        addToDeckList("Indiana Jones 3", "hard", "Sol'kanar + buff + Terminate");
        addToDeckList("Jabba the Hut 3", "hard", "Creatures with exalted and land walking abilities");
        addToDeckList("Jack Sparrow 1", "easy", "Pirate type creatures + draw cards + counter spells");
        addToDeckList("Jack Sparrow 2", "medium", "Pirate type creatures + draw cards + threat removal");
        addToDeckList("Jack Sparrow 3", "hard", "Pirate type creatures + draw cards + creature control");
        addToDeckList("James Bond 1", "easy", "gohongohon's easy WG Agro with several Slivers");
        addToDeckList("James Bond 2", "medium", "gohongohon's Medium WG Agro with several Slivers + Glorious Anthem");
        addToDeckList("James Bond 3", "hard", "gohongohon's Hard WGR Agro");
        addToDeckList("James T Kirk 3", "hard", "Rares 40 card black discard deck + Liliana Vess");
        addToDeckList("Joe Kidd 1", "easy", "Voracious Hatchling, Nightsky Mimic, no planeswalkers + a few WB spells");
        addToDeckList("Joe Kidd 2", "medium", "Voracious Hatchling, Nightsky Mimic, no planeswalkers + some WB spells");
        addToDeckList("Joe Kidd 3", "hard", "Voracious Hatchling, Nightsky Mimic, no planeswalkers + many WB spells");
        addToDeckList("King Arthur 1", "easy", "Wilt-Leaf Cavaliers; Knight of the Skyward Eye and Leonin Skyhunter");
        addToDeckList("King Arthur 2", "medium", "Wilt-Leaf Cavaliers; Knights with flanking");
        addToDeckList("King Arthur 3", "hard", "Sir Shandlar of Eberyn; Knights with first strike");
        addToDeckList("King Edward 1", "easy", "Elementals, 5 color deck with Tribal Flames");
        addToDeckList("King Edward 2", "medium", "Elementals, 5 color deck with Tribal Flames");
        addToDeckList("King Edward 3", "hard", "Elementals, 5 color deck with Tribal Flames featuring Horde of Notions");
        addToDeckList("King Kong 1", "easy", "Squirrel tokens, changelings and Deranged Hermit + curse type auras");
        addToDeckList("King Kong 2", "medium", "Squirrel tokens, changelings and Deranged Hermit + curse type auras");
        addToDeckList("King Kong 3", "hard", "Squirrel tokens, changelings and Deranged Hermit + threat removal");
        addToDeckList("Kojak 1", "easy", "Mono blue deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Kojak 2", "medium", "Mono blue deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Kojak 3", "hard", "Mono blue deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Lisa Simpson 3", "hard", "GW deck, creates tokens which are devoured by Skullmulcher and Gluttonous Slime");
        addToDeckList("Luke Skywalker 3", "hard", "GWU weenie style deck with Garruk Wildspeaker and Gaea's Anthem");
        addToDeckList("Maggie Simpson 3", "hard", "This is a jund deck from the deck forum with some modifications");
        addToDeckList("Magneto 3", "hard", "Shriekmaw, Assassins, creature removal + Liliana Vess");
        addToDeckList("Magnum 1", "easy", "Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        addToDeckList("Magnum 2", "medium", "Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        addToDeckList("Magnum 3", "hard", "Sturdy Hatchling, Shorecrasher Mimic, Garruk & Jace, the Mind Sculptor + GU spells");
        addToDeckList("Marge Simpson 3 ", "hard", "RG deck, creates tokens which are devoured by R and RG creatures with devour");
        addToDeckList("Morpheus 3", "hard", "Elves with Overrun, Gaea's Anthem, Imperious Perfect and other pumps");
        addToDeckList("Mr Slate 2", "medium", "Corwin72's Don't Go in the Water deck with mods, Merfolk and Merfolk pumps");
        addToDeckList("Mr Slate 3", "hard", "Corwin72's Don't Go in the Water deck with mods, Merfolk and Merfolk pumps");
        addToDeckList("Napoleon 3", "hard", "Walls, Rolling Stones and Doran, the Siege Tower");
        addToDeckList("Ned Flanders 1", "easy", "B reanimator deck, a few large creatures and some spells");
        addToDeckList("Ned Flanders 2", "medium", "B reanimator deck, a few large creatures and some spells");
        addToDeckList("Ned Flanders 3", "hard", "B reanimator deck, a few large creatures and some spells");
        addToDeckList("Neo 3", "hard", "RG with Groundbreaker and other attack once then sacrifice at EoT creatures");
        addToDeckList("Newton 3", "hard", "Relentless Rats, Ratcatcher, Aluren and Harmonize");
        addToDeckList("Pebbles Flintstone 2", "medium", "WU Meekstone deck, Meekstone, Marble Titan and creatures with vigilance");
        addToDeckList("Pebbles Flintstone 3", "hard", "WU Meekstone deck, Meekstone, Marble Titan and creatures with vigilance");
        addToDeckList("Picard 3", "hard", "UWG Elf deck similar to Morpheus but also has flying elves");
        addToDeckList("Pinky and the Brain 3", "hard", "Royal Assassin, WoG + Damnation, Liliana Vess, Beacon of Unrest");
        addToDeckList("Professor X 3", "hard", "Master of Etherium + Vedalken Archmage and many artifacts");
        addToDeckList("R2-D2 3", "hard", "Black Vise, bounce (Boomerang) spells, Howling Mine");
        addToDeckList("Radagast 2", "medium ", "Muraganda Petroglyphs, green vanilla creatures and a few tokens");
        addToDeckList("Radiant 3", "medium ", "Flying Creatures with Radiant, Archangel, Gravitational Shift and Moat");
        addToDeckList("Rocky 1", "easy", "Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        addToDeckList("Rocky 2", "medium", "Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        addToDeckList("Rocky 3", "hard", "Pro red, Flamebreak + Tremor + Pyroclasm but no Pyrohemia");
        addToDeckList("Rogue 3", "hard", "Dragons including Tarox Bladewing, Dragon Roost, Chandra Nalaar");
        addToDeckList("Samantha Stephens 1", "easy", "WU Painter's Servant anti-red deck");
        addToDeckList("Samantha Stephens 2", "medium", "WU Painter's Servant anti-red deck");
        addToDeckList("Samantha Stephens 3", "hard", "WU Painter's Servant anti-red deck with Grindstone combo");
        addToDeckList("Saruman 2", "medium", "Discard deck with Megrim, Liliana's Caress and Burning Inquiry ");
        addToDeckList("Saruman 3", "hard", "Discard deck with Megrim, Liliana's Caress and Burning Inquiry ");
        addToDeckList("Sauron 2", "medium", "Black Vise and Underworld Dreams with lots of card draw for both players");
        addToDeckList("Scooby Doo 3", "hard", "Rares' Red deck, Dragonmaster Outcast, Rakdos Pit Dragon, Kamahl, Pit Fighter");
        addToDeckList("Scotty 2", "medium", "Pestilence + Castle + Penumbra Kavu/Spider/Wurm but no pro black");
        addToDeckList("Seabiscuit 1", "easy", "Some Fear creatures, bounce and draw card spells");
        addToDeckList("Seabiscuit 2", "medium", "Some Fear creatures, Garza Zol, Plague Queen + draw card spells");
        addToDeckList("Seabiscuit 3", "hard", "Some Fear creatures, Garza Zol, Plague Queen + draw card & control spells");
        addToDeckList("Secret Squirrel 3", "hard", "Dennis' squirrel deck, Squirrel Mob + Deranged Hermit + Coat of Arms + Nut Collector");
        addToDeckList("Sherlock Holmes 1", "easy", "Mono green deck, Baru, Fist of Krosa + land fetch + some buff cards.");
        addToDeckList("Sherlock Holmes 2", "medium", "Mono green deck, Baru, Fist of Krosa + lots of good green creatures.");
        addToDeckList("Sherlock Holmes 3", "hard", "Mono green deck, Baru, Fist of Krosa + lots of great green creatures.");
        addToDeckList("Silver Surfer 3", "hard", "Green creature beat down deck with several pump spells");
        addToDeckList("Spiderman 2", "medium", "White weenies with WoG, Armageddon, Mass Calcify");
        addToDeckList("Spock 2", "medium", "Rares elf deck with just a single copy of most of the elves");
        addToDeckList("Storm 1", "easy", "Creatures with Lifelink + filler");
        addToDeckList("Storm 2", "medium", "Creatures with Lifelink + filler");
        addToDeckList("Storm 3", "hard", "Creatures with Lifelink + filler");
        addToDeckList("Superman 1", "easy", "Vecc\'s easy Slivers deck, Raise Dead + Breath of Life");
        addToDeckList("Superman 2", "medium", "Vecc\'s medium Slivers deck, Zombify + Tribal Flames");
        addToDeckList("Tarzan 1", "easy", "Jungle creatures + pump spells");
        addToDeckList("Tarzan 2", "medium", "Tarzan with Silverback Ape + pump spells");
        addToDeckList("Terminator 3", "hard", "Master of Etherium + Control Magic and Memnarch + many artifacts");
        addToDeckList("The Great Gazoo 3", "hard", "Sloth's Sun Lotion deck, red damage all spells and pro from red creatures");
        addToDeckList("Totoro 2", "medium", "Blue, black, green deck with spirits and arcane spells");
        addToDeckList("Treebeard 1", "easy", "Treefolk creatures, a lumberjack's dream. Bosk Banneret, Dauntless Dourbark, Leaf-Crowned Elder");
        addToDeckList("Treebeard 2", "medium", "Treefolk creatures. Bosk Banneret, Dauntless Dourbark, Timber Protector, Leaf-Crowned Elder, Doran");
        addToDeckList("Treebeard 3", "hard", "Treefolk creatures. Bosk Banneret, Dauntless Dourbark, Timber Protector, Leaf-Crowned Elder, Doran");
        addToDeckList("Uncle Owen 3", "hard", "Creature removal/control with Liliana Vess");
        addToDeckList("Wilma Flintstone 1", "easy", "Noxious Hatchling, Woodlurker Mimic, Liliana Vess + a few BG spells");
        addToDeckList("Wilma Flintstone 2", "medium", "Noxious Hatchling, Woodlurker Mimic, Liliana Vess + some BG spells");
        addToDeckList("Wilma Flintstone 3", "hard", "Noxious Hatchling, Woodlurker Mimic, Liliana Vess + many BG spells");
        addToDeckList("Wolverine 3", "hard", "Nightmare + Korlash, Heir to Blackblade + Kodama's Reach");
        addToDeckList("Wyatt Earp 1", "easy", "Mono white deck, Crovax, Ascendant Hero + Crusade + small to medium sized creatures.");
        addToDeckList("Wyatt Earp 2", "medium", "Mono white deck, Crovax, Ascendant Hero + Crusade + small to medium sized creatures.");
        addToDeckList("Wyatt Earp 3", "hard", "Mono white deck, Crovax, Ascendant Hero + Honor of the Pure + small to medium sized creatures.");
    }

    private static void addToDeckList(String name, String difficulty, String description) {
        nameDeckMap.put(name, new DeckInfo(name, description, difficulty));
    }


    public static void showDeckList() {

        File base = ForgeProps.getFile(IMAGE_ICON);
        File file = new File(base, "notesIcon.png");
        ImageIcon icon = new ImageIcon(file.toString());

        Object[][] data  = new Object[nameDeckMap.size()][];
        Object[] headers = {"Name", "Difficulty", "Description"};
        
        int i = 0;
        for (DeckInfo deck : nameDeckMap.values()){
            data[i] = new Object[3];
            data[i][0] = deck.name;
            data[i][1] = deck.difficulty;
            data[i][2] = deck.description;
            i++;
        }

        JTable table = new JTable(){
			private static final long serialVersionUID = 4794007259716860046L;

			public TableCellRenderer getCellRenderer(int row, int column){
                TableCellRenderer renderer = new DefaultTableCellRenderer(){
					private static final long serialVersionUID = -901181777024884454L;

					public String getToolTipText(){
                        return this.getText();
                    }
                };
                return renderer;
            }
        };

        table.setModel(new DefaultTableModel(data, headers));
        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setMinWidth(150);
        tcm.getColumn(0).setMaxWidth(150);
        tcm.getColumn(1).setMinWidth(80);
        tcm.getColumn(1).setMaxWidth(80);

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scrollPane.setMinimumSize(new Dimension(700,500));
        scrollPane.setPreferredSize(new Dimension(700,500));
        JOptionPane.showMessageDialog(null, scrollPane, "Opponent Deck Notes", JOptionPane.INFORMATION_MESSAGE, icon);
    }

    public static String getDescription(String deckName) {
        if (nameDeckMap.containsKey(deckName)){
            return nameDeckMap.get(deckName).description;
        }

        else{
            System.out.println("Deck " +deckName+" does not have a description.");
            return "";
        }
    }


    private static class DeckInfo {
        String name;
        String difficulty;
        String description;

        private DeckInfo(String name, String description, String difficulty) {
            this.description = description;
            this.difficulty = difficulty;
            this.name = name;
        }
    }
}
