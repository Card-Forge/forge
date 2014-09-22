package forge.achievement;

import java.util.HashSet;

import forge.game.Game;
import forge.game.player.Player;
import forge.interfaces.IGuiBase;
import forge.item.IPaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;

public class PlaneswalkerAchievements extends AchievementCollection {
    public static final PlaneswalkerAchievements instance = new PlaneswalkerAchievements();

    private final HashSet<String> activatedUltimates = new HashSet<String>();

    private PlaneswalkerAchievements() {
        super("Planeswalker Ultimates", ForgeConstants.ACHIEVEMENTS_DIR + "planeswalkers.xml", false);
    }

    @Override
    protected void addSharedAchivements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add("Ajani Goldmane", "Ajani's Better Half", "Serra? Is that you?");
        add("Ajani Steadfast", "Ajani's Forcefield", "Shields up!");
        add("Ajani Vengeant", "Ajani's Tantrum", "Look Ma! No lands!");
        add("Ajani, Caller of the Pride", "Ajani's Menagerie", "You're a crazy cat lady now!");
        add("Ajani, Mentor of Heroes", "Ajani's Vitality", "Do sta let!");
        add("Ashiok, Nightmare Weaver", "Ashiok's Identity", "I subscribe to the theory of blank slate!");
        add("Chandra Ablaze", "Chandra's Flashback", "I feel like I've seen those before...");
        add("Chandra Nalaar", "Chandra's Hot Spring", "Careful! It's scalding!");
        add("Chandra, Pyromaster", "Chandra's Menial Job", "Please file in triplicate!");
        add("Chandra, the Firebrand", "Chandra's Hex", "When burning five is just not enough.");
        add("Dack Fayden", "Dack's Discount", "Do you have any evidence it was me?");
        add("Domri Rade", "Domri's Surprise", "That's GOTTA get me a date!");
        add("Elspeth Tirel", "Elspeth's Solitude", "Tokens are my only friends...");
        add("Elspeth, Knight-Errant", "Elspeth's Immortality", "Bant will prevail!");
        add("Elspeth, Sun's Champion", "Elspeth's Crusade", "With Heliod on my side, I'm invincible!");
        add("Garruk, the Veil-Cursed", "Garruk's Grave Romp", "The dead shall help the living!");
        add("Garruk Wildspeaker", "Garruk's Overrun", "I speak seven languages, including Wild!");
        add("Garruk, Apex Predator", "Garruk's Prey", "Funny, it's usually a good thing to have an emblem...");
        add("Garruk, Caller of Beasts", "Garruk's Wild Pair", "Can you beat two for the price of one?");
        add("Garruk, Primal Hunter", "Garruk's Garden", "They came out after a rain...");
        add("Gideon Jura", "Gideon's Brawl", "Are you man enough to mess with me?");
        add("Gideon, Champion of Justice", "Gideon's Aftermath", "Gideon stands alone!");
        add("Jace Beleren", "Jace's Grind", "Nice memories... not!");
        add("Jace, Architect of Thought", "Jace's Incantation", "I like that one. I'll steal it!");
        add("Jace, Memory Adept", "Jace's Revelation", "So many things to learn!");
        add("Jace, the Living Guildpact", "Jace's Timetwister", "Symmetry? What symmetry?");
        add("Jace, the Mind Sculptor", "Jace's Lobotomy", "What do you mean, \"overpowered\"?");
        add("Karn Liberated", "Karn's Reset", "Let's do this again!");
        add("Kiora, the Crashing Wave", "Kiora's Best Friend", "Say hello to Cthulhu for me!");
        add("Koth of the Hammer", "Koth's Eruption", "You won't like the mountains when they are angry!");
        add("Liliana Vess", "Liliana's Ritual", "Stop being lazy and go to work!");
        add("Liliana of the Dark Realms", "Liliana's Swamp", "What do you mean, \"out of character\"?");
        add("Liliana of the Veil", "Liliana's Choice", "Which one of your children do you love best?");
        add("Nicol Bolas, Planeswalker", "Bolas's Ultimatum", "Whatever you do, don't call him \"Nicol\"!");
        add("Nissa Revane", "Nissa's Summoning", "The whole village is here!");
        add("Nissa, Worldwaker", "Nissa's Awakening", "The whole country is here!");
        add("Ral Zarek", "Ral's Long Day", "So much work, so much time!");
        add("Sarkhan Vol", "Sarkhan's Dragons", "Go forth, my minions!");
        add("Sarkhan the Mad", "Sarkhan's Beatdown", "Tag! You're it!");
        add("Sarkhan, the Dragonspeaker", "Sarkhan's Voices", "Huh? What are you saying?)");
        add("Sorin Markov", "Sorin's Hypnosis", "You're getting sleepy... very sleepy...");
        add("Sorin, Lord of Innistrad", "Sorin's Recruitment", "My favorite game is Shogi!");
        add("Sorin, Solemn Visitor", "Sorin's Abyss", "Don't get so close to the edge!");
        add("Tamiyo, the Moon Sage", "Tamiyo's Recycling", "Wash, rinse, repeat!");
        add("Teferi, Temporal Archmage", "Teferi's Time Slip", "Let's speed this up a bit!");
        add("Tezzeret the Seeker", "Tezzeret's Robots", "You can't stop the progress!");
        add("Tezzeret, Agent of Bolas", "Tezzeret's Drain", "Technological superiority for the win!");
        add("Tibalt, the Fiend-Blooded", "Tibalt's Insurrection", "My side is the winning side!");
        add("Venser, the Sojourner", "Venser's Oblivion", "Let's just clean this up a bit...");
        add("Vraska, the Unseen", "Vraska's Crew", "Say hello to my little friends!");
        add("Xenagos, the Reveler", "Xenagos's Reveal", "Mwahahaha! Now I'm a god!");
    }

    private void add(String cardName0, String displayName0, String flavorText0) {
        add(new PlaneswalkerUltimate(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(IGuiBase gui, Player player) {
        //only call update achievements for any ultimates activated during the game
        if (player.getOutcome().hasWon()) {
            boolean needSave = false;
            for (String ultimate : activatedUltimates) {
                Achievement achievement = achievements.get(ultimate);
                if (achievement != null) {
                    achievement.update(gui, player);
                    needSave = true;
                }
            }
            if (needSave) {
                save();
            }
        }
        activatedUltimates.clear();
    }

    private class PlaneswalkerUltimate extends Achievement {
        private PlaneswalkerUltimate(String cardName0, String displayName0, String flavorText0) {
            super(cardName0, displayName0, "Win a game after activating " + cardName0 + "'s ultimate", flavorText0);
        }

        @Override
        protected int evaluate(Player player, Game game) {
            return current + 1; //if this reaches this point, it can be presumed that alternate win condition achieved
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(getKey());
        }

        @Override
        public String getSubTitle() {
            return current + " Win" + (current != 1 ? "s" : "");
        }
    }
}
