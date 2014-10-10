package forge.game.card;

import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ImageKeys;
import forge.card.CardCharacteristicName;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.Direction;
import forge.game.GameEntityView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty;


public class CardView extends GameEntityView {
    public static CardView get(Card c) {
        return c == null ? null : c.getView();
    }
    public static CardStateView getState(Card c, CardCharacteristicName state) {
        if (c == null) { return null; }
        CardCharacteristics s = c.getState(state);
        return s == null ? null : s.getView();
    }

    public static CardView getCardForUi(IPaperCard pc) {
        return Card.getCardForUi(pc).getView();
    }

    public static TrackableCollection<CardView> getCollection(Iterable<Card> cards) {
        if (cards == null) {
            return null;
        }
        TrackableCollection<CardView> collection = new TrackableCollection<CardView>();
        for (Card c : cards) {
            collection.add(c.getView());
        }
        return collection;
    }

    public static boolean mayViewAny(Iterable<CardView> cards) {
        if (cards == null) { return false; }

        for (CardView cv : cards) {
            if (cv.mayBeShown) {
                return true;
            }
        }
        return false;
    }

    public CardView(int id0) {
        super(id0);
        set(TrackableProperty.Original, new CardStateView(id0));
    }
    public CardView(int id0, String name0) {
        this(id0);
        getOriginal().setName(name0);
    }
    public CardView(int id0, String name0, PlayerView ownerAndController, String imageKey) {
        this(id0, name0);
        set(TrackableProperty.Owner, ownerAndController);
        set(TrackableProperty.Controller, ownerAndController);
    }

    public PlayerView getOwner() {
        return get(TrackableProperty.Owner);
    }
    void updateOwner(Card c) {
        set(TrackableProperty.Owner, PlayerView.get(c.getOwner()));
    }

    public PlayerView getController() {
        return get(TrackableProperty.Controller);
    }
    void updateController(Card c) {
        set(TrackableProperty.Controller, PlayerView.get(c.getController()));
    }

    public ZoneType getZone() {
        return get(TrackableProperty.Zone);
    }
    void updateZone(Card c) {
        set(TrackableProperty.Zone, c.getZone() == null ? null : c.getZone().getZoneType());
    }

    public boolean isCloned() {
        return get(TrackableProperty.Cloned);
    }

    public boolean isFaceDown() {
        return get(TrackableProperty.FaceDown);
    }

    public boolean isFlipCard() {
        return get(TrackableProperty.FlipCard);
    }

    public boolean isFlipped() {
        return get(TrackableProperty.Flipped);
    }

    public boolean isSplitCard() {
        return get(TrackableProperty.SplitCard);
    }

    public boolean isTransformed() {
        return get(TrackableProperty.Transformed);
    }

    public String getSetCode() {
        return get(TrackableProperty.SetCode);
    }
    void updateSetCode(Card c) {
        set(TrackableProperty.SetCode, c.getCurSetCode());
    }

    public CardRarity getRarity() {
        return get(TrackableProperty.Rarity);
    }
    void updateRarity(Card c) {
        set(TrackableProperty.Rarity, c.getRarity());
    }

    public boolean isAttacking() {
        return get(TrackableProperty.Attacking);
    }
    void updateAttacking(Card c) {
        set(TrackableProperty.Attacking, c.getGame().getCombat().isAttacking(c));
    }

    public boolean isBlocking() {
        return get(TrackableProperty.Blocking);
    }
    void updateBlocking(Card c) {
        set(TrackableProperty.Blocking, c.getGame().getCombat().isBlocking(c));
    }

    public boolean isPhasedOut() {
        return get(TrackableProperty.PhasedOut);
    }
    void updatePhasedOut(Card c) {
        set(TrackableProperty.PhasedOut, c.isPhasedOut());
    }

    public boolean isFirstTurnControlled() {
        return get(TrackableProperty.Sickness);
    }
    public boolean hasSickness() {
        return isFirstTurnControlled() && !getOriginal().hasHaste();
    }
    public boolean isSick() {
        return getZone() == ZoneType.Battlefield && getOriginal().isCreature() && hasSickness();
    }
    void updateSickness(Card c) {
        set(TrackableProperty.Sickness, c.isFirstTurnControlled());
    }

