package forge.game.ability.effects;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardZoneTable;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.MyRandom;

public class DigUntilEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
         * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
         */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        String desc = sa.getParamOrDefault("ValidDescription", "Card");

        int untilAmount = 1;
        if (sa.hasParam("Amount")) {
            untilAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa);
        }

        for (final Player pl : getTargetPlayers(sa)) {
            sb.append(pl).append(" ");
        }

        final ZoneType revealed = ZoneType.smartValueOf(sa.getParam("RevealedDestination"));
        sb.append(ZoneType.Exile.equals(revealed) ? "exiles cards from their library until they exile " :
                "reveals cards from their library until revealing ");
        sb.append(Lang.nounWithNumeralExceptOne(untilAmount, desc + " card"));
        if (untilAmount != 1) {
            sb.append("s");
        }
        if (sa.hasParam("MaxRevealed")) {
            untilAmount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("MaxRevealed"), sa);
            sb.append(" or ").append(untilAmount).append(" card/s");
        }
        sb.append(".");

        if (!sa.hasParam("NoPutDesc")) {
            sb.append(" Put ");

            final ZoneType found = ZoneType.smartValueOf(sa.getParam("FoundDestination"));
            if (found != null) {
                sb.append(untilAmount > 1 ? "those cards" : "that card");
                sb.append(" ");

                if (ZoneType.Hand.equals(found)) {
                    sb.append("into their hand ");
                }

                if (ZoneType.Graveyard.equals(revealed)) {
                    sb.append("and all other cards into their graveyard.");
                }
                if (ZoneType.Exile.equals(revealed)) {
                    sb.append("and exile all other cards revealed this way.");
                }
            } else if (revealed != null) {
                if (ZoneType.Hand.equals(revealed)) {
                    sb.append("all cards revealed this way into their hand");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();

        String[] type = new String[]{"Card"};
        if (sa.hasParam("Valid")) {
            type = sa.getParam("Valid").split(",");
        }

        int untilAmount = 1;
        if (sa.hasParam("Amount")) {
            untilAmount = AbilityUtils.calculateAmount(host, sa.getParam("Amount"), sa);
            if (untilAmount == 0) return;
        }

        Integer maxRevealed = null;
        if (sa.hasParam("MaxRevealed")) {
            maxRevealed = AbilityUtils.calculateAmount(host, sa.getParam("MaxRevealed"), sa);
        }

        final boolean remember = sa.hasParam("RememberFound");
        final boolean imprint = sa.hasParam("ImprintFound");

        final ZoneType foundDest = ZoneType.smartValueOf(sa.getParam("FoundDestination"));
        final int foundLibPos = AbilityUtils.calculateAmount(host, sa.getParam("FoundLibraryPosition"), sa);
        final ZoneType revealedDest = ZoneType.smartValueOf(sa.getParam("RevealedDestination"));
        final int revealedLibPos = AbilityUtils.calculateAmount(host, sa.getParam("RevealedLibraryPosition"), sa);
        final ZoneType noneFoundDest = ZoneType.smartValueOf(sa.getParam("NoneFoundDestination"));
        final int noneFoundLibPos = AbilityUtils.calculateAmount(host, sa.getParam("NoneFoundLibraryPosition"), sa);
        final ZoneType digSite = sa.hasParam("DigZone") ? ZoneType.smartValueOf(sa.getParam("DigZone")) : ZoneType.Library;
        boolean shuffle = sa.hasParam("Shuffle");
        final boolean optional = sa.hasParam("Optional");
        final boolean optionalFound = sa.hasParam("OptionalFoundMove");

        CardZoneTable table = new CardZoneTable();
        boolean combatChanged = false;
        CardCollectionView lastStateBattlefield = game.copyLastStateBattlefield();
        CardCollectionView lastStateGraveyard = game.copyLastStateGraveyard();

        for (final Player p : getTargetPlayers(sa)) {
            if (p == null || !p.isInGame()) {
                continue;
            }
            if (optional && !p.getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantDigYourLibrary"), null)) {
                continue;
            }
            CardCollection found = new CardCollection();
            CardCollection revealed = new CardCollection();

            final PlayerZone library = p.getZone(digSite);

            final int maxToDig = maxRevealed != null ? maxRevealed : library.size();

            for (int i = 0; i < maxToDig; i++) {
                final Card c = library.get(i);
                revealed.add(c);
                if (c.isValid(type, sa.getActivatingPlayer(), host, sa)) {
                    found.add(c);
                    if (sa.hasParam("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    if (remember) {
                        host.addRemembered(c);
                    }
                    if (imprint) {
                        host.addImprintedCard(c);
                    }
                    if (found.size() == untilAmount) {
                        break;
                    }
                }
            }

            if (shuffle && sa.hasParam("ShuffleCondition")) {
                if (sa.getParam("ShuffleCondition").equals("NoneFound")) {
                    shuffle = found.isEmpty();
                }
            }

            if (revealed.size() > 0) {
                game.getAction().reveal(revealed, p, false);
            }

            if (foundDest != null) {
                // Allow ordering of found cards
                if (foundDest.isKnown() && found.size() >= 2 && !foundDest.equals(ZoneType.Exile)) {
                    found = (CardCollection)p.getController().orderMoveToZoneList(found, foundDest, sa);
                }

                final Iterator<Card> itr = found.iterator();
                while (itr.hasNext()) {
                    final Card c = itr.next();
                    final ZoneType origin = c.getZone().getZoneType();
                    if (optionalFound && !p.getController().confirmAction(sa, null,
                            Localizer.getInstance().getMessage("lblDoYouWantPutCardToZone", foundDest.getTranslatedName()), null)) {
                        continue;
                    }
                    Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
                    moveParams.put(AbilityKey.LastStateBattlefield, lastStateBattlefield);
                    moveParams.put(AbilityKey.LastStateGraveyard, lastStateGraveyard);
                    Card m = null;
                    if (foundDest.equals(ZoneType.Battlefield)) {
                        if (sa.hasParam("GainControl")) {
                            c.setController(sa.getActivatingPlayer(), game.getNextTimestamp());
                        }
                        if (sa.hasParam("Tapped")) {
                            c.setTapped(true);
                        }
                        m = game.getAction().moveTo(c.getController().getZone(foundDest), c, sa, moveParams);
                        if (addToCombat(c, c.getController(), sa, "Attacking", "Blocking")) {
                            combatChanged = true;
                        }
                    } else if (sa.hasParam("NoMoveFound") && foundDest.equals(ZoneType.Library)) {
                        //Don't do anything
                    } else {
                        m = game.getAction().moveTo(foundDest, c, foundLibPos, sa, moveParams);
                    }
                    revealed.remove(c);
                    if (m != null && !origin.equals(m.getZone().getZoneType())) {
                        table.put(origin, m.getZone().getZoneType(), m);
                    }
                }
            }

            if (sa.hasParam("RememberRevealed")) {
                host.addRemembered(revealed);
            }
            if (sa.hasParam("ImprintRevealed")) {
                host.addImprintedCards(revealed);
            }
            if (sa.hasParam("RevealRandomOrder")) {
                Collections.shuffle(revealed, MyRandom.getRandom());
            }

            if (sa.hasParam("NoMoveRevealed")) {
                //don't do anything
            } else if (sa.hasParam("NoneFoundDestination") && found.size() < untilAmount) {
                // Allow ordering the revealed cards
                if (noneFoundDest.isKnown() && revealed.size() >= 2) {
                    revealed = (CardCollection)p.getController().orderMoveToZoneList(revealed, noneFoundDest, sa);
                }
                if (noneFoundDest == ZoneType.Library && !shuffle
                        && !sa.hasParam("RevealRandomOrder") && revealed.size() >= 2) {
                    revealed = (CardCollection)p.getController().orderMoveToZoneList(revealed, noneFoundDest, sa);
                }

                final Iterator<Card> itr = revealed.iterator();
                while (itr.hasNext()) {
                    final Card c = itr.next();
                    final ZoneType origin = c.getZone().getZoneType();
                    final Card m = game.getAction().moveTo(noneFoundDest, c, noneFoundLibPos, sa);
                    if (m != null && !origin.equals(m.getZone().getZoneType())) {
                        table.put(origin, m.getZone().getZoneType(), m);
                    }
                }
            } else {
                // Allow ordering the rest of the revealed cards
                if (revealedDest.isKnown() && revealed.size() >= 2 && !sa.hasParam("SkipReorder")) {
                    revealed = (CardCollection)p.getController().orderMoveToZoneList(revealed, revealedDest, sa);
                }
                if (revealedDest == ZoneType.Library && !shuffle
                        && !sa.hasParam("RevealRandomOrder") && revealed.size() >= 2) {
                    revealed = (CardCollection)p.getController().orderMoveToZoneList(revealed, revealedDest, sa);
                }

                final Iterator<Card> itr = revealed.iterator();
                while (itr.hasNext()) {
                    final Card c = itr.next();
                    final ZoneType origin = c.getZone().getZoneType();
                    final Card m = game.getAction().moveTo(revealedDest, c, revealedLibPos, sa);
                    if (m != null && !origin.equals(m.getZone().getZoneType())) {
                        table.put(origin, m.getZone().getZoneType(), m);
                    }
                }
            }

            if (shuffle) {
                p.shuffle(sa);
            }
        } // end foreach player
        if (combatChanged) {
            game.updateCombatForView();
            game.fireEvent(new GameEventCombatChanged());
        }
        table.triggerChangesZoneAll(game, sa);
    }

}
