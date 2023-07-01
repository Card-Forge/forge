package forge.itemmanager.filters;

import forge.deck.DeckProxy;
import forge.game.GameFormat;
import forge.itemmanager.ItemManager;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DeckBlockFilter extends DeckSetFilter {

    private final Set<GameFormat> selectedBlocks = new HashSet<>();
    private GameFormat cardBlock;

    public DeckBlockFilter(ItemManager<? super DeckProxy> itemManager0, final GameFormat cardBlock) {
        super(itemManager0, cardBlock.getAllowedSetCodes(), false);
        this.formats.add(cardBlock);
        this.selectedBlocks.add(cardBlock);
    }

    private DeckBlockFilter(ItemManager<? super DeckProxy> itemManager0, GameFormat cardBlock,
                            Set<GameFormat> selectedBlocks){
        this(itemManager0, cardBlock);
        this.selectedBlocks.addAll(selectedBlocks);
    }

    @Override
    public ItemFilter<DeckProxy> createCopy() {
        return new DeckBlockFilter(itemManager, this.cardBlock, this.selectedBlocks);
    }

    public static boolean canAddCardBlock(final GameFormat cardBlock, final ItemFilter<DeckProxy> existingFilter) {
        if (cardBlock == null || FModel.getBlocks() == null) {
            return false; //must have format
        }
        return existingFilter == null || !((DeckBlockFilter)existingFilter).selectedBlocks.contains(cardBlock);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(final ItemFilter<?> filter) {
        final DeckBlockFilter cardBlockFilter = (DeckBlockFilter)filter;
        this.selectedBlocks.addAll(cardBlockFilter.selectedBlocks);
        this.sets.addAll(cardBlockFilter.sets);
        List<String> allBannedCards = new ArrayList<>();
        for (GameFormat f : this.formats){
            List<String> bannedCards = f.getBannedCardNames();
            if (bannedCards != null && bannedCards.size() > 0)
                allBannedCards.addAll(bannedCards);
        }
        this.formats.clear();
        this.formats.add(new GameFormat(null, this.sets, allBannedCards));
        return true;
    }

    @Override
    protected String getCaption() {
        return "Block";
    }

    @Override
    protected int getCount() {
        int setCount = 0;
        for (GameFormat block : this.selectedBlocks)
            setCount += block.getAllowedSetCodes().size();
        return setCount;
    }

    @Override
    protected Iterable<String> getList() {
        final Set<String> strings = new HashSet<>();
        for (final GameFormat f : this.selectedBlocks) {
            strings.add(f.getName());
        }
        return strings;
    }
}
