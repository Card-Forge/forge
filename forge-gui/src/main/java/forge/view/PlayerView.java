package forge.view;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.zone.ZoneType;

public class PlayerView extends GameEntityView {

    private final LobbyPlayer lobbyPlayer;
    private final int id;

    private Set<PlayerView> opponents;
    private int life, poisonCounters, maxHandSize, numDrawnThisTurn, preventNextDamage;
    private List<String> keywords;
    private String commanderInfo;
    private List<CardView>
            anteCards      = Lists.newArrayList(),
            bfCards        = Lists.newArrayList(),
            commandCards   = Lists.newArrayList(),
            exileCards     = Lists.newArrayList(),
            flashbackCards = Lists.newArrayList(),
            graveCards     = Lists.newArrayList(),
            handCards      = Lists.newArrayList(),
            libraryCards   = Lists.newArrayList();
    private int
            nHandCards,
            nLibraryCards;
    private boolean hasUnlimitedHandSize;
    private Map<Byte, Integer> mana = Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1);

    public PlayerView(final LobbyPlayer lobbyPlayer, final int id) {
        this.lobbyPlayer = lobbyPlayer;
        this.id = id;
    }

    /**
     * @return the lobbyPlayer
     */
    public LobbyPlayer getLobbyPlayer() {
        return lobbyPlayer;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof PlayerView && this.getId() == ((PlayerView) obj).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    public void setOpponents(final Iterable<PlayerView> opponents) {
        this.opponents = Sets.newHashSet(opponents);
    }

    public boolean isOpponentOf(final PlayerView other) {
        return opponents.contains(other);
    }

    public String getName() {
        return this.getLobbyPlayer().getName();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * @return the life
     */
    public int getLife() {
        return life;
    }

    /**
     * @param life the life to set
     */
    public void setLife(final int life) {
        this.life = life;
    }

    /**
     * @return the poisonCounters
     */
    public int getPoisonCounters() {
        return poisonCounters;
    }

    /**
     * @param poisonCounters the poisonCounters to set
     */
    public void setPoisonCounters(final int poisonCounters) {
        this.poisonCounters = poisonCounters;
    }

    /**
     * @return the maxHandSize
     */
    public int getMaxHandSize() {
        return maxHandSize;
    }

    /**
     * @param maxHandSize the maxHandSize to set
     */
    public void setMaxHandSize(final int maxHandSize) {
        this.maxHandSize = maxHandSize;
    }

    /**
     * @return the numDrawnThisTurn
     */
    public int getNumDrawnThisTurn() {
        return numDrawnThisTurn;
    }

    /**
     * @param numDrawnThisTurn the numDrawnThisTurn to set
     */
    public void setNumDrawnThisTurn(final int numDrawnThisTurn) {
        this.numDrawnThisTurn = numDrawnThisTurn;
    }

    /**
     * @return the preventNextDamage
     */
    public int getPreventNextDamage() {
        return preventNextDamage;
    }

    /**
     * @param preventNextDamage the preventNextDamage to set
     */
    public void setPreventNextDamage(final int preventNextDamage) {
        this.preventNextDamage = preventNextDamage;
    }

    /**
     * @return the keywords
     */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(final List<String> keywords) {
        this.keywords = ImmutableList.copyOf(keywords);
    }

    /**
     * @return the commanderInfo
     */
    public String getCommanderInfo() {
        return commanderInfo;
    }

    /**
     * @param commanderInfo the commanderInfo to set
     */
    public void setCommanderInfo(final String commanderInfo) {
        this.commanderInfo = commanderInfo;
    }

    /**
     * @return the anteCards
     */
    public List<CardView> getAnteCards() {
        return anteCards;
    }

    /**
     * @param anteCards the anteCards to set
     */
    public void setAnteCards(final List<CardView> anteCards) {
        this.anteCards = ImmutableList.copyOf(anteCards);
    }

    /**
     * @return the bfCards
     */
    public List<CardView> getBfCards() {
        return bfCards;
    }

    /**
     * @param bfCards the bfCards to set
     */
    public void setBfCards(final List<CardView> bfCards) {
        this.bfCards = ImmutableList.copyOf(bfCards);
    }

    /**
     * @return the commandCards
     */
    public List<CardView> getCommandCards() {
        return commandCards;
    }

    /**
     * @param commandCards the commandCards to set
     */
    public void setCommandCards(List<CardView> commandCards) {
        this.commandCards = ImmutableList.copyOf(commandCards);
    }

    /**
     * @return the exileCards
     */
    public List<CardView> getExileCards() {
        return exileCards;
    }

    /**
     * @param exileCards the exileCards to set
     */
    public void setExileCards(final List<CardView> exileCards) {
        this.exileCards = ImmutableList.copyOf(exileCards);
    }

    /**
     * @return the flashbackCards
     */
    public List<CardView> getFlashbackCards() {
        return flashbackCards;
    }

    /**
     * @param flashbackCards the flashbackCards to set
     */
    public void setFlashbackCards(final List<CardView> flashbackCards) {
        this.flashbackCards = ImmutableList.copyOf(flashbackCards);
    }

    /**
     * @return the graveCards
     */
    public List<CardView> getGraveCards() {
        return graveCards;
    }

    /**
     * @param graveCards the graveCards to set
     */
    public void setGraveCards(final List<CardView> graveCards) {
        this.graveCards = ImmutableList.copyOf(graveCards);
    }

    /**
     * @return the handCards
     */
    public List<CardView> getHandCards() {
        return handCards;
    }

    /**
     * @param handCards the handCards to set
     */
    public void setHandCards(final List<CardView> handCards) {
        this.handCards = ImmutableList.copyOf(handCards);
    }

    /**
     * @return the libraryCards
     */
    public List<CardView> getLibraryCards() {
        return libraryCards;
    }

    /**
     * @param libraryCards the libraryCards to set
     */
    public void setLibraryCards(final List<CardView> libraryCards) {
        this.libraryCards = ImmutableList.copyOf(libraryCards);
    }

    public List<CardView> getCards(final ZoneType zone) {
        switch (zone) {
        case Ante:
            return getAnteCards();
        case Battlefield:
            return getBfCards();
        case Command:
            return getCommandCards();
        case Exile:
            return getExileCards();
        case Graveyard:
            return getGraveCards();
        case Hand:
            return getHandCards();
        case Library:
            return getLibraryCards();
        default:
            return ImmutableList.of();
        }
    }

    /**
     * @return the nHandCards
     */
    public int getnHandCards() {
        return nHandCards;
    }

    /**
     * @param nHandCards the nHandCards to set
     */
    public void setnHandCards(int nHandCards) {
        this.nHandCards = nHandCards;
    }

    /**
     * @return the nLibraryCards
     */
    public int getnLibraryCards() {
        return nLibraryCards;
    }

    /**
     * @param nLibraryCards the nLibraryCards to set
     */
    public void setnLibraryCards(int nLibraryCards) {
        this.nLibraryCards = nLibraryCards;
    }

    /**
     * @return the hasUnlimitedHandSize
     */
    public boolean hasUnlimitedHandSize() {
        return hasUnlimitedHandSize;
    }

    /**
     * @param hasUnlimitedHandSize the hasUnlimitedHandSize to set
     */
    public void setHasUnlimitedHandSize(final boolean hasUnlimitedHandSize) {
        this.hasUnlimitedHandSize = hasUnlimitedHandSize;
    }

    public int getMana(final Byte color) {
        return this.mana.containsKey(color) ? this.mana.get(color).intValue() : 0;
    }

    public void setMana(final byte color, final int mana) {
        this.mana.put(Byte.valueOf(color), Integer.valueOf(mana));
    }

}