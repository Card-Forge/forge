package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.card.CardZoom;
import forge.deck.Deck;
import forge.gamemodes.rogue.CodexHelper.CodexState;
import forge.gamemodes.rogue.CodexHelper.JournalEntry;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueConfig;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.TextHelper;
import forge.gamemodes.rogue.effect.RogueEffect;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.achievements.RogueCommanderAchievements;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the player has met across runs: stats, commanders, planebounds, traits and the journal. Sideways, the
 * commander, planebound and journal pages keep their list beside the details; upright, the list opens the details.
 */
public class RogueCodexScreen extends RogueScreen {
    private enum Page { STATS, COMMANDERS, PLANEBOUNDS, TRAITS, JOURNAL }

    private static final Color LINE = new Color(0.36f, 0.32f, 0.27f, 0.4f);

    private Page page = Page.STATS;
    private RogueDeck commander;
    private RoguePlanebound planebound;
    private JournalEntry entry;
    private boolean detail;
    private float slide;
    private final Pane side;
    private final Pane main;
    private final RogueButton back;
    private final List<RogueButton> pageTabs = new ArrayList<>();

    public RogueCodexScreen() {
        super(null);
        Localizer localizer = Localizer.getInstance();
        side = add(new Pane());
        main = add(new Pane());
        back = add(new RogueButton(localizer.getMessage("lblBack"), null, RogueButton.State.NORMAL, this::goBack));
        String[] names = {localizer.getMessage("lblRogueGlobalStats"), localizer.getMessage("lblCommanders"),
            localizer.getMessage("lblRoguePlanebounds"), localizer.getMessage("lblRogueTraits"), localizer.getMessage("lblRogueJournal")};
        for (Page p : Page.values()) {
            pageTabs.add(add(new RogueButton(names[p.ordinal()], null, RogueButton.State.NORMAL, () -> show(p))));
        }
    }

    @Override
    public void onActivate() {
        RogueController.get().submitIfIdle(() -> RogueCommanderAchievements.getInstance().evaluateCodexAchievements(RogueMetaProgress.getInstance()));
        RogueController.get().showTutorials(List.of(RogueTutorial.CODEX));
        build();
    }

    /** The new page slides in from the side its tab is on. */
    private void show(Page next) {
        int side = Integer.signum(next.ordinal() - page.ordinal());
        page = next;
        detail = false;
        build();
        if (side != 0) {
            RogueMotion.play(0.35f, t -> {
                slide = side * getWidth() * (1 - Interpolation.pow3Out.apply(t));
                revalidate();
            }, null);
        }
    }

    private void goBack() {
        if (detail && !Forge.isLandscapeMode()) {
            detail = false;
            build();
        } else {
            Forge.back();
        }
    }

    private boolean twoPanes() {
        return Forge.isLandscapeMode() && (page == Page.COMMANDERS || page == Page.PLANEBOUNDS || page == Page.JOURNAL);
    }

    private void build() {
        for (Page p : Page.values()) {
            pageTabs.get(p.ordinal()).setState(p == page ? RogueButton.State.SELECTED : RogueButton.State.NORMAL);
        }
        side.clearBlocks();
        main.clearBlocks();
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        switch (page) {
            case STATS -> stats(progress);
            case COMMANDERS -> commanders(progress);
            case PLANEBOUNDS -> planebounds(progress);
            case TRAITS -> traits(progress);
            case JOURNAL -> journal(progress);
        }
        side.setVisible(twoPanes());
        side.revalidate();
        main.revalidate();
        main.scrollToTop();
        revalidate();
    }

    private Pane listPane() {
        return twoPanes() ? side : main;
    }

    private boolean showsDetail() {
        return twoPanes() || detail;
    }

