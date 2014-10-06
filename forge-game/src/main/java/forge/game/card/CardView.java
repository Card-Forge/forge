package forge.game.card;

import java.util.Set;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.card.CardCharacteristicName;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.GameEntityView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableObject;
import forge.trackable.TrackableProperty.CardProp;
import forge.trackable.TrackableProperty.CardStateProp;


public class CardView extends GameEntityView<CardProp> {
    public static CardView get(Card c) {
        return c == null ? null : c.getView();
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

    public CardView(int id0) {
        super(id0, CardProp.class);
        set(CardProp.Original, new CardStateView(id0));
    }

    public PlayerView getOwner() {
        return get(CardProp.Owner);
    }
    void updateOwner(Card c) {
        set(CardProp.Owner, PlayerView.get(c.getOwner()));
    }

    public PlayerView getController() {
        return get(CardProp.Controller);
    }
    void updateController(Card c) {
        set(CardProp.Controller, PlayerView.get(c.getController()));
    }

    public ZoneType getZone() {
        return get(CardProp.Zone);
    }
    void updateZone(Card c) {
        set(CardProp.Zone, c.getZone() == null ? null : c.getZone().getZoneType());
    }

    public boolean isCloned() {
        return get(CardProp.Cloned);
    }

    public boolean isFaceDown() {
        return get(CardProp.FaceDown);
    }

    public boolean isFlipCard() {
        return get(CardProp.FlipCard);
    }

    public boolean isFlipped() {
        return get(CardProp.Flipped);
    }

    public boolean isSplitCard() {
        return get(CardProp.SplitCard);
    }

    public boolean isTransformed() {
        return get(CardProp.Transformed);
    }

    public String getSetCode() {
        return get(CardProp.SetCode);
    }
    void updateSetCode(Card c) {
        set(CardProp.SetCode, c.getCurSetCode());
    }

    public CardRarity getRarity() {
        return get(CardProp.Rarity);
    }
    void updateRarity(Card c) {
        set(CardProp.Rarity, c.getRarity());
    }

    public boolean isAttacking() {
        return get(CardProp.Attacking);
    }
    void updateAttacking(Card c) {
        set(CardProp.Attacking, c.getGame().getCombat().isAttacking(c));
    }

    public boolean isBlocking() {
        return get(CardProp.Blocking);
    }
    void updateBlocking(Card c) {
        set(CardProp.Blocking, c.getGame().getCombat().isBlocking(c));
    }

    public boolean isPhasedOut() {
        return get(CardProp.PhasedOut);
    }
    void updatePhasedOut(Card c) {
        set(CardProp.PhasedOut, c.isPhasedOut());
    }

    public boolean isFirstTurnControlled() {
        return get(CardProp.Sickness);
    }
    public boolean hasSickness() {
        return isFirstTurnControlled() && !getOriginal().hasHaste();
    }
    public boolean isSick() {
        return getZone() == ZoneType.Battlefield && hasSickness();
    }
    void updateSickness(Card c) {
        set(CardProp.Sickness, c.isInPlay() && c.isSick());
    }

    public boolean isTapped() {
        return get(CardProp.Tapped);
    }
    void updateTapped(Card c) {
        set(CardProp.Tapped, c.isTapped());
    }

    public boolean isToken() {
        return get(CardProp.Token);
    }
    void updateToken(Card c) {
        set(CardProp.Token, c.isToken());
    }

    public Map<CounterType, Integer> getCounters() {
        return get(CardProp.Counters);
    }
    void updateCounters(Card c) {
        set(CardProp.Counters, c.getCounters());
    }

    public int getDamage() {
        return get(CardProp.Damage);
    }
    void updateDamage(Card c) {
        set(CardProp.Damage, c.getDamage());
    }

    public int getAssignedDamage() {
        return get(CardProp.AssignedDamage);
    }
    void updateAssignedDamage(Card c) {
        set(CardProp.AssignedDamage, c.getTotalAssignedDamage());
    }

    public int getLethalDamage() {
        return getOriginal().getToughness() - getDamage() - getAssignedDamage();
    }

    public int getShieldCount() {
        return get(CardProp.ShieldCount);
    }
    void updateShieldCount(Card c) {
        set(CardProp.ShieldCount, c.getShieldCount());
    }

    public String getChosenType() {
        return get(CardProp.ChosenType);
    }
    void updateChosenType(Card c) {
        set(CardProp.ChosenType, c.getChosenType());
    }

    public List<String> getChosenColors() {
        return get(CardProp.ChosenColors);
    }
    void updateChosenColors(Card c) {
        set(CardProp.ChosenColors, c.getChosenColors());
    }

    public PlayerView getChosenPlayer() {
        return get(CardProp.ChosenPlayer);
    }
    void updateChosenPlayer(Card c) {
        set(CardProp.ChosenPlayer, c.getChosenPlayer());
    }

    public String getNamedCard() {
        return get(CardProp.NamedCard);
    }
    void updateNamedCard(Card c) {
        set(CardProp.NamedCard, c.getNamedCard());
    }

    public CardView getEquipping() {
        return get(CardProp.Equipping);
    }

    public Iterable<CardView> getEquippedBy() {
        return get(CardProp.EquippedBy);
    }

    public boolean isEquipped() {
        return getEquippedBy() != null;
    }

    public GameEntityView<?> getEnchanting() {
        return get(CardProp.Enchanting);
    }
    void updateEnchanting(Card c) {
        set(CardProp.Owner, GameEntityView.get(c.getEnchanting()));
    }

    public CardView getEnchantingCard() {
        GameEntityView<?> enchanting = getEnchanting();
        if (enchanting instanceof CardView) {
            return (CardView) enchanting;
        }
        return null;
    }
    public PlayerView getEnchantingPlayer() {
        GameEntityView<?> enchanting = getEnchanting();
        if (enchanting instanceof PlayerView) {
            return (PlayerView) enchanting;
        }
        return null;
    }

    public CardView getFortifying() {
        return get(CardProp.Fortifying);
    }

    public Iterable<CardView> getFortifiedBy() {
        return get(CardProp.FortifiedBy);
    }

    public boolean isFortified() {
        return getFortifiedBy() != null;
    }

    public Iterable<CardView> getGainControlTargets() {
        return get(CardProp.GainControlTargets);
    }

    public CardView getCloneOrigin() {
        return get(CardProp.CloneOrigin);
    }

    public Iterable<CardView> getImprintedCards() {
        return get(CardProp.ImprintedCards);
    }

    public Iterable<CardView> getHauntedBy() {
        return get(CardProp.HauntedBy);
    }

    public CardView getHaunting() {
        return get(CardProp.Haunting);
    }

    public Iterable<CardView> getMustBlockCards() {
        return get(CardProp.MustBlockCards);
    }

    public CardView getPairedWith() {
        return get(CardProp.PairedWith);
    }

    public CardStateView getOriginal() {
        return get(CardProp.Original);
    }

    public CardStateView getAlternate() {
        return get(CardProp.Alternate);
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

        set(CardProp.Alternate, hasAltState ? new CardStateView(getId()) : null);
        set(CardProp.Cloned, c.isCloned());
        set(CardProp.FaceDown, isFaceDown);
        set(CardProp.SplitCard, isSplitCard);
        set(CardProp.FlipCard, isFlipCard);

        if (fromCurStateChange) {
            set(CardProp.Flipped, isFlipped);
            set(CardProp.Transformed, isTransformed);
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
            updateState(c, getOriginal(), orig);
            updateState(c, getAlternate(), alt);
            return;
        }

        final CardStateView origView = getOriginal();
        origView.updateName(c);
        origView.updateColors(c);
        origView.updateImageKey(c);
        origView.updateType(c);
        origView.updateManaCost(c);
        origView.updatePower(c);
        origView.updateToughness(c);
        origView.updateLoyalty(c);
        origView.updateText(c);
        origView.updateChangedColorWords(c);
        origView.updateChangedTypes(c);
        origView.updateManaCost(c);
        origView.updateKeywords(c);
        origView.updateFoilIndex(c);

        if (hasAltState) {
            if (isFlipCard && !isFlipped) {
                updateState(c, getAlternate(), CardCharacteristicName.Flipped);
            }
            else if (isDoubleFaced && !isTransformed) {
                updateState(c, getAlternate(), CardCharacteristicName.Transformed);
            }
            else {
                updateState(c, getAlternate(), CardCharacteristicName.Original);
            }
        }
    }
    private void updateState(Card c, CardStateView view, CardCharacteristicName state) {
        final CardCharacteristics chars = c.getState(state);
        if (chars == null) { return; } //can happen when split card initialized before both sides have been initialized

        view.updateName(chars);
        view.updateColors(chars);
        view.updateImageKey(chars);
        view.updateType(chars);
        view.updateManaCost(chars);
        view.updatePower(chars);
        view.updateToughness(chars);
        view.updateLoyalty(chars); 
        view.updateText(chars);
        view.updateFoilIndex(chars);
    }

    @Override
    public String toString() {
        if (getId() <= 0) { //if fake card, just return name
            return getOriginal().getName();
        }

        /*if (!mayBeShown) {
            return "(Unknown card)";
        }*/

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

    public class CardStateView extends TrackableObject<CardStateProp> {
        public CardStateView(int id0) {
            super(id0, CardStateProp.class);
        }

        @Override
        public String toString() {
            return getCard().determineName(this);
        }

        public CardView getCard() {
            return CardView.this;
        }

        public String getName() {
            return get(CardStateProp.Name);
        }
        void updateName(Card c) {
            set(CardStateProp.Name, c.getName());
        }
        void updateName(CardCharacteristics c) {
            set(CardStateProp.Name, c.getName());
        }

        public ColorSet getColors() {
            return get(CardStateProp.Colors);
        }
        void updateColors(Card c) {
            set(CardStateProp.Colors, c.determineColor());
        }
        void updateColors(CardCharacteristics c) {
            set(CardStateProp.Colors, c.determineColor());
        }

        public String getImageKey() {
            return get(CardStateProp.ImageKey);
        }
        void updateImageKey(Card c) {
            set(CardStateProp.ImageKey, c.getImageKey());
        }
        void updateImageKey(CardCharacteristics c) {
            set(CardStateProp.ImageKey, c.getImageKey());
        }

        public Set<String> getType() {
            return get(CardStateProp.Type);
        }
        void updateType(Card c) {
            set(CardStateProp.Type, c.getType());
        }
        void updateType(CardCharacteristics c) {
            set(CardStateProp.Type, c.getType());
        }

        public ManaCost getManaCost() {
            return get(CardStateProp.ManaCost);
        }
        void updateManaCost(Card c) {
            set(CardStateProp.ManaCost, c.getManaCost());
        }
        void updateManaCost(CardCharacteristics c) {
            set(CardStateProp.ManaCost, c.getManaCost());
        }

        public int getPower() {
            return get(CardStateProp.Power);
        }
        void updatePower(Card c) {
            set(CardStateProp.Power, c.getNetAttack());
        }
        void updatePower(CardCharacteristics c) {
            set(CardStateProp.Power, c.getBaseAttack());
        }

        public int getToughness() {
            return get(CardStateProp.Toughness);
        }
        void updateToughness(Card c) {
            set(CardStateProp.Toughness, c.getNetDefense());
        }
        void updateToughness(CardCharacteristics c) {
            set(CardStateProp.Toughness, c.getBaseDefense());
        }

        public int getLoyalty() {
            return get(CardStateProp.Loyalty);
        }
        void updateLoyalty(Card c) {
            set(CardStateProp.Loyalty, c.getCurrentLoyalty());
        }
        void updateLoyalty(CardCharacteristics c) {
            set(CardStateProp.Loyalty, 0); // Q why is loyalty not a property of CardCharacteristic? A: because no alt states have a base loyalty (only candidate is Garruk Relentless).
        }

        public String getText() {
            return get(CardStateProp.Text);
        }
        void updateText(Card c) {
            set(CardStateProp.Text, c.getText());
        }
        void updateText(CardCharacteristics c) {
            set(CardStateProp.Text, c.getOracleText());
        }

        public int getFoilIndex() {
            return get(CardStateProp.FoilIndex);
        }
        void updateFoilIndex(Card c) {
            set(CardStateProp.FoilIndex, c.getCharacteristics().getFoil());
        }
        void updateFoilIndex(CardCharacteristics c) {
            set(CardStateProp.FoilIndex, c.getFoil());
        }

        public Map<String, String> getChangedColorWords() {
            return get(CardStateProp.ChangedColorWords);
        }
        void updateChangedColorWords(Card c) {
            set(CardStateProp.ChangedColorWords, c.getChangedTextColorWords());
        }

        public Map<String, String> getChangedTypes() {
            return get(CardStateProp.ChangedTypes);
        }
        void updateChangedTypes(Card c) {
            set(CardStateProp.ChangedTypes, c.getChangedTextTypeWords());
        }

        public boolean hasDeathtouch() {
            return get(CardStateProp.HasDeathtouch);
        }
        public boolean hasHaste() {
            return get(CardStateProp.HasHaste);
        }
        public boolean hasInfect() {
            return get(CardStateProp.HasInfect);
        }
        public boolean hasStorm() {
            return get(CardStateProp.HasStorm);
        }
        public boolean hasTrample() {
            return get(CardStateProp.HasTrample);
        }
        void updateKeywords(Card c) {
            set(CardStateProp.HasDeathtouch, c.hasKeyword("Deathtouch"));
            set(CardStateProp.HasHaste, c.hasKeyword("Haste"));
            set(CardStateProp.HasInfect, c.hasKeyword("Infect"));
            set(CardStateProp.HasStorm, c.hasKeyword("Storm"));
            set(CardStateProp.HasTrample, c.hasKeyword("Trample"));
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

    @Override
    protected CardProp preventNextDamageProp() {
        return CardProp.PreventNextDamage;
    }

    @Override
    protected CardProp enchantedByProp() {
        return CardProp.EnchantedBy;
    }

    //special methods for updating card and player properties as needed and returning the new collection
    Card setCard(Card oldCard, Card newCard, CardProp key) {
        if (newCard != oldCard) {
            set(key, CardView.get(newCard));
        }
        return newCard;
    }
    CardCollection setCards(CardCollection oldCards, CardCollection newCards, CardProp key) {
        set(key, CardView.getCollection(newCards)); //TODO prevent overwriting list if not necessary
        return newCards;
    }
    CardCollection setCards(CardCollection oldCards, Iterable<Card> newCards, CardProp key) {
        if (newCards == null) {
            set(key, null);
            return null;
        }
        return setCards(oldCards, new CardCollection(newCards), key);
    }
    CardCollection addCard(CardCollection oldCards, Card cardToAdd, CardProp key) {
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
    CardCollection addCards(CardCollection oldCards, Iterable<Card> cardsToAdd, CardProp key) {
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
    CardCollection removeCard(CardCollection oldCards, Card cardToRemove, CardProp key) {
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
    CardCollection removeCards(CardCollection oldCards, Iterable<Card> cardsToRemove, CardProp key) {
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
    CardCollection clearCards(CardCollection oldCards, CardProp key) {
        if (oldCards != null) {
            set(key, null);
        }
        return null;
    }
}
