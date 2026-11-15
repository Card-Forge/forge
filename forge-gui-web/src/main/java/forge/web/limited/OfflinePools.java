package forge.web.limited;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.card.DraftOptions;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.CubeImporter;
import forge.gamemodes.limited.CustomLimited;
import forge.gamemodes.limited.LimitedPoolType;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gamemodes.limited.ThemedChaosDraft;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.storage.IStorage;
import forge.web.deck.CardCatalog;
import forge.web.deck.DeckCatalog;
import forge.web.protocol.FromBrowser.DraftStart;
import forge.web.protocol.FromBrowser.EventKind;
import forge.web.protocol.FromBrowser.SealedCreate;
import forge.web.protocol.ToBrowser.DraftBlockOption;
import forge.web.protocol.ToBrowser.LimitedEdition;
import forge.web.protocol.ToBrowser.LimitedOptions;
import forge.web.protocol.ToBrowser.LimitedPools;
import forge.web.protocol.ToBrowser.Opponent;
import forge.web.protocol.ToBrowser.PoolRow;
import forge.web.protocol.ToBrowser.SealedBlock;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** The host's offline sealed and draft pools, stored where desktop stores them. This class lists the setup form's options, makes pools and drafts, and lists saved pools. */
public final class OfflinePools {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

    private OfflinePools() {
    }

    public static IStorage<DeckGroup> sealed() {
        return FModel.getDecks().getSealed();
    }

    /** Where pools of a kind are stored: desktop's drafts or its sealed pools. */
    public static IStorage<DeckGroup> storage(final EventKind kind) {
        return kind == EventKind.draft ? FModel.getDecks().getDraft() : sealed();
    }

    /** What a limited event can be built from. It is read without prompting, for a frontend that asks for every choice at once. */
    public static LimitedOptions options() {
        final List<CardEdition> prereleases = new ArrayList<>();
        StaticData.instance().getEditions().getPrereleaseEditions().forEach(prereleases::add);
        prereleases.sort(Comparator.reverseOrder());
        final List<ThemedChaosDraft> themes = new ArrayList<>();
        FModel.getThemedChaosDrafts().forEach(themes::add);
        Collections.sort(themes);
        return new LimitedOptions(sealedBlocks(FModel.getBlocks()), sealedBlocks(FModel.getFantasyBlocks()),
                prereleases.stream().map(e -> new LimitedEdition(e.getCode(), e.getName())).toList(),
                SealedCardPoolGenerator.loadCustomSealed().stream().map(CustomLimited::getName).toList(),
                draftBlocks(FModel.getBlocks()), draftBlocks(FModel.getFantasyBlocks()),
                BoosterDraft.loadCustomDrafts().stream().map(CustomLimited::getName).sorted().toList(),
                themes.stream().map(ThemedChaosDraft::getLabel).toList(), FModel.getPreferences().getPref(FPref.LAST_IMPORTED_CUBE_ID));
    }

    /** The pool of a kind that was saved or played most recently, judged by its file times on disk. Null if there is none. */
    public static String latest(final EventKind kind) {
        final String dir = kind == EventKind.draft ? ForgeConstants.DECK_DRAFT_DIR : ForgeConstants.DECK_SEALED_DIR;
        String latest = null;
        long newest = Long.MIN_VALUE;
        synchronized (DeckCatalog.DECKS) {
            for (final String name : storage(kind).getItemNames()) {
                final long time = changed(new File(dir, name));
                if (time > newest) {
                    newest = time;
                    latest = name;
                }
            }
        }
        return latest;
    }

    /** When a pool's folder or any file in it last changed on disk, or 0 if it has no folder. */
    private static long changed(final File folder) {
        long time = folder.lastModified();
        final File[] files = folder.listFiles();
        for (final File f : files == null ? new File[0] : files) {
            time = Math.max(time, f.lastModified());
        }
        return time;
    }

    public static LimitedPools pools() {
        return new LimitedPools(rows(storage(EventKind.sealed), ForgeConstants.DECK_SEALED_DIR), rows(storage(EventKind.draft), ForgeConstants.DECK_DRAFT_DIR));
    }

    private static List<PoolRow> rows(final IStorage<DeckGroup> storage, final String dir) {
        final List<PoolRow> rows = new ArrayList<>();
        synchronized (DeckCatalog.DECKS) {
            for (final DeckGroup group : storage) {
                final Deck human = group.getHumanDeck();
                final int size = human == null ? 0 : human.getMain().countAll();
                final List<Opponent> opponents = new ArrayList<>();
                for (int i = 0; i < group.getAiDecks().size(); i++) {
                    opponents.add(new Opponent(Localizer.getInstance().getMessage("lblWebSessionOpponent", i + 1), colours(group.getAiDecks().get(i))));
                }
                final long changed = changed(new File(dir, group.getName()));
                rows.add(new PoolRow(group.getName(), size > 0, size, size > 0 ? colours(human) : "",
                        changed > 0 ? DAY.format(Instant.ofEpochMilli(changed)) : null, opponents));
            }
        }
        return rows;
    }