    private void stats(RogueMetaProgress progress) {
        Localizer l = Localizer.getInstance();
        int completed = progress.getTotalRunsCompleted();
        int won = progress.getTotalRunsWon();
        main.put(new Stats(List.of(
            List.of(l.getMessage("lblRogueRunStatistics"), l.getMessage("lblRogueRunsStarted", progress.getTotalRunsStarted()),
                l.getMessage("lblRogueRunsCompleted", completed), l.getMessage("lblRogueRunsWon", won),
                l.getMessage("lblRogueRunsLost", completed - won),
                l.getMessage("lblRogueTotalPlaytime", TextHelper.formatHoursMinutesSeconds(progress.getTotalPlaytimeMillis()))),
            List.of(l.getMessage("lblRogueMatchStatistics"), l.getMessage("lblRogueMatchesWon", progress.getTotalMatchesWon()),
                l.getMessage("lblRogueMatchesLost", progress.getTotalMatchesLost())),
            List.of(l.getMessage("lblRogueMilestones"), l.getMessage("lblRogueMaxLife", progress.getMaxLifeInRun()),
                l.getMessage("lblRogueMaxGold", progress.getMaxGoldInRun()),
                l.getMessage("lblRogueMaxCreatureTypes", progress.getMaxCreatureTypesInDeck()),
                l.getMessage("lblRogueMaxArtifacts", progress.getMaxArtifactsInDeck()),
                l.getMessage("lblRogueMaxSharedCreatureType", progress.getMaxSharedCreatureTypeInDeck()),
                l.getMessage("lblRogueMaxLegendaryPermanents", progress.getMaxLegendaryPermanentsInDeck()),
                l.getMessage("lblRogueMaxNonbasicLands", progress.getMaxNonbasicLandsInDeck())))));
        List<RogueButton> buttons = new ArrayList<>();
        buttons.add(new RogueButton(l.getMessage("lblRogueResetTutorials"), null, RogueButton.State.NORMAL,
            () -> confirm("lblRogueResetTutorials", "lblRogueResetTutorialsConfirm", () -> RogueMetaProgress.getInstance().resetTutorials())));
        buttons.add(new RogueButton(l.getMessage("lblRogueResetProgress"), null, RogueButton.State.NORMAL,
            () -> confirm("lblRogueResetGameProgression", "lblRogueResetProgressConfirm", () -> {
                RogueMetaProgress.getInstance().reset();
                commander = null;
                planebound = null;
                entry = null;
            })));
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            buttons.add(new RogueButton(l.getMessage("lblRogueDevUnlockCodex"), null, RogueButton.State.NORMAL, () -> act(() -> {
                CodexHelper.unlockCodex();
                RogueCommanderAchievements.getInstance().evaluateCodexAchievements(RogueMetaProgress.getInstance());
            })));
        }
        main.put(new Buttons(buttons));
    }

    private void confirm(String titleKey, String textKey, Runnable change) {
        Localizer l = Localizer.getInstance();
        RogueDialog.show(l.getMessage(titleKey), l.getMessage(textKey), List.of(l.getMessage("lblCancel"), l.getMessage("lblReset")),
            choice -> {
                if (choice == 1) {
                    act(change);
                }
            });
    }

    /** Changes to progress can announce achievements, so they run on the flow thread; the page follows. */
    private void act(Runnable change) {
        RogueController.get().submit(() -> {
            change.run();
            FThreads.invokeInEdtLater(this::build);
        });
    }

    private void commanders(RogueMetaProgress progress) {
        Localizer l = Localizer.getInstance();
        List<RogueDeck> all = CodexHelper.codexCommanders();
        if (commander == null || all.stream().noneMatch(d -> d.isUnlocked() && d.getCommanderCardName().equals(commander.getCommanderCardName()))) {
            commander = all.stream().filter(RogueDeck::isUnlocked).findFirst().orElse(null);
        }
        boolean compact = twoPanes();
        if (!showsDetail() || compact) {
            Pane list = listPane();
            if (!compact) {
                list.put(new Head(l.getMessage("lblRogueCommanders"), 1.4f, RogueLook.INK));
            }
            for (RogueDeck deck : all) {
                boolean open = deck.isUnlocked();
                String sub = open && !compact ? l.getMessage("lblRogueSeenCards", CodexHelper.countSeen(rewardStates(deck, progress)),
                    deck.getRewardPoolCards().size()) : null;
                list.put(new Choice(open ? deck.getAvatarIndex() : -1, open ? deck.getCommanderCardName() : l.getMessage("lblRogueLockedCommander"),
                    sub, open, compact && deck == commander, compact, () -> {
                        commander = deck;
                        detail = true;
                        build();
                    }));
            }
        }
        if (!showsDetail()) {
            return;
        }
        if (!compact) {
            main.put(new Link("< " + l.getMessage("lblCommanders"), () -> {
                detail = false;
                build();
            }));
        }
        if (commander == null) {
            main.put(new Note(l.getMessage("lblRogueNoUnlockedCommanders"), RogueLook.SOFT, Align.center));
            return;
        }
        List<Entry> nonMythics = new ArrayList<>();
        List<Entry> mythics = new ArrayList<>();
        for (PaperCard card : commander.getRewardPoolCards()) {
            (PaperCardPredicates.IS_MYTHIC_RARE.test(card) ? mythics : nonMythics).add(new Entry(card, CodexHelper.state(progress, commander, card)));
        }
        Deck start = commander.getStartDeck();
        List<Entry> startDeck = start == null ? new ArrayList<>() : deckEntries(start.getMain().toFlatList(), card -> CodexState.ACQUIRED);
        List<Entry> commanderCards = new ArrayList<>();
        if (start != null) {
            for (PaperCard card : start.getCommanders()) {
                commanderCards.add(new Entry(card, CodexState.ACQUIRED));
            }
        }
        sort(nonMythics);
        sort(mythics);
        sort(startDeck);
        List<CodexState> rewards = rewardStates(commander, progress);
        String name = commander.getCommanderCardName();
        main.put(new Head(commander.getName(), 1.3f, RogueLook.INK));
        main.put(new Note(String.join("   ", l.getMessage("lblRogueCommanderRunCount", progress.getRunsStartedWithCommander(name)),
            l.getMessage("lblRogueCommanderWins", progress.getRunsWonWithCommander(name)),
            l.getMessage("lblRogueMaxDescension", progress.getMaxDescensionWon(name)),
            l.getMessage("lblRogueMaxEndless", progress.getMaxEndlessCompleted(name)),
            l.getMessage("lblRogueSeenCards", CodexHelper.countSeen(rewards), rewards.size()),
            l.getMessage("lblRogueAcquiredCards", CodexHelper.countAcquired(rewards), rewards.size())), RogueLook.SOFT, Align.left));
        section(l.getMessage("lblCommander"), commanderCards);
        section(l.getMessage("lblRogueRewardPoolNonMythics"), nonMythics);
        section(l.getMessage("lblRogueRewardPoolMythics"), mythics);
        section(l.getMessage("lblRogueStartDeck"), startDeck);
    }

    private static List<CodexState> rewardStates(RogueDeck deck, RogueMetaProgress progress) {
        List<CodexState> states = new ArrayList<>();
        for (PaperCard card : deck.getRewardPoolCards()) {
            states.add(CodexHelper.state(progress, deck, card));
        }
        return states;
    }

    private void section(String title, List<Entry> entries) {
        main.put(new Head(title, 1.1f, RogueLook.DEEP));
        main.put(new Grid(entries));
    }

    private void planebounds(RogueMetaProgress progress) {
        Localizer l = Localizer.getInstance();
        List<RoguePlanebound> all = CodexHelper.codexPlanebounds();
        if (planebound == null || all.stream().noneMatch(p -> p.deckPath().equals(planebound.deckPath()) && progress.hasEncounteredPlanebound(p))) {
            planebound = all.stream().filter(progress::hasEncounteredPlanebound).findFirst().orElse(null);
        }
        boolean compact = twoPanes();
        if (!showsDetail() || compact) {
            Pane list = listPane();
            if (!compact) {
                list.put(new Head(l.getMessage("lblRoguePlanebounds"), 1.4f, RogueLook.INK));
            }
            for (RoguePlaneboundType type : RoguePlaneboundType.values()) {
                list.put(new Head(type.name(), 1, RogueLook.DEEP));
                for (RoguePlanebound p : all) {
                    if (p.type() != type) {
                        continue;
                    }
                    boolean known = progress.hasEncounteredPlanebound(p);
                    list.put(new Choice(known ? p.avatarIndex() : -1, known ? p.planeboundName() : l.getMessage("lblRogueUnknownPlanebound"),
                        known && !compact ? p.planeName() : null, known, compact && p == planebound, compact, () -> {
                            planebound = p;
                            detail = true;
                            build();
                        }));
                }
            }
        }
        if (!showsDetail()) {
            return;
        }
        if (!compact) {
            main.put(new Link("< " + l.getMessage("lblRoguePlanebounds"), () -> {
                detail = false;
                build();
            }));
        }
        if (planebound == null) {
            main.put(new Note(l.getMessage("lblRogueEncounterAPlaneboundToUnlockItsDeck"), RogueLook.SOFT, Align.center));
            return;
        }
        RoguePlanebound shown = planebound;
        Deck deck = RogueConfig.loadPlaneboundDeck(shown);
        List<Entry> commanders = new ArrayList<>();
        List<Entry> cards = new ArrayList<>();
        if (deck != null) {
            for (PaperCard card : deck.getCommanders()) {
                commanders.add(new Entry(card, CodexHelper.state(progress, shown, card)));
            }
            cards = deckEntries(deck.getAllCardsInASinglePool(false, false).toFlatList(), card -> CodexHelper.state(progress, shown, card));
        }
        sort(commanders);
        sort(cards);
        List<CodexState> states = cards.stream().map(Entry::state).collect(Collectors.toList());
        main.put(new Head(shown.planeboundName() + " - " + shown.planeName(), 1.3f, RogueLook.INK));
        main.put(new Note(String.join("   ", l.getMessage("lblRoguePlaneboundSlain", progress.getPlaneboundWins(shown)),
            l.getMessage("lblRoguePlaneboundDefeats", progress.getPlaneboundLosses(shown)),
            l.getMessage("lblRogueSeenCards", CodexHelper.countSeen(states), states.size())), RogueLook.SOFT, Align.left));
        main.put(new PlaneboundTop(commanders, CodexHelper.getPlaneCard(shown)));
        section(l.getMessage("lblDeck"), cards);
    }

    private void traits(RogueMetaProgress progress) {
        Localizer l = Localizer.getInstance();
        Map<CodexHelper.TraitCategory, List<RogueEffect>> byCategory = CodexHelper.getCodexTraitsByCategory();
        List<CodexState> states = new ArrayList<>();
        List<Runnable> sections = new ArrayList<>();
        for (CodexHelper.TraitCategory category : CodexHelper.TraitCategory.values()) {
            List<TraitTile> tiles = new ArrayList<>();
            for (RogueEffect effect : byCategory.getOrDefault(category, List.of())) {
                if (effect.getEffectCard() == null) {
                    continue;
                }
                CodexState state = CodexHelper.state(progress, effect);
                states.add(state);
                tiles.add(new TraitTile(effect, state));
            }
            sections.add(() -> {
                main.put(new Head(category.label(), 1.2f, RogueLook.DEEP));
                main.put(new TraitColumns(tiles));
            });
        }
        main.put(new Note(l.getMessage("lblRogueSeenTraits", CodexHelper.countSeen(states), states.size()) + "    "
            + l.getMessage("lblRogueAcquiredTraits", CodexHelper.countAcquired(states), states.size()), RogueLook.INK, Align.left));
        sections.forEach(Runnable::run);
    }

    private void journal(RogueMetaProgress progress) {
        Localizer l = Localizer.getInstance();
        List<JournalEntry> tutorials = new ArrayList<>();
        List<JournalEntry> story = new ArrayList<>();
        for (JournalEntry e : CodexHelper.journal(progress)) {
            if (e.story()) {
                story.add(e);
            } else if (RogueNodeQuestions.DESKTOP_ONLY.stream().noneMatch(t -> t.getId().equals(e.id()))) {
                tutorials.add(e);
            }
        }
        List<JournalEntry> all = new ArrayList<>(tutorials);
        all.addAll(story);
        if (entry == null || all.stream().noneMatch(e -> e.id().equals(entry.id()))) {
            entry = all.isEmpty() ? null : all.get(0);
        }
        boolean compact = twoPanes();
        if (!showsDetail() || compact) {
            Pane list = listPane();
            journalSection(list, l.getMessage("lblRogueJournalTutorials"), l.getMessage("lblRogueJournalNoTutorials"), tutorials, compact);
            journalSection(list, l.getMessage("lblRogueJournalStory"), l.getMessage("lblRogueJournalNoStory"), story, compact);
        }
        if (!showsDetail() || entry == null) {
            return;
        }
        if (!compact) {
            main.put(new Link("< " + l.getMessage("lblRogueJournal"), () -> {
                detail = false;
                build();
            }));
        }
        main.put(new Head(entry.title(), 1.4f, RogueLook.INK));
        main.put(new Body(entry.text()));
    }

    private void journalSection(Pane list, String title, String empty, List<JournalEntry> entries, boolean compact) {
        list.put(new Head("- " + title, 1.2f, RogueLook.DEEP));
        if (entries.isEmpty()) {
            list.put(new Note(empty, RogueLook.SOFT, Align.left));
        }
        for (JournalEntry e : entries) {
            list.put(new Choice(-2, e.title(), null, true, compact && entry != null && e.id().equals(entry.id()), true, () -> {
                entry = e;
                detail = true;
                build();
            }));
        }
    }

    private interface StateFor {
        CodexState of(PaperCard card);
    }

    /** One entry per card, but a basic land only once. */
    private static List<Entry> deckEntries(List<PaperCard> cards, StateFor state) {
        List<Entry> entries = new ArrayList<>();
        for (PaperCard card : CodexHelper.eachBasicOnce(cards)) {
            entries.add(new Entry(card, state.of(card)));
        }
        return entries;
    }

    private static void sort(List<Entry> entries) {
        entries.sort(Comparator.comparing(Entry::card, CodexHelper.CODEX_CARD_ORDER));
    }

    @Override
    protected String background() {
        return "spellsmith";
    }

    @Override
    protected float backgroundDim() {
        return 0.15f;
    }

    @Override
    protected int tab() {
        return 3;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(pageTabs, List.of(back));
    }

    @Override
    protected List<RogueButton> wideRow() {
        List<RogueButton> row = new ArrayList<>();
        row.add(back);
        row.addAll(pageTabs);
        return row;
    }

    private float sideWidth() {
        return (page == Page.JOURNAL ? 150 : 120) * RogueLook.unit();
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        if (twoPanes()) {
            float sw = sideWidth();
            side.setBounds(slide + 6 * u, 6 * u, sw - 6 * u, h - 9 * u);
            main.setBounds(slide + sw + 9 * u, 6 * u, w - sw - 15 * u, h - 9 * u);
        } else {
            main.setBounds(slide + 9 * u, 6 * u, w - 18 * u, h - 9 * u);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float bottom = main.getBottom() + 3 * u;
        if (twoPanes()) {
            float sw = sideWidth();
            RogueLook.paper(g, slide + 3 * u, 3 * u, sw, bottom - 3 * u);
            RogueLook.paper(g, slide + sw + 6 * u, 3 * u, getWidth() - sw - 9 * u, bottom - 3 * u);
        } else {
            RogueLook.paper(g, slide + 3 * u, 3 * u, getWidth() - 6 * u, bottom - 3 * u);
        }
    }

    private record Entry(PaperCard card, CodexState state) {
    }

    /** A column of blocks, each as wide as the pane. */
    private static class Pane extends RogueScrollPane {
        private final List<Block> blocks = new ArrayList<>();

        void clearBlocks() {
            clear();
            blocks.clear();
        }

        void put(Block block) {
            blocks.add(super.add(block));
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            float y = 4 * u;
            float w = visibleWidth - 8 * u;
            for (Block block : blocks) {
                float h = block.height(w);
                block.setBounds(4 * u, y, w, h);
                y += h;
            }
            return new ScrollBounds(visibleWidth, y + 6 * u);
        }
    }

    private abstract static class Block extends FContainer {
        abstract float height(float w);

        @Override
        protected void doLayout(float width, float height) {
        }
    }

    private static class Head extends Block {
        private final String text;
        private final float scale;
        private final Color color;

        Head(String text, float scale, Color color) {
            this.text = text;
            this.scale = scale;
            this.color = color;
        }

        @Override
        float height(float w) {
            return RogueLook.lineHeight(scale) + 8 * RogueLook.unit();
        }

        @Override
        protected void drawBackground(Graphics g) {
            RogueLook.fit(g, text, 0, 4 * RogueLook.unit(), scale, color, getWidth(), Align.left);
        }
    }

    private static class Note extends Block {
        private final String text;
        private final Color color;
        private final int align;

        Note(String text, Color color, int align) {
            this.text = text;
            this.color = color;
            this.align = align;
        }

        @Override
        float height(float w) {
            float u = RogueLook.unit();
            return RogueLook.wrappedHeight(text, w, 1) + (align == Align.center ? 40 : 6) * u;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            RogueLook.wrap(g, text, 0, (align == Align.center ? 20 : 2) * u, getWidth(), 1, color, align);
        }
    }

    private static class Link extends Block {
        private final String text;
        private final Runnable command;

        Link(String text, Runnable command) {
            this.text = text;
            this.command = command;
        }

        @Override
        float height(float w) {
            return 20 * RogueLook.unit();
        }

        @Override
        public boolean tap(float x, float y, int count) {
            command.run();
            return true;
        }

        @Override
        protected void drawBackground(Graphics g) {
            RogueLook.text(g, text, 0, 3 * RogueLook.unit(), 1, RogueLook.SOFT);
        }
    }

    /** A row to pick: a commander, a planebound or a journal entry. avatar is -1 for the unknown face, -2 for none. */
    private static class Choice extends Block {
        private final int avatar;
        private final String name;
        private final String sub;
        private final boolean open;
        private final boolean chosen;
        private final boolean compact;
        private final Runnable command;

        Choice(int avatar, String name, String sub, boolean open, boolean chosen, boolean compact, Runnable command) {
            this.avatar = avatar;
            this.name = name;
            this.sub = sub;
            this.open = open;
            this.chosen = chosen;
            this.compact = compact;
            this.command = command;
        }

        @Override
        float height(float w) {
            return (compact ? 25 : 44) * RogueLook.unit();
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (open) {
                command.run();
            }
            return true;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight() - 2 * u;
            if (!open) {
                RogueLook.dashTile(g, 0, 0, w, h);
            } else if (chosen || !compact) {
                RogueLook.tile(g, 0, 0, w, h, chosen);
            }
            float s = compact ? 17 * u : 30 * u;
            float tx = 8 * u;
            if (avatar != -2) {
                RogueLook.avatar(g, avatar < 0 ? 125 : avatar, 6 * u, (h - s) / 2, s);
                tx = s + 13 * u;
            }
            Color color = open ? RogueLook.INK : RogueLook.SOFT;
            if (sub == null) {
                RogueLook.fit(g, name, tx, h / 2 - 7 * u, compact ? 0.95f : 1.15f, color, w - tx - 4 * u, Align.left);
            } else {
                RogueLook.fit(g, name, tx, 4 * u, 1.15f, color, w - tx - 6 * u, Align.left);
                RogueLook.fit(g, sub, tx, 21 * u, 1, RogueLook.SOFT, w - tx - 6 * u, Align.left);
            }
        }
    }

    /** Card faces by what is known of them: a "?" for unknown, faded for seen. A tap zooms a known card. */
    private static class Grid extends Block {
        private final List<Entry> entries;

        Grid(List<Entry> entries) {
            this.entries = entries;
            for (int i = 0; i < entries.size(); i++) {
                add(new Tile(i));
            }
        }

        private int cols() {
            return Forge.isLandscapeMode() ? 7 : 4;
        }

        private float cardWidth(float w) {
            return (w - (cols() - 1) * 6 * RogueLook.unit()) / cols();
        }

        @Override
        float height(float w) {
            float u = RogueLook.unit();
            int rows = (entries.size() + cols() - 1) / cols();
            return rows * (cardWidth(w) * RogueLook.CARD_RATIO + 6 * u) + 6 * u;
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            float cw = cardWidth(width);
            float ch = cw * RogueLook.CARD_RATIO;
            int i = 0;
            for (FDisplayObject child : getChildren()) {
                child.setBounds((i % cols()) * (cw + 6 * u), (i / cols()) * (ch + 6 * u), cw, ch);
                i++;
            }
        }

        private class Tile extends FDisplayObject {
            private final int index;

            Tile(int index) {
                this.index = index;
            }

            @Override
            public boolean tap(float x, float y, int count) {
                return longPress(x, y);
            }

            @Override
            public boolean longPress(float x, float y) {
                Entry e = entries.get(index);
                if (e.state() != CodexState.UNKNOWN) {
                    CardZoom.show(e.card());
                }
                return true;
            }

            @Override
            public void draw(Graphics g) {
                float w = getWidth();
                float h = getHeight();
                Entry e = entries.get(index);
                if (e.state() == CodexState.UNKNOWN) {
                    RogueLook.dashTile(g, 0, 0, w, h);
                    RogueLook.text(g, "?", 0, h / 2 - 14 * RogueLook.unit(), 2, RogueLook.SOFT, w, Align.center);
                } else {
                    RogueLook.card(g, e.card(), 0, 0, w, e.state() == CodexState.SEEN ? 0.45f : 1);
                }
            }
        }
    }

    /** The planebound's commander cards and its plane in one row, the plane as tall as a card. */
    private static class PlaneboundTop extends Block {
        private final List<Entry> commanders;
        private final PaperCard plane;

        PlaneboundTop(List<Entry> commanders, PaperCard plane) {
            this.commanders = commanders;
            this.plane = plane;
        }

        private static float cardWidth(float w) {
            int cols = Forge.isLandscapeMode() ? 7 : 4;
            return (w - (cols - 1) * 6 * RogueLook.unit()) / cols;
        }

        private float planeLeft(float w) {
            return commanders.size() * (cardWidth(w) + 6 * RogueLook.unit()) + 6 * RogueLook.unit();
        }

        @Override
        float height(float w) {
            float u = RogueLook.unit();
            return 22 * u + cardWidth(w) * RogueLook.CARD_RATIO + 8 * u;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            float u = RogueLook.unit();
            float w = getWidth();
            if (plane != null && x >= planeLeft(w)) {
                CardZoom.show(plane);
                return true;
            }
            int i = (int) (x / (cardWidth(w) + 6 * u));
            if (i < commanders.size() && commanders.get(i).state() != CodexState.UNKNOWN) {
                CardZoom.show(commanders.get(i).card());
            }
            return true;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float cw = cardWidth(w);
            float ch = cw * RogueLook.CARD_RATIO;
            float top = 22 * u;
            Localizer l = Localizer.getInstance();
            RogueLook.text(g, l.getMessage("lblCommander"), 0, 4 * u, 1.1f, RogueLook.DEEP);
            for (int i = 0; i < commanders.size(); i++) {
                Entry e = commanders.get(i);
                float x = i * (cw + 6 * u);
                if (e.state() == CodexState.UNKNOWN) {
                    RogueLook.dashTile(g, x, top, cw, ch);
                    RogueLook.text(g, "?", x, top + ch / 2 - 14 * u, 2, RogueLook.SOFT, cw, Align.center);
                } else {
                    RogueLook.card(g, e.card(), x, top, cw, e.state() == CodexState.SEEN ? 0.45f : 1);
                }
            }
            if (plane == null) {
                return;
            }
            float px = planeLeft(w);
            float pw = Math.min(w - px, ch * 1.4f);
            RogueLook.text(g, l.getMessage("lblPlane"), px, 4 * u, 1.1f, RogueLook.DEEP);
            g.fillRect(RogueLook.INK, px, top, pw, ch);
            RogueLook.art(g, plane, px + u, top + u, pw - 2 * u, ch - 2 * u, 1);
        }
    }

    private static class TraitTile extends FDisplayObject {
        private final RogueEffect effect;
        private final CodexState state;

        TraitTile(RogueEffect effect, CodexState state) {
            this.effect = effect;
            this.state = state;
        }

        String text() {
            String text = effect.getTooltipText();
            return text == null ? "" : text;
        }

        float height(float w) {
            float u = RogueLook.unit();
            return state == CodexState.UNKNOWN ? 26 * u : Math.max(38 * u, RogueLook.wrappedHeight(text(), w - 46 * u, 1) + 23 * u);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (state != CodexState.UNKNOWN) {
                CardZoom.show(effect.getEffectCard());
            }
            return true;
        }

        @Override
        public void draw(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            float h = getHeight();
            if (state == CodexState.ACQUIRED) {
                RogueLook.tile(g, 0, 0, w, h, false);
            } else {
                RogueLook.dashTile(g, 0, 0, w, h);
            }
            if (state == CodexState.UNKNOWN) {
                RogueLook.text(g, Localizer.getInstance().getMessage("lblRogueUnknownTrait"), 10 * u, 6 * u, 1, RogueLook.SOFT);
                return;
            }
            boolean seen = state == CodexState.SEEN;
            if (seen) {
                g.setAlphaComposite(0.45f);
            }
            RogueTraitsScreen.mark(g, effect, 18 * u, h / 2, 11 * u);
            if (seen) {
                g.resetAlphaComposite();
            }
            RogueLook.fit(g, effect.getEffectCard().getName(), 36 * u, 2 * u, 1.1f, seen ? RogueLook.SOFT : RogueLook.DEEP, w - 44 * u, Align.left);
            RogueLook.wrap(g, text(), 36 * u, 18 * u, w - 46 * u, 1, seen ? RogueLook.SOFT : RogueLook.INK, Align.left);
        }
    }

    /** Trait tiles in one column upright and two sideways, each tile below the shorter column. */
    private static class TraitColumns extends Block {
        private final List<TraitTile> tiles;

        TraitColumns(List<TraitTile> tiles) {
            this.tiles = tiles;
            tiles.forEach(this::add);
        }

        private int cols() {
            return Forge.isLandscapeMode() ? 2 : 1;
        }

        private float[] place(float w, boolean apply) {
            float u = RogueLook.unit();
            int cols = cols();
            float cw = (w - (cols - 1) * 6 * u) / cols;
            float[] bottoms = new float[cols];
            for (TraitTile tile : tiles) {
                int col = cols == 1 || bottoms[0] <= bottoms[1] ? 0 : 1;
                float h = tile.height(cw);
                if (apply) {
                    tile.setBounds(col * (cw + 6 * u), bottoms[col], cw, h);
                }
                bottoms[col] += h + 5 * u;
            }
            return bottoms;
        }

        @Override
        float height(float w) {
            float max = 0;
            for (float b : place(w, false)) {
                max = Math.max(max, b);
            }
            return max + 6 * RogueLook.unit();
        }

        @Override
        protected void doLayout(float width, float height) {
            place(width, true);
        }
    }

    private static class Body extends Block {
        private final RogueText text;

        Body(String raw) {
            text = add(new RogueText(raw, null, 1, RogueLook.INK));
        }

        @Override
        float height(float w) {
            return text.getPreferredHeight(w) + 6 * RogueLook.unit();
        }

        @Override
        protected void doLayout(float width, float height) {
            text.setBounds(0, 2 * RogueLook.unit(), width, height - 6 * RogueLook.unit());
        }
    }

    /** The stats, a column per group sideways and one after another upright. Each line's value sits on the right. */
    private static class Stats extends Block {
        private final List<List<String>> groups;

        Stats(List<List<String>> groups) {
            this.groups = groups;
        }

        @Override
        float height(float w) {
            float u = RogueLook.unit();
            if (Forge.isLandscapeMode()) {
                int most = groups.stream().mapToInt(List::size).max().orElse(0);
                return 30 * u + (most - 1) * 20 * u;
            }
            float h = 0;
            for (List<String> group : groups) {
                h += 30 * u + (group.size() - 1) * 20 * u;
            }
            return h;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            boolean wide = Forge.isLandscapeMode();
            float cw = wide ? (getWidth() - 12 * u) / groups.size() : getWidth();
            float y = 0;
            for (int i = 0; i < groups.size(); i++) {
                List<String> group = groups.get(i);
                float x = wide ? i * (cw + 6 * u) : 0;
                if (wide) {
                    y = 0;
                }
                RogueLook.text(g, group.get(0), x, y + 4 * u, 1.3f, RogueLook.DEEP);
                y += 26 * u;
                for (String line : group.subList(1, group.size())) {
                    int cut = line.lastIndexOf(": ");
                    String label = cut < 0 ? line : line.substring(0, cut);
                    String value = cut < 0 ? "" : line.substring(cut + 2);
                    RogueLook.fit(g, label, x, y, 1, RogueLook.INK, cw - 60 * u, Align.left);
                    RogueLook.text(g, value, x, y, 1.1f, RogueLook.INK, cw - 4 * u, Align.right);
                    RogueLook.line(g, x, y + 16 * u, x + cw - 4 * u, y + 16 * u, 0.5f, LINE);
                    y += 20 * u;
                }
                y += 4 * u;
            }
        }
    }

    private static class Buttons extends Block {
        private final List<RogueButton> buttons;

        Buttons(List<RogueButton> buttons) {
            this.buttons = buttons;
            buttons.forEach(this::add);
        }

        @Override
        float height(float w) {
            return 44 * RogueLook.unit();
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            float bw = Math.min(124 * u, (width - (buttons.size() - 1) * 4 * u) / buttons.size());
            for (int i = 0; i < buttons.size(); i++) {
                buttons.get(i).setBounds(i * (bw + 4 * u), 8 * u, bw, 30 * u);
            }
        }
    }
}