    public boolean isTapped() {
        return get(TrackableProperty.Tapped);
    }
    void updateTapped(Card c) {
        set(TrackableProperty.Tapped, c.isTapped());
    }

    public boolean isToken() {
        return get(TrackableProperty.Token);
    }
    void updateToken(Card c) {
        set(TrackableProperty.Token, c.isToken());
    }

    public Map<CounterType, Integer> getCounters() {
        return get(TrackableProperty.Counters);
    }
    public boolean hasSameCounters(CardView otherCard) {
        Map<CounterType, Integer> counters = getCounters();
        if (counters == null) {
            return otherCard.getCounters() == null;
        }
        return counters.equals(otherCard.getCounters());
    }
    void updateCounters(Card c) {
        set(TrackableProperty.Counters, c.getCounters());
        CardStateView state = getOriginal();
        state.updatePower(c);
        state.updateToughness(c);
        state.updateLoyalty(c);
    }

    public int getDamage() {
        return get(TrackableProperty.Damage);
    }
    void updateDamage(Card c) {
        set(TrackableProperty.Damage, c.getDamage());
    }

    public int getAssignedDamage() {
        return get(TrackableProperty.AssignedDamage);
    }
    void updateAssignedDamage(Card c) {
        set(TrackableProperty.AssignedDamage, c.getTotalAssignedDamage());
    }

    public int getLethalDamage() {
        return getOriginal().getToughness() - getDamage() - getAssignedDamage();
    }

    public int getShieldCount() {
        return get(TrackableProperty.ShieldCount);
    }
    void updateShieldCount(Card c) {
        set(TrackableProperty.ShieldCount, c.getShieldCount());
    }

    public String getChosenType() {
        return get(TrackableProperty.ChosenType);
    }
    void updateChosenType(Card c) {
        set(TrackableProperty.ChosenType, c.getChosenType());
    }

    public List<String> getChosenColors() {
        return get(TrackableProperty.ChosenColors);
    }
    void updateChosenColors(Card c) {
        set(TrackableProperty.ChosenColors, c.getChosenColors());
    }

    public PlayerView getChosenPlayer() {
        return get(TrackableProperty.ChosenPlayer);
    }
    void updateChosenPlayer(Card c) {
        set(TrackableProperty.ChosenPlayer, PlayerView.get(c.getChosenPlayer()));
    }

    public Direction getChosenDirection() {
        return get(TrackableProperty.ChosenDirection);
    }
    void updateChosenDirection(Card c) {
        set(TrackableProperty.ChosenDirection, c.getChosenDirection());
    }

