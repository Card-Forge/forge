package forge.game.card;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.ability.effects.DetachedCardEffect;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

import java.util.List;
import java.util.Map;

import static forge.game.card.CardFactory.getCard;

public class CardCopyService {
    // A service to copy all sorts of things from a card to a new card.
    // This pulled in functions from CardFactory and CardUtil

    private final Card copyFrom;
    private final Game toGame;
    public CardCopyService(Card copyFrom) {
        this(copyFrom, copyFrom == null ? null : copyFrom.getGame());
    }

    public CardCopyService(Card copyFrom, Game toGame) {
        this.copyFrom = copyFrom;
        this.toGame = toGame;
    }

    public final Card copyCard(boolean assignNewId) {
        return copyCard(assignNewId, copyFrom.getOwner());
    }

    // Straight copying for things like moving a card to a different zone or using GameCopier
    public final Card copyCard(boolean assignNewId, Player owner) {
        Card out;
        if (copyFrom.isRealToken() || copyFrom.getCopiedPermanent() != null || copyFrom.getPaperCard() == null) {
            out = copyStats(copyFrom, owner, assignNewId);
            out.setEffectSource(copyFrom.getEffectSource());
            out.setBoon(copyFrom.isBoon());
            out.dangerouslySetGame(toGame);

            // need to copy this values for the tokens
            out.setTokenSpawningAbility(copyFrom.getTokenSpawningAbility());
            out.setCopiedPermanent(copyFrom.getCopiedPermanent());
        } else {
            out = assignNewId ? getCard(copyFrom.getPaperCard(), owner, toGame)
                    : getCard(copyFrom.getPaperCard(), owner, copyFrom.getId(), toGame);
        }

        out.setZone(copyFrom.getZone());
        out.setState(copyFrom.getFaceupCardStateName(), true);
        out.setBackSide(copyFrom.isBackSide());
        out.setGamePieceType(copyFrom.getGamePieceType());
        out.setTokenCard(copyFrom.isTokenCard());

        if (toGame == copyFrom.getGame()) {
            // Only copy these things if we're not copying them into a new game

            out.setCollectible(copyFrom.isCollectible());

            // this's necessary for forge.game.GameAction.unattachCardLeavingBattlefield(Card)
            if (copyFrom.hasCardAttachments()) {
                out.setAttachedCards(copyFrom.getAttachedCards());
            }
            if (copyFrom.isAttachedToEntity()) {
                out.setEntityAttachedTo(copyFrom.getEntityAttachedTo());
            }
            if (copyFrom.hasMergedCard()) {
                out.setMergedCards(copyFrom.getMergedCards());
            }

            out.setLeavesPlayCommands(copyFrom.getLeavesPlayCommands());

            out.setSpecialized(copyFrom.isSpecialized());
            out.addRemembered(copyFrom.getRemembered());
            out.addImprintedCards(copyFrom.getImprintedCards());
            out.setCommander(copyFrom.isRealCommander());
            //out.setFaceDown(copyFrom.isFaceDown());
        }

        int foil = copyFrom.getCurrentState().getFoil();
        if (foil > 0) {
            out.setFoil(foil);
        }

        return out;
    }

    private static void copyState(final Card from, final CardStateName fromState, final Card to,
                                 final CardStateName toState) {
        copyState(from, fromState, to, toState, true);
    }

    private static void copyState(final Card from, final CardStateName fromState, final Card to,
                                 final CardStateName toState, boolean updateView) {
        // copy characteristics not associated with a state
        to.setText(from.getSpellText());

        // get CardCharacteristics for desired state
        if (!to.getStates().contains(toState)) {
            to.addAlternateState(toState, updateView);
        }
        final CardState toCharacteristics = to.getState(toState), fromCharacteristics = from.getState(fromState);
        toCharacteristics.copyFrom(fromCharacteristics, false);
    }

    public static Card copyStats(final Card in, final Player newOwner, boolean assignNewId) {
        int id = in.getId();
        if (assignNewId) {
            id = newOwner == null ? 0 : newOwner.getGame().nextCardId();
        }
        final Card c;
        if(in instanceof DetachedCardEffect)
            c = new DetachedCardEffect((DetachedCardEffect) in, assignNewId);
        else
            c = new Card(id, in.getPaperCard(), in.getGame());

        c.setOwner(newOwner);
        c.setSetCode(in.getSetCode());

        for (final CardStateName state : in.getStates()) {
            copyState(in, state, c, state, false);
        }

        c.setState(in.getCurrentStateName(), false);
        c.setRules(in.getRules());
        if (in.isTransformed()) {
            c.incrementTransformedTimestamp();
        }

        return c;
    }

