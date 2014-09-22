package forge.view;

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

public abstract class LocalGameView implements IGameView {

    private final Game game;
    public LocalGameView(final Game game) {
        this.game = game;
    }

    protected final Game getGame() {
        return this.game;
    }

    /** Cache of players. */
    private final Cache<Player, PlayerView> players = new Cache<>();
    /** Cache of cards. */
    private final Cache<Card, CardView> cards = new Cache<>();
    /** Cache of spellabilities. */
    private final Cache<SpellAbility, SpellAbilityView> spabs = new Cache<>();
    /** Cache of stack items. */
    private final Cache<SpellAbilityStackInstance, StackItemView> stackItems = new Cache<>();

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
        return this.game.getMatch().getRules().getGameType();
    }

    @Override
    public int getTurnNumber() {
        return this.game.getPhaseHandler().getTurn();
    }

    @Override
    public boolean isCommandZoneNeeded() {
        return this.game.getMatch().getRules().getGameType().isCommandZoneNeeded();
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
        return this.game.getMatch().getPlayedGames().isEmpty();
    }

    @Override
    public boolean isMatchOver() {
        return this.game.getMatch().isMatchOver();
    }

    @Override
    public int getNumGamesInMatch() {
        return this.game.getMatch().getRules().getGamesPerMatch();
    }

    @Override
    public int getNumPlayedGamesInMatch() {
        return this.game.getMatch().getPlayedGames().size();
    }

    @Override
    public Iterable<GameOutcome> getOutcomesOfMatch() {
        return Iterables.unmodifiableIterable(this.game.getMatch().getPlayedGames());
    }

    @Override
    public boolean isMatchWonBy(final LobbyPlayer p) {
        return this.game.getMatch().isWonBy(p);
    }

    @Override
    public int getGamesWonBy(final LobbyPlayer p) {
        return this.game.getMatch().getGamesWonBy(p);
    }

    @Override
    public GameOutcome.AnteResult getAnteResult() {
        return null;
    }

    @Override
    public Deck getDeck(final LobbyPlayer player) {
        for (final RegisteredPlayer rp : this.game.getMatch().getPlayers()) {
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
        return getCombat(game.getCombat());
    }

    /* (non-Javadoc)
     * @see forge.view.IGameView#getCombat()
     */
    public CombatView getCombat(final Combat combat) {
        if (combat == null) {
            return null;
        }

        final CombatView combatView = new CombatView();
        for (final AttackingBand b : combat.getAttackingBands()) {
            if (b == null) continue;
            final GameEntity defender = combat.getDefenderByAttacker(b);
            final List<Card> blockers = combat.getBlockers(b);
            final boolean isBlocked = b.isBlocked() == Boolean.TRUE;
            combatView.addAttackingBand(
                    getCardViews(b.getAttackers()),
                    getGameEntityView(defender),
                    blockers == null || !isBlocked ? null : getCardViews(blockers));
        }
        return combatView;
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
        return getPlayerViews(game.getRegisteredPlayers());
    }

    @Override
    public PlayerView getPlayerTurn() {
        return getPlayerView(game.getPhaseHandler().getPlayerTurn());
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
        stackItems.retainAllKeys(stack);
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
        synchronized (this) {
            stackItems.retainAllKeys(Lists.newArrayList(stack));
            final List<StackItemView> items = Lists.newLinkedList();
            for (final SpellAbilityStackInstance si : stack) {
                if (stackItems.containsKey(si)) {
                    items.add(stackItems.get(si));
                } else {
                    items.add(getStackItemView(si));
                }
            }
            return items;
        }
    }

    public StackItemView getStackItemView(final SpellAbilityStackInstance si) {
        final StackItemView newItem = new StackItemView(
                si.getSpellAbility().toUnsuppressedString(),
                si.getSpellAbility().getSourceTrigger(),
                si.getStackDescription(), getCardView(si.getSourceCard()),
                getPlayerView(si.getActivator()), getCardViews(si.getTargetChoices().getTargetCards()),
                getPlayerViews(si.getTargetChoices().getTargetPlayers()), si.isAbility(), si.isOptionalTrigger());
        stackItems.put(si, newItem);
        return newItem;
    }

    public SpellAbilityStackInstance getStackItem(final StackItemView view) {
        return stackItems.getKey(view);
    }

    public final GameEntityView getGameEntityView(final GameEntity e) {
        if (e instanceof Card) {
            return getCardView((Card)e);
        } else if (e instanceof Player) {
            return getPlayerView((Player)e);
        }
        return null;
    }

    private final Function<GameEntity, GameEntityView> FN_GET_GAME_ENTITY_VIEW = new Function<GameEntity, GameEntityView>() {
        @Override
        public GameEntityView apply(GameEntity input) {
            return getGameEntityView(input);
        }
    };

    public final List<GameEntityView> getGameEntityViews(final Iterable<GameEntity> entities) {
        return ViewUtil.transformIfNotNull(entities, FN_GET_GAME_ENTITY_VIEW);
    }

    public final GameEntity getGameEntity(final GameEntityView view) {
        if (view instanceof CardView) {
            return getCard((CardView) view);
        } else if (view instanceof PlayerView) {
            return getPlayer((PlayerView) view);
        }
        return null;
    }

    private final Function<Player, PlayerView> FN_GET_PLAYER_VIEW = new Function<Player, PlayerView>() {
        @Override
        public PlayerView apply(final Player input) {
            return getPlayerView(input);
        }
    };

    public final List<PlayerView> getPlayerViews(final Iterable<Player> players) {
        return ViewUtil.transformIfNotNull(players, FN_GET_PLAYER_VIEW);
    }

    public PlayerView getPlayerView(final Player p) {
        if (p == null) {
            return null;
        }

        final PlayerView view;
        if (players.containsKey(p)) {
            view = players.get(p);
            getPlayerView(p, view);
        } else {
            view = new PlayerView(p.getLobbyPlayer(), p.getId());
            players.put(p, view);
            getPlayerView(p, view);
            view.setOpponents(getPlayerViews(p.getOpponents()));
        }
        return view;
    }

    private PlayerView getPlayerViewFast(final Player p) {
        return players.get(p);
    }

    public Player getPlayer(final PlayerView p) {
        return players.getKey(p);
    }

    private void getPlayerView(final Player p, final PlayerView view) {
        view.setCommanderInfo(CardFactoryUtil.getCommanderInfo(p).trim().replace("\r\n", "; "));
        view.setKeywords(p.getKeywords());
        view.setLife(p.getLife());
        view.setMaxHandSize(p.getMaxHandSize());
        view.setNumDrawnThisTurn(p.getNumDrawnThisTurn());
        view.setPoisonCounters(p.getPoisonCounters());
        view.setPreventNextDamage(p.getPreventNextDamageTotalShields());
        view.setHasUnlimitedHandSize(p.isUnlimitedHandSize());
        view.setAnteCards(getCardViews(p.getCardsIn(ZoneType.Ante)));
        view.setBfCards(getCardViews(p.getCardsIn(ZoneType.Battlefield, false)));
        view.setCommandCards(getCardViews(p.getCardsIn(ZoneType.Command)));
        view.setExileCards(getCardViews(p.getCardsIn(ZoneType.Exile)));
        view.setFlashbackCards(getCardViews(p.getCardsActivableInExternalZones(false)));
        view.setGraveCards(getCardViews(p.getCardsIn(ZoneType.Graveyard)));
        final List<Card> handCards = p.getCardsIn(ZoneType.Hand),
                libraryCards = p.getCardsIn(ZoneType.Library);
        view.setHandCards(getCardViews(handCards));
        view.setLibraryCards(getCardViews(libraryCards));
        view.setnHandCards(handCards.size());
        view.setnLibraryCards(libraryCards.size());

        for (final byte b : MagicColor.WUBRGC) {
            view.setMana(b, p.getManaPool().getAmountOfColor(b));
        }
    }

    public CardView getCardView(final Card c) {
        if (c == null) {
            return null;
        }

        final Card cUi = c.getCardForUi();
        final boolean isDisplayable = cUi == c;

        CardView view = cards.get(c);
        final boolean mayShow;
        if (view != null) {
            // Put here again to ensure the Card reference in the cache
            // is not an outdated Card.
            cards.put(c, view);
            mayShow = mayShowCard(view);
        }
        else {
            view = new CardView(isDisplayable);
            mayShow = mayShowCard(view);
            if (isDisplayable && mayShow) {
                cards.put(c, view);
            }
        }

        if (mayShow) {
            synchronized (cUi) {
                writeCardToView(cUi, view);
            }
        }
        else if (isDisplayable) {
            view.reset();
        }
        else {
            return null;
        }

        return view;
    }

    private final Function<Card, CardView> FN_GET_CARD_VIEW = new Function<Card, CardView>() {
        @Override
        public CardView apply(final Card input) {
            return getCardView(input);
        }
    };

    public final List<CardView> getCardViews(final Iterable<Card> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_CARD_VIEW);
    }

    public final List<CardView> getRefreshedCardViews(final Iterable<CardView> cardViews) {
        return ViewUtil.transformIfNotNull(cardViews, new Function<CardView, CardView>() {
            @Override
            public CardView apply(CardView input) {
                return cards.getValue(input);
            }
        });
    }

    private CardView getCardViewFast(final Card c) {
        if (c == null) {
            return null;
        }

        final CardView view = cards.get(c);
        if (mayShowCard(view)) {
            return view;
        } else if (view.isUiDisplayable()) {
            view.reset();
            return view;
        }

        return null;
    }

    private final Function<Card, CardView> FN_GET_CARDVIEW_FAST = new Function<Card, CardView>() {
        @Override
        public CardView apply(Card input) {
            return getCardViewFast(input);
        }
    };

    private List<CardView> getCardViewsFast(final Iterable<Card> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_CARDVIEW_FAST);
    }

    public Card getCard(final CardView c) {
        return cards.getKey(c);
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

    private void writeCardToView(final Card c, final CardView view) {
        synchronized (c) {
            // First, write the values independent of other views.
            ViewUtil.writeNonDependentCardViewProperties(c, view, mayShowCardFace(view));
            // Next, write the values that depend on other views.
            final Combat combat = game.getCombat();
            view.setOwner(getPlayerViewFast(c.getOwner()));
            view.setController(getPlayerViewFast(c.getController()));
            view.setAttacking(combat != null && combat.isAttacking(c));
            view.setBlocking(combat != null && combat.isBlocking(c));
            view.setChosenPlayer(getPlayerViewFast(c.getChosenPlayer()));
            view.setEquipping(getCardViewFast(Iterables.getFirst(c.getEquipping(), null)));
            view.setEquippedBy(getCardViewsFast(c.getEquippedBy()));
            view.setEnchantingCard(getCardViewFast(c.getEnchantingCard()));
            view.setEnchantingPlayer(getPlayerViewFast(c.getEnchantingPlayer()));
            view.setEnchantedBy(getCardViewsFast(c.getEnchantedBy()));
            view.setFortifiedBy(getCardViewsFast(c.getFortifiedBy()));
            view.setGainControlTargets(getCardViewsFast(c.getGainControlTargets()));
            view.setCloneOrigin(getCardViewFast(c.getCloneOrigin()));
            view.setImprinted(getCardViewsFast(c.getImprinted()));
            view.setHauntedBy(getCardViewsFast(c.getHauntedBy()));
            view.setHaunting(getCardViewFast(c.getHaunting()));
            view.setMustBlock(c.getMustBlockCards() == null ? Collections.<CardView>emptySet() : getCardViewsFast(c.getMustBlockCards()));
            view.setPairedWith(getCardViewFast(c.getPairedWith()));
        }
    }

    public SpellAbilityView getSpellAbilityView(final SpellAbility sa) {
        if (sa == null) {
            return null;
        }

        final SpellAbilityView view;
        if (spabs.containsKey(sa)) {
            view = spabs.get(sa);
            writeSpellAbilityToView(sa, view);
        } else {
            view = new SpellAbilityView();
            writeSpellAbilityToView(sa, view);
            spabs.put(sa, view);
        }
        return view;
    }

    private final Function<SpellAbility, SpellAbilityView> FN_GET_SPAB_VIEW = new Function<SpellAbility, SpellAbilityView>() {
        @Override
        public SpellAbilityView apply(final SpellAbility input) {
            return getSpellAbilityView(input);
        }
    };

    public final List<SpellAbilityView> getSpellAbilityViews(final List<SpellAbility> cards) {
        return ViewUtil.transformIfNotNull(cards, FN_GET_SPAB_VIEW);
    }

    public SpellAbility getSpellAbility(final SpellAbilityView c) {
        return spabs.getKey(c);
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
        view.setHostCard(getCardView(sa.getHostCard()));
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

}
