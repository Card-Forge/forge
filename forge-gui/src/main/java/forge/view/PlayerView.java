package forge.view;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.game.player.PlayerController;

public class PlayerView extends GameEntityView {

    private final LobbyPlayer lobbyPlayer;
    private final PlayerController controller;

    private int life, poisonCounters, maxHandSize, numDrawnThisTurn, preventNextDamage;
    private List<String> keywords;
    private String commanderInfo;
    private List<CardView> anteCards, bfCards, commandCards, exileCards, flashbackCards, graveCards, handCards, libraryCards;
    private boolean hasUnlimitedHandSize;
    private Map<Byte, Integer> mana = Maps.newHashMapWithExpectedSize(MagicColor.NUMBER_OR_COLORS + 1);

    public PlayerView(final LobbyPlayer lobbyPlayer, final PlayerController controller) {
        this.lobbyPlayer = lobbyPlayer;
        this.controller = controller;
    }

    /**
     * @return the lobbyPlayer
     */
    public LobbyPlayer getLobbyPlayer() {
        return lobbyPlayer;
    }

    /**
     * @return the controller
     */
    @Deprecated
    public PlayerController getController() {
        return controller;
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
        this.commandCards = commandCards;
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
        return this.mana.get(color).intValue();
    }

    private void setMana(final byte color, final int mana) {
        this.mana.put(Byte.valueOf(color), Integer.valueOf(mana));
    }
    public void setWhiteMana(final int mana) {
        this.setMana(MagicColor.WHITE, mana);
    }
    public void setBlueMana(final int mana) {
        this.setMana(MagicColor.BLUE, mana);
    }
    public void setBlackMana(final int mana) {
        this.setMana(MagicColor.BLACK, mana);
    }
    public void setRedMana(final int mana) {
        this.setMana(MagicColor.RED, mana);
    }
    public void setGreenMana(final int mana) {
        this.setMana(MagicColor.GREEN, mana);
    }
    public void setColorlessMana(final int mana) {
        this.setMana(MagicColor.COLORLESS, mana);
    }
}