    @Deprecated
    public void copyCopiableCharacteristics(final Card to, SpellAbility sourceSA, SpellAbility targetSA) {
        final boolean toIsFaceDown = to.isFaceDown();
        if (toIsFaceDown) {
            // If to is face down, copy to its front side
            to.setState(CardStateName.Original, false);
            copyCopiableCharacteristics(to, sourceSA, targetSA);
            to.setState(CardStateName.FaceDown, false);
            return;
        }

        final boolean fromIsFlipCard = copyFrom.isFlipCard();
        final boolean fromIsTransformedCard = copyFrom.getCurrentStateName() == CardStateName.Backside || copyFrom.getCurrentStateName() == CardStateName.Meld;

        if (fromIsFlipCard) {
            if (to.getCurrentStateName().equals(CardStateName.Flipped)) {
                copyState(copyFrom, CardStateName.Original, to, CardStateName.Original);
            } else {
                copyState(copyFrom, CardStateName.Original, to, to.getCurrentStateName());
            }
            copyState(copyFrom, CardStateName.Flipped, to, CardStateName.Flipped);
        } else if (copyFrom.isTransformable()
                && sourceSA != null && ApiType.CopySpellAbility.equals(sourceSA.getApi())
                && targetSA != null && targetSA.isSpell() && targetSA.getHostCard().isPermanent()) {
            copyState(copyFrom, CardStateName.Original, to, CardStateName.Original);
            copyState(copyFrom, CardStateName.Backside, to, CardStateName.Backside);
            // 707.10g If an effect creates a copy of a transforming permanent spell, the copy is also a transforming permanent spell that has both a front face and a back face.
            // The characteristics of its front and back face are determined by the copiable values of the same face of the spell it is a copy of, as modified by any other copy effects.
            // If the spell it is a copy of has its back face up, the copy is created with its back face up. The token that’s put onto the battlefield as that spell resolves is a transforming token.
            to.setBackSide(copyFrom.isBackSide());
            if (copyFrom.isTransformed()) {
                to.incrementTransformedTimestamp();
            }
        } else if (fromIsTransformedCard) {
            copyState(copyFrom, copyFrom.getCurrentStateName(), to, CardStateName.Original);
        } else {
            copyState(copyFrom, copyFrom.getCurrentStateName(), to, to.getCurrentStateName());
        }
    }

    // ========================================================
    // LKI functions

    public static List<Card> getLKICopyList(final Iterable<Card> in, Map<Integer, Card> cachedMap) {
        if (in == null) {
            return null;
        }
        List<Card> result = Lists.newArrayList();
        for (final Card c : in) {
            result.add(new CardCopyService(c).getLKICopy(cachedMap));
        }
        return result;
    }

    public static Card getLKICopy(Card c) {
        // Ideally, we'd just convert all calls to getLKICopy to use the Map version
        return new CardCopyService(c).getLKICopy();
    }

    public static Card getLKICopy(final Card c, Map<Integer, Card> cachedMap) {
        return new CardCopyService(c).getLKICopy(cachedMap);
    }

    public static GameEntity getLKICopy(final GameEntity c, Map<Integer, Card> cachedMap) {
        // Ideally, we'd just convert all calls to getLKICopy to use the Map version
        if (c instanceof Card) {
            return new CardCopyService((Card) c).getLKICopy(cachedMap);
        }
        return c;
    }

    public Card getLKICopy() {
        return getLKICopy(Maps.newHashMap());
    }

