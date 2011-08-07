package forge.quest.gui.main;


import forge.gui.GuiUtils;
import forge.quest.data.QuestBattleManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * <p>QuestBattle class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class QuestBattle extends QuestSelectablePanel {
    /** Constant <code>serialVersionUID=3112668476017792084L</code> */
    private static final long serialVersionUID = 3112668476017792084L;

    /** Constant <code>nameDeckMap</code> */
    static TreeMap<String, DeckInfo> nameDeckMap = new TreeMap<String, DeckInfo>();

    String deckName;

    static {
        buildDeckList();
    }

    /**
     * <p>buildDeckList.</p>
     */
    private static void buildDeckList() {
        //TODO: Build this list dynamically from the deck files.

        addToDeckList("Abraham Lincoln 3", "hard", "WUR flying creatures deck with Flamebreak and Earthquake");
        addToDeckList("Albert Einstein 2", "medium", "WG deck with Garruk Wildspeaker, Needle Storm and Retribution of the Meek");
        addToDeckList("Albert Einstein 3", "hard", "WG deck with Garruk Wildspeaker, Needle Storm and Retribution of the Meek");
        addToDeckList("Alice in Wonderland 2", "medium", "WG Lands deck with manlands, cycle lands and Life from the Loam");
        addToDeckList("Alice in Wonderland 3", "hard", "WG Lands deck with manlands, cycle lands and Life from the Loam");
        addToDeckList("Apu Nahasapeemapetilon 2", "medium", "WG persist deck with Heartmender and Juniper Order Ranger");
        addToDeckList("Aquaman 1", "easy", "WU Control deck");
        addToDeckList("Aquaman 2", "medium", "WU Caw-Blade deck");
        addToDeckList("Aquaman 3", "hard", "WU Caw-Blade deck");
        addToDeckList("Aragorn 1", "medium", "WBRG Landfall deck");
        addToDeckList("Aragorn 2", "medium", "WUBRG Landfall deck");
//        addToDeckList("Ash 1", "easy", "WB Singleton deck");
//        addToDeckList("Ash 2", "medium", "WB Singleton deck");
//        addToDeckList("Ash 3", "hard", "WB Oversold Cemetery deck");
//        addToDeckList("Atomic Robo 1", "easy", "Artifact Singleton deck");
//        addToDeckList("Atomic Robo 2", "medium", "Artifact Singleton deck");
        addToDeckList("Atomic Robo 3", "hard", "UB artifact sacrifice deck with Thopter Foundry and Sword of the Meek");

        addToDeckList("Bamm Bamm Rubble 1", "easy", "WUBRG Domain deck");
        addToDeckList("Barney Rubble 1", "easy", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Barney Rubble 2", "medium", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Barney Rubble 3", "hard", "WU Sovereigns of Lost Alara deck with walls and auras");
        addToDeckList("Bart Simpson 1", "easy", "UBG deck with Rathi Trapper and Paralyzing Grasp");
        addToDeckList("Bart Simpson 2", "medium", "WUG deck with Minister of Impediments and Paralyzing Grasp");
        addToDeckList("Bart Simpson 3", "hard", "WUG deck with Harrier Griffin and Entangling Vines");
        addToDeckList("Batman 3", "hard", "RG Valakut Titan deck");
//        addToDeckList("Bear 1", "easy", "G Bear theme deck");
//        addToDeckList("Bear 2", "medium", "2/2s with abilities deck");
//        addToDeckList("Bear 3", "hard", "Token 2/2s, a lot of Token 2/2s deck");
//        addToDeckList("Beast 2", "medium", "GR Furnace Celebration deck");
//        addToDeckList("Beast 3", "hard", "UWB Standard Constructed deck");
        addToDeckList("Bela Lugosi 3", "hard", "Mono B Vampire Aggro deck");
        addToDeckList("Betty Rubble 3", "hard", "BRG Hand of Emrakul deck with Broodwarden and Eldrazi Spawn tokens");
        addToDeckList("Blackbeard 3", "hard", "Mono W Soldiers deck with Preeminent Captain, Captain of the Watch and Daru Warchief");
        addToDeckList("Boba Fett 3", "hard", "WRG Dragons deck with Chandra Nalaar, Crucible of Fire and Dragon Roost");
        addToDeckList("Boris Karloff 3", "hard", "WR Boros Aggro deck with Kors, levelers and threat removal");
        addToDeckList("Boromir 2", "medium", "Mono G Elvish Piper deck with Quicksilver Amulet and huge creatures");
        addToDeckList("Boromir 3", "hard", "Mono G Elvish Piper deck with Quicksilver Amulet and huge creatures");
        addToDeckList("Boromir 4", "very hard", "UG Show and Tell deck with huge creatures");
        addToDeckList("Bridge Troll 3", "hard", "Mono B Quest for the Nihil Stone deck with The Rack and Bottomless Pit");
//        addToDeckList("Brood 2", "medium", "W Battlecry deck");
        addToDeckList("Buffy 1", "easy", "Mono B Zombie deck");
        addToDeckList("Buffy 2", "medium", "Mono B Zombie deck");
        addToDeckList("Buffy 3", "hard", "Mono B Zombie deck");

        addToDeckList("C3PO 1", "easy", "BR Goblin deck with Goblin Ringleader, Mad Auntie and Sensation Gorger");
        addToDeckList("C3PO 2", "medium", "BR Goblin deck with Goblin Ringleader, Kiki-Jiki, Mad Auntie and Sensation Gorger");
        addToDeckList("C3PO 3", "hard", "BR Goblin deck with Goblin Ringleader, Kiki-Jiki, Mad Auntie and Sensation Gorger");
//        addToDeckList("Cable 2", "medium", "UR Artifact deck");
//        addToDeckList("Cable 3", "hard", "R Artifact deck deck");
        addToDeckList("Captain America 2", "medium", "Bant Exalted deck");
        addToDeckList("Captain America 3", "hard", "Bant Exalted deck");
        addToDeckList("Catwoman 1", "easy", "WG Cat deck with Kjeldoran War Cry");
        addToDeckList("Catwoman 2", "medium", "WRG Cat deck with Lightning Helix");
        addToDeckList("Cave Troll 3", "hard", "Mono B Quest for the Nihil Stone deck with The Rack, Bottomless Pit and Nyxathid");
//        addToDeckList("Colbert 2", "medium", "WG Cats deck");
//        addToDeckList("Colbert 3", "hard", "WUR Extended deck");
//        addToDeckList("Colossus 2", "medium", "RG Changeling deck");
//        addToDeckList("Colossus 3", "hard", "UG Standard Constructed deck");
        addToDeckList("Comic Book Guy 3", "hard", "WR deck with Roc and Rukh Eggs, Flamebrake, Earthquake and Auriok Champion");
        addToDeckList("Conan the Barbarian 1", "easy", "BRG Barbarian deck with Balthor the Stout and Spellgorger Barbarian");
        addToDeckList("Conan the Barbarian 2", "medium", "BRG Barbarian deck with Lovisa Coldeyes, Balthor the Stout and weapons");
        addToDeckList("Cow 1", "easy", "Mono R Minotaur deck");
//        addToDeckList("Conan 3", "hard", "Red monsters deck");
        addToDeckList("Crocodile Dundee 1", "easy", "Mono R deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Crocodile Dundee 2", "medium", "Mono R deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Crocodile Dundee 3", "hard", "Mono R deck with Mudbrawler Cohort and Bloodmark Mentor");
        addToDeckList("Cyclops 2", "medium", "WUBRG Sliver deck with four copies of almost every sliver");
        addToDeckList("Cyclops 3", "hard", "WUBRG Sliver deck with a few spells");

        addToDeckList("Da Vinci 1", "easy", "Mono B deck with Ashenmoor Cohort, Badmoon and some Fear");
        addToDeckList("Da Vinci 2", "medium", "Mono B deck with Korlash, Heir to Blackblade, Badmoon and threat removal");
        addToDeckList("Da Vinci 3", "hard", "Mono B deck with Korlash, Heir to Blackblade, Badmoon and threat removal");
//        addToDeckList("Darkseid 2", "medium", "B Sacrifice");
        addToDeckList("Darrin Stephens 1", "easy", "Mono U Affinity deck with Affinity for artifacts and Modular cards");
        addToDeckList("Darrin Stephens 2", "medium", "Mono U Affinity deck with Affinity for artifacts and Modular cards");
        addToDeckList("Darrin Stephens 3", "hard", "Mono U Affinity deck with Affinity for artifacts and Modular cards");
        addToDeckList("Darrin Stephens 4", "very hard", "Mono U Affinity deck");
        addToDeckList("Darth Vader 3", "hard", "WU Battle of Wits style alternate win type deck with WoG");
        addToDeckList("Data 3", "hard", "WB Swampwalk Karma deck with Contaminated Ground and Crusading Knight");
        addToDeckList("Deadpool 2", "medium", "BR deck with Ashenmoor Liege and Grixis Grimblade");
        addToDeckList("Deadpool 3", "hard", "BR deck with Ashenmoor Liege and Grixis Grimblade");
        addToDeckList("Demon 3", "hard", "Mono B Demon deck");
//        addToDeckList("Dick Grayson 3", "hard", "WU Constructed");
        addToDeckList("Dilbert 3", "hard", "BRG Ball Lightning deck with Smoke, Mighty Emergence and Kavu Lair");
        addToDeckList("Dino 1", "easy", "Mono brown artifact deck with Affinity for artifacts, Metalcraft and Modular cards");
        addToDeckList("Dino 2", "medium", "Mono brown affinity deck with Affinity for artifacts and Modular cards");
        addToDeckList("Dino 3", "hard", "Mono brown affinity deck with Affinity for artifacts and Modular cards");
        addToDeckList("Dino 4", "very hard", "Mono brown affinity deck with Affinity for artifacts, Metalcraft and Modular cards");
//        addToDeckList("Dinosaur 1", "easy", "GR Large Creatures");
//        addToDeckList("Dinosaur 2", "medium", "WGR Naya");
        addToDeckList("Doc Holiday 1", "easy", "WUG Morph deck");
        addToDeckList("Doc Holiday 2", "medium", "WUG deck with Sunscape Familiar and Bant Sureblade");
        addToDeckList("Doc Holiday 3", "hard", "WUG deck with Stoic Angel, Murkfiend Liege and Knight of New Alara");
        addToDeckList("Dogbert 2", "medium", "WRG Berserker deck with Blade-Tribe Berserkers and Spiraling Duelist");
//        addToDeckList("Dog 2", "medium", "GRB Sacrifice");
        addToDeckList("Doran 3", "hard", "WBG Doran, the Siege Tower deck with high toughness creatures");
//        addToDeckList("Dr Doom 2", "medium", "GWB");
//        addToDeckList("Dr Doom 3", "hard", "GWB Constructed");
//        addToDeckList("Dr Fate 3", "hard", "UB Infect");
        addToDeckList("Dr No 3", "hard", "WUB Combo & Control deck with The Rack, Balance, Propaganda and discard spells");
        addToDeckList("Dr Strangelove 3", "hard", "Mono U Sanity Grinding deck");

        addToDeckList("Electro 2", "medium", "WRG Naya deck with creatures with power 5 or greater");
        addToDeckList("Electro 3", "hard", "WRG Naya deck with creatures with power 5 or greater");
        addToDeckList("Elrond 1", "easy", "WG Aura deck with Rabid Wombat");
        addToDeckList("Elrond 2", "medium", "RGW Aura deck with Rabid Wombat");
        addToDeckList("Elrond 3", "hard", "RGW Aura deck with Kor Spiritdancer");
//        addToDeckList("En Sabah Nur 2", "medium", "RUB Singleton");
//        addToDeckList("En Sabah Nur 3", "hard", "UBR Standard Constructed");
        addToDeckList("Endora 2", "medium", "WG Enchantress deck with enchantments and cards with enchantment effects");
        addToDeckList("Endora 3", "hard", "WG Enchantress deck with enchantments and cards with enchantment effects");
        addToDeckList("Eomer 2", "medium", "Mono W flanking deck");
        addToDeckList("Ezio 3", "hard", "Mono B Assassin deck");

        addToDeckList("Fat Albert 1", "easy", "WBRG Winter Orb deck with Keldon Warlord and mana Elves/Slivers");
        addToDeckList("Fat Albert 2", "medium", "WUBRG Winter Orb deck with Keldon Warlord and mana Elves/Slivers");
        addToDeckList("Fat Albert 3", "hard", "UG Winter Orb deck with Kalonian Behemoth and mana Elves/Slivers");
//        addToDeckList("Fin Fang Foom 1", "easy", "B Artifact");
        addToDeckList("Fin Fang Foom 1", "easy", "Mono G Poison deck");
        addToDeckList("Fin Fang Foom 2", "medium", "Mono G Infect deck");
        addToDeckList("Fin Fang Foom 3", "hard", "GB Infect deck");
        addToDeckList("Fred Flintstone 3", "hard", "WUG deck with Phytohydra and Lorescale Coatl");
        addToDeckList("Frodo 1", "easy", "WRG Zoo deck with some threat removal");
        addToDeckList("Frodo 2", "medium", "WRG Zoo deck with some threat removal and Glorious Anthem");
        addToDeckList("Frodo 3", "hard", "WRG Zoo deck with threat removal and Glorious Anthems");

        addToDeckList("Galadriel 2", "medium", "Mono G Deck with Amulet of Vigor, mana ramp, Time Vault and Howl of the Night Pack");
        addToDeckList("Galahad 1", "easy", "WB Knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Galahad 2", "medium", "WB Knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Galahad 3", "hard", "WB Knight deck with Kinsbaile Cavalier and Knight Exemplar");
        addToDeckList("Gambit 2", "medium", "URG Bounce deck with Taurean Mauler and Forgotten Ancient");
        addToDeckList("Genghis Khan 1", "easy", "WRG deck with mana ramp, Armageddon and Mungha Wurm");
        addToDeckList("Genghis Khan 2", "medium", "WG deck with mana ramp, Armageddon and Terravore");
        addToDeckList("Genghis Khan 3", "hard", "WRG deck with mana ramp, Armageddon, and Terravore");
        addToDeckList("George of the Jungle 1", "easy", "WR deck with Belligerent Hatchling and Battlegate Mimic");
        addToDeckList("George of the Jungle 2", "medium", "WR deck with Belligerent Hatchling, Battlegate Mimic and Ajani Vengeant");
        addToDeckList("George of the Jungle 3", "hard", "WR deck with Belligerent Hatchling, Battlegate Mimic and Ajani Vengeant");
//        addToDeckList("Ghost Rider 3", "hard", "W Aggressive Life deck");
        addToDeckList("Gimli 2", "medium", "WB Indestructible permanents deck with mass removal");
        addToDeckList("Gimli 3", "hard", "WB Indestructible permanents deck with mass removal");
//        addToDeckList("Goblin King 2", "medium", "RG Singleton deck");
//        addToDeckList("Goblin King 3", "hard", "RG Extended deck");
//        addToDeckList("Goblin Recruit 2", "medium", "RG Skullclamp deck");
//        addToDeckList("Goblin Recruit 3", "hard", "BR Goblin Sacrifice deck");
        addToDeckList("Gold Finger 3", "hard", "Mono U control deck with various counter spells, Serra Sphinx and Memnarch");
        addToDeckList("Gold Finger 4", "very hard", "Mono U control deck with various counter spells");
        addToDeckList("Grampa Simpson 1", "easy", "WR Double and First Strike deck with equipments and auras");
        addToDeckList("Grampa Simpson 2", "medium", "WR Double Strike deck with equipments and auras");
        addToDeckList("Grampa Simpson 3", "hard", "WRG Double Strike deck with equipments and auras");
        addToDeckList("Green Arrow 1", "easy", "WG Archer deck with Jagged-Scar Archers and Titania's Chosen");
        addToDeckList("Green Arrow 2", "medium", "Mono G Archer deck with Greatbow Doyen and Jagged-Scar Archers");
        addToDeckList("Green Arrow 3", "hard", "WRG Archer Aggro deck with Greatbow Doyen and Changelings");
        addToDeckList("Green Lantern 3", "hard", "UBR Deathtouch deck with auras and several equipments");
//        addToDeckList("Gunslinger 3", "hard", "WBRG Cascade deck");

        addToDeckList("Hagrid 2", "medium", "Mono R Giants deck");
        addToDeckList("Han Solo 3", "hard", "WU Enchanted Evening deck with lots of enchantment removal");
        addToDeckList("Hans 3", "hard", "WRG Allies deck");
        addToDeckList("Harry Potter 1", "easy", "Mono U Mill and counter spell deck");
        addToDeckList("Harry Potter 2", "medium", "UB Mill and counter spell deck");
        addToDeckList("Harry Potter 3", "hard", "UB Mill and counter spell deck with card draw");
        addToDeckList("Hellboy 3", "hard", "BR direct damage deck");
        addToDeckList("Hercules 1", "easy", "GW Deck with Safehold Duo, Bant Sureblade and Naya Hushblade");
        addToDeckList("Hercules 2", "medium", "GW Deck with Bant Sureblade and Naya Hushblade");
        addToDeckList("Hercules 3", "hard", "GW Deck with Wilt-Leaf Liege, Bant Sureblade and Naya Hushblade");
        addToDeckList("Hermione Granger 3", "hard", "UR deck with Riverfall Mimic and Mindwrack Liege");
        addToDeckList("Higgins 3", "hard", "UBR Grixis Control deck");
        addToDeckList("Hobbes 3", "hard", "UBG Dredge deck with Hermit Druid, Ichorid and Narcomoeba");
        addToDeckList("Hobbes 4", "very hard", "UBG Dredge deck with Hermit Druid, Ichorid and Narcomoeba");
        addToDeckList("Homer Simpson 1", "easy", "UBR Sacrifice deck with Mortician Beetle");
        addToDeckList("Homer Simpson 2", "medium", "UBR Sacrifice deck with Mortician Beetle");
        addToDeckList("Homer Simpson 3", "hard", "UBR Sacrifice deck with Mortician Beetle");
//        addToDeckList("Hulk 2", "medium", "G Men with Pants deck");
//        addToDeckList("Hulk 3", "hard", "G Midrange deck");

        addToDeckList("Iceman 3", "hard", "UB Bounce deck with Warped Devotion");
        addToDeckList("Indiana Jones 1", "easy", "UBR Sol'kanar the Swamp King and buff");
        addToDeckList("Indiana Jones 2", "medium", "UBR Sol'kanar the Swamp King, buff and Raise Dead");
        addToDeckList("Indiana Jones 3", "hard", "UBR Sol'kanar the Swamp King, buff and Terminate");

        addToDeckList("Jabba the Hut 3", "hard", "Mono B Infect deck");
//        addToDeckList("Jack 2", "medium", "BG Aggressive deck");
//        addToDeckList("Jack 3", "hard", "WUB Sphinx Cascade deck");
        addToDeckList("Jack Sparrow 1", "easy", "UB Pirate deck with Talas Warrior, Coastal Piracy, Drowned and Sea's Claim");
        addToDeckList("Jack Sparrow 2", "medium", "UB Pirate deck with Talas Warrior, Coastal Piracy and Spreading Seas");
        addToDeckList("James Bond 1", "easy", "WG Sliver deck");
        addToDeckList("James Bond 2", "medium", "WG Sliver deck");
        addToDeckList("James Bond 3", "hard", "WG Sliver deck");
        addToDeckList("James T Kirk 3", "hard", "Mono B discard deck with Liliana Vess");
//        addToDeckList("Jason Todd 3", "hard", "BRG Sacrifice deck");
        addToDeckList("Joe Kidd 1", "easy", "WB deck with Voracious Hatchling and Nightsky Mimic");
        addToDeckList("Joe Kidd 2", "medium", "WB deck with Voracious Hatchling and Nightsky Mimic");
        addToDeckList("Joe Kidd 3", "hard", "WB deck with Voracious Hatchling and Nightsky Mimic");
//        addToDeckList("Joker 2", "medium", "WG Novablast deck");
//        addToDeckList("Jon Stewart 2", "medium", "WG Midrange deck");
//        addToDeckList("Jon Stewart 3", "hard", "WG Extended deck");

        addToDeckList("Kang 1", "easy", "UB deck with Gravelgill Duo and Dire Undercurrents");
        addToDeckList("Kang 2", "medium", "UB deck with Glen Elendra Liege, Gravelgill Duo and Dire Undercurrents");
        addToDeckList("Kang 3", "hard", "UB deck with Glen Elendra Liege and Dire Undercurrents");
        addToDeckList("King Arthur 1", "easy", "WG Knight deck with Wilt-Leaf Cavaliers, Raven's Run Dragoon and Llanowar Knight");
        addToDeckList("King Arthur 2", "medium", "WG Hybrid Knight deck with Knight of New Alara and Knight of the Reliquary");
        addToDeckList("King Arthur 3", "hard", "WG Hybrid Knight deck with Wilt-Leaf Liege, Knight of New Alara and equipments");
        addToDeckList("King Edward 1", "easy", "WUBRG Elementals deck with Tribal Flames");
        addToDeckList("King Edward 2", "medium", "WUBRG Elementals deck with Tribal Flames");
        addToDeckList("King Edward 3", "hard", "WUBRG Elementals deck with Tribal Flames and Horde of Notions");
        addToDeckList("King Goldemar 1", "easy", "Mono R Kobold deck");
        addToDeckList("King Kong 1", "easy", "WBG Squirrel deck with tokens, changelings, Deranged Hermit and curse type auras");
        addToDeckList("King Kong 2", "medium", "WBG Squirrel deck with tokens, changelings, Deranged Hermit and curse type auras");
        addToDeckList("King Kong 3", "hard", "WRG Squirrel deck with tokens, changelings, Deranged Hermit and threat removal");
        addToDeckList("Kojak 1", "easy", "Mono U deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Kojak 2", "medium", "Mono U deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Kojak 3", "hard", "Mono U deck with Sunken City, Inundate, counterspells and bounce");
        addToDeckList("Krypto 2", "medium", "BRG Bloodthirst deck with Honden of Infinite Rage, Kyren Sniper and Rumbling Slum");
        addToDeckList("Krypto 3", "hard", "BRG Bloodthirst deck with Doubling Season, Kyren Sniper and Rumbling Slum");

        addToDeckList("Leprechaun 1", "easy", "WBG anti green deck with Aisling Leprechaun, Beast Within and Waiting in the Weeds");
//        addToDeckList("Lex 3", "hard", "Ninjas deck");
//        addToDeckList("Link 3", "hard", "GUR Standard Constructed deck");
        addToDeckList("Lisa Simpson 2", "medium", "WG Devour deck with tokens, Skullmulcher and Mycoloth");
//        addToDeckList("Lucifer 2", "medium", "W Sacrifice deck");
//        addToDeckList("Lucifer 3", "hard", "W Sacrifice deck");
        addToDeckList("Luke Skywalker 3", "hard", "WU Rebels deck with Training Grounds");

        addToDeckList("Maggie Simpson 3", "hard", "BRG jund deck with Sprouting Thrinax, Jund Hackblade and Bloodbraid Elf");
        addToDeckList("Magneto 3", "hard", "Mono B Shriekmaw deck with creature removal and re-animation");
        addToDeckList("Magnum 1", "easy", "UG deck with Sturdy Hatchling and Shorecrasher Mimic");
        addToDeckList("Magnum 2", "medium", "UG deck with Sturdy Hatchling and Shorecrasher Mimic");
        addToDeckList("Magnum 3", "hard", "UG deck with Sturdy Hatchling and Shorecrasher Mimic");
        addToDeckList("Marge Simpson 2", "medium", "RG deck with tokens which are devoured by creatures with devour");
        addToDeckList("Mister Fantastic 3", "hard", "UG Intruder Alarm deck with Imperious Perfect and mana elves");
//        addToDeckList("Michael 3", "hard", "W Angels deck");
        addToDeckList("Morpheus 3", "hard", "Mono G Elf deck with Overrun, Gaea's Anthem, Imperious Perfect and other pumps");
        addToDeckList("Mr Slate 2", "medium", "WUG Merfolk deck with Lord of Atlantis, Stonybrook Banneret and Stonybrook Schoolmaster");
        addToDeckList("Mr Slate 3", "hard", "WUG Merfolk deck with Lord of Atlantis, Stonybrook Banneret and Stonybrook Schoolmaster");
//        addToDeckList("Mummy 1", "easy", "W Life deck");

        addToDeckList("Nagini 2", "medium", "Mono G Snake deck");
//        addToDeckList("Namor 2", "medium", "U Control deck");
//        addToDeckList("Namor 3", "hard", "U Standard Constructed deck");
        addToDeckList("Napoleon 3", "hard", "WBG Wall deck with Rolling Stones and Doran, the Siege Tower");
        addToDeckList("Ned Flanders 1", "easy", "Mono B reanimator deck with a few large creatures and some spells");
        addToDeckList("Ned Flanders 2", "medium", "Mono B reanimator deck with a few large creatures and some spells");
        addToDeckList("Ned Flanders 3", "hard", "Mono B reanimator deck with a few large creatures and some spells");
        addToDeckList("Ned Flanders 4", "very hard", "Mono B reanimator deck with a few large creatures and some spells");
        addToDeckList("Neo 2", "medium", "RG deck with Boartusk Liege, Jund Hackblade and Naya Hushblade");
        addToDeckList("Neo 3", "hard", "RG deck with Boartusk Liege, Jund Hackblade and Naya Hushblade");
        addToDeckList("Newton 3", "hard", "WB Relentless Rats deck with Thrumming Stone, Vindicate and Swords to Plowshares");

        addToDeckList("Oberon 1", "easy", "UB Faerie deck");
        addToDeckList("Oberon 2", "medium", "UB Faerie deck");
        addToDeckList("Oberon 3", "hard", "UB Faerie deck");
        addToDeckList("Odin 1", "easy", "WU deck with Thistledown Duo");
        addToDeckList("Odin 2", "medium", "WU deck with Thistledown Duo, Thistledown Liege and Grand Arbiter Augustin IV");
        addToDeckList("Odin 3", "hard", "WU deck with Thistledown Liege and Grand Arbiter Augustin IV");
        addToDeckList("Optimus Prime 3", "hard", "Mono U deck with Modular creatures and proliferate spells");
//        addToDeckList("Owlman 2", "medium", "U Ebony Owl deck");
//        addToDeckList("Owlman 3", "hard", "B Control Standard deck");

        addToDeckList("Pebbles Flintstone 2", "medium", "WU Meekstone deck with Meekstone, Marble Titan and creatures with vigilance");
        addToDeckList("Pebbles Flintstone 3", "hard", "WU Meekstone deck with Meekstone, Marble Titan and creatures with vigilance");
//        addToDeckList("Phoenix 3", "hard", "R Burn");
        addToDeckList("Picard 3", "hard", "WUG Elf deck with elf lords");
        addToDeckList("Pinky and the Brain 2", "medium", "WB Royal Assassin deck with white tap abilities");
//        addToDeckList("Predator 2", "medium", "WG Purity Ramp deck");
//        addToDeckList("Predator 3", "hard", "UG Beastmaster Ascension deck");
        addToDeckList("Pointy Haired Boss 3", "hard", "WUG Combo deck with Hokori, Dust Drinker, Mana Leak and Sunscape Master");
        addToDeckList("Princess Selenia 1", "easy", "BUG Song of Serenity deck");
        addToDeckList("Professor X 2", "medium", "WUB Esper Artifacts deck with Master of Etherium and Esper Stormblade");
        addToDeckList("Professor X 3", "hard", "WUB Esper Artifacts deck with Master of Etherium and Esper Stormblade");

        addToDeckList("R2-D2 3", "hard", "Mono U Black Vise deck with bounce spells and Howling Mine");
        addToDeckList("Radagast 2", "medium", "Mono G Muraganda Petroglyphs deck with vanilla creatures and a few tokens");
        addToDeckList("Radiant 3", "hard", "WU flying creature deck with Radiant, Archangel, Gravitational Shift and Moat");
        addToDeckList("Radioactive Man 3", "hard", "WR Sneak Attack deck");
        addToDeckList("Radioactive Man 4", "very hard", "WR Sneak Attack deck");
        addToDeckList("Ratbert 2", "medium", "Mono B Fear deck with Thran Lens");
//        addToDeckList("Ras Al Ghul 2", "medium", "RG Biorhythm deck");
//        addToDeckList("Ras Al Ghul 3", "hard", "WG Eldrazi Monument deck");
//        addToDeckList("Raven 1", "easy", "Birds deck");
//        addToDeckList("Raven 2", "medium", "Birds deck");
//        addToDeckList("Raven 3", "hard", " Possessed Birds deck");
//        addToDeckList("Red Skull 2", "medium", "BR Metalcraft deck");
        addToDeckList("Redwall 2", "medium", "Mono R Defender deck with Vent Sentinel");
        addToDeckList("Reverend Lovejoy 2", "medium", "WRG deck with Kavu Predator and Punishing Fire");
        addToDeckList("Riddler 3", "hard", "WR deck with mass damage spells, Spitemare and Swans of Bryn Argoll");
        addToDeckList("Riddler 4", "very hard", "WR deck with mass damage spells, Stuffy Doll and Guilty Conscience");
//        addToDeckList("Robin 2", "medium", "G Big Green deck");
//        addToDeckList("Robin 3", "hard", "WG Standard deck");
        addToDeckList("Rocky 1", "easy", "WUR Pro red deck with Flamebreak, Tremor, Pyroclasm");
        addToDeckList("Rocky 2", "medium", "WUR Pro red deck with Flamebreak, Tremor, Pyroclasm");
        addToDeckList("Rocky 3", "hard", "WUR Pro red deck with Flamebreak, Tremor, Pyroclasm");
        addToDeckList("Rogue 3", "hard", "Mono R Dragon deck with Tarox Bladewing, Dragon Roost and Chandra Nalaar");

//        addToDeckList("Sabertooth 2", "medium", "G Smokestack deck");
        addToDeckList("Samantha Stephens 1", "easy", "WU Painter's Servant anti-red deck");
        addToDeckList("Samantha Stephens 2", "medium", "WU Painter's Servant anti-red deck");
        addToDeckList("Samantha Stephens 3", "hard", "WU Painter's Servant anti-red deck with Grindstone combo");
        addToDeckList("Samantha Stephens 4", "very hard", "WU Painter's Servant - Grindstone combo");
        addToDeckList("Samwise Gamgee 2", "medium", "Mono W Kithkin deck");
        addToDeckList("Samwise Gamgee 3", "hard", "Mono W Kithkin deck");
        addToDeckList("Saruman 2", "medium", "UBR discard deck with Megrim, Liliana's Caress and Burning Inquiry ");
        addToDeckList("Saruman 3", "hard", "UBR discard deck with Megrim, Liliana's Caress and Burning Inquiry ");
        addToDeckList("Sauron 2", "medium", "UB Underworld Dreams deck with Black Vise and lots of card draw for both players");
        addToDeckList("Scooby Doo 3", "hard", "WR Giants Aggro deck with a few changelings");
        addToDeckList("Scotty 2", "medium", "WBG protection from black Famine deck with Phantom Centaur and Nightwind Glider");
        addToDeckList("Scotty 3", "hard", "WBG protection from black Famine deck with Chameleon Colossus and Oversoul of Dusk");
        addToDeckList("Seabiscuit 1", "easy", "Mono W Metalcraft deck with Ardent Recruit and Indomitable Archangel");
        addToDeckList("Seabiscuit 2", "medium", "Mono W Metalcraft deck with Ardent Recruit and Indomitable Archangel");
        addToDeckList("Seabiscuit 3", "hard", "Mono W Metalcraft deck with Ardent Recruit and Indomitable Archangel");
        addToDeckList("Secret Squirrel 3", "hard", "Mono G Squirrel deck with Squirrel Mob, Deranged Hermit, Coat of Arms and Nut Collector");
//        addToDeckList("Sentinel 2", "medium", "WB Token deck");
//        addToDeckList("Sentinel 3", "hard", "WB Token deck");
//        addToDeckList("Shelob 1", "easy", "G Reach deck");
        addToDeckList("Sherlock Holmes 1", "easy", "Mono G deck with Baru, Fist of Krosa, land fetch and some buff cards");
        addToDeckList("Sherlock Holmes 2", "medium", "Mono G deck with Baru, Fist of Krosa and lots of good green creatures");
        addToDeckList("Sherlock Holmes 3", "hard", "Mono G deck with Baru, Fist of Krosa and lots of great green creatures");
        addToDeckList("Shrek 1", "easy", "Mono B Demon and Ogre deck");
        addToDeckList("Silver Surfer 3", "hard", "BG deck with Hunted Wumpus and Iwamori of the Open Fist");
        addToDeckList("Silver Samurai 2", "medium", "Mono W Samurai deck");
        addToDeckList("Spiderman 2", "medium", "Mono W weenie deck with WoG, Armageddon and Mass Calcify");
        addToDeckList("Spiderman 3", "hard", "Mono W weenie deck with WoG, Armageddon, Honor of the Pure and Mass Calcify");
        addToDeckList("Spock 2", "medium", "Mono G Elf singleton deck with several Winnower Patrol and Wolf-Skull Shaman");
//        addToDeckList("Starfire 2", "medium", "Incarnations deck");
//        addToDeckList("Starfire 3", "hard", "Incarnations deck");
        addToDeckList("Storm 1", "easy", "WBG Lifegain deck with Ajani's Pridemate and Serra Ascendant");
        addToDeckList("Storm 2", "medium", "WBG Lifegain deck with Ajani's Pridemate and Serra Ascendant");
        addToDeckList("Storm 3", "hard", "WBG Lifegain deck with Ajani's Pridemate and Serra Ascendant");
        addToDeckList("Sun Quan 2", "medium", "Mono U Horsemanship deck");
//        addToDeckList("Superboy 3", "hard", "R Artifact deck");
        addToDeckList("Superman 3", "hard", "WRG counters deck with Borborygmos, Rushwood Elemental, Doubling Season and Titania's Boon");
//        addToDeckList("Swamp Thing 1", "easy", "BG deck");
//        addToDeckList("Swamp Thing 2", "medium", "BG deck");
//        addToDeckList("Swamp Thing 3", "hard", "BG deck");

        addToDeckList("Tarzan 1", "easy", "WR Ape tribal deck with Ancient Silverback, Earthbind, Claws of Wirewood and pump spells");
        addToDeckList("Tarzan 2", "medium", "WR Ape tribal deck with Ancient Silverback, Raking Canopy and Treetop Village");
        addToDeckList("Terminator 3", "hard", "Mono B artifact deck with The Abyss");
        addToDeckList("The Great Gazoo 3", "hard", "WR deck with, red damage all spells and pro from red creatures");
        addToDeckList("The Thing 2", "medium", "Mono W creatureless deck with Urza's Armor, Sphere of Purity, Copper Tablet and Ankh of Mishra");
//        addToDeckList("Thing 2", "medium", "WG Elves deck");
//        addToDeckList("Thing 3", "hard", "G Garruk Elves deck");
//        addToDeckList("Thor 1", "easy", "WR Singleton deck");
//        addToDeckList("Thor 2", "medium", "BR 1cc deck");
//        addToDeckList("Thor 3", "hard", "WR Constructed deck");
//        addToDeckList("Thugs 2", "medium", "WG Elves deck");
//        addToDeckList("Thugs 3", "hard", "WG Strength in Numbers deck");
        addToDeckList("Tom Bombadil 3", "hard", "Mono G deck with Garruk's Packleader, Garruk Wildspeaker and Garruk's Companion");
        addToDeckList("Totoro 2", "medium", "UBG deck with spirits and arcane spells");
        addToDeckList("Treebeard 1", "easy", "Mono G Treefolk deck with Bosk Banneret, Dauntless Dourbark and Leaf-Crowned Elder");
        addToDeckList("Treebeard 2", "medium", "WBG Treefolk deck with Bosk Banneret, Dauntless Dourbark, Timber Protector, Leaf-Crowned Elder and Doran");
        addToDeckList("Treebeard 3", "hard", "WBG Treefolk deck with Bosk Banneret, Dauntless Dourbark, Timber Protector, Leaf-Crowned Elder and Doran");

        addToDeckList("Uncle Owen 3", "hard", "WUB Control deck");

//        addToDeckList("Vampire 2", "medium", "Vampire Singleton");
//        addToDeckList("Vampire 3", "hard", "Vampire Constructed");

        addToDeckList("Walle 2", "medium", "Mono W Myr deck");
        addToDeckList("Wally 3", "hard", "WB Artifact deck with Tempered Steel");
//        addToDeckList("Werewolf 2", "medium", "UGB UBG Fungal Shambler deck");
        addToDeckList("White Knight 1", "easy", "Mono W Knights deck");
        addToDeckList("White Knight 2", "medium", "Mono W Knights deck");
        addToDeckList("White Knight 3", "hard", "Mono W Knights deck");
        addToDeckList("Wilma Flintstone 1", "easy", "BG deck with Noxious Hatchling and Woodlurker Mimic");
        addToDeckList("Wilma Flintstone 2", "medium", "BG deck with Noxious Hatchling and Woodlurker Mimic");
        addToDeckList("Wilma Flintstone 3", "hard", "BG deck with Noxious Hatchling and Woodlurker Mimic");
        addToDeckList("Wolverine 3", "hard", "BG deck with Nightmare, Korlash, Heir to Blackblade and Kodama's Reach");
        addToDeckList("Wonder Woman 2", "medium", "Mono W Equipment deck");
        addToDeckList("Wyatt Earp 1", "easy", "Mono W deck with Crovax, Ascendant Hero, Crusade and small to medium sized creatures.");
        addToDeckList("Wyatt Earp 2", "medium", "Mono W deck with Crovax, Ascendant Hero, Crusade and small to medium sized creatures.");
        addToDeckList("Wyatt Earp 3", "hard", "Mono W deck with Crovax, Ascendant Hero, Honor of the Pure and small to medium sized creatures.");

//        addToDeckList("Xavier 2", "medium", "UR Twitch");
    }

    /**
     * <p>addToDeckList.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param difficulty a {@link java.lang.String} object.
     * @param description a {@link java.lang.String} object.
     */
    private static void addToDeckList(String name, String difficulty, String description) {
        nameDeckMap.put(name, new DeckInfo(name, description, difficulty));
    }

    /**
     * <p>getDescription.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getDescription(String deckName) {
        if (nameDeckMap.containsKey(deckName)) {
            return nameDeckMap.get(deckName).description;
        } else {
            System.out.println("Deck " + deckName + " does not have a description.");
            return "";
        }
    }

    private static class DeckInfo {
        //String name;
        String difficulty;
        String description;

        private DeckInfo(String name, String description, String difficulty) {
            this.description = description;
            this.difficulty = difficulty;
            //this.name = name;
        }
    }

    /**
     * <p>getBattles.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<QuestSelectablePanel> getBattles() {
        List<QuestSelectablePanel> opponentList = new ArrayList<QuestSelectablePanel>();

        String[] opponentNames = QuestBattleManager.getOpponents();
        for (String opponentName : opponentNames) {

            String oppIconName = opponentName.substring(0, opponentName.length() - 1).trim() + ".jpg";
            ImageIcon icon = GuiUtils.getIconFromFile(oppIconName);

            try {
                opponentList.add(new QuestBattle(opponentName,
                        nameDeckMap.get(opponentName).difficulty,
                        nameDeckMap.get(opponentName).description,
                        icon));
            } catch (NullPointerException e) {
                System.out.println("Missing Deck Description. Fix me:" + opponentName);
                opponentList.add(new QuestBattle(opponentName,
                        "<<Unknown>>",
                        "<<Unknown>>",
                        icon));

            }
        }

        return opponentList;
    }

    /**
     * <p>Constructor for QuestBattle.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param difficulty a {@link java.lang.String} object.
     * @param description a {@link java.lang.String} object.
     * @param icon a {@link javax.swing.ImageIcon} object.
     */
    private QuestBattle(String name, String difficulty, String description, ImageIcon icon) {
        super(name.substring(0, name.length() - 2), difficulty, description, icon);

        this.deckName = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return deckName;
    }
}
