package forge.game.ability.effects;

import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.GameEventFlipOntoBattlefield;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FlipOntoBattlefieldEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        // Basic parameters defining the chances
        final float chanceToFlip = 0.85f;
        final int maxFlipTimes = 2;
        final float chanceToHitCorner = 0.06f;    // 3-4 cards: orb lands on the corner where cards meet (needs the mobile board)
        final float chanceToHitTwoCards = 0.20f;  // cumulative: corner + two cards

        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = host.getGame();

        // TODO: allow to make a bounding box of sorts somehow, ideally - upgrade to a full system allowing to actually target by location
        CardCollectionView tgtBox = p.getController().chooseCardsForEffect(game.getCardsIn(ZoneType.Battlefield), sa, Localizer.getInstance().getMessage("lblChooseDesiredLocation"), 1, 1, sa.hasParam("AllowRandom"), null);

        Card tgtLoc = tgtBox.getFirst();

        final int rowSize = CardLists.filter(tgtLoc.getController().getCardsIn(ZoneType.Battlefield), c -> c.sharesCardTypeWith(tgtLoc)).size();
        final float chanceToHit = Math.min(1.0f, 0.45f + 0.07f * rowSize);

        Card lhsNeighbor = getNeighboringCard(tgtLoc, -1);
        Card rhsNeighbor = getNeighboringCard(tgtLoc, 1);

        CardCollection randChoices = new CardCollection();
        randChoices.add(tgtLoc);
        if (lhsNeighbor != null && lhsNeighbor != tgtLoc) randChoices.add(lhsNeighbor);
        if (rhsNeighbor != null && rhsNeighbor != tgtLoc) randChoices.add(rhsNeighbor);

        // TODO: would be fun to add a small chance (e.g. 3-5%) to land unpredictably on some random target?
        boolean flipped = MyRandom.getRandom().nextFloat() <= chanceToFlip;
        int flippedTimes = flipped ? MyRandom.getRandom().nextInt(maxFlipTimes) + 1 : 0;
        sa.setSVar("TimesFlipped", String.valueOf(flippedTimes));

        // how many cards the orb should hit: 0 (miss), 1, 2, or 3-4 for a corner landing
        int wanted = 0;
        if (flipped) {
            float outcome = MyRandom.getRandom().nextFloat();
            if (outcome <= chanceToHitCorner) {
                wanted = 3 + MyRandom.getRandom().nextInt(2);
            } else if (outcome <= chanceToHitTwoCards) {
                wanted = 2;
            } else if (outcome <= chanceToHit) {
                wanted = 1;
            }
        }

        // Fallback pick, used when no board UI knows the real layout (desktop, headless, netplay client).
        // The mobile board replaces this list with the cards the orb really touches.
        CardCollection hit = new CardCollection();
        if (wanted == 1) {
            hit.add(Aggregates.random(randChoices));
        } else if (wanted > 1) {
            hit.add(tgtLoc);
            for (Card c : randChoices) {
                if (hit.size() < wanted && !hit.contains(c)) hit.add(c);
            }
            for (Card c : tgtLoc.getController().getCardsIn(ZoneType.Battlefield)) {
                if (hit.size() < wanted && !hit.contains(c)) hit.add(c);
            }
        }

        final CardView hostView = host.getView();
        final CardView tgtView = tgtLoc.getView();
        // thread-safe: the board UI may rewrite it from the render thread while this thread waits
        final List<CardView> hitViews = new CopyOnWriteArrayList<>();
        for (Card c : hit) { hitViews.add(c.getView()); }

        // first event (animation starts; with the mobile UI this returns once the orb has landed)
        game.fireEvent(new GameEventFlipOntoBattlefield(hostView, tgtView, hitViews, flippedTimes, false));

        // a board that knows the real layout may have replaced the list with the cards the orb touched
        if (flipped) {
            hit = new CardCollection();
            for (CardView v : hitViews) {
                Card c = game.findById(v.getId());
                if (c != null && c.isInPlay()) hit.add(c);
            }
        }

        if (!flipped) {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblDidNotFlipOver"), null);
            game.fireEvent(new GameEventFlipOntoBattlefield(hostView, tgtView, hitViews, 0, true));
            return;
        }
        game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblFlippedOver", flippedTimes), null);
        if (hit.size() >= 3) {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnOneCard", Lang.joinHomogenous(hit)), null);
        } else if (hit.size() == 2) {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnTwoCards", hit.getFirst(), hit.getLast()), null);
        } else if (hit.size() == 1) {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnOneCard", hit.getFirst()), null);
        } else {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblDidNotLandOnCards"), null);
        }
        host.addRemembered(hit);
        game.fireEvent(new GameEventFlipOntoBattlefield(hostView, tgtView, hitViews, flippedTimes, true));
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();

        sb.append("Flip ");
        sb.append(host.toString());
        sb.append(" onto the battlefield from a height of at least one foot.");

        return sb.toString();
    }

    private Card getNeighboringCard(Card c, int direction) {
        // Currently gets the nearest (in zone order) card to the left or to the right of the designated one by type,
        // as well as the current card attachments that are visually located next to the requested card or are assumed to be near it.
        Player controller = c.getController();
        ArrayList<Card> attachments = Lists.newArrayList();
        CardCollection cardsOTB = CardLists.filter(
                controller.getCardsIn(ZoneType.Battlefield), card -> {
                    if (card.isAttachedToEntity(c)) {
                        attachments.add(card);
                        return true;
                    } else if (c.isCreature()) {
                        return card.isCreature();
                    } else if (c.isPlaneswalker() || c.isArtifact() || (c.isEnchantment() && !c.isAura())) {
                        return card.isPlaneswalker() || card.isArtifact() || (c.isEnchantment() && !c.isAura());
                    } else if (c.isLand()) {
                        return card.isLand();
                    } else if (c.isAttachedToEntity()) {
                        return card.isAttachedToEntity(c.getEntityAttachedTo()) || c.equals(card.getAttachedTo());
                    }
                    return card.sharesCardTypeWith(c);
                }
        );

        // Chance to hit an attachment
        float hitAttachment = 0.50f;
        if (!attachments.isEmpty() && direction < 0 && MyRandom.getRandom().nextFloat() <= hitAttachment) {
            return Aggregates.random(attachments);
        }

        int loc = cardsOTB.indexOf(c);
        if (direction < 0) {
            if (loc > 0) return cardsOTB.get(loc - 1);
        } else if (loc < cardsOTB.size() - 1) {
            return cardsOTB.get(loc + 1);
        }
        return c;
    }
}