    public Card getLKICopy(Map<Integer, Card> cachedMap) {
        if (copyFrom == null) {
            return null;
        }
        Card cachedCard = cachedMap.get(copyFrom.getId());
        if (cachedCard != null) {
            return cachedCard;
        }

        String msg = "CardUtil:getLKICopy copy object";
        Breadcrumb bread = new Breadcrumb(msg);
        bread.setData("Card", copyFrom.getName());
        bread.setData("CardState", copyFrom.getCurrentStateName().toString());
        bread.setData("Player", copyFrom.getController().getName());
        Sentry.addBreadcrumb(bread);

        final Card newCopy;
        if(copyFrom instanceof DetachedCardEffect)
            newCopy = new DetachedCardEffect((DetachedCardEffect) copyFrom, false);
        else
            newCopy = new Card(copyFrom.getId(), copyFrom.getPaperCard(), copyFrom.getGame(), null);
        cachedMap.put(copyFrom.getId(), newCopy);
        newCopy.setSetCode(copyFrom.getSetCode());
        newCopy.setOwner(copyFrom.getOwner());
        newCopy.setController(copyFrom.getController(), 0);
        newCopy.setCommander(copyFrom.isCommander());
        newCopy.setCollectible(copyFrom.isCollectible());

        newCopy.setRules(copyFrom.getRules());

        // needed to ensure that the LKI object has correct CMC info no matter what state the original card was in
        // (e.g. Scrap Trawler + transformed Harvest Hand)
        newCopy.setLKICMC(copyFrom.getCMC());
        // used for the purpose of cards that care about the zone the card was known to be in last
        newCopy.setLastKnownZone(copyFrom.getLastKnownZone());
        // copy EffectSource for description
        newCopy.setEffectSource(getLKICopy(copyFrom.getEffectSource(), cachedMap));

        if (copyFrom.isFlipCard()) {
            newCopy.getState(CardStateName.Original).copyFrom(copyFrom.getState(CardStateName.Original), true);
            newCopy.addAlternateState(CardStateName.Flipped, false);
            newCopy.getState(CardStateName.Flipped).copyFrom(copyFrom.getState(CardStateName.Flipped), true);
        } else if (copyFrom.isTransformable()) {
            newCopy.getState(CardStateName.Original).copyFrom(copyFrom.getState(CardStateName.Original), true);
            newCopy.addAlternateState(CardStateName.Backside, false);
            newCopy.getState(CardStateName.Backside).copyFrom(copyFrom.getState(CardStateName.Backside), true);
        } else if (copyFrom.hasState(CardStateName.Secondary)) {
            newCopy.getState(CardStateName.Original).copyFrom(copyFrom.getState(CardStateName.Original), true);
            newCopy.addAlternateState(CardStateName.Secondary, false);
            newCopy.getState(CardStateName.Secondary).copyFrom(copyFrom.getState(CardStateName.Secondary), true);
        } else if (copyFrom.isSplitCard()) {
            newCopy.getState(CardStateName.Original).copyFrom(copyFrom.getState(CardStateName.Original), true);
            newCopy.addAlternateState(CardStateName.LeftSplit, false);
            newCopy.getState(CardStateName.LeftSplit).copyFrom(copyFrom.getState(CardStateName.LeftSplit), true);
            newCopy.addAlternateState(CardStateName.RightSplit, false);
            newCopy.getState(CardStateName.RightSplit).copyFrom(copyFrom.getState(CardStateName.RightSplit), true);
        } else {
            newCopy.getCurrentState().copyFrom(copyFrom.getState(copyFrom.getFaceupCardStateName()), true);
        }
        newCopy.setFlipped(copyFrom.isFlipped());
        newCopy.setBackSide(copyFrom.isBackSide());
        if (copyFrom.isTransformed()) {
            newCopy.incrementTransformedTimestamp();
        }
        if (newCopy.hasAlternateState()) {
            newCopy.setState(copyFrom.getCurrentStateName(), false, true);
        }
        if (copyFrom.isFaceDown()) {
            newCopy.turnFaceDownNoUpdate();
            newCopy.setType(new CardType(copyFrom.getFaceDownState().getType()));
        }
        // prevent StackDescription from revealing face
        newCopy.updateStateForView();

        /*
        if (in.isCloned()) {
            newCopy.addAlternateState(CardStateName.Cloner, false);
            newCopy.getState(CardStateName.Cloner).copyFrom(in.getState(CardStateName.Cloner), true);
        }
        */

        newCopy.setGamePieceType(copyFrom.getGamePieceType());
        newCopy.setTokenCard(copyFrom.isTokenCard());
        newCopy.setEmblem(copyFrom.isEmblem());

        // lock in the current P/T
        newCopy.setBasePower(copyFrom.getCurrentPower());
        newCopy.setBaseToughness(copyFrom.getCurrentToughness());

        // printed P/T
        newCopy.setBasePowerString(copyFrom.getCurrentState().getBasePowerString());
        newCopy.setBaseToughnessString(copyFrom.getCurrentState().getBaseToughnessString());

        // extra copy PT boost
        newCopy.setPTBoost(copyFrom.getPTBoostTable());

        newCopy.setCounters(Maps.newHashMap(copyFrom.getCounters()));

        newCopy.setColor(copyFrom.getColor());
        newCopy.setPhasedOut(copyFrom.getPhasedOut());
        newCopy.setTapped(copyFrom.isTapped());
        newCopy.setTributed(copyFrom.isTributed());
        newCopy.setUnearthed(copyFrom.isUnearthed());
        newCopy.setMonstrous(copyFrom.isMonstrous());
        newCopy.setRenowned(copyFrom.isRenowned());
        newCopy.setSolved(copyFrom.isSolved());
        newCopy.setPromisedGift(copyFrom.getPromisedGift());
        newCopy.setSaddled(copyFrom.isSaddled());
        if (newCopy.isSaddled()) newCopy.setSaddledByThisTurn(copyFrom.getSaddledByThisTurn());
        if (copyFrom.isSuspected()) {
            newCopy.setSuspectedEffect(getLKICopy(copyFrom.getSuspectedEffect(), cachedMap));
        }

        newCopy.setDamageHistory(copyFrom.getDamageHistory());
        newCopy.setDamageReceivedThisTurn(copyFrom.getDamageReceivedThisTurn());
        newCopy.setExcessDamageReceivedThisTurn(copyFrom.getExcessDamageThisTurn());

        // these are LKI already
        newCopy.getBlockedThisTurn().addAll(copyFrom.getBlockedThisTurn());
        newCopy.getBlockedByThisTurn().addAll(copyFrom.getBlockedByThisTurn());

        newCopy.setAttachedCards(getLKICopyList(copyFrom.getAttachedCards(), cachedMap));
        newCopy.setEntityAttachedTo(getLKICopy(copyFrom.getEntityAttachedTo(), cachedMap));

        newCopy.setCopiedPermanent(copyFrom.getCopiedPermanent());

        newCopy.setHaunting(copyFrom.getHaunting());
        for (final Card haunter : copyFrom.getHauntedBy()) {
            newCopy.addHauntedBy(haunter, false);
        }

        newCopy.setIntensity(copyFrom.getIntensity(false));
        newCopy.setPerpetual(copyFrom);

        newCopy.addRemembered(copyFrom.getRemembered());
        newCopy.addImprintedCards(copyFrom.getImprintedCards());
        newCopy.setChosenCards(copyFrom.getChosenCards());

        newCopy.setChosenType(copyFrom.getChosenType());
        newCopy.setChosenType2(copyFrom.getChosenType2());
        newCopy.setNamedCards(Lists.newArrayList(copyFrom.getNamedCards()));
        newCopy.setChosenColors(Lists.newArrayList(copyFrom.getChosenColors()));
        if (copyFrom.hasChosenNumber()) {
            newCopy.setChosenNumber(copyFrom.getChosenNumber());
        }
        newCopy.setChosenEvenOdd(copyFrom.getChosenEvenOdd());

        newCopy.copyFrom(copyFrom);

        // for getReplacementList (run after setChangedCardKeywords for caching)
        newCopy.setStoredKeywords(copyFrom.getStoredKeywords(), true);
        newCopy.setStoredReplacements(copyFrom.getStoredReplacements());

        newCopy.copyChangedTextFrom(copyFrom);
        newCopy.changedTypeByText = copyFrom.changedTypeByText;
        newCopy.changedCardKeywordsByWord = copyFrom.changedCardKeywordsByWord.copy(newCopy, true);

        newCopy.setGameTimestamp(copyFrom.getGameTimestamp());
        newCopy.setLayerTimestamp(copyFrom.getLayerTimestamp());

        newCopy.setBestowTimestamp(copyFrom.getBestowTimestamp());

        newCopy.setTurnInZone(copyFrom.getTurnInZone());

        newCopy.setForetold(copyFrom.isForetold());
        newCopy.setForetoldCostByEffect(copyFrom.isForetoldCostByEffect());

        newCopy.setPlotted(copyFrom.isPlotted());

        newCopy.setMeldedWith(getLKICopy(copyFrom.getMeldedWith(), cachedMap));

        // update keyword cache on all states
        for (CardStateName s : newCopy.getStates()) {
            newCopy.updateKeywordsCache(newCopy.getState(s));
        }

        if (copyFrom.getCastSA() != null) {
            SpellAbility castSA = copyFrom.getCastSA().copy(newCopy, true);
            castSA.setLastStateBattlefield(CardCollection.EMPTY);
            castSA.setLastStateGraveyard(CardCollection.EMPTY);
            newCopy.setCastSA(castSA);
        }
        newCopy.setCastFrom(copyFrom.getCastFrom());

        newCopy.setExiledBy(copyFrom.getExiledBy());
        newCopy.setExiledWith(getLKICopy(copyFrom.getExiledWith(), cachedMap));
        newCopy.setExiledSA(copyFrom.getExiledSA());
        newCopy.addExiledCards(copyFrom.getExiledCards());

        newCopy.setDiscarded(copyFrom.wasDiscarded());
        newCopy.setMilled(copyFrom.wasMilled());
        newCopy.setSurveilled(copyFrom.wasSurveilled());

        newCopy.getAbilityActivatedThisTurn().putAll(copyFrom.getAbilityActivatedThisTurn());
        newCopy.getAbilityActivatedThisGame().putAll(copyFrom.getAbilityActivatedThisGame());
        newCopy.getAbilityResolvedThisTurn().putAll(copyFrom.getAbilityResolvedThisTurn());

        if (copyFrom.getGame().getCombat() != null && copyFrom.isPermanent()) {
            newCopy.setCombatLKI(copyFrom.getGame().getCombat().saveLKI(newCopy));
        }

        newCopy.getGoadMap().putAll(copyFrom.getGoadMap());

        return newCopy;
    }

}
