package forge.game.ability.effects;

import java.util.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;

public class RepeatEachEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(forge.card.spellability.SpellAbility)
     */
    @SuppressWarnings("serial")
    @Override
    public void resolve(SpellAbility sa) {
        // Things to loop over: Cards, Players, or SAs
        final Card source = sa.getHostCard();

        final SpellAbility repeat = sa.getAdditionalAbility("RepeatSubAbility");

        final Player player = sa.getActivatingPlayer();
        final Game game = player.getGame();
        if (sa.hasParam("Optional") && sa.hasParam("OptionPrompt") && //for now, OptionPrompt is needed
                !player.getController().confirmAction(sa, null, sa.getParam("OptionPrompt"), null)) {
            return;
        }

        boolean useImprinted = sa.hasParam("UseImprinted");

        CardCollectionView repeatCards = null;
        List<SpellAbility> repeatSas = null;

        if (sa.hasParam("RepeatCards")) {
            List<ZoneType> zone = Lists.newArrayList();
            if (sa.hasParam("Zone")) {
                zone = ZoneType.listValueOf(sa.getParam("Zone"));
            } else {
                zone.add(ZoneType.Battlefield);
            }
            repeatCards = CardLists.getValidCards(game.getCardsIn(zone),
                    sa.getParam("RepeatCards"), source.getController(), source, sa);
        }
        else if (sa.hasParam(("RepeatSpellAbilities"))) {
            repeatSas = Lists.newArrayList();
            String[] restrictions = sa.getParam("RepeatSpellAbilities").split(",");
            for (SpellAbilityStackInstance stackInstance : game.getStack()) {
                if (stackInstance.getSpellAbility(false).isValid(restrictions, source.getController(), source, sa)) {
                    repeatSas.add(stackInstance.getSpellAbility(false));
                }
            }

        }
        else if (sa.hasParam("DefinedCards")) {
            repeatCards = AbilityUtils.getDefinedCards(source, sa.getParam("DefinedCards"), sa);
        }
        boolean loopOverCards = repeatCards != null && !repeatCards.isEmpty();

        if (sa.hasParam("ClearRemembered")) {
            source.clearRemembered();
        }

        if (sa.hasParam("DamageMap")) {
            sa.setDamageMap(new CardDamageMap());
            sa.setPreventMap(new CardDamageMap());
            sa.setCounterTable(new GameEntityCounterTable());
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.setChangeZoneTable(new CardZoneTable());
        }
        if (sa.hasParam("LoseLifeMap")) {
            sa.setLoseLifeMap(Maps.newHashMap());
        }

        if (loopOverCards) {
            if (sa.hasParam("ChooseOrder") && repeatCards.size() > 1) {
                final Player chooser = sa.getParam("ChooseOrder").equals("True") ? player :
                        AbilityUtils.getDefinedPlayers(source, sa.getParam("ChooseOrder"), sa).get(0);
                repeatCards = chooser.getController().orderMoveToZoneList(repeatCards, ZoneType.None, sa);
            }

            for (Card card : repeatCards) {
                if (useImprinted) {
                    source.addImprintedCard(card);
                } else {
                    source.addRemembered(card);
                }
                AbilityUtils.resolve(repeat);
                if (useImprinted) {
                    source.removeImprintedCard(card);
                } else {
                    source.removeRemembered(card);
                }
            }
        }
        if (repeatSas != null) {
            for (SpellAbility card : repeatSas) {
                source.addRemembered(card);
                AbilityUtils.resolve(repeat);
                source.removeRemembered(card);
            }
        }

        // for a mixed list of target permanents and players, e.g. Soulfire Eruption
        if (sa.hasParam("RepeatTargeted")) {
            for (final GameObject o : getTargets(sa)) {
                source.addRemembered(o);
                AbilityUtils.resolve(repeat);
                source.removeRemembered(o);
            }
        }

        if (sa.hasParam("RepeatTypesFrom")) {
            final Set<String> validTypes = new HashSet<>();
            final String def = sa.getParam("RepeatTypesFrom");
            final List<Card> res;
            if (def.startsWith("ThisTurnCast")) {
                final String[] workingCopy = def.split("_");
                final String validFilter = workingCopy[1];
                res = CardUtil.getThisTurnCast(validFilter, source, sa, player);
            } else if (def.startsWith("Defined ")) {
                res = AbilityUtils.getDefinedCards(source, def.substring(8), sa);
            } else {
                final ZoneType zone = sa.hasParam("TypesFromZone") ?
                        ZoneType.smartValueOf(sa.getParam("TypesFromZone")) : ZoneType.Battlefield;
                res = CardLists.getValidCards(game.getCardsIn(zone), def, source.getController(), source, sa);
            }
            for (final Card c : res) {
                for (CardType.CoreType type : c.getType().getCoreTypes()) {
                    validTypes.add(type.name());
                }
            }

            final String storedType = source.getChosenType();
            Player chooser = player;
            if (sa.hasParam("ChooseOrder") && !sa.getParam("ChooseOrder").equals("True")) {
                chooser = AbilityUtils.getDefinedPlayers(source, sa.getParam("ChooseOrder"), sa).get(0);
            }
            while (!validTypes.isEmpty()) {
                String chosenT = chooser.getController().chooseSomeType("card", sa, validTypes, null);
                source.setChosenType(chosenT);
                AbilityUtils.resolve(repeat);
                validTypes.remove(chosenT);
            }
            source.setChosenType(storedType);
        }

        if (sa.hasParam("RepeatPlayers")) {
            final FCollection<Player> repeatPlayers = AbilityUtils.getDefinedPlayers(source, sa.getParam("RepeatPlayers"), sa);
            if (sa.hasParam("ClearRememberedBeforeLoop")) {
                source.clearRemembered();
            }
            boolean optional = sa.hasParam("RepeatOptionalForEachPlayer");
            boolean nextTurn = sa.hasParam("NextTurnForEachPlayer");
            if (sa.hasParam("StartingWithActivator")) {
                int aidx = repeatPlayers.indexOf(player);
                if (aidx != -1) {
                    Collections.rotate(repeatPlayers, -aidx);
                }
            }
            for (final Player p : repeatPlayers) {
                if (optional) {
                    if (!p.getController().confirmAction(repeat, null, sa.getParam("RepeatOptionalMessage"), null)) {
                        continue;
                    } else if (sa.hasParam("RememberDeciders")) {
                        source.addRemembered(p);
                    }
                }
                if (nextTurn) {
                    game.getCleanup().addUntil(p, new GameCommand() {
                        @Override
                        public void run() {
                            List<Object> tempRemembered = Lists.newArrayList(Iterables.filter(source.getRemembered(), Player.class));
                            source.removeRemembered(tempRemembered);
                            source.addRemembered(p);
                            AbilityUtils.resolve(repeat);
                            source.removeRemembered(p);
                            source.addRemembered(tempRemembered);
                        }
                    });
                } else {
                    // to avoid risk of collision with other abilities swap out other Remembered Player while resolving
                    List<Object> tempRemembered = Lists.newArrayList(Iterables.filter(source.getRemembered(), Player.class));
                    source.removeRemembered(tempRemembered);
                    source.addRemembered(p);
                    AbilityUtils.resolve(repeat);
                    source.removeRemembered(p);
                    source.addRemembered(tempRemembered);
                }
            }
        }

        if (sa.hasParam("DamageMap")) {
            game.getAction().dealDamage(false, sa.getDamageMap(), sa.getPreventMap(), sa.getCounterTable(), sa);
        }
        if (sa.hasParam("ChangeZoneTable")) {
            sa.getChangeZoneTable().triggerChangesZoneAll(game, sa);
            sa.setChangeZoneTable(null);
        }
        if (sa.hasParam("LoseLifeMap")) {
            Map<Player, Integer> lossMap = sa.getLoseLifeMap();
            if (!lossMap.isEmpty()) {
                final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromPIMap(lossMap);
                game.getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runParams2, false);
            }
            sa.setLoseLifeMap(null);
        }
    }
}
