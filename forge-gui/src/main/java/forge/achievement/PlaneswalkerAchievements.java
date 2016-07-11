package forge.achievement;

import forge.GuiBase;
import forge.assets.FSkinProp;
import forge.assets.ISkinImage;
import forge.game.Game;
import forge.game.player.Player;
import forge.item.IPaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;

public class PlaneswalkerAchievements extends AchievementCollection {
    public static final PlaneswalkerAchievements instance = new PlaneswalkerAchievements();

    public static ISkinImage getTrophyImage(String planeswalkerName) {
        return GuiBase.getInterface().createLayeredImage(FSkinProp.IMG_SPECIAL_TROPHY, ForgeConstants.CACHE_ACHIEVEMENTS_DIR + "/" + planeswalkerName + ".png", 1);
    }

    private PlaneswalkerAchievements() {
        super("Planeswalker Ultimates", ForgeConstants.ACHIEVEMENTS_DIR + "planeswalkers.xml", false);
    }

    @Override
    protected void addSharedAchivements() {
        //prevent including shared achievements
    }

    @Override
    protected void addAchievements() {
        add("Ajani Goldmane", "Ajani's Avatar", "Serra? Is that you?");
        add("Ajani Steadfast", "Ajani's Forcefield", "Shields up!");
        add("Ajani Vengeant", "Ajani's Tantrum", "Look Ma! No lands!");
        add("Ajani, Caller of the Pride", "Ajani's Menagerie", "You're a crazy cat lady now!");
        add("Ajani, Mentor of Heroes", "Ajani's Vitality", "Do sta let!");
        add("Arlinn, Embraced by the Moon", "Arlinn's Hunt", "Fair fight? What fair fight?");
        add("Ashiok, Nightmare Weaver", "Ashiok's Identity", "I subscribe to the theory of blank slate!");
        add("Chandra Ablaze", "Chandra's Bargain", "I feel like I've seen those before...");
        add("Chandra Nalaar", "Chandra's Rage", "Feel the power of my wrath!");
        add("Chandra, Flamecaller", "Chandra's Sea of Flames", "You didn't activate this for 0 just to get the achievement, did you?");
        add("Chandra, Pyromaster", "Chandra's Vengeance", "Please file in triplicate!");
        add("Chandra, Roaring Flame", "Chandra's Roar", "The opponent continues to burn...");
        add("Chandra, the Firebrand", "Chandra's Hex", "When burning five is just not enough.");
        add("Dack Fayden", "Dack's Discount", "Do you have any evidence it was me?");
        add("Daretti, Scrap Savant", "Daretti's Recycler", "Save the environment!");
        add("Domri Rade", "Domri's Surprise", "That's GOTTA get me a date!");
        add("Elspeth Tirel", "Elspeth's Solitude", "Tokens are my only friends...");
        add("Elspeth, Knight-Errant", "Elspeth's Endurance", "Bant will prevail!");
        add("Elspeth, Sun's Champion", "Elspeth's Crusade", "With Heliod on my side, I'm invincible!");
        add("Freyalise, Llanowar's Fury", "Freyalise's Big Party", "Let's celebrate each and every one of you!");
        add("Garruk, the Veil-Cursed", "Garruk's Graveyard", "The dead shall help the living!");
        add("Garruk Wildspeaker", "Garruk's Overrun", "I speak seven languages, including Wild!");
        add("Garruk, Apex Predator", "Garruk's Prey", "Funny, it's usually a good thing to have an emblem...");
        add("Garruk, Caller of Beasts", "Garruk's Wild Pair", "Can you beat two for the price of one?");
        add("Garruk, Primal Hunter", "Garruk's Garden", "They came out after a rain...");
        add("Gideon Jura", "Gideon's Brawl", "Are you man enough to mess with me?");
        add("Gideon, Battle-Forged", "Gideon's First Steps", "I still have a long road ahead of me...");
        add("Gideon, Champion of Justice", "Gideon's Aftermath", "Gideon stands alone!");
        add("Gideon, Ally of Zendikar", "Gideon's Anthem", "Giddy up! Let's all rally against the Eldrazi threat!");
        add("Jace Beleren", "Jace's Grind", "Nice memories... not!");
        add("Jace, Architect of Thought", "Jace's Incantation", "I like that one. I'll steal it!");
        add("Jace, Memory Adept", "Jace's Revelation", "So many things to learn!");
        add("Jace, Telepath Unbound", "Jace's Erasure", "First empty your mind and then... nope, that's it.");
        add("Jace, the Living Guildpact", "Jace's Timetwister", "Symmetry? What symmetry?");
        add("Jace, the Mind Sculptor", "Jace's Lobotomy", "What do you mean, \"overpowered\"?");
        add("Jace, Unraveler of Secrets", "Jace's Perfect Plan", "So, I've read about this neat guy named Erayo...");
        add("Karn Liberated", "Karn's Reset", "Let's do this again!");
        add("Kiora, Master of the Depths", "Kiora's Fight Club", "First rule is not to talk about the sucker punches");
        add("Kiora, the Crashing Wave", "Kiora's Kraken", "Say hello to Cthulhu for me!");
        add("Koth of the Hammer", "Koth's Eruption", "You won't like the mountains when they are angry!");
        add("Liliana Vess", "Liliana's Bidding", "Stop being lazy and go to work!");
        add("Liliana of the Dark Realms", "Liliana's Ritual", "What do you mean, \"out of character\"?");
        add("Liliana of the Veil", "Liliana's Choice", "Which one of your children do you love best?");
        add("Liliana, Defiant Necromancer", "Liliana's Necromastery", "Come join the Dark Side...");
        add("Liliana, the Last Hope", "Liliana's Army", "The dead will always outnumber the living...");
        add("Nahiri, the Harbinger", "Nahiri's Mystery Guest", "This should be your cue to run.");
        add("Nahiri, the Lithomancer", "Nahiri's Gift", "I pulled it out! Now I'm a king!");
        add("Narset Transcendent", "Narset's One Rule", "No spells for you!");
        add("Nicol Bolas, Planeswalker", "Bolas's Ultimatum", "Whatever you do, don't call him \"Nicol\"!");
        add("Nissa Revane", "Nissa's Summoning", "The whole village is here!");
        add("Nissa, Sage Animist", "Nissa's Wake-Up Call", "Wake up! Time to work!");
        add("Nissa, Voice of Zendikar", "Nissa's Bounty", "It's harvest time!");
        add("Nissa, Worldwaker", "Nissa's Awakening", "The whole country is here!");
        add("Ob Nixilis of the Black Oath", "Nixilis's Black Oath", "We all have to bring sacrifices sometimes.");
        add("Ob Nixilis Reignited", "Ob Nixilis's Torment", "I'll give you dreams from beyond the underworld!");
        add("Ral Zarek", "Ral's Long Day", "So much work, so much time!");
        add("Sarkhan Vol", "Sarkhan's Dragons", "Go forth, my minions!");
        add("Sarkhan the Mad", "Sarkhan's Beatdown", "Tag! You're it!");
        add("Sarkhan, the Dragonspeaker", "Sarkhan's Voices", "Huh? What are you saying?)");
        add("Sarkhan Unbroken", "Sarkhan's Dragonstorm", "Skies full of dragons! Oh, what a glorious day!");
        add("Sorin Markov", "Sorin's Hypnosis", "You're getting sleepy... very sleepy...");
        add("Sorin, Grim Nemesis", "Sorin's Vampire Army", "Taste the might of my blood relatives!");
        add("Sorin, Lord of Innistrad", "Sorin's Recruitment", "My favorite game is Shogi!");
        add("Sorin, Solemn Visitor", "Sorin's Abyss", "Don't get so close to the edge!");
        add("Tamiyo, Field Researcher", "Tamiyo's Omniscience", "I know exactly how your story ends...");
        add("Tamiyo, the Moon Sage", "Tamiyo's Recycling", "Wash, rinse, repeat!");
        add("Teferi, Temporal Archmage", "Teferi's Time Slip", "Let's speed this up a bit!");
        add("Tezzeret the Seeker", "Tezzeret's Robots", "You can't stop the progress!");
        add("Tezzeret, Agent of Bolas", "Tezzeret's Drain", "Technological superiority for the win!");
        add("Tibalt, the Fiend-Blooded", "Tibalt's Treason", "My side is the winning side!");
        add("Ugin, the Spirit Dragon", "Ugin's Anti-Ultimatum", "A ragtag band of misfits, brought from the future...");
        add("Venser, the Sojourner", "Venser's Oblivion", "Let's just clean this up a bit...");
        add("Vraska the Unseen", "Vraska's Crew", "Say hello to my little friends!");
        add("Xenagos, the Reveler", "Xenagos's Reveal", "Mwahahaha! Now I'm a god!");
    }

    private void add(String cardName0, String displayName0, String flavorText0) {
        add(new PlaneswalkerUltimate(cardName0, displayName0, flavorText0));
    }

    @Override
    public void updateAll(Player player) {
        //only call update achievements for any ultimates activated during the game
        if (player.getOutcome().hasWon()) {
            boolean needSave = false;
            for (String ultimate : player.getAchievementTracker().activatedUltimates) {
                Achievement achievement = achievements.get(ultimate);
                if (achievement != null) {
                    achievement.update(player);
                    needSave = true;
                }
            }
            if (needSave) {
                save();
            }
        }
    }

    private class PlaneswalkerUltimate extends ProgressiveAchievement {
        private PlaneswalkerUltimate(String cardName0, String displayName0, String flavorText0) {
            super(cardName0, displayName0, "Win a game after activating " + cardName0 + "'s ultimate", flavorText0);
        }

        @Override
        protected boolean eval(Player player, Game game) {
            return true; //if this reaches this point, it can be presumed that alternate win condition achieved
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(getKey());
        }

        @Override
        protected String getNoun() {
            return "Win";
        }
    }
}
