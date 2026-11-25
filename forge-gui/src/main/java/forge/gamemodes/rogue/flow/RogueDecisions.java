package forge.gamemodes.rogue.flow;

import forge.deck.Deck;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.rogue.BazaarPricing;
import forge.gamemodes.rogue.CardRewardHelper;
import forge.gamemodes.rogue.RogueCustomDeckGenerator;
import forge.gamemodes.rogue.RogueEvent;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.effect.BazaarItem;
import forge.gamemodes.rogue.effect.ChestEffect;
import forge.gamemodes.rogue.effect.ChoiceRerollContext;
import forge.gamemodes.rogue.effect.CursedEffect;
import forge.gamemodes.rogue.effect.NPCEffect;
import forge.gamemodes.rogue.effect.SanctumContext;
import forge.gamemodes.rogue.effect.WrathfulEffect;
import forge.gamemodes.rogue.npc.NPCContext;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** What a {@link RogueFlow} asks and announces. A question or an announcement blocks until the player is done with it. */
public interface RogueDecisions extends CardRewardHelper.RewardDialog {
    /** REPLACE: the picked cards take the place of others in the deck. */
    enum CardChoiceKind { STARTING_DECK, ADD, REMOVE, REPLACE }
    enum SanctumAction { HEAL, COOK, REFLECT, CUSTOM, SKIP }
    /** SPARK: a Spark won for a new highest Descension level. */
    enum ResultKind { EVENT, BOON, SANCTUM, REVIVE, DEFEAT, SPARK }

    /** choice is null when the player skips. */
    record Pick<T>(boolean reroll, T choice) { }
    record SanctumOffer(String restDescription, boolean restEnabled, String restDisabledReason,
                        SanctumContext context, RogueRun run) {
        /** In the order they are shown: Rest, Cook, Reflect, then the context's own. */
        public List<SanctumOption> options() {
            Localizer localizer = Localizer.getInstance();
            List<SanctumOption> options = new ArrayList<>();
            options.add(new SanctumOption(SanctumAction.HEAL, localizer.getMessage("lblRogueRest"), restDescription,
                restEnabled, restDisabledReason, null));
            options.add(new SanctumOption(SanctumAction.COOK, localizer.getMessage("lblRogueCook"),
                localizer.getMessage("lblRogueCookDescription", "{{Item}}"), true, null, null));
            options.add(new SanctumOption(SanctumAction.REFLECT, localizer.getMessage("lblRogueReflect"),
                localizer.getMessage("lblRogueReflectDescription", "{{Removal Credits}}"), true, null, null));
            for (SanctumContext.SanctumChoice choice : context.extraChoices) {
                options.add(new SanctumOption(SanctumAction.CUSTOM, choice.label(), choice.description(), true, null, choice));
            }
            return options;
        }

        public boolean accepts(SanctumAnswer answer) {
            return switch (answer.action()) {
                case HEAL -> restEnabled;
                case CUSTOM -> context.extraChoices.contains(answer.custom());
                case COOK, REFLECT, SKIP -> true;
            };
        }
    }
    record SanctumAnswer(SanctumAction action, SanctumContext.SanctumChoice custom) { }
    /** description may hold card and keyword markers. custom is set for a choice of the context's own. */
    record SanctumOption(SanctumAction action, String label, String description, boolean enabled, String disabledReason,
                         SanctumContext.SanctumChoice custom) {
        public SanctumAnswer answer() {
            return new SanctumAnswer(action, custom);
        }
    }
    record Heading(String title, String text) { }
    /** rerollLabel is null in a shop that has no reroll. */
    record ShopOffer(List<BazaarItem> items, RogueRun run, int gold, String title, String rerollLabel,
                     int rerollCost, boolean rerollEnabled, int tab) {
        public boolean accepts(ShopAnswer answer) {
            return items.containsAll(answer.selected())
                && BazaarPricing.calculateItemsTotalCost(answer.selected()) <= gold
                && (!answer.reroll() || rerollEnabled && rerollCost <= gold);
        }
    }
    record ShopAnswer(boolean reroll, Set<BazaarItem> selected, int tab) { }
    /** preview makes a deck of the cards picked so far, and is null when the deck to view is the run's. */
    record CardChoice(CardChoiceKind kind, String title, String subtitle, List<PaperCard> candidates, RogueRun run,
                      int min, int max, Function<List<PaperCard>, Deck> preview) {
        public boolean accepts(List<PaperCard> picked) {
            if (picked.size() < min || picked.size() > max) {
                return false;
            }
            List<PaperCard> left = new ArrayList<>(candidates);
            return picked.stream().allMatch(left::remove);
        }
    }
    record Section(String label, String text, List<PaperCard> cards) { }
    /** recordPublicCards is for the end of the game. */
    record RogueMatch(RogueRun run, NodePlanebound node, Set<GameType> variants, List<RegisteredPlayer> players,
                      RegisteredPlayer human, Consumer<HostedMatch> recordPublicCards) {
        /** The flow reads each game's result as the battle's, so a match is one game. */
        public GameRules rules() {
            GameRules rules = HostedMatch.getDefaultRules(GameType.RogueCommander);
            rules.setGamesPerMatch(1);
            return rules;
        }
    }

    /** Whether they are one eligible commander, or two that partner. */
    static boolean commanders(List<PaperCard> picked) {
        return !RogueCustomDeckGenerator.findChosenCommanders(picked).isEmpty();
    }

    /** What showPlaneboundEffects says above the effects. */
    static Heading planeboundEffects(List<WrathfulEffect> wrathful, List<CursedEffect> cursed) {
        String key = !wrathful.isEmpty() && !cursed.isEmpty() ? "lblRogueWrathfulAndCursed"
            : !cursed.isEmpty() ? "lblRogueCursed" : "lblRogueWrathful";
        return new Heading(Localizer.getInstance().getMessage(key), Localizer.getInstance().getMessage(key + "Message"));
    }

    /** Empty when the player cancels. */
    List<PaperCard> chooseCommanders();
    int choosePortrait(PaperCard commander, int current);
    List<PaperCard> chooseCards(CardChoice choice);
    boolean confirmReplaceRun();
    /** A story is a context with no choices. */
    Pick<NPCEffect> chooseBoon(NPCContext context, ChoiceRerollContext rerolls);
    Pick<ChestEffect> chooseChest(List<ChestEffect> loot, RogueRun run, ChoiceRerollContext rerolls);
    Pick<RogueEvent.EventChoice> chooseEvent(RogueEvent event, RogueRun run, ChoiceRerollContext rerolls);
    SanctumAnswer chooseSanctum(SanctumOffer offer);
    ShopAnswer shop(ShopOffer offer);
    /** Answers current when the player cancels. */
    <T> List<T> devPick(String title, List<T> all, List<T> current, int count);

    void showPlaneboundEffects(NodePlanebound node, List<WrathfulEffect> wrathful, List<CursedEffect> cursed);
    void showResult(ResultKind kind, String title, String text, List<Section> sections);
    void showMessage(String title, String text);
    /** The client decides whether to show it, and marks it seen. */
    void showTutorial(RogueTutorial tutorial);

    /** Returns once the match has started. It is the last thing a flow does. */
    void startMatch(RogueMatch match) throws Exception;
    void mapChanged(RogueRun run);
    void runStarted(RogueRun run);
    void runEnded();
}
