package forge.localinstance.achievements;

import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;
import forge.deck.Deck;
import forge.gamemodes.rogue.AetherUpgrade;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gamemodes.rogue.effect.RogueEffectComposite;
import forge.gamemodes.rogue.npc.GontiEncounter;
import forge.gamemodes.rogue.npc.HenzieEncounter;
import forge.gamemodes.rogue.npc.NPC;
import forge.gamemodes.rogue.npc.NarsetEncounter;
import forge.gamemodes.rogue.npc.TyvarEncounter;
import forge.item.PaperCard;
import forge.gui.GuiBase;
import forge.item.IPaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.sound.SoundEffectType;
import forge.sound.SoundSystem;
import forge.util.CardTranslation;
import forge.util.Localizer;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public class RogueCommanderAchievements extends AchievementCollection {
    public RogueCommanderAchievements() {
        super("lblRogueCommanderRuns", ForgeConstants.ACHIEVEMENTS_DIR + "rogue-commander.xml",
              false, ForgeConstants.ROGUE_COMMANDER_ACHIEVEMENT_LIST_FILE);
    }

    public static RogueCommanderAchievements getInstance() {
        return (RogueCommanderAchievements) FModel.getAchievements(GameType.RogueCommander);
    }

    @Override
    protected void addSharedAchievements() { }

    @Override
    protected void add(String cardName, String displayName, String flavorText) {
        add(new RogueCommanderRunWin(cardName, displayName, flavorText));
        add(new RogueCommanderHighestDescensionWin(cardName, displayName, flavorText));
    }

    @Override
    protected void addAchievements() {
        super.addAchievements(); // Load commander achievements from file
        add(new LifeAbundance());
        add(new GoldHoarder());
        add(new LegendaryArmy());
        add(new Speedrunner());
        add(new Roguelike());
        add(new Carrier());
        add(new Gifted());
        add(new HenzieBoonsUnlocked());
        add(new NarsetBoonsUnlocked());
        add(new TyvarBoonsUnlocked());
        add(new AllRogueCommandersUnlocked());
        add(new AetherFullyUpgraded());
        add(new BazaarFullyUpgraded());
        add(new CodexComplete());
        add(new PlaneboundSlayer());
        add(new EndlessModeWin());
    }

    @Override
    public void updateAll(Player player) {
        if (player.getGame().getRules().getGameType() != GameType.RogueCommander) {
            return;
        }
        for (Achievement achievement : achievements.values()) {
            achievement.update(player);
        }
        save();
    }

    /**
     * Evaluate run-level and deck-level achievements using RogueRun state.
     * Called from: handleVictory(), handleDefeat(), CEditorRogue.canSwitchAway()
     */
    public void evaluateRunAchievements(RogueRun run) {
        if (run == null) return;

        // Life Abundant: have 50+ life at the end of a match, before persistent life is clamped
        if (run.getLastMatchRawLife() >= 50) {
            updateAchievement("LifeAbundance");
        }

        // Gold Hoarder: have 15+ gold
        if (run.getCurrentGold() >= 15) {
            updateAchievement("GoldHoarder");
        }

        List<RoguePlanebound> planebounds = RogueConfig.loadPlanebounds();
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        if (!planebounds.isEmpty()
            && planebounds.stream().allMatch(planebound -> progress.getPlaneboundWins(planebound) > 0)) {
            updateAchievement("PlaneboundSlayer");
        }

        // Legendary Army: 20+ legendary permanents in deck
        Deck deck = run.getCurrentDeck();
        if (deck != null && deck.getMain() != null) {
            int legendaryCount = 0;
            for (PaperCard card : deck.getMain().toFlatList()) {
                if (card.getRules().getType().isLegendary() && card.getRules().getType().isPermanent()) {
                    legendaryCount++;
                }
            }
            if (legendaryCount > 20) {
                updateAchievement("LegendaryArmy");
            }
        }

        if (run.isRunWon()) {
            if (run.getRunTimeMillis() < 20 * 60 * 1000) {
                updateAchievement("Speedrunner");
            }
            if (run.getActiveAetherEffects().isEmpty()) {
                updateAchievement("Roguelike");
            }
            if (run.getCarryCards().size() >= 4) {
                updateAchievement("Carrier");
            }

            long traitCount = RogueEffectComposite.getAllEffects(run).stream()
                .filter(effect -> effect.getEffectType() == RogueEffect.EffectType.PERMANENT)
                .map(RogueEffect::getEffectCard)
                .filter(card -> card != null && card.getRules() != null
                    && card.getRules().getType().hasSubtype("Trait"))
                .count();
            if (traitCount >= 3) {
                updateAchievement("Gifted");
            }
            if (run.getDescensionLevel() == DescensionLevel.getMaxLevel()) {
                updateAchievement(getHighestDescensionAchievementKey(run.getCurrentCommanderName()));
            }
        }
    }

    public void evaluateNpcBoonUnlockAchievements(RogueMetaProgress progress) {
        if (progress == null) {
            return;
        }
        if (progress.getNPCLevel(NPC.HENZIE.id) >= HenzieEncounter.OFFERING_BOONS.getRequiredLevel()) {
            updateAchievement("HenzieBoonsUnlocked");
        }
        if (progress.getNPCLevel(NPC.NARSET.id) >= NarsetEncounter.OFFERING_BOONS.getRequiredLevel()) {
            updateAchievement("NarsetBoonsUnlocked");
        }
        if (progress.getNPCLevel(NPC.TYVAR.id) >= TyvarEncounter.OFFERING_BOONS.getRequiredLevel()) {
            updateAchievement("TyvarBoonsUnlocked");
        }
    }

    public void evaluateCommanderUnlockAchievements() {
        List<RogueDeck> decks = RogueConfig.loadRogueDecks().stream()
            .filter(deck -> !deck.isExperimental())
            .collect(Collectors.toList());
        if (decks.isEmpty()) {
            return;
        }
        for (RogueDeck deck : decks) {
            if (!deck.isUnlocked()) {
                return;
            }
        }
        updateAchievement("AllRogueCommandersUnlocked");
    }

    public void evaluateUpgradeAchievements(RogueMetaProgress progress) {
        if (progress == null) {
            return;
        }
        if (progress.getAetherUpgradeLevel() >= AetherUpgrade.getMaxLevel()) {
            updateAchievement("AetherFullyUpgraded");
        }
        if (progress.getNPCLevel(NPC.GONTI.id)
            >= GontiEncounter.OFFERING_DISCOUNTED_TRAITS_AND_CARRY_CARDS.getRequiredLevel()) {
            updateAchievement("BazaarFullyUpgraded");
        }
    }

    public void evaluateCodexAchievements(RogueMetaProgress progress) {
        if (CodexHelper.isCodexComplete(progress)) {
            updateAchievement("CodexComplete");
        }
    }

    private static String getHighestDescensionAchievementKey(String commanderName) {
        return commanderName + " Highest Descension";
    }

    /** Update a non-tiered (special) achievement to earned state using loadFromXml. */
    private void updateAchievement(String key) {
        Achievement a = achievements.get(key);
        if (a == null || a.getBest() > 0) return; // already earned or not found

        try {
            Element el = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .newDocument().createElement("a");
            el.setAttribute("best", "1");
            el.setAttribute("time", String.valueOf(new Date().getTime()));
            a.loadFromXml(el);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        a.updateTrophyImage();
        SoundSystem.instance.play(SoundEffectType.Enchantment,
            SoundEffectType.Enchantment.isSynced());
        GuiBase.getInterface().showImageDialog(a.getImage(),
            a.getDisplayName() + "\n" + a.getSharedDesc() + "\n" + a.getMythicDesc(),
            Localizer.getInstance().getMessage("lblAchievementEarned"));
        save();
    }

    public void recordRunWon(String commanderName) {
        Achievement a = achievements.get(commanderName);
        if (a == null) return;

        boolean firstWin = a.getBest() == 0;

        try {
            Element el = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .newDocument().createElement("a");
            el.setAttribute("best", String.valueOf(a.getBest() + 1));
            el.setAttribute("time", String.valueOf(new Date().getTime()));
            a.loadFromXml(el);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (firstWin) {
            a.updateTrophyImage();
            SoundSystem.instance.play(SoundEffectType.Enchantment,
                SoundEffectType.Enchantment.isSynced());
            GuiBase.getInterface().showImageDialog(a.getImage(),
                a.getDisplayName() + "\n" + a.getSharedDesc() + "\n" + a.getMythicDesc(),
                Localizer.getInstance().getMessage("lblAchievementEarned"));
        }
        save();
    }

    public void recordEndlessLevelWon(int level) {
        Achievement a = achievements.get("EndlessMode");
        if (a == null || level < 1 || level > 4 || level <= a.getBest()) {
            return;
        }

        try {
            Element el = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .newDocument().createElement("a");
            el.setAttribute("best", String.valueOf(level));
            el.setAttribute("time", String.valueOf(new Date().getTime()));
            a.loadFromXml(el);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String tier = switch (level) {
            case 1 -> Localizer.getInstance().getMessage("lblCommon");
            case 2 -> Localizer.getInstance().getMessage("lblUncommon");
            case 3 -> Localizer.getInstance().getMessage("lblRare");
            default -> Localizer.getInstance().getMessage("lblMythic");
        };
        String description = switch (level) {
            case 1 -> a.getCommonDesc();
            case 2 -> a.getUncommonDesc();
            case 3 -> a.getRareDesc();
            default -> a.getMythicDesc();
        };
        a.updateTrophyImage();
        SoundSystem.instance.play(SoundEffectType.Enchantment,
            SoundEffectType.Enchantment.isSynced());
        GuiBase.getInterface().showImageDialog(a.getImage(),
            a.getDisplayName() + " (" + tier + ")\n" + a.getSharedDesc() + " " + description,
            Localizer.getInstance().getMessage("lblAchievementEarned"));
        save();
    }

    private static class EndlessModeWin extends Achievement {
        private EndlessModeWin() {
            super("EndlessMode", "Endless Mode", "Complete an Endless Trail at", 0,
                "Level 1.", 1,
                "Level 2.", 2,
                "Level 3.", 3,
                "Level 4.", 4);
        }

        @Override
        protected int evaluate(Player player, Game game) {
            return 0;
        }

        @Override
        protected String getNoun() {
            return "Endless Level";
        }

        @Override
        protected boolean displayNounBefore() {
            return true;
        }
    }

    private class RogueCommanderRunWin extends ProgressiveAchievement {
        private RogueCommanderRunWin(String cardName, String displayName, String flavorText) {
            super(cardName, displayName,
                  "Win a Rogue Commander run with " + CardTranslation.getTranslatedName(cardName),
                  flavorText);
            setHidden(true);
        }

        @Override
        protected boolean eval(Player player, Game game) {
            return false; // Not used; commander wins are tracked via recordRunWon()
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(getKey());
        }

        @Override
        protected String getNoun() {
            return Localizer.getInstance().getMessage("lblWin");
        }
    }

    private class RogueCommanderHighestDescensionWin extends Achievement {
        private final String commanderName;

        private RogueCommanderHighestDescensionWin(String cardName, String displayName, String flavorText) {
            super(getHighestDescensionAchievementKey(cardName), getDescensionDisplayName(displayName),
                  "Win a Rogue Commander run with " + CardTranslation.getTranslatedName(cardName)
                      + " on the highest Descension Level",
                  flavorText, 0);
            this.commanderName = cardName;
            setHidden(true);
        }

        @Override
        protected int evaluate(Player player, Game game) {
            return 0; // Not used; evaluated via evaluateRunAchievements()
        }

        @Override
        public IPaperCard getPaperCard() {
            return FModel.getMagicDb().getCommonCards().getCard(commanderName);
        }

        @Override
        protected String getNoun() {
            return null;
        }

        @Override
        public String getSubTitle(boolean includeTimestamp) {
            if (includeTimestamp) {
                String formattedTimestamp = getFormattedTimestamp();
                if (formattedTimestamp != null) {
                    return "Earned " + formattedTimestamp;
                }
            }
            return null;
        }
    }

    private static String getDescensionDisplayName(String displayName) {
        int possessiveIndex = displayName.indexOf("'s ");
        if (possessiveIndex > 0) {
            return displayName.substring(0, possessiveIndex) + "'s Descension";
        }
        return displayName + "'s Descension";
    }
}
