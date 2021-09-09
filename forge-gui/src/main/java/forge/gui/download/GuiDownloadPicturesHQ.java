/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.download;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.ImageUtil;

public class GuiDownloadPicturesHQ extends GuiDownloadService {
    final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> existingSets;
    ArrayList<String> existingImages;

    @Override
    public String getTitle() {
        return "Download HQ Card Pictures";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        File f = new File(ForgeConstants.CACHE_CARD_PICS_DIR);
        existingImages = new ArrayList<>(Arrays.asList(f.list()));
        existingSets = retrieveManifestDirectory();

        for (final PaperCard c : FModel.getMagicDb().getCommonCards().getAllCards()) {
            addDLObject(c, false);
            if (c.hasBackFace()) {
                addDLObject(c, true);
            }
        }

        for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            addDLObject(c, false);
        }

        // Add missing tokens to the list of things to download.
        addMissingItems(downloads, ForgeConstants.IMAGE_LIST_TOKENS_FILE, ForgeConstants.CACHE_TOKEN_PICS_DIR);

        return downloads;
    }

    private void addDLObject(final PaperCard c, final boolean backFace) {
        final String imageKey = ImageUtil.getImageKey(c, backFace, false);
        final String destPath = ForgeConstants.CACHE_CARD_PICS_DIR + imageKey  + ".jpg";

        if (existingImages.contains(imageKey + ".jpg")) {
            return;
        }

        if (downloads.containsKey(destPath)) {
            return;
        }

        String setCode = c.getEdition();
        String cardname = imageKey.replace(".full", "");

        switch(setCode) {
            case "CFX":     setCode="CON"; break;
            case "COM":     setCode="CMD"; break;
            case "FVE":     setCode="V09"; break;
            case "FVL":     setCode="V11"; break;
            case "FVR":     setCode="V10"; break;
            case "MED":     setCode="ME1"; break;
            case "MPS_AKH": setCode="AKH"; break;
            case "MPS_KLD": setCode="MPS"; break;
            case "MPS_RNA": setCode="MED"; break;
            case "PDS":     setCode="H09"; break;
            case "PO2":     setCode="P02"; break;
            case "UGF":     setCode="UGIN"; break;
        }

        setCode = getManualCode(cardname, setCode);

        cardname = cardname.replace(" ", "+");
        cardname = cardname.replace("'", "");
        String scryfallurl = ForgeConstants.URL_PIC_SCRYFALL_DOWNLOAD + "named?fuzzy=" + cardname;
        if(!setCode.equals("???")) scryfallurl += "&set=" + setCode.toLowerCase();
        if(backFace) scryfallurl += "&face=back";
        scryfallurl += "&format=image";

        downloads.put(destPath, scryfallurl);
    }

    private String getManualCode(String cardname, String originalSetCode) {
        String setCode=originalSetCode;

        switch(cardname) {
            case "Daretti, Ingenious Iconoclast":
            case "Elspeth, Knight-Errant":
            case "Garruk, Apex Predator":
            case "Jace, the Mind Sculptor":
            case "Liliana, the Last Hope":
            case "Nahiri, the Harbinger":
            case "Nicol Bolas, Planeswalker":
            case "Sarkhan Unbroken":
            case "Teferi, Hero of Dominaria":
            case "Tezzeret, Agent of Bolas":
            case "Tezzeret the Seeker":
            case "Ugin, the Spirit Dragon":					setCode="MED"; break;

            case "Attrition":
            case "Boil":
            case "Capsize":
            case "Counterbalance":
            case "Daze":
            case "Desolation Angel":
            case "Divert":
            case "Forbid":
            case "Force of Will":
            case "Loyal Retainers":
            case "Mind Twist":
            case "No Mercy":
            case "Opposition":
            case "Shatterstorm":
            case "Slaughter Pact":
            case "Stifle":
            case "Sunder":
            case "Worship":
            case "Wrath of God": 							setCode="MP2"; break;

            case "Behold the Power of Destruction":
            case "Choose Your Champion":
            case "Dance, Pathetic Marionette":
            case "Embrace My Diabolical Vision":
            case "Every Hope Shall Vanish":
            case "Every Last Vestige Shall Rot":
            case "Evil Comes to Fruition":
            case "Feed the Machine":
            case "I Bask in Your Silent Awe":
            case "I Call on the Ancient Magics":
            case "I Delight in Your Convulsions":
            case "I Know All, I See All":
            case "Ignite the Cloneforge!":
            case "Into the Earthen Maw":
            case "Introductions Are in Order":
            case "Know Naught but Fire":
            case "Look Skyward and Despair":
            case "May Civilization Collapse":
            case "Mortal Flesh Is Weak":
            case "My Crushing Masterstroke":
            case "My Genius Knows No Bounds":
            case "My Undead Horde Awakens":
            case "My Wish Is Your Command":
            case "Nature Demands an Offering":
            case "Nature Shields Its Own":
            case "Nothing Can Stop Me Now":
            case "Only Blood Ends Your Nightmares":
            case "Realms Befitting My Majesty":
            case "Roots of All Evil":
            case "Rotted Ones, Lay Siege":
            case "Surrender Your Thoughts":
            case "The Dead Shall Serve":
            case "The Fate of the Flammable":
            case "The Iron Guardian Stirs":
            case "The Pieces Are Coming Together":
            case "The Very Soil Shall Shake":
            case "Tooth, Claw, and Tail":
            case "Which of You Burns Brightest":
            case "Your Fate Is Thrice Sealed":
            case "Your Puny Minds Cannot Fathom":
            case "Your Will Is Not Your Own": 				setCode="OARC"; break;

            case "Because I Have Willed It":
            case "Behold My Grandeur":
            case "Bow to My Command":
            case "Choose Your Demise":
            case "Delight in the Hunt":
            case "Every Dream a Nightmare":
            case "For Each of You, a Gift":
            case "Know Evil":
            case "Make Yourself Useful":
            case "My Forces Are Innumerable":
            case "My Laughter Echoes":
            case "No One Will Hear Your Cries":
            case "Pay Tribute to Me":
            case "Power Without Equal":
            case "The Mighty Will Fall":
            case "There Is No Refuge":
            case "This World Belongs to Me":
            case "What's Yours Is Now Mine":
            case "When Will You Learn":						setCode="OE01"; break;

            case "Bloodhill Bastion":
            case "Celestine Reef":
            case "Chaotic Aether":
            case "Choke":
            case "Cliffside Market":
            case "Edge of Malacol":
            case "Eloren Wilds":
            case "Feeding Grounds":
            case "Fields of Summer":
            case "Furnace Layer":
            case "Glimmervoid Basin":
            case "Grand Ossuary":
            case "Grove of the Dreampods":
            case "Hedron Fields of Agadeem":
            case "Horizon Boughs":
            case "Immersturm":
            case "Interplanar Tunnel":
            case "Isle of Vesuva":
            case "Izzet Steam Maze":
            case "Kharasha Foothills":
            case "Kilnspire District":
            case "Lair of the Ashen Idol":
            case "Lethe Lake":
            case "Mirrored Depths":
            case "Morphic Tide":
            case "Mount Keralia":
            case "Mutual Epiphany":
            case "Norn's Dominion":
            case "Onakke Catacomb":
            case "Orochi Colony":
            case "Panopticon":
            case "Planewide Disaster":
            case "Pools of Becoming":
            case "Quicksilver Sea":
            case "Reality Shaping":
            case "Sanctum of Serra":
            case "Sea of Sand":
            case "Selesnya Loft Gardens":
            case "Skybreen":
            case "Spatial Merging":
            case "Stairs to Infinity":
            case "Stronghold Furnace":
            case "Talon Gates":
            case "Tember City":
            case "Windriddle Palaces":
            case "The Aether Flues":
            case "The Dark Barony":
            case "The Eon Fog":
            case "The Fourth Sphere":
            case "The Great Forest":
            case "The Hippodrome":
            case "The Maelstrom":
            case "The Zephyr Maze":
            case "Time Distortion":
            case "Trail of the Mage-Rings":
            case "Truga Jungle":
            case "Turri Island":
            case "Undercity Reaches":						setCode="OPCA"; break;

            case "Drench the Soil in Their Blood":
            case "Imprison This Insolent Wretch":
            case "Perhaps You've Met My Cohort":
            case "Plots That Span Centuries":
            case "Your Inescapable Doom":					setCode="PARC"; break;

            case "Stoneforge Mystic":						setCode="PGPX"; break;

            case "Birds of Paradise Avatar1":
            case "Bosh, Iron Golem Avatar":
            case "Braids, Conjurer Adept Avatar":
            case "Chronatog Avatar":
            case "Dakkon Blackblade Avatar":
            case "Dauntless Escort Avatar":
            case "Diamond Faerie Avatar":
            case "Eight-and-a-Half-Tails Avatar":
            case "Enigma Sphinx Avatar":
            case "Eladamri, Lord of Leaves Avatar":
            case "Elvish Champion Avatar":
            case "Erhnam Djinn Avatar1":
            case "Etched Oracle Avatar":
            case "Fallen Angel Avatar":
            case "Figure of Destiny Avatar":
            case "Flametongue Kavu Avatar":
            case "Frenetic Efreet Avatar":
            case "Goblin Warchief Avatar1":
            case "Grinning Demon Avatar1":
            case "Haakon, Stromgald Scourge Avatar":
            case "Heartwood Storyteller Avatar":
            case "Hell's Caretaker Avatar":
            case "Hermit Druid Avatar":
            case "Higure, the Still Wind Avatar":
            case "Ink-Eyes, Servant of Oni Avatar":
            case "Jaya Ballard Avatar":
            case "Jhoira of the Ghitu Avatar":
            case "Karona, False God Avatar":
            case "Kresh the Bloodbraided Avatar":
            case "Loxodon Hierarch Avatar":
            case "Lyzolda, the Blood Witch Avatar":
            case "Malfegor Avatar":
            case "Maralen of the Mornsong Avatar":
            case "Maro Avatar":
            case "Master of the Wild Hunt Avatar":
            case "Mayael the Anima Avatar":
            case "Mirri the Cursed Avatar":
            case "Mirror Entity Avatar":
            case "Momir Vig, Simic Visionary Avatar":
            case "Morinfen Avatar":
            case "Murderous Redcap Avatar":
            case "Necropotence Avatar":
            case "Nekrataal Avatar":
            case "Oni of Wild Places Avatar":
            case "Orcish Squatters Avatar":
            case "Peacekeeper Avatar":
            case "Phage the Untouchable Avatar":
            case "Platinum Angel Avatar1":
            case "Prodigal Sorcerer Avatar1":
            case "Raksha Golden Cub Avatar":
            case "Reaper King Avatar":
            case "Rith, the Awakener Avatar1":
            case "Royal Assassin Avatar1":
            case "Rumbling Slum Avatar":
            case "Sakashima the Impostor Avatar":
            case "Serra Angel Avatar1":
            case "Seshiro the Anointed Avatar":
            case "Sisters of Stone Death Avatar":
            case "Sliver Queen Avatar":
            case "Squee, Goblin Nabob Avatar":
            case "Stalking Tiger Avatar":
            case "Stonehewer Giant Avatar":
            case "Stuffy Doll Avatar":
            case "Teysa, Orzhov Scion Avatar":
            case "Tradewind Rider Avatar1":
            case "Two-Headed Giant of Foriys Avatar":
            case "Vampire Nocturnus Avatar":
            case "Viridian Zealot Avatar":					setCode="PMOA"; break;

            case "Nalathni Dragon":
            case "Sewers of Estark":
            case "Windseeker Centaur":						setCode="PRM"; break;

            case "Lyna":
            case "Sliver Queen, Brood Mother":
            case "Takara":									setCode="PVAN"; break;

            case "Goblin Hero":								setCode="S99"; break;

            case "Pyrostatic Pillar":
            case "Weathered Wayfarer":						setCode="TD0"; break;

            case "Hero's Resolve":
            case "Python":									setCode="6ED"; break;
        }

        return setCode;
    }
}