    private String getRemembered() {
        return get(TrackableProperty.Remembered);
    }
    void updateRemembered(Card c) {
        if (c.getRemembered() == null) {
            set(TrackableProperty.Remembered, null);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\r\nRemembered: \r\n");
        for (final Object o : c.getRemembered()) {
            if (o instanceof Card) {
                final Card card = (Card) o;
                if (card.isFaceDown()) {
                    sb.append("Face Down");
                    // face-down cards don't show unique number to avoid cheating
                }
                else {
                    sb.append(card.getName());
                    sb.append(" (");
                    sb.append(card.getId());
                    sb.append(")");
                }
            }
            else if (o != null) {
                sb.append(o.toString());
            }
            sb.append("\r\n");
        }
        set(TrackableProperty.Remembered, sb.toString());
    }

    public String getNamedCard() {
        return get(TrackableProperty.NamedCard);
    }
    void updateNamedCard(Card c) {
        set(TrackableProperty.NamedCard, c.getNamedCard());
    }

    public CardView getEquipping() {
        return get(TrackableProperty.Equipping);
    }

    public Iterable<CardView> getEquippedBy() {
        return get(TrackableProperty.EquippedBy);
    }

    public boolean isEquipped() {
        return getEquippedBy() != null;
    }

    public GameEntityView getEnchanting() {
        return get(TrackableProperty.Enchanting);
    }
    void updateEnchanting(Card c) {
        set(TrackableProperty.Owner, GameEntityView.get(c.getEnchanting()));
    }

    public CardView getEnchantingCard() {
        GameEntityView enchanting = getEnchanting();
        if (enchanting instanceof CardView) {
            return (CardView) enchanting;
        }
        return null;
    }
    public PlayerView getEnchantingPlayer() {
        GameEntityView enchanting = getEnchanting();
        if (enchanting instanceof PlayerView) {
            return (PlayerView) enchanting;
        }
        return null;
    }

    public CardView getFortifying() {
        return get(TrackableProperty.Fortifying);
    }

    public Iterable<CardView> getFortifiedBy() {
        return get(TrackableProperty.FortifiedBy);
    }

    public boolean isFortified() {
        return getFortifiedBy() != null;
    }

    public Iterable<CardView> getGainControlTargets() {
        return get(TrackableProperty.GainControlTargets);
    }

    public CardView getCloneOrigin() {
        return get(TrackableProperty.CloneOrigin);
    }

    public Iterable<CardView> getImprintedCards() {
        return get(TrackableProperty.ImprintedCards);
    }

    public Iterable<CardView> getHauntedBy() {
        return get(TrackableProperty.HauntedBy);
    }

    public CardView getHaunting() {
        return get(TrackableProperty.Haunting);
    }

    public Iterable<CardView> getMustBlockCards() {
        return get(TrackableProperty.MustBlockCards);
    }

    public CardView getPairedWith() {
        return get(TrackableProperty.PairedWith);
    }

    public Map<String, String> getChangedColorWords() {
        return get(TrackableProperty.ChangedColorWords);
    }
    void updateChangedColorWords(Card c) {
        set(TrackableProperty.ChangedColorWords, c.getChangedTextColorWords());
    }

    public Map<String, String> getChangedTypes() {
        return get(TrackableProperty.ChangedTypes);
    }
    void updateChangedTypes(Card c) {
        set(TrackableProperty.ChangedTypes, c.getChangedTextTypeWords());
    }

    public String getText() {
        final Card card = Card.get(this); //TODO: Avoid needing this
        final CardStateView state = getOriginal();
        if (card == null) {
            return state.getOracleText().trim();
        }

        final StringBuilder sb = new StringBuilder();

        // Vanguard Modifiers
        if (card.isType("Vanguard")) {
            sb.append("Hand Modifier: ").append(card.getRules().getHand());
            sb.append("\r\nLife Modifier: ").append(card.getRules().getLife());
            sb.append("\r\n\r\n");
        }
        if (card.isCommander()) {
            sb.append(getOwner()).append("'s Commander\r\n");
            sb.append(CardFactoryUtil.getCommanderInfo(Player.get(getOwner()))).append("\r\n");
        }
        sb.append(card.getAbilityText());

        String nonAbilityText = card.getNonAbilityText();
        if (card.getAmountOfKeyword("CARDNAME can block an additional creature.") > 1) {
            final StringBuilder ab = new StringBuilder();
            ab.append("CARDNAME can block an additional ");
            ab.append(card.getAmountOfKeyword("CARDNAME can block an additional creature."));
            ab.append(" creatures.");
            nonAbilityText = nonAbilityText.replaceFirst("CARDNAME can block an additional creature.", ab.toString());
            nonAbilityText = nonAbilityText.replaceAll("CARDNAME can block an additional creature.", "");
            nonAbilityText = nonAbilityText.replaceAll("\r\n\r\n\r\n", "");
        }
        if (nonAbilityText.length() > 0) {
            sb.append("\r\n \r\nNon ability features: \r\n");
            sb.append(nonAbilityText.replaceAll("CARDNAME", getName()));
        }

        sb.append(getRemembered());

        PlayerView chosenPlayer = getChosenPlayer();
        if (chosenPlayer != null) {
            sb.append("\r\n[Chosen player: ");
            sb.append(chosenPlayer);
            sb.append("]\r\n");
        }

        Direction chosenDirection = getChosenDirection();
        if (chosenDirection != null) {
            sb.append("\r\n[Chosen direction: ");
            sb.append(chosenDirection);
            sb.append("]\r\n");
        }

        Iterable<CardView> hauntedBy = getHauntedBy();
        if (hauntedBy != null) {
            sb.append("Haunted by: ");
            boolean needDelim = false;
            for (final CardView c : hauntedBy) {
                if (needDelim) {
                    sb.append(",");
                }
                else { needDelim = false; }
                sb.append(c);
            }
            sb.append("\r\n");
        }

        CardView haunting = getHaunting();
        if (haunting != null) {
            sb.append("Haunting: ").append(haunting);
            sb.append("\r\n");
        }

        CardView pairedWith = getPairedWith();
        if (pairedWith != null) {
            sb.append("\r\n \r\nPaired With: ").append(pairedWith);
            sb.append("\r\n");
        }

        CardCharacteristics cloner = card.getState(CardCharacteristicName.Cloner);
        if (cloner != null) {
            sb.append("\r\nCloned by: ").append(cloner.getView().getName());
            sb.append(" (").append(cloner.getView().getId()).append(")");
        }

        return sb.toString().trim();
    }

    public CardStateView getOriginal() {
        return get(TrackableProperty.Original);
    }

    public boolean hasAltState() {
        return getAlternate() != null;
    }
    public CardStateView getAlternate() {
        return get(TrackableProperty.Alternate);
    }
    CardStateView createAlternateState() {
        return new CardStateView(getId());
    }

    public CardStateView getState(final boolean alternate0) {
        return alternate0 ? getAlternate() : getOriginal();
    }
    void updateState(Card c, boolean fromCurStateChange) {
        boolean isDoubleFaced = c.isDoubleFaced();
        boolean isFaceDown = c.isFaceDown();
        boolean isFlipCard = c.isFlipCard();
        boolean isFlipped = c.getCurState() == CardCharacteristicName.Flipped;
        boolean isSplitCard = c.isSplitCard();
        boolean isTransformed = c.getCurState() == CardCharacteristicName.Transformed;
        boolean hasAltState = isDoubleFaced || isFlipCard || isSplitCard || (isFaceDown/* && mayShowCardFace*/);

        set(TrackableProperty.Cloned, c.isCloned());
        set(TrackableProperty.FaceDown, isFaceDown);
        set(TrackableProperty.SplitCard, isSplitCard);
        set(TrackableProperty.FlipCard, isFlipCard);

        if (fromCurStateChange) {
            set(TrackableProperty.Flipped, isFlipped);
            set(TrackableProperty.Transformed, isTransformed);
            updateRarity(c); //rarity and set based on current state
            updateSetCode(c);
        }

        if (isSplitCard) {
            final CardCharacteristicName orig, alt;
            if (c.getCurState() == CardCharacteristicName.RightSplit) {
                // If right half on stack, place it first
                orig = CardCharacteristicName.RightSplit;
                alt = CardCharacteristicName.LeftSplit;
            }
            else {
                orig = CardCharacteristicName.LeftSplit;
                alt = CardCharacteristicName.RightSplit;
            }
            set(TrackableProperty.Original, CardView.getState(c, orig));
            set(TrackableProperty.Alternate, CardView.getState(c, alt));
        }
        else if (hasAltState) {
            if (isFlipCard && !isFlipped) {
                set(TrackableProperty.Alternate, CardView.getState(c, CardCharacteristicName.Flipped));
            }
            else if (isDoubleFaced && !isTransformed) {
                set(TrackableProperty.Alternate, CardView.getState(c, CardCharacteristicName.Transformed));
            }
            else {
                set(TrackableProperty.Alternate, CardView.getState(c, CardCharacteristicName.Original));
            }
        }
        else {
            set(TrackableProperty.Alternate, null);
        }
    }

    @Override
    public String toString() {
        if (getId() <= 0) { //if fake card, just return name
            return getOriginal().getName();
        }

        if (!mayBeShown) {
            return "(Unknown card)";
        }

        if (StringUtils.isEmpty(getOriginal().getName())) {
            CardStateView alternate = getAlternate();
            if (alternate != null) {
                return "Face-down card (" + getAlternate().getName() + ")";
            }
            return "(" + getId() + ")";
        }
        return getOriginal().getName() + " (" + getId() + ")";
    }

    public String determineName(final CardStateView state) {
        if (state == getOriginal()) {
            return toString();
        }
        return getAlternate().getName() + " (" + getId() + ")";
    }

    public class CardStateView extends TrackableObject {
        public CardStateView(int id0) {
            super(id0);
        }

        @Override
        public String toString() {
            return getCard().determineName(this);
        }

        public CardView getCard() {
            return CardView.this;
        }

        public String getName() {
            return get(TrackableProperty.Name);
        }
        void updateName(Card c) {
            setName(c.getName());
        }
        void updateName(CardCharacteristics c) {
            setName(c.getName());
        }
        private void setName(String name0) {
            set(TrackableProperty.Name, name0);
        }

        public ColorSet getColors() {
            return get(TrackableProperty.Colors);
        }
        void updateColors(Card c) {
            set(TrackableProperty.Colors, c.determineColor());
        }
        void updateColors(CardCharacteristics c) {
            set(TrackableProperty.Colors, c.determineColor());
        }

        public String getImageKey(boolean ignoreMayBeShown) {
            if (mayBeShown || ignoreMayBeShown) {
                return get(TrackableProperty.ImageKey);
            }
            return ImageKeys.HIDDEN_CARD;
        }
        void updateImageKey(Card c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }
        void updateImageKey(CardCharacteristics c) {
            set(TrackableProperty.ImageKey, c.getImageKey());
        }

        public Set<String> getType() {
            return get(TrackableProperty.Type);
        }
        void updateType(CardCharacteristics c) {
            set(TrackableProperty.Type, c.getType());
        }

        public ManaCost getManaCost() {
            return get(TrackableProperty.ManaCost);
        }
        void updateManaCost(CardCharacteristics c) {
            set(TrackableProperty.ManaCost, c.getManaCost());
        }

        public int getPower() {
            return get(TrackableProperty.Power);
        }
        void updatePower(Card c) {
            set(TrackableProperty.Power, c.getNetAttack());
        }
        void updatePower(CardCharacteristics c) {
            if (CardView.this.getOriginal() == this) {
                Card card = Card.get(CardView.this);
                if (card != null) {
                    updatePower(card); //TODO: find a better way to do this
                    return;
                }
            }
            set(TrackableProperty.Power, c.getBaseAttack());
        }

        public int getToughness() {
            return get(TrackableProperty.Toughness);
        }
        void updateToughness(Card c) {
            set(TrackableProperty.Toughness, c.getNetDefense());
        }
        void updateToughness(CardCharacteristics c) {
            if (CardView.this.getOriginal() == this) {
                Card card = Card.get(CardView.this);
                if (card != null) {
                    updateToughness(card); //TODO: find a better way to do this
                    return;
                }
            }
            set(TrackableProperty.Toughness, c.getBaseDefense());
        }

        public int getLoyalty() {
            return get(TrackableProperty.Loyalty);
        }
        void updateLoyalty(Card c) {
            set(TrackableProperty.Loyalty, c.getCurrentLoyalty());
        }
        void updateLoyalty(CardCharacteristics c) {
            if (CardView.this.getOriginal() == this) {
                Card card = Card.get(CardView.this);
                if (card != null) {
                    updateLoyalty(card); //TODO: find a better way to do this
                    return;
                }
            }
            set(TrackableProperty.Loyalty, 0); //alternates don't need loyalty
        }

        public String getOracleText() {
            return get(TrackableProperty.OracleText);
        }
        void updateOracleText(CardCharacteristics c) {
            set(TrackableProperty.OracleText, c.getOracleText());
        }

        private int foilIndexOverride = -1;
        public int getFoilIndex() {
            if (foilIndexOverride >= 0) {
                return foilIndexOverride;
            }
            return get(TrackableProperty.FoilIndex);
        }
        void updateFoilIndex(Card c) {
            updateFoilIndex(c.getCharacteristics());
        }
        void updateFoilIndex(CardCharacteristics c) {
            set(TrackableProperty.FoilIndex, c.getFoil());
        }
        public void setFoilIndexOverride(int index0) {
            if (index0 < 0) {
                index0 = CardEdition.getRandomFoil(getSetCode());
            }
            foilIndexOverride = index0;
        }

        public boolean hasDeathtouch() {
            return get(TrackableProperty.HasDeathtouch);
        }
        public boolean hasHaste() {
            return get(TrackableProperty.HasHaste);
        }
        public boolean hasInfect() {
            return get(TrackableProperty.HasInfect);
        }
        public boolean hasStorm() {
            return get(TrackableProperty.HasStorm);
        }
        public boolean hasTrample() {
            return get(TrackableProperty.HasTrample);
        }
        void updateKeywords(Card c) {
            set(TrackableProperty.HasDeathtouch, c.hasKeyword("Deathtouch"));
            set(TrackableProperty.HasHaste, c.hasKeyword("Haste"));
            set(TrackableProperty.HasInfect, c.hasKeyword("Infect"));
            set(TrackableProperty.HasStorm, c.hasKeyword("Storm"));
            set(TrackableProperty.HasTrample, c.hasKeyword("Trample"));
        }

        public boolean isBasicLand() {
            return isLand() && Iterables.any(getType(), Predicates.in(CardType.getBasicTypes()));
        }
        public boolean isCreature() {
            return getType().contains("Creature");
        }
        public boolean isLand() {
            return getType().contains("Land");
        }
        public boolean isPlane() {
            return getType().contains("Plane");
        }
        public boolean isPhenomenon() {
            return getType().contains("Phenomenon");
        }
        public boolean isPlaneswalker() {
            return getType().contains("Planeswalker");
        }
    }

    //special methods for updating card and player properties as needed and returning the new collection
    Card setCard(Card oldCard, Card newCard, TrackableProperty key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    CardCollection setCards(CardCollection oldCards, CardCollection newCards, TrackableProperty key) {
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, TrackableProperty key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    CardCollection addCard(CardCollection oldCards, Card cardToAdd, TrackableProperty key) {
        if (cardToAdd == null) { return oldCards; }

        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        if (oldCards.add(cardToAdd)) {
            TrackableCollection<CardView> views = get(key);
            if (views == null) {
                views = new TrackableCollection<CardView>();
                views.add(cardToAdd.getView());;
                set(key, views);
            }
            else if (views.add(cardToAdd.getView())) {
                flagAsChanged(key);
            }
        }
        return oldCards;
    }
    CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, TrackableProperty key) {
        if (cardsToAdd == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        if (oldCards == null) {
            oldCards = new CardCollection();
        }
        boolean needFlagAsChanged = false;
        for (Card c : cardsToAdd) {
            if (oldCards.add(c)) {
                if (views == null) {
                    views = new TrackableCollection<CardView>();
                    views.add(c.getView());
                    set(key, views);
                }
                else if (views.add(c.getView())) {
                    needFlagAsChanged = true;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection removeCard(CardCollection oldCards, Card cardToRemove, TrackableProperty key) {
        if (cardToRemove == null || oldCards == null) { return oldCards; }

        if (oldCards.remove(cardToRemove)) {
            TrackableCollection<CardView> views = get(key);
            if (views != null && views.remove(cardToRemove.getView())) {
                if (views.isEmpty()) {
                    set(key, null); //avoid keeping around an empty collection
                }
                else {
                    flagAsChanged(key);
                }
            }
            if (oldCards.isEmpty()) {
                oldCards = null; //avoid keeping around an empty collection
            }
        }
        return oldCards;
    }
    CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, TrackableProperty key) {
        if (cardsToRemove == null || oldCards == null) { return oldCards; }

        TrackableCollection<CardView> views = get(key);
        boolean needFlagAsChanged = false;
        for (Card c : cardsToRemove) {
            if (oldCards.remove(c)) {
                if (views != null && views.remove(c.getView())) {
                    if (views.isEmpty()) {
                        views = null;
                        set(key, null); //avoid keeping around an empty collection
                        needFlagAsChanged = false; //doesn't need to be flagged a second time
                    }
                    else {
                        needFlagAsChanged = true;
                    }
                }
                if (oldCards.isEmpty()) {
                    oldCards = null; //avoid keeping around an empty collection
                    break;
                }
            }
        }
        if (needFlagAsChanged) {
            flagAsChanged(key);
        }
        return oldCards;
    }
    CardCollection clearCards(CardCollection oldCards, TrackableProperty key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }

    //below are properties not shared across game instances
    private boolean mayBeShown = true; //TODO: Make may be shown get updated
    public boolean mayBeShown() {
        return mayBeShown;
    }
    public void setMayBeShown(boolean mayBeShown0) {
        //mayBeShown = mayBeShown0;
    }
}
