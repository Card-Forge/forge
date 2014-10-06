package forge.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observer;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntry;
import forge.game.GameLogEntryType;
import forge.game.GameOutcome;
import forge.game.GameType;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.match.MatchUtil;
import forge.match.input.InputProxy;
import forge.match.input.InputQueue;

public abstract class LocalGameView implements IGameView {
    protected final Game game;
    protected final InputQueue inputQueue;
    protected final InputProxy inputProxy;
    private PlayerView localPlayerView;

    public LocalGameView(Game game0) {
        game = game0;
        inputProxy = new InputProxy(this);
        inputQueue = new InputQueue(game, inputProxy);
    }

    public final Game getGame() {
        return game;
    }

    public final InputQueue getInputQueue() {
        return inputQueue;
    }

    public InputProxy getInputProxy() {
        return inputProxy;
    }

    public PlayerView getLocalPlayerView() {
        return localPlayerView;
    }

    public void setLocalPlayer(Player localPlayer) {
        localPlayerView = getPlayerView(localPlayer, false);
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isCommander()
     */
    @Override
    public boolean isCommander() {
        return game.getRules().hasAppliedVariant(GameType.Commander);
    }
    /* (non-Javadoc)
     * @see forge.view.IGameView#getGameType()
     */
    @Override
    public GameType getGameType() {
        return game.getMatch().getRules().getGameType();
    }

    @Override
    public int getTurnNumber() {
        return game.getPhaseHandler().getTurn();
    }

    @Override
    public boolean isCommandZoneNeeded() {
        return game.getMatch().getRules().getGameType().isCommandZoneNeeded();
    }

    @Override
    public boolean isWinner(final LobbyPlayer p) {
        return game.getOutcome() == null ? null : game.getOutcome().isWinner(p);
    }

    @Override
    public LobbyPlayer getWinningPlayer() {
        return game.getOutcome() == null ? null : game.getOutcome().getWinningLobbyPlayer();
    }

    @Override
    public int getWinningTeam() {
        return game.getOutcome() == null ? -1 : game.getOutcome().getWinningTeam();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isFirstGameInMatch()
     */
    @Override
    public boolean isFirstGameInMatch() {
        return game.getMatch().getPlayedGames().isEmpty();
    }

    @Override
    public boolean isMatchOver() {
        return game.getMatch().isMatchOver();
    }

    @Override
    public int getNumGamesInMatch() {
        return game.getMatch().getRules().getGamesPerMatch();
    }

    @Override
    public int getNumPlayedGamesInMatch() {
        return game.getMatch().getPlayedGames().size();
    }

    @Override
    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return Iterables.unmodifiableIterable(game.getMatch().getPlayedGames());
    }

    @Override
    public boolean isMatchWonBy(final LobbyPlayer p) {
        return game.getMatch().isWonBy(p);
    }

    @Override
    public int getGamesWonBy(final LobbyPlayer p) {
        return game.getMatch().getGamesWonBy(p);
    }

    @Override
    public GameOutcome.AnteResult getAnteResult() {
        return null;
    }

    @Override
    public Deck getDeck(final LobbyPlayer player) {
        for (final RegisteredPlayer rp : game.getMatch().getPlayers()) {
            if (rp.getPlayer().equals(player)) {
                return rp.getDeck();
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#isGameOver()
     */
    @Override
    public boolean isGameOver() {
        return game.isGameOver();
    }

    @Override
    public int getPoisonCountersToLose() {
        return game.getRules().getPoisonCountersToLose();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#subscribeToEvents(java.lang.Object)
     */
    @Override
    public void subscribeToEvents(final Object subscriber) {
        game.subscribeToEvents(subscriber);
    }

    @Override
    public CombatView getCombat() {
        synchronized (MatchUtil.class) {
            if (MatchUtil.cachedCombatView != null) {
                return MatchUtil.cachedCombatView;
            }

            final Combat combat = game.getCombat();
            final CombatView combatView;
            if (combat == null) {
                combatView = null;
            } else {
                combatView = new CombatView();
                for (final AttackingBand b : combat.getAttackingBands()) {
                    if (b == null) continue;
                    final GameEntity defender = combat.getDefenderByAttacker(b);
                    final List<Card> blockers = combat.getBlockers(b);
                    final boolean isBlocked = b.isBlocked() == Boolean.TRUE;
                    combatView.addAttackingBand(
                            getCardViews(b.getAttackers(), true),
                            getGameEntityView(defender, true),
                            blockers == null || !isBlocked ? null : getCardViews(blockers, true),
                                    blockers == null ? null : getCardViews(blockers, true));
                }
            }
            MatchUtil.cachedCombatView = combatView;
            return combatView;
        }
    }

    public final void refreshCombat() {
        synchronized (MatchUtil.class) {
            MatchUtil.cachedCombatView = null;
            this.getCombat();
        }
    }

    @Override
    public void addLogObserver(final Observer o) {
        game.getGameLog().addObserver(o);
    }

    @Override
    public List<GameLogEntry> getLogEntries(final GameLogEntryType maxLogLevel) {
        return game.getGameLog().getLogEntries(maxLogLevel);
    }

    @Override
    public List<GameLogEntry> getLogEntriesExact(final GameLogEntryType logLevel) {
        return game.getGameLog().getLogEntriesExact(logLevel);
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#getRegisteredPlayers()
     */
    @Override
    public List<PlayerView> getPlayers() {
        return getPlayerViews(game.getRegisteredPlayers(), false);
    }
    public List<PlayerView> getPlayers(Boolean update) {
        return getPlayerViews(game.getRegisteredPlayers(), update);
    }

    @Override
    public PlayerView getPlayerTurn() {
        return getPlayerView(game.getPhaseHandler().getPlayerTurn(), false);
    }

    @Override
    public PhaseType getPhase() {
        return game.getPhaseHandler().getPhase();
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#getStack()
     */
    @Override
    public List<StackItemView> getStack() {
        final List<SpellAbilityStackInstance> stack = Lists.newArrayList(game.getStack());
        final List<StackItemView> items = Collections.unmodifiableList(getStack(stack));
        // clear the cache
        MatchUtil.stackItems.retainAllKeys(stack);
        return items;
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#peekStack()
     */
    @Override
    public StackItemView peekStack() {
        final SpellAbilityStackInstance top = game.getStack().peek();
        if (top == null) {
            return null;
        }
        return getStack(Lists.newArrayList(top)).iterator().next();
    }

    private List<StackItemView> getStack(final Iterable<SpellAbilityStackInstance> stack) {
        MatchUtil.stackItems.retainAllKeys(Lists.newArrayList(stack));
        final List<StackItemView> items = Lists.newLinkedList();
        for (final SpellAbilityStackInstance si : stack) {
            final int id = si.getId();
            if (MatchUtil.stackItems.containsKey(id)) {
                items.add(MatchUtil.stackItems.get(id));
            } else {
                items.add(getStackItemView(si));
            }
        }
        return items;
    }

    public StackItemView getStackItemView(final SpellAbilityStackInstance si) {
        if (si == null) {
            return null;
        }

        final StackItemView newItem = new StackItemView(si, this);
        MatchUtil.stackItems.put(si, newItem);
        return newItem;
    }

    public SpellAbilityStackInstance getStackItem(final StackItemView view) {
        if (view == null) {
            return null;
        }
        return MatchUtil.stackItems.getKey(view.getId());
    }

    public final GameEntityView getGameEntityView(final GameEntity e, final Boolean update) {
        if (e instanceof Card) {
            return getCardView((Card)e, update);
        }
        if (e instanceof Player) {
            return getPlayerView((Player)e, update);
        }
        return null;
    }

    public final List<GameEntityView> getGameEntityViews(final Iterable<? extends GameEntity> entities, final Boolean update) {
        List<GameEntityView> views = new ArrayList<GameEntityView>();
        for (GameEntity e : entities) {
            views.add(getGameEntityView(e, update));
        }
        return views;
    }

    public final GameEntity getGameEntity(final GameEntityView view) {
        if (view instanceof CardView) {
            return getCard((CardView) view);
        }
        if (view instanceof PlayerView) {
            return getPlayer((PlayerView) view);
        }
        return null;
    }

    public void updatePlayers(Iterable<PlayerView> views) {
        for (PlayerView pv : views) {
            writePlayerToView(getPlayer(pv), pv, true);
        }
    }

    public final List<PlayerView> getPlayerViews(final Iterable<Player> players, final Boolean update) {
        List<PlayerView> views = new ArrayList<PlayerView>();
        for (Player p : players) {
            views.add(getPlayerView(p, update));
        }
        return views;
    }

    public Player getPlayer(final PlayerView p) {
        if (p == null) {
            return null;
        }
        return MatchUtil.players.getKey(p.getId());
    }

    public PlayerView getPlayerView(final Player p, final Boolean update) {
        if (p == null) {
            return null;
        }

        PlayerView view = MatchUtil.players.get(p.getId());
        if (view == null) {
            view = new PlayerView(p.getLobbyPlayer(), p.getId());
            if (update != null) { //passing null signifies to delay writing card to view even first time
                writePlayerToView(p, view, update);
            }
            MatchUtil.players.put(p, view);
        }
        else if (Boolean.TRUE.equals(update)) {
            writePlayerToView(p, view, true);
        }
        return view;
    }

    private void writePlayerToView(final Player p, final PlayerView view, final Boolean update) {
        view.setCommanderInfo(CardFactoryUtil.getCommanderInfo(p).trim().replace("\r\n", "; "));
        view.setKeywords(p.getKeywords());
        view.setLife(p.getLife());
        view.setMaxHandSize(p.getMaxHandSize());
        view.setNumDrawnThisTurn(p.getNumDrawnThisTurn());
        view.setPoisonCounters(p.getPoisonCounters());
        view.setPreventNextDamage(p.getPreventNextDamageTotalShields());
        view.setHasUnlimitedHandSize(p.isUnlimitedHandSize());
        view.setAnteCards(getCardViews(p.getCardsIn(ZoneType.Ante), update));
        view.setBfCards(getCardViews(p.getCardsIn(ZoneType.Battlefield, false), update));
        view.setCommandCards(getCardViews(p.getCardsIn(ZoneType.Command), update));
        view.setExileCards(getCardViews(p.getCardsIn(ZoneType.Exile), update));
        view.setFlashbackCards(getCardViews(p.getCardsActivableInExternalZones(false), update));
        view.setGraveCards(getCardViews(p.getCardsIn(ZoneType.Graveyard), update));
        final List<Card> handCards = p.getCardsIn(ZoneType.Hand),
                libraryCards = p.getCardsIn(ZoneType.Library);
        view.setHandCards(getCardViews(handCards, update));
        view.setLibraryCards(getCardViews(libraryCards, update));
        view.setnHandCards(handCards.size());
        view.setnLibraryCards(libraryCards.size());

        for (final byte b : MagicColor.WUBRGC) {
            view.setMana(b, p.getManaPool().getAmountOfColor(b));
        }
    }

    public CardView getCardView(final Card c, final Boolean update) {
        if (c == null || c != c.getCardForUi()) {
            return null;
        }

        CardView view = MatchUtil.cards.get(c.getId());
        if (view == null) {
            view = new CardView(c.getId());
            if (update != null) { //passing null signifies to delay writing card to view even first time
                writeCardToView(c, view, MatchUtil.getGameView());
            }
            MatchUtil.cards.put(c, view);
        }
        else {
            if (Boolean.TRUE.equals(update)) {
                writeCardToView(c, view, MatchUtil.getGameView());
            }
            MatchUtil.cards.updateKey(view.getId(), c); //ensure the Card reference in the cache is not outdated
        }
        return view;
    }

    public void updateCards(Iterable<CardView> views) {
        LocalGameView gameView = MatchUtil.getGameView();
        for (CardView cv : views) {
            writeCardToView(getCard(cv), cv, gameView);
        }
    }

    public void updateAllCards() {
        LocalGameView gameView = MatchUtil.getGameView();
        for (Card c : MatchUtil.cards.getKeys()) {
            writeCardToView(c, getCardView(c, false), gameView);
        }
    }

    public final List<CardView> getCardViews(final Iterable<Card> cards, final Boolean update) {
        List<CardView> cardViews = new ArrayList<CardView>();
        for (Card c : cards) {
            CardView cv = getCardView(c, update);
            if (cv != null) {
                cardViews.add(cv);
            }
        }
        return cardViews;
    }

    public Card getCard(final CardView c) {
        if (c == null) {
            return null;
        }
        return MatchUtil.cards.getKey(c.getId());
    }

    private final Function<CardView, Card> FN_GET_CARD = new Function<CardView, Card>() {
        @Override
        public Card apply(final CardView input) {
            return getCard(input);
        }
    };

    public final List<Card> getCards(final List<CardView> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_CARD);
    }

    private void writeCardToView(final Card c, final CardView view, final LocalGameView gameView) {
        // First, write the values independent of other views
        ViewUtil.writeNonDependentCardViewProperties(c, view, gameView.mayShowCard(c), gameView.mayShowCardFace(c));

        // Next, write the values that depend on other views
        final Combat combat = game.getCombat();
        view.setOwner(getPlayerView(c.getOwner(), false));
        view.setController(getPlayerView(c.getController(), false));
        view.setAttacking(combat != null && combat.isAttacking(c));
        view.setBlocking(combat != null && combat.isBlocking(c));
        view.setChosenPlayer(getPlayerView(c.getChosenPlayer(), false));
        view.setEquipping(getCardView(c.getEquipping(), false));
        view.setEquippedBy(getCardViews(c.getEquippedBy(), false));
        view.setEnchantingCard(getCardView(c.getEnchantingCard(), false));
        view.setEnchantingPlayer(getPlayerView(c.getEnchantingPlayer(), false));
        view.setEnchantedBy(getCardViews(c.getEnchantedBy(), false));
        view.setFortifiedBy(getCardViews(c.getFortifiedBy(), false));
        view.setGainControlTargets(getCardViews(c.getGainControlTargets(), false));
        view.setCloneOrigin(getCardView(c.getCloneOrigin(), false));
        view.setImprinted(getCardViews(c.getImprinted(), false));
        view.setHauntedBy(getCardViews(c.getHauntedBy(), false));
        view.setHaunting(getCardView(c.getHaunting(), false));
        view.setMustBlock(c.getMustBlockCards() == null ? Collections.<CardView>emptySet() : getCardViews(c.getMustBlockCards(), false));
        view.setPairedWith(getCardView(c.getPairedWith(), false));
    }

    public SpellAbilityView getSpellAbilityView(final SpellAbility sa) {
        if (sa == null) {
            return null;
        }

        final SpellAbilityView view;
        if (MatchUtil.spabs.containsKey(sa.getId())) {
            view = MatchUtil.spabs.get(sa.getId());
            writeSpellAbilityToView(sa, view);
        } else {
            view = new SpellAbilityView(sa.getId());
            writeSpellAbilityToView(sa, view);
            MatchUtil.spabs.put(sa, view);
        }
        return view;
    }

    private final Function<SpellAbility, SpellAbilityView> FN_GET_SPAB_VIEW = new Function<SpellAbility, SpellAbilityView>() {
        @Override
        public SpellAbilityView apply(final SpellAbility input) {
            return getSpellAbilityView(input);
        }
    };

    public final List<SpellAbilityView> getSpellAbilityViews(final List<? extends SpellAbility> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_SPAB_VIEW);
    }

    public SpellAbility getSpellAbility(final SpellAbilityView spabView) {
        if (spabView == null) {
            return null;
        }
        return getSpellAbility(spabView.getId());
    }
    public SpellAbility getSpellAbility(final int id) {
        return id >= 0 ? MatchUtil.spabs.getKey(id) : null;
    }

    private final Function<SpellAbilityView, SpellAbility> FN_GET_SPAB = new Function<SpellAbilityView, SpellAbility>() {
        @Override
        public SpellAbility apply(final SpellAbilityView input) {
            return getSpellAbility(input);
        }
    };

    public final List<SpellAbility> getSpellAbilities(final List<SpellAbilityView> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_SPAB);
    }

    private void writeSpellAbilityToView(final SpellAbility sa, final SpellAbilityView view) {
        view.setHostCard(getCardView(sa.getHostCard(), false));
        view.setDescription(sa.toUnsuppressedString());
        view.setCanPlay(sa.canPlay());
        view.setPromptIfOnlyPossibleAbility(sa.promptIfOnlyPossibleAbility());
    }

    @Override
    public boolean getDisableAutoYields() {
        return this.game.getDisableAutoYields();
    }
    @Override
    public void setDisableAutoYields(final boolean b) {
        this.game.setDisableAutoYields(b);
    }

    /**
     * @param c
     *            a card.
     * @return whether this player may view the specified card.
     */
    protected abstract boolean mayShowCard(Card c);

    /**
     * @param c
     *            a card.
     * @return whether this player may view the front card face of the specified
     *         card.
     */
    protected abstract boolean mayShowCardFace(Card c);
}
