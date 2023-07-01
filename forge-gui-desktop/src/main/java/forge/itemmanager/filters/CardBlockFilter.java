package forge.itemmanager.filters;

import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.ItemManager;
import forge.model.FModel;

import java.util.*;


public class CardBlockFilter extends CardSetFilter {

    private final Set<GameFormat> selectedBlocks = new HashSet<>();
    private GameFormat cardBlock;

    public CardBlockFilter(final ItemManager<? super PaperCard> itemManager0, final GameFormat cardBlock) {
        super(itemManager0, cardBlock.getAllowedSetCodes(), false);
        this.formats.add(cardBlock);
        this.cardBlock = cardBlock;
        this.selectedBlocks.add(cardBlock);
    }

    private CardBlockFilter(final ItemManager<? super PaperCard> itemManager0, GameFormat cardBlock,
                            Set<GameFormat> selectedBlocks){
        this(itemManager0, cardBlock);
        this.selectedBlocks.addAll(selectedBlocks);
    }

    @Override
    public ItemFilter<PaperCard> createCopy() {
        return new CardBlockFilter(itemManager, this.cardBlock, this.selectedBlocks);
    }

    public static boolean canAddCardBlockWorld(final GameFormat cardBlock, final ItemFilter<PaperCard> existingFilter) {
        if (cardBlock == null || FModel.getBlocks() == null) {
            return false; //must have format
        }
        return existingFilter == null || !((CardBlockFilter)existingFilter).selectedBlocks.contains(cardBlock);
    }

    /**
     * Merge the given filter with this filter if possible
     * @param filter
     * @return true if filter merged in or to suppress adding a new filter, false to allow adding new filter
     */
    @Override
    public boolean merge(final ItemFilter<?> filter) {
        final CardBlockFilter cardBlockFilter = (CardBlockFilter)filter;
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