    /** Builds the pool that the form describes, or returns null if cancelled. It may ask the host's browser a question, so do not call it on the socket thread. */
    static DeckGroup create(final SealedCreate c, final String name) {
        final SealedCardPoolGenerator gen = generator(c);
        if (gen.isEmpty()) {
            throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoPacks"));
        }
        final CardPool pool = gen.getCardPool(true);
        return pool == null ? null : gen.buildDeckGroup(name, pool, false);
    }

    /** Returns the code that builds the draft the form describes. It runs later on the draft's own thread, because importing a cube waits for a web site. */
    public static Supplier<BoosterDraft> draft(final DraftStart d) {
        final Supplier<BoosterDraft> make = product(d);
        if (d.pickRule() == null) {
            return make;
        }
        // The player's choice replaces the set's own rule, as desktop's picks question does
        final DraftOptions.DoublePick rule = DraftOptions.DoublePick.valueOf(d.pickRule());
        return () -> {
            final BoosterDraft draft = make.get();
            if (draft != null) {
                draft.setDoublePick(rule);
            }
            return draft;
        };
    }

    private static Supplier<BoosterDraft> product(final DraftStart d) {
        final LimitedPoolType type = poolType(d.product());
        return switch (type) {
            case Full -> BoosterDraft::createFullDraft;
            case Block, FantasyBlock -> {
                final CardBlock block = d.block() == null ? null : block(d.block(), type == LimitedPoolType.FantasyBlock);
                if (block == null || !BoosterDraft.isDraftable(block, true)) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsBlockNotDraftable"));
                }
                final List<String> sets = BoosterDraft.blockSets(block);
                final String combo = sets.size() == 1 ? sets.get(0) : d.combo();
                if (combo == null || (sets.size() > 1 && !validCombo(combo, sets, block.getCntBoostersDraft()))) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsSetPerPack"));
                }
                yield () -> BoosterDraft.createBlockDraft(type, block, combo);
            }
            case Custom -> {
                final CustomLimited cube = BoosterDraft.loadCustomDrafts().stream().filter(c -> c.getName().equals(d.cube())).findFirst().orElse(null);
                if (cube == null) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoCube", String.valueOf(d.cube())));
                }
                yield () -> BoosterDraft.createCubeDraft(LimitedPoolType.Custom, cube);
            }
            case Chaos -> {
                final ThemedChaosDraft theme = FModel.getThemedChaosDrafts().stream().filter(t -> t.getLabel().equals(d.theme())).findFirst().orElse(null);
                if (theme == null) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoTheme", String.valueOf(d.theme())));
                }
                yield () -> {
                    final BoosterDraft draft = BoosterDraft.createChaosDraft(theme);
                    if (draft == null) {
                        throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoSetForTheme", theme.getLabel()));
                    }
                    return draft;
                };
            }
            case Import -> {
                if (d.cubeId() == null || d.cubeId().isBlank()) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsEnterCube"));
                }
                final String id = d.cubeId().trim();
                yield () -> BoosterDraft.createCubeDraft(LimitedPoolType.Import, importCube(id));
            }
            case Prerelease -> throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsPrereleaseNotDrafted"));
        };
    }

    private static boolean validCombo(final String combo, final List<String> sets, final int packs) {
        final String[] parts = combo.split("/");
        return parts.length == packs && sets.containsAll(List.of(parts));
    }

    /** Stores a pool, replacing one of the same name, as desktop's limited screens do once the player agrees. */
    static void store(final IStorage<DeckGroup> storage, final DeckGroup group) {
        synchronized (DeckCatalog.DECKS) {
            if (storage.contains(group.getName())) {
                storage.delete(group.getName());
            }
            storage.add(group);
        }
    }

    private static LimitedPoolType poolType(final String product) {
        try {
            return LimitedPoolType.valueOf(product);
        } catch (final IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoProduct", String.valueOf(product)));
        }
    }

    public static SealedCardPoolGenerator generator(final SealedCreate c) {
        final LimitedPoolType type = poolType(c.product());
        return switch (type) {
            case Full -> SealedCardPoolGenerator.createFull(packs(c.packs()));
            case Prerelease -> {
                final CardEdition edition = c.edition() == null ? null : StaticData.instance().getEditions().get(c.edition());
                if (edition == null || edition.getPrerelease() == null) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoPrerelease", String.valueOf(c.edition())));
                }
                yield SealedCardPoolGenerator.createPrerelease(edition);
            }
            case Block, FantasyBlock -> {
                final CardBlock block = c.block() == null ? null : block(c.block(), type == LimitedPoolType.FantasyBlock);
                if (block == null || c.combo() == null || !SealedCardPoolGenerator.blockCombos(block).contains(c.combo())) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsBadCombo"));
                }
                yield SealedCardPoolGenerator.createBlock(block, c.combo());
            }
            case Custom -> {
                final CustomLimited template = SealedCardPoolGenerator.loadCustomSealed().stream().filter(t -> t.getName().equals(c.template())).findFirst().orElse(null);
                if (template == null) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsNoTemplate", String.valueOf(c.template())));
                }
                yield SealedCardPoolGenerator.createCustom(template, packs(c.packs()));
            }
            case Import -> {
                if (c.cubeId() == null || c.cubeId().isBlank()) {
                    throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsEnterCube"));
                }
                yield SealedCardPoolGenerator.createImported(importCube(c.cubeId().trim()), packs(c.packs()));
            }
            case Chaos -> throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsChaosNotSealed"));
        };
    }

    /** Desktop asks for between 3 and 12 packs. */
    private static int packs(final int wanted) {
        if (wanted < 3 || wanted > 12) {
            throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblWebEventsPackCount"));
        }
        return wanted;
    }

    private static CardBlock block(final String name, final boolean fantasy) {
        return (fantasy ? FModel.getFantasyBlocks() : FModel.getBlocks()).get(name);
    }

    /** Throws IllegalArgumentException, with a message a player can read, when the cube can't be imported. */
    private static CustomLimited importCube(final String cubeId) {
        final CustomLimited imported;
        try {
            imported = new CubeImporter(cubeId).importCube();
        } catch (Exception e) {
            throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblErrorImportingCube") + ": " + e.getMessage(), e);
        }
        if (imported == null) {
            throw new IllegalArgumentException(Localizer.getInstance().getMessage("lblFailedToImportCube") + ": " + cubeId);
        }
        return imported;
    }

    /** The draftable blocks with desktop's preset set combinations. combos is empty when desktop asks for one set per pack instead. */
    private static List<DraftBlockOption> draftBlocks(final IStorage<CardBlock> storage) {
        final List<DraftBlockOption> out = new ArrayList<>();
        for (final CardBlock b : storage) {
            // Like a network draft, the web leaves out Conspiracy blocks
            final List<String> sets = BoosterDraft.isDraftable(b, true) ? BoosterDraft.blockSets(b) : List.of();
            if (!sets.isEmpty()) {
                out.add(new DraftBlockOption(b.getName(), b.getCntBoostersDraft(), sets, codes(b), BoosterDraft.presetCombos(b, sets), podSize(sets), picks(sets)));
            }
        }
        return out;
    }

    /** The pod size, as BoosterDraft.setupBlock sets it: a single set's recommended pod size, or a full pod for anything else. */
    private static int podSize(final List<String> sets) {
        final CardEdition edition = sets.size() == 1 ? StaticData.instance().getEditions().get(sets.get(0)) : null;
        return edition == null ? BoosterDraft.N_PLAYERS : edition.getDraftOptions().getRecommendedPodSize();
    }

    /** The pick rule a draft of these sets starts with, as BoosterDraft.setupBlock sets it: a single set's own rule at its pod size, or one pick. */
    private static String picks(final List<String> sets) {
        final CardEdition edition = sets.size() == 1 ? StaticData.instance().getEditions().get(sets.get(0)) : null;
        return edition == null ? DraftOptions.DoublePick.NEVER.name() : edition.getDraftOptions().getDoublePick().resolve(podSize(sets)).name();
    }

    private static List<SealedBlock> sealedBlocks(final IStorage<CardBlock> storage) {
        final List<SealedBlock> out = new ArrayList<>();
        for (final CardBlock b : storage) {
            try {
                out.add(new SealedBlock(b.getName(), b.getCntBoostersSealed(), codes(b), SealedCardPoolGenerator.blockCombos(b)));
            } catch (final RuntimeException e) {
                // Desktop's dialog fails on a block with no combo for its pack count, so it is not offered
            }
        }
        return out;
    }

    /** The block's set codes in release order, to show beside its name. */
    private static List<String> codes(final CardBlock b) {
        return b.getSets().stream().map(CardEdition::getCode).toList();
    }

    private static String colours(final Deck deck) {
        byte mask = 0;
        for (final Map.Entry<PaperCard, Integer> e : deck.getMain()) {
            mask |= e.getKey().getRules().getColor().getColor();
        }
        return CardCatalog.letters(ColorSet.fromMask(mask));
    }
}
