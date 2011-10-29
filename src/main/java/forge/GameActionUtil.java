package forge;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import forge.Constant.Zone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.SpellAbility;
import forge.game.GameLossReason;
import forge.gui.GuiUtils;
import forge.gui.input.Input_PayManaCostUtil;
import forge.gui.input.Input_PayManaCost_Ability;

/**
 * <p>
 * GameActionUtil class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GameActionUtil {

    private GameActionUtil() {
        throw new AssertionError();
    }

    /**
     * <p>
     * executePlayCardEffects.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static void executePlayCardEffects(final SpellAbility sa) {
        // experimental:
        // this method check for cards that have triggered abilities whenever a
        // card gets played
        // (called in MagicStack.java)
        Card c = sa.getSourceCard();

        playCardCascade(c);
        playCardRipple(c);
    }

    /**
     * <p>
     * playCard_Cascade.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void playCardCascade(final Card c) {
        Command cascade = new Command() {
            private static final long serialVersionUID = -845154812215847505L;

            public void execute() {

                if (!c.isCopiedSpell()) {
                    CardList humanNexus = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield, "Maelstrom Nexus");
                    CardList computerNexus = AllZone.getComputerPlayer()
                            .getCardsIn(Zone.Battlefield, "Maelstrom Nexus");

                    CardList maelstromNexii = new CardList();
                    maelstromNexii.addAll(humanNexus);
                    maelstromNexii.addAll(computerNexus);

                    for (Card nexus : maelstromNexii) {
                        if (CardUtil.getThisTurnCast("Card.YouCtrl", nexus).size() == 1) {
                            doCascade(c);
                        }
                    }
                }
                if (c.hasKeyword("Cascade") || c.getName().equals("Bituminous Blast")) // keyword
                                                                                       // gets
                                                                                       // cleared
                                                                                       // for
                                                                                       // Bitumonous
                                                                                       // Blast
                {
                    doCascade(c);
                }
            } // execute()

            void doCascade(final Card c) {
                final Player controller = c.getController();
                final Card cascCard = c;

                final Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        CardList topOfLibrary = controller.getCardsIn(Zone.Library);
                        CardList revealed = new CardList();

                        if (topOfLibrary.size() == 0) {
                            return;
                        }

                        Card cascadedCard = null;
                        Card crd;
                        int count = 0;
                        while (cascadedCard == null) {
                            crd = topOfLibrary.get(count++);
                            revealed.add(crd);
                            if ((!crd.isLand() && CardUtil.getConvertedManaCost(crd.getManaCost()) < CardUtil
                                    .getConvertedManaCost(cascCard.getManaCost()))) {
                                cascadedCard = crd;
                            }

                            if (count == topOfLibrary.size()) {
                                break;
                            }

                        } // while
                        GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());

                        if (cascadedCard != null && !cascadedCard.isUnCastable()) {

                            if (cascadedCard.getController().isHuman()) {
                                StringBuilder title = new StringBuilder();
                                title.append(cascCard.getName()).append(" - Cascade Ability");
                                StringBuilder question = new StringBuilder();
                                question.append("Cast ").append(cascadedCard.getName());
                                question.append(" without paying its mana cost?");

                                int answer = JOptionPane.showConfirmDialog(null, question.toString(), title.toString(),
                                        JOptionPane.YES_NO_OPTION);

                                if (answer == JOptionPane.YES_OPTION) {
                                    AllZone.getGameAction().playCardNoCost(cascadedCard);
                                    revealed.remove(cascadedCard);
                                }
                            } else {
                                ArrayList<SpellAbility> choices = cascadedCard.getBasicSpells();

                                for (SpellAbility sa : choices) {
                                    if (sa.canPlayAI()) {
                                        ComputerUtil.playStackFree(sa);
                                        revealed.remove(cascadedCard);
                                        break;
                                    }
                                }
                            }
                        }
                        revealed.shuffle();
                        for (Card bottom : revealed) {
                            AllZone.getGameAction().moveToBottomOfLibrary(bottom);
                        }
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append(c).append(" - Cascade.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        };
        cascade.execute();
    }

    /**
     * <p>
     * playCard_Ripple.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void playCardRipple(final Card c) {
        Command ripple = new Command() {
            private static final long serialVersionUID = -845154812215847505L;

            public void execute() {

                CardList humanThrummingStone = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield, "Thrumming Stone");
                CardList computerThrummingStone = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield,
                        "Thrumming Stone");

                for (int i = 0; i < humanThrummingStone.size(); i++) {
                    if (c.getController().isHuman()) {
                        c.addExtrinsicKeyword("Ripple:4");
                    }
                }
                for (int i = 0; i < computerThrummingStone.size(); i++) {
                    if (c.getController().isComputer()) {
                        c.addExtrinsicKeyword("Ripple:4");
                    }
                }
                ArrayList<String> a = c.getKeyword();
                for (int x = 0; x < a.size(); x++) {
                    if (a.get(x).toString().startsWith("Ripple")) {
                        String parse = c.getKeyword().get(x).toString();
                        String[] k = parse.split(":");
                        doRipple(c, Integer.valueOf(k[1]));
                    }
                }
            } // execute()

            void doRipple(final Card c, final int rippleCount) {
                final Player controller = c.getController();
                final Card rippleCard = c;
                boolean activateRipple = false;
                if (controller.isHuman()) {
                    Object[] possibleValues = { "Yes", "No" };
                    AllZone.getDisplay().showMessage("Activate Ripple? ");
                    Object q = JOptionPane.showOptionDialog(null, "Activate Ripple for " + c, "Ripple",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, possibleValues,
                            possibleValues[0]);
                    if (q.equals(0)) {
                        activateRipple = true;
                    }
                } else {
                    activateRipple = true;
                }
                if (activateRipple) {
                    final Ability ability = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            CardList topOfLibrary = controller.getCardsIn(Zone.Library);
                            CardList revealed = new CardList();
                            int rippleNumber = rippleCount;
                            if (topOfLibrary.size() == 0) {
                                return;
                            }

                            // Shouldn't Have more than Ripple 10, seeing as no
                            // cards exist with a ripple greater than 4
                            int rippleMax = 10;
                            Card[] rippledCards = new Card[rippleMax];
                            Card crd;
                            if (topOfLibrary.size() < rippleNumber) {
                                rippleNumber = topOfLibrary.size();
                            }

                            for (int i = 0; i < rippleNumber; i++) {
                                crd = topOfLibrary.get(i);
                                revealed.add(crd);
                                if (crd.getName().equals(rippleCard.getName())) {
                                    rippledCards[i] = crd;
                                }
                            } // for
                            GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());
                            for (int i = 0; i < rippleMax; i++) {
                                if (rippledCards[i] != null && !rippledCards[i].isUnCastable()) {

                                    if (rippledCards[i].getController().isHuman()) {
                                        Object[] possibleValues = { "Yes", "No" };
                                        Object q = JOptionPane.showOptionDialog(null,
                                                "Cast " + rippledCards[i].getName() + "?", "Ripple",
                                                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                                                possibleValues, possibleValues[0]);
                                        if (q.equals(0)) {
                                            AllZone.getGameAction().playCardNoCost(rippledCards[i]);
                                            revealed.remove(rippledCards[i]);
                                        }
                                    } else {
                                        ArrayList<SpellAbility> choices = rippledCards[i].getBasicSpells();

                                        for (SpellAbility sa : choices) {
                                            if (sa.canPlayAI() && !sa.getSourceCard().isType("Legendary")) {
                                                ComputerUtil.playStackFree(sa);
                                                revealed.remove(rippledCards[i]);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            revealed.shuffle();
                            for (Card bottom : revealed) {
                                AllZone.getGameAction().moveToBottomOfLibrary(bottom);
                            }
                        }
                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append(c).append(" - Ripple.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            }
        };
        ripple.execute();
    } // playCard_Ripple()

    /**
     * <p>
     * payManaDuringAbilityResolve.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param manaCost
     *            a {@link java.lang.String} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param unpaid
     *            a {@link forge.Command} object.
     */
    public static void payManaDuringAbilityResolve(final String message, final String manaCost, final Command paid,
            final Command unpaid) {
        // temporarily disable the Resolve flag, so the user can payMana for the
        // resolving Ability
        boolean bResolving = AllZone.getStack().getResolving();
        AllZone.getStack().setResolving(false);
        AllZone.getInputControl().setInput(new Input_PayManaCost_Ability(message, manaCost, paid, unpaid));
        AllZone.getStack().setResolving(bResolving);
    }

    // START ENDOFTURN CARDS

    /**
     * <p>
     * endOfTurn_Wall_Of_Reverence.
     * </p>
     */
    public static void endOfTurnWallOfReverence() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = player.getCardsIn(Zone.Battlefield, "Wall of Reverence");

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                public void resolve() {
                    CardList creats = AllZoneUtil.getCreaturesInPlay(player);
                    creats = creats.getTargetableCards(card);
                    if (creats.size() == 0) {
                        return;
                    }

                    if (player.isHuman()) {
                        Object o = GuiUtils.getChoiceOptional("Select target creature for Wall of Reverence life gain",
                                creats.toArray());
                        if (o != null) {
                            Card c = (Card) o;
                            int power = c.getNetAttack();
                            player.gainLife(power, card);
                        }
                    } else { // computer
                        CardListUtil.sortAttack(creats);
                        Card c = creats.get(0);
                        if (c != null) {
                            int power = c.getNetAttack();
                            player.gainLife(power, card);
                        }
                    }
                } // resolve
            }; // ability

            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - ").append(player).append(" gains life equal to target creature's power.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    } // endOfTurn_Wall_Of_Reverence()

    /**
     * <p>
     * endOfTurn_Lighthouse_Chronologist.
     * </p>
     */
    public static void endOfTurnLighthouseChronologist() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final Player opponent = player.getOpponent();
        CardList list = opponent.getCardsIn(Zone.Battlefield);

        list = list.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.getName().equals("Lighthouse Chronologist") && c.getCounters(Counters.LEVEL) >= 7;
            }
        });

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                public void resolve() {
                    AllZone.getPhase().addExtraTurn(card.getController());
                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - ").append(card.getController()).append(" takes an extra turn.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }

    // END ENDOFTURN CARDS

    /**
     * <p>
     * removeAttackedBlockedThisTurn.
     * </p>
     */
    public static void removeAttackedBlockedThisTurn() {
        // resets the status of attacked/blocked this turn
        Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = AllZoneUtil.getCreaturesInPlay(player);

        for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (c.getCreatureAttackedThisCombat()) {
                c.setCreatureAttackedThisCombat(false);
            }
            if (c.getCreatureBlockedThisCombat()) {
                c.setCreatureBlockedThisCombat(false);
                // do not reset setCreatureAttackedThisTurn(), this appears to
                // be combat specific
            }

            if (c.getCreatureGotBlockedThisCombat()) {
                c.setCreatureGotBlockedThisCombat(false);
            }
        }
    }

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean showYesNoDialog(final Card c, final String question) {
        return showYesNoDialog(c, question, false);
    }

    /**
     * <p>
     * showYesNoDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param question
     *            a {@link java.lang.String} object.
     * @param defaultNo
     *            true if the default option should be "No", false otherwise
     * @return a boolean.
     */
    public static boolean showYesNoDialog(final Card c, String question, final boolean defaultNo) {
        AllZone.getDisplay().setCard(c);
        StringBuilder title = new StringBuilder();
        title.append(c.getName()).append(" - Ability");

        if (!(question.length() > 0)) {
            question = "Activate card's ability?";
        }

        int answer;
        if (defaultNo) {
            Object[] options = { "Yes", "No" };
            answer = JOptionPane.showOptionDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        } else {
            answer = JOptionPane.showConfirmDialog(null, question, title.toString(), JOptionPane.YES_NO_OPTION);
        }

        if (answer == JOptionPane.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * <p>
     * showInfoDialg.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     */
    public static void showInfoDialg(final String message) {
        JOptionPane.showMessageDialog(null, message);
    }

    /**
     * <p>
     * flipACoin.
     * </p>
     * 
     * @param caller
     *            a {@link forge.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public static boolean flipACoin(final Player caller, final Card source) {
        String choice = "";
        String[] choices = { "heads", "tails" };

        boolean flip = MyRandom.random.nextBoolean();
        if (caller.isHuman()) {
            choice = (String) GuiUtils.getChoice(source.getName() + " - Call coin flip", choices);
        } else {
            choice = choices[MyRandom.random.nextInt(2)];
        }

        boolean winFlip = flip == choice.equals("heads");
        String winMsg = winFlip ? " wins flip." : " loses flip.";

        JOptionPane.showMessageDialog(null, source.getName() + " - " + caller + winMsg, source.getName(),
                JOptionPane.PLAIN_MESSAGE);
        return winFlip;
    }

    /**
     * <p>
     * executeLandfallEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void executeLandfallEffects(final Card c) {
        if (c.getName().equals("Lotus Cobra")) {
            landfallLotusCobra(c);
        }
    }

    /**
     * <p>
     * showLandfallDialog.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    private static boolean showLandfallDialog(final Card c) {
        AllZone.getDisplay().setCard(c);
        String[] choices = { "Yes", "No" };

        Object q = null;

        q = GuiUtils.getChoiceOptional("Use " + c.getName() + " Landfall?", choices);

        if (q == null || q.equals("No")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * <p>
     * landfall_Lotus_Cobra.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void landfallLotusCobra(final Card c) {
        Ability ability = new Ability(c, "0") {
            @Override
            public void resolve() {
                String color = "";

                Object o = GuiUtils.getChoice("Choose mana color", Constant.Color.ONLY_COLORS);
                color = Input_PayManaCostUtil.getShortColorString((String) o);

                Ability_Mana abMana = new Ability_Mana(c, "0", color) {
                    private static final long serialVersionUID = -2182129023960978132L;
                };
                abMana.produceMana();
            }
        };

        StringBuilder sb = new StringBuilder();
        sb.append(c.getName()).append(" - add one mana of any color to your mana pool.");
        ability.setStackDescription(sb.toString());

        if (c.getController().isHuman()) {
            if (showLandfallDialog(c)) {
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
        } else {
            // TODO once AI has a mana pool he should choose add Ability and
            // choose a mana as appropriate
        }
    }

    // not restricted to combat damage, not restricted to dealing damage to
    // creatures/players
    /**
     * <p>
     * executeDamageDealingEffects.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageDealingEffects(final Card source, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(damage, source);
        }

    }

    // restricted to combat damage and dealing damage to creatures
    /**
     * <p>
     * executeCombatDamageToCreatureEffects.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param affected
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeCombatDamageToCreatureEffects(final Card source, final Card affected, final int damage) {

        if (damage <= 0) {
            return;
        }

        // placeholder for any future needs (everything that was here is
        // converted to script)
    }

    // not restricted to combat damage, restricted to dealing damage to
    // creatures
    /**
     * <p>
     * executeDamageToCreatureEffects.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param affected
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageToCreatureEffects(final Card source, final Card affected, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (affected.hasKeyword("Whenever CARDNAME is dealt damage, put a +1/+1 counter on it.")) {
            Ability ability2 = new Ability(affected, "0") {
                @Override
                public void resolve() {
                    affected.addCounter(Counters.P1P1, 1);
                }
            }; // ability2

            StringBuilder sb2 = new StringBuilder();
            sb2.append(affected.getName()).append(" - gets a +1/+1 counter");
            ability2.setStackDescription(sb2.toString());
            int amount = affected.getAmountOfKeyword("Whenever CARDNAME is dealt damage, put a +1/+1 counter on it.");

            for (int i = 0; i < amount; i++) {
                AllZone.getStack().addSimultaneousStackEntry(ability2);
            }

        }

        if (affected.hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    AllZone.getGameAction().destroy(affected);
                }
            };

            final Ability ability2 = new Ability(source, "0") {
                @Override
                public void resolve() {
                    AllZone.getGameAction().destroyNoRegeneration(affected);
                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(affected).append(" - destroy");
            ability.setStackDescription(sb.toString());
            ability2.setStackDescription(sb.toString());

            if (affected.hasKeyword("When CARDNAME is dealt damage, destroy it. It can't be regenerated.")) {
                int amount = affected
                        .getAmountOfKeyword("When CARDNAME is dealt damage, destroy it. It can't be regenerated.");

                for (int i = 0; i < amount; i++) {
                    AllZone.getStack().addSimultaneousStackEntry(ability2);
                }

            }
            int amount = affected.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it.");

            for (int i = 1; i < amount; i++) {
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
            AllZone.getStack().addSimultaneousStackEntry(ability);
        }

        if (source.hasKeyword("Deathtouch") && affected.isCreature()) {
            AllZone.getGameAction().destroy(affected);
        }
    }

    // this is for cards like Sengir Vampire
    /**
     * <p>
     * executeVampiricEffects.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public static void executeVampiricEffects(final Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (AllZoneUtil.isCardInPlay(c)
                    && a.get(i)
                            .toString()
                            .startsWith(
                                    "Whenever a creature dealt damage by CARDNAME "
                            + "this turn is put into a graveyard, put")) {
                final Card thisCard = c;
                final String kw = a.get(i).toString();
                Ability ability2 = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        Counters counter = Counters.P1P1;
                        if (kw.contains("+2/+2")) {
                            counter = Counters.P2P2;
                        }
                        if (AllZoneUtil.isCardInPlay(thisCard)) {
                            thisCard.addCounter(counter, 1);
                        }
                    }
                }; // ability2

                StringBuilder sb = new StringBuilder();
                sb.append(c.getName());
                if (kw.contains("+2/+2")) {
                    sb.append(" - gets a +2/+2 counter");
                } else {
                    sb.append(" - gets a +1/+1 counter");
                }
                ability2.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability2);

            }
        }
    }

    // not restricted to just combat damage, restricted to players
    /**
     * <p>
     * executeDamageToPlayerEffects.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeDamageToPlayerEffects(final Player player, final Card c, final int damage) {
        if (damage <= 0) {
            return;
        }

        CardList playerPerms = player.getCardsIn(Zone.Battlefield);

        if (AllZoneUtil.isCardInPlay("Lich", player)) {
            CardList lichs = playerPerms.getName("Lich");
            for (Card crd : lichs) {
                final Card lich = crd;
                SpellAbility ability = new Ability(lich, "0") {
                    public void resolve() {
                        for (int i = 0; i < damage; i++) {
                            CardList nonTokens = player.getCardsIn(Zone.Battlefield);
                            nonTokens = nonTokens.filter(CardListFilter.NON_TOKEN);
                            if (nonTokens.size() == 0) {
                                player.loseConditionMet(GameLossReason.SpellEffect, lich.getName());
                            } else {
                                player.sacrificePermanent("Select a permanent to sacrifice", nonTokens);
                            }
                        }
                    }
                };

                StringBuilder sb = new StringBuilder();
                sb.append(lich.getName()).append(" - ").append(lich.getController());
                sb.append(" sacrifices ").append(damage).append(" nontoken Permanents.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }

        if (c.getName().equals("Whirling Dervish") || c.getName().equals("Dunerider Outlaw")) {
            playerCombatDamageWhirlingDervish(c);
        }

        if (player.isPlayer(AllZone.getHumanPlayer())) {
            c.setDealtDmgToHumanThisTurn(true);
        }
        if (player.isPlayer(AllZone.getComputerPlayer())) {
            c.setDealtDmgToComputerThisTurn(true);
        }
    }

    // restricted to combat damage, restricted to players
    /**
     * <p>
     * executeCombatDamageToPlayerEffects.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public static void executeCombatDamageToPlayerEffects(final Player player, final Card c, final int damage) {

        if (damage <= 0) {
            return;
        }

        if (c.isCreature() && AllZoneUtil.isCardInPlay("Contested War Zone", player)) {
            CardList zones = player.getCardsIn(Zone.Battlefield, "Contested War Zone");
            for (final Card zone : zones) {
                Ability ability = new Ability(zone, "0") {
                    @Override
                    public void resolve() {
                        if (AllZoneUtil.isCardInPlay(zone)) {
                            zone.addController(c.getController());
                            // AllZone.getGameAction().changeController(new
                            // CardList(zone), zone.getController(),
                            // c.getController());
                        }
                    }
                };
                ability.setStackDescription(zone + " - " + c.getController() + " gains control of " + zone);

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }

        if (c.hasStartOfKeyword("Poisonous")) {
            int keywordPosition = c.getKeywordPosition("Poisonous");
            String parse = c.getKeyword().get(keywordPosition).toString();
            String[] k = parse.split(" ");
            final int poison = Integer.parseInt(k[1]);
            final Card crd = c;

            Ability ability = new Ability(c, "0") {
                public void resolve() {
                    final Player player = crd.getController();
                    final Player opponent = player.getOpponent();

                    if (opponent.isHuman()) {
                        AllZone.getHumanPlayer().addPoisonCounters(poison);
                    } else {
                        AllZone.getComputerPlayer().addPoisonCounters(poison);
                    }
                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(c);
            sb.append(" - Poisonous: ");
            sb.append(c.getController().getOpponent());
            sb.append(" gets ");
            sb.append(poison);
            sb.append(" poison counters.");

            ability.setStackDescription(sb.toString());
            ArrayList<String> keywords = c.getKeyword();

            for (int i = 0; i < keywords.size(); i++) {
                if (keywords.get(i).startsWith("Poisonous")) {
                    AllZone.getStack().addSimultaneousStackEntry(ability);
                }

            }
        }

        if (CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike") > 0 && c.getNetAttack() > 0) {
            for (int k = 0; k < CardFactoryUtil.hasNumberEquipments(c, "Quietus Spike"); k++) {
                playerCombatDamageLoseHalfLifeUp(c);
            }
        }

        if (c.getName().equals("Scalpelexis")) {
            playerCombatDamageScalpelexis(c);
        } else if (c.getName().equals("Spawnwrithe")) {
            playerCombatDamageSpawnwrithe(c);
        } else if (c.getName().equals("Treva, the Renewer")) {
            playerCombatDamageTreva(c);
        } else if (c.isEnchantedBy("Celestial Mantle")) {
            executeCelestialMantle(c);
        }

    } // executeCombatDamageToPlayerEffects

    /**
     * <p>
     * execute_Celestial_Mantle.
     * </p>
     * 
     * @param enchanted
     *            a {@link forge.Card} object.
     */
    private static void executeCelestialMantle(final Card enchanted) {
        ArrayList<Card> auras = enchanted.getEnchantedBy();
        for (final Card aura : auras) {
            if (aura.getName().equals("Celestial Mantle")) {
                Ability doubleLife = new Ability(aura, "0") {
                    public void resolve() {
                        int life = enchanted.getController().getLife();
                        enchanted.getController().setLife(life * 2, aura);
                    }
                };
                doubleLife.setStackDescription(aura.getName() + " - " + enchanted.getController()
                        + " doubles his or her life total.");

                AllZone.getStack().addSimultaneousStackEntry(doubleLife);

            }
        }
    }

    /**
     * <p>
     * playerCombatDamage_Treva.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageTreva(final Card c) {
        SpellAbility[] sa = c.getSpellAbility();
        if (c.getController().isHuman()) {
            AllZone.getGameAction().playSpellAbility(sa[1]);
        } else {
            ComputerUtil.playNoStack(sa[1]);
        }

    }

    /**
     * <p>
     * playerCombatDamage_Whirling_Dervish.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageWhirlingDervish(final Card c) {
        final int power = c.getNetAttack();
        final Card card = c;

        if (power > 0) {
            final Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.P1P1, 1);
                }
            }; // ability2

            StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - gets a +1/+1 counter.");
            ability2.setStackDescription(sb.toString());

            Command dealtDmg = new Command() {
                private static final long serialVersionUID = 2200679209414069339L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability2);

                }
            };
            AllZone.getEndOfTurn().addAt(dealtDmg);

        } // if
    }

    /**
     * <p>
     * playerCombatDamage_lose_halflife_up.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageLoseHalfLifeUp(final Card c) {
        final Player player = c.getController();
        final Player opponent = player.getOpponent();
        final Card fCard = c;
        if (c.getNetAttack() > 0) {
            Ability ability2 = new Ability(c, "0") {
                @Override
                public void resolve() {
                    int x = 0;
                    int y = 0;
                    if (player.isHuman()) {
                        y = (AllZone.getComputerPlayer().getLife() % 2);
                        if (!(y == 0)) {
                            y = 1;
                        } else {
                            y = 0;
                        }

                        x = (AllZone.getComputerPlayer().getLife() / 2) + y;
                    } else {
                        y = (AllZone.getHumanPlayer().getLife() % 2);
                        if (!(y == 0)) {
                            y = 1;
                        } else {
                            y = 0;
                        }

                        x = (AllZone.getHumanPlayer().getLife() / 2) + y;
                    }
                    opponent.loseLife(x, fCard);

                }
            }; // ability2

            StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - ").append(opponent);
            sb.append(" loses half his or her life, rounded up.");
            ability2.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability2);

        }
    }

    /**
     * <p>
     * playerCombatDamage_Scalpelexis.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageScalpelexis(final Card c) {
        final Player player = c.getController();
        final Player opponent = player.getOpponent();

        if (c.getNetAttack() > 0) {
            Ability ability = new Ability(c, "0") {
                @Override
                public void resolve() {

                    CardList libList = opponent.getCardsIn(Zone.Library);
                    int count = 0;
                    int broken = 0;
                    for (int i = 0; i < libList.size(); i = i + 4) {
                        Card c1 = null;
                        Card c2 = null;
                        Card c3 = null;
                        Card c4 = null;
                        if (i < libList.size()) {
                            c1 = libList.get(i);
                        } else {
                            broken = 1;
                        }
                        if (i + 1 < libList.size()) {
                            c2 = libList.get(i + 1);
                        } else {
                            broken = 1;
                        }
                        if (i + 2 < libList.size()) {
                            c3 = libList.get(i + 2);
                        } else {
                            broken = 1;
                        }
                        if (i + 3 < libList.size()) {
                            c4 = libList.get(i + 3);
                        } else {
                            broken = 1;
                        }
                        if (broken == 0) {
                            if ((c1.getName().contains(c2.getName()) || c1.getName().contains(c3.getName())
                                    || c1.getName().contains(c4.getName()) || c2.getName().contains(c3.getName())
                                    || c2.getName().contains(c4.getName()) || c3.getName().contains(c4.getName()))) {
                                count = count + 1;
                            } else {
                                broken = 1;
                            }
                        }

                    }
                    count = (count * 4) + 4;
                    int max = count;
                    if (libList.size() < count) {
                        max = libList.size();
                    }

                    for (int j = 0; j < max; j++) {
                        Card c = libList.get(j);
                        AllZone.getGameAction().exile(c);
                    }
                }
            }; // ability

            StringBuilder sb = new StringBuilder();
            sb.append("Scalpelexis - ").append(opponent);
            sb.append(" exiles the top four cards of his or her library. ");
            sb.append("If two or more of those cards have the same name, repeat this process.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }

    /**
     * <p>
     * playerCombatDamage_Spawnwrithe.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    private static void playerCombatDamageSpawnwrithe(final Card c) {
        final Player player = c.getController();
        final Card crd = c;

        Ability ability2 = new Ability(c, "0") {
            @Override
            public void resolve() {
                CardList cl = CardFactoryUtil.makeToken("Spawnwrithe", "", crd.getController(), "2 G", new String[] {
                        "Creature", "Elemental" }, 2, 2, new String[] { "Trample" });

                for (Card c : cl) {
                    c.setText("Whenever Spawnwrithe deals combat damage to a player, "
                + "put a token that's a copy of Spawnwrithe onto the battlefield.");
                    c.setCopiedToken(true);
                }
            }
        }; // ability2

        StringBuilder sb = new StringBuilder();
        sb.append(c.getName()).append(" - ").append(player).append(" puts copy onto the battlefield.");
        ability2.setStackDescription(sb.toString());

        AllZone.getStack().addSimultaneousStackEntry(ability2);

    }

    /** Constant <code>Elspeth_Emblem</code>. */
    private static Command elspethEmblem = new Command() {

        private static final long serialVersionUID = 7414127991531889390L;
        private CardList gloriousAnthemList = new CardList();

        public void execute() {
            String keyword = "Indestructible";

            CardList list = gloriousAnthemList;
            Card c;
            // reset all cards in list - aka "old" cards
            for (int i = 0; i < list.size(); i++) {
                c = list.get(i);
                c.removeExtrinsicKeyword(keyword);
            }

            list.clear();

            CardList emblem = AllZoneUtil.getCardsIn(Zone.Battlefield);
            emblem = emblem.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.isEmblem()
                            && c.hasKeyword("Artifacts, creatures, enchantments, "
                    + "and lands you control are indestructible.");
                }
            });

            for (int i = 0; i < emblem.size(); i++) {
                CardList perms = emblem.get(i).getController().getCardsIn(Zone.Battlefield);

                for (int j = 0; j < perms.size(); j++) {
                    c = perms.get(j);
                    if (!c.hasKeyword(keyword)) {
                        c.addExtrinsicKeyword(keyword);
                        gloriousAnthemList.add(c);
                    }
                }
            }
        } // execute()
    };

    // Special Conditions
    /**
     * <p>
     * specialConditionsMet.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param specialConditions
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean specialConditionsMet(final Card sourceCard, final String specialConditions) {

        if (specialConditions.contains("CardsInHandMore")) {
            CardList specialConditionsCardList = sourceCard.getController().getCardsIn(Zone.Hand);
            String condition = specialConditions.split("/")[1];
            if (specialConditionsCardList.size() < Integer.valueOf(condition)) {
                return false;
            }
        }
        if (specialConditions.contains("OppHandEmpty")) {
            CardList oppHand = sourceCard.getController().getOpponent().getCardsIn(Zone.Hand);
            if (!(oppHand.size() == 0)) {
                return false;
            }
        }
        if (specialConditions.contains("TopCardOfLibraryIsBlack")) {
            PlayerZone lib = sourceCard.getController().getZone(Constant.Zone.Library);
            if (!(lib.get(0).isBlack())) {
                return false;
            }
        }
        if (specialConditions.contains("LibraryLE")) {
            CardList library = sourceCard.getController().getCardsIn(Zone.Library);
            String maxnumber = specialConditions.split("/")[1];
            if (library.size() > Integer.valueOf(maxnumber)) {
                return false;
            }
        }
        if (specialConditions.contains("LifeGE")) {
            int life = sourceCard.getController().getLife();
            String maxnumber = specialConditions.split("/")[1];
            if (!(life >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppCreatureInPlayGE")) {
            CardList oppInPlay = sourceCard.getController().getOpponent().getCardsIn(Zone.Battlefield);
            oppInPlay = oppInPlay.getType("Creature");
            String maxnumber = specialConditions.split("/")[1];
            if (!(oppInPlay.size() >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("LandYouCtrlLE")) {
            CardList landInPlay = sourceCard.getController().getCardsIn(Zone.Battlefield);
            landInPlay = landInPlay.getType("Land");
            String maxnumber = specialConditions.split("/")[1];
            if (!(landInPlay.size() <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("LandOppCtrlLE")) {
            CardList oppLandInPlay = sourceCard.getController().getOpponent().getCardsIn(Zone.Battlefield);
            oppLandInPlay = oppLandInPlay.getType("Land");
            String maxnumber = specialConditions.split("/")[1];
            if (!(oppLandInPlay.size() <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppCtrlMoreCreatures")) {
            CardList creaturesInPlayYou = sourceCard.getController().getCardsIn(Zone.Battlefield);
            creaturesInPlayYou = creaturesInPlayYou.getType("Creature");
            CardList creaturesInPlayOpp = sourceCard.getController().getOpponent().getCardsIn(Zone.Battlefield);
            creaturesInPlayOpp = creaturesInPlayOpp.getType("Creature");
            if (creaturesInPlayYou.size() > creaturesInPlayOpp.size()) {
                return false;
            }
        }
        if (specialConditions.contains("OppCtrlMoreLands")) {
            CardList landsInPlayYou = sourceCard.getController().getCardsIn(Zone.Battlefield);
            landsInPlayYou = landsInPlayYou.getType("Land");
            CardList landsInPlayOpp = sourceCard.getController().getOpponent().getCardsIn(Zone.Battlefield);
            landsInPlayOpp = landsInPlayOpp.getType("Land");
            if (landsInPlayYou.size() > landsInPlayOpp.size()) {
                return false;
            }
        }
        if (specialConditions.contains("EnchantedControllerCreaturesGE")) {
            CardList enchantedControllerInPlay = sourceCard.getEnchantingCard().getController()
                    .getCardsIn(Zone.Battlefield);
            enchantedControllerInPlay = enchantedControllerInPlay.getType("Creature");
            String maxnumber = specialConditions.split("/")[1];
            if (!(enchantedControllerInPlay.size() >= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("OppLifeLE")) {
            int life = sourceCard.getController().getOpponent().getLife();
            String maxnumber = specialConditions.split("/")[1];
            if (!(life <= Integer.valueOf(maxnumber))) {
                return false;
            }
        }
        if (specialConditions.contains("Threshold")) {
            if (!sourceCard.getController().hasThreshold()) {
                return false;
            }
        }
        if (specialConditions.contains("Imprint")) {
            if (sourceCard.getImprinted().isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("Hellbent")) {
            CardList handcards = sourceCard.getController().getCardsIn(Zone.Hand);
            if (handcards.size() > 0) {
                return false;
            }
        }
        if (specialConditions.contains("Metalcraft")) {
            CardList cardsinPlay = sourceCard.getController().getCardsIn(Zone.Battlefield);
            cardsinPlay = cardsinPlay.getType("Artifact");
            if (cardsinPlay.size() < 3) {
                return false;
            }
        }
        if (specialConditions.contains("Morbid")) {
            CardList res = CardUtil.getThisTurnEntered(Zone.Graveyard, Zone.Battlefield, "Creature", sourceCard);
            if (res.size() < 1) {
                return false;
            }
        }

        if (specialConditions.contains("isPresent")) { // is a card of a certain
                                                       // type/color present?
            String requirements = specialConditions.replaceAll("isPresent ", "");
            CardList cardsinPlay = AllZoneUtil.getCardsIn(Zone.Battlefield);
            String[] conditions = requirements.split(",");
            cardsinPlay = cardsinPlay.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (cardsinPlay.isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("isInGraveyard")) { // is a card of a
                                                           // certain type/color
                                                           // present in yard?
            String requirements = specialConditions.replaceAll("isInGraveyard ", "");
            CardList cardsinYards = AllZoneUtil.getCardsIn(Zone.Graveyard);
            String[] conditions = requirements.split(",");
            cardsinYards = cardsinYards.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (cardsinYards.isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("isNotPresent")) { // is no card of a
                                                          // certain type/color
                                                          // present?
            String requirements = specialConditions.replaceAll("isNotPresent ", "");
            CardList cardsInPlay = AllZoneUtil.getCardsIn(Zone.Battlefield);
            String[] conditions = requirements.split(",");
            cardsInPlay = cardsInPlay.getValidCards(conditions, sourceCard.getController(), sourceCard);
            if (!cardsInPlay.isEmpty()) {
                return false;
            }
        }
        if (specialConditions.contains("isEquipped")) {
            if (!sourceCard.isEquipped()) {
                return false;
            }
        }
        if (specialConditions.contains("isEnchanted")) {
            if (!sourceCard.isEnchanted()) {
                return false;
            }
        }
        if (specialConditions.contains("isUntapped")) {
            if (!sourceCard.isUntapped()) {
                return false;
            }
        }
        if (specialConditions.contains("isValid")) { // does this card meet the
                                                     // valid description?
            String requirements = specialConditions.replaceAll("isValid ", "");
            if (!sourceCard.isValid(requirements, sourceCard.getController(), sourceCard)) {
                return false;
            }
        }
        if (specialConditions.contains("isYourTurn")) {
            if (!AllZone.getPhase().isPlayerTurn(sourceCard.getController())) {
                return false;
            }
        }
        if (specialConditions.contains("notYourTurn")) {
            if (!AllZone.getPhase().isPlayerTurn(sourceCard.getController().getOpponent())) {
                return false;
            }
        }
        if (specialConditions.contains("OppPoisoned")) {
            if (sourceCard.getController().getOpponent().getPoisonCounters() == 0) {
                return false;
            }
        }
        if (specialConditions.contains("OppNotPoisoned")) {
            if (sourceCard.getController().getOpponent().getPoisonCounters() > 0) {
                return false;
            }
        }
        if (specialConditions.contains("ThisTurnCast")) {
            String valid = specialConditions.split(" ")[1];
            if (CardUtil.getThisTurnCast(valid, sourceCard).size() == 0) {
                return false;
            }
        }
        if (specialConditions.contains("ThisTurnNotCast")) {
            String valid = specialConditions.split(" ")[1];
            if (CardUtil.getThisTurnCast(valid, sourceCard).size() != 0) {
                return false;
            }
        }
        return true;

    }

    /** Constant <code>stLandManaAbilities</code>. */
    private static Command stLandManaAbilities = new Command() {
        private static final long serialVersionUID = 8005448956536998277L;

        public void execute() {

            HashMap<String, String> produces = new HashMap<String, String>();
            /*
             * for future use boolean naked =
             * AllZoneUtil.isCardInPlay("Naked Singularity"); boolean twist =
             * AllZoneUtil.isCardInPlay("Reality Twist"); //set up what they
             * produce produces.put("Forest", naked || twist ? "B" : "G");
             * produces.put("Island", naked == true ? "G" : "U"); if(naked)
             * produces.put("Mountain", "U"); else if(twist)
             * produces.put("Mountain", "W"); else produces.put("Mountain",
             * "R"); produces.put("Plains", naked || twist ? "R" : "W");
             * if(naked) produces.put("Swamp", "W"); else if(twist)
             * produces.put("Swamp", "G"); else produces.put("Swamp", "B");
             */
            produces.put("Forest", "G");
            produces.put("Island", "U");
            produces.put("Mountain", "R");
            produces.put("Plains", "W");
            produces.put("Swamp", "B");

            CardList lands = AllZoneUtil.getCardsInGame();
            lands = lands.filter(CardListFilter.LANDS);

            // remove all abilities granted by this Command
            for (Card land : lands) {
                ArrayList<Ability_Mana> sas = land.getManaAbility();
                for (SpellAbility sa : sas) {
                    if (sa.getType().equals("BasicLandTypeMana")) {
                        land.removeSpellAbility(sa);
                    }
                }
            }

            // add all appropriate mana abilities based on current types
            for (Card land : lands) {
                if (land.isType("Swamp")) {
                    AbilityFactory af = new AbilityFactory();
                    SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Swamp")
                            + " | SpellDescription$ Add " + produces.get("Swamp") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Forest")) {
                    AbilityFactory af = new AbilityFactory();
                    SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Forest")
                            + " | SpellDescription$ Add " + produces.get("Forest") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Island")) {
                    AbilityFactory af = new AbilityFactory();
                    SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Island")
                            + " | SpellDescription$ Add " + produces.get("Island") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Mountain")) {
                    AbilityFactory af = new AbilityFactory();
                    SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Mountain")
                            + " | SpellDescription$ Add " + produces.get("Mountain") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
                if (land.isType("Plains")) {
                    AbilityFactory af = new AbilityFactory();
                    SpellAbility sa = af.getAbility("AB$ Mana | Cost$ T | Produced$ " + produces.get("Plains")
                            + " | SpellDescription$ Add " + produces.get("Plains") + " to your mana pool.", land);
                    sa.setType("BasicLandTypeMana");
                    land.addSpellAbility(sa);
                }
            }
        } // execute()

    }; // stLandManaAbilities

    /** Constant <code>Coat_of_Arms</code>. */
    private static Command coatOfArms = new Command() {
        private static final long serialVersionUID = 583505612126735693L;

        private CardList gloriousAnthemList = new CardList();

        public void execute() {
            CardList list = gloriousAnthemList;
            // reset all cards in list - aka "old" cards
            for (int i2 = 0; i2 < list.size(); i2++) {
                list.get(i2).addSemiPermanentAttackBoost(-1);
                list.get(i2).addSemiPermanentDefenseBoost(-1);
            }
            // add +1/+1 to cards
            list.clear();
            PlayerZone[] zone = getZone("Coat of Arms");

            // for each zone found add +1/+1 to each card
            for (int outer = 0; outer < zone.length; outer++) {
                CardList creature = AllZoneUtil.getCardsIn(Zone.Battlefield);

                for (int i = 0; i < creature.size(); i++) {
                    final Card crd = creature.get(i);
                    CardList type = AllZoneUtil.getCardsIn(Zone.Battlefield);
                    type = type.filter(new CardListFilter() {
                        public boolean addCard(final Card card) {
                            return !card.equals(crd) && card.isCreature() && !crd.getName().equals("Mana Pool");
                        }
                    });
                    CardList alreadyAdded = new CardList();
                    for (int x = 0; x < type.size(); x++) {
                        alreadyAdded.clear();
                        for (int x2 = 0; x2 < type.get(x).getType().size(); x2++) {
                            if (!alreadyAdded.contains(type.get(x))) {
                                if (!type.get(x).getType().get(x2).equals("Creature")
                                        && !type.get(x).getType().get(x2).equals("Legendary")
                                        && !type.get(x).getType().get(x2).equals("Artifact")) {
                                    if (crd.isType(type.get(x).getType().get(x2))) {
                                        alreadyAdded.add(type.get(x));
                                        crd.addSemiPermanentAttackBoost(1);
                                        crd.addSemiPermanentDefenseBoost(1);
                                        gloriousAnthemList.add(crd);
                                    }
                                }
                            }
                        }
                    }
                } // for inner
            } // for outer
        } // execute
    }; // Coat of Arms

    private static Command alphaStatus = new Command() {
        private static final long serialVersionUID = -3213793711304934358L;

        private CardList previouslyPumped = new CardList();
        private ArrayList<Integer> previouslyPumpedValue = new ArrayList<Integer>();

        @Override
        public void execute() {
            CardList alphaStatuses = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield).getName("Alpha Status");
            alphaStatuses.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getName("Alpha Status"));

            CardList allCreatures = AllZone.getHumanPlayer().getCardsIn(Zone.Battlefield).getType("Creature");
            allCreatures.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield).getType("Creature"));

            for (int i = 0; i < previouslyPumped.size(); i++) {
                previouslyPumped.get(i).addSemiPermanentAttackBoost(0 - previouslyPumpedValue.get(i));
                previouslyPumped.get(i).addSemiPermanentDefenseBoost(0 - previouslyPumpedValue.get(i));
            }
            previouslyPumped.clear();
            previouslyPumpedValue.clear();

            for (Card alpha : alphaStatuses) {
                Card enchanted = alpha.getEnchantingCard();
                int totalbuff = 0;

                for (Card othercreat : allCreatures) {
                    boolean sharesatype = false;
                    if (enchanted != othercreat) {
                        for (String type : enchanted.getType()) {
                            if (CardUtil.getCreatureTypes().contains(type)) {
                                if (othercreat.getType().contains(type)) {
                                    sharesatype = true;
                                    break;
                                }
                            }
                        }
                        if (sharesatype) {
                            totalbuff += 2;
                        }
                    }
                }

                enchanted.addSemiPermanentAttackBoost(totalbuff);
                enchanted.addSemiPermanentDefenseBoost(totalbuff);
                previouslyPumped.add(enchanted);
                previouslyPumpedValue.add(totalbuff);
            }
        }

    };

    /** stores the Command. */
    private static Command umbraStalker = new Command() {
        private static final long serialVersionUID = -3500747003228938898L;

        public void execute() {
            // get all creatures
            CardList cards = AllZoneUtil.getCardsIn(Zone.Battlefield, "Umbra Stalker");
            for (Card c : cards) {
                Player player = c.getController();
                CardList grave = player.getCardsIn(Zone.Graveyard);
                int pt = CardFactoryUtil.getNumberOfManaSymbolsByColor("B", grave);
                c.setBaseAttack(pt);
                c.setBaseDefense(pt);
            }
        } // execute()
    };

    /** Constant <code>Ajani_Avatar_Token</code>. */
    private static Command ajaniAvatarToken = new Command() {
        private static final long serialVersionUID = 3027329837165436727L;

        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);

            list = list.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getName().equals("Avatar") && c.getImageName().equals("W N N Avatar");
                }
            });
            for (int i = 0; i < list.size(); i++) {
                Card card = list.get(i);
                int n = card.getController().getLife();
                card.setBaseAttack(n);
                card.setBaseDefense(n);
            } // for
        } // execute
    }; // Ajani Avatar

    /** Constant <code>Old_Man_of_the_Sea</code>. */
    private static Command oldManOfTheSea = new Command() {
        private static final long serialVersionUID = 8076177362922156784L;

        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Old Man of the Sea");
            for (Card oldman : list) {
                if (!oldman.getGainControlTargets().isEmpty()) {
                    if (oldman.getNetAttack() < oldman.getGainControlTargets().get(0).getNetAttack()) {
                        ArrayList<Command> coms = oldman.getGainControlReleaseCommands();
                        for (int i = 0; i < coms.size(); i++) {
                            coms.get(i).execute();
                        }
                    }
                }
            }
        }
    }; // Old Man of the Sea

    /** Constant <code>Homarid</code>. */
    private static Command homarid = new Command() {
        private static final long serialVersionUID = 7156319758035295773L;

        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Homarid");

            for (Card homarid : list) {
                int tide = homarid.getCounters(Counters.TIDE);
                if (tide == 4) {
                    homarid.setCounter(Counters.TIDE, 0, true);
                }
            }
        } // execute()
    };

    /** Constant <code>Liu_Bei</code>. */
    private static Command liuBei = new Command() {

        private static final long serialVersionUID = 4235093010715735727L;

        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Liu Bei, Lord of Shu");

            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {

                    Card c = list.get(i);
                    if (getsBonus(c)) {
                        c.setBaseAttack(4);
                        c.setBaseDefense(6);
                    } else {
                        c.setBaseAttack(2);
                        c.setBaseDefense(4);
                    }

                }
            }
        } // execute()

        private boolean getsBonus(final Card c) {
            CardList list = c.getController().getCardsIn(Zone.Battlefield);
            list = list.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getName().equals("Guan Yu, Sainted Warrior")
                            || c.getName().equals("Zhang Fei, Fierce Warrior");
                }
            });

            return list.size() > 0;
        }

    }; // Liu_Bei

    /** Constant <code>Sound_the_Call_Wolf</code>. */
    private static Command soundTheCallWolf = new Command() {
        private static final long serialVersionUID = 4614281706799537283L;

        public void execute() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            list = list.filter(new CardListFilter() {
                public boolean addCard(final Card c) {
                    return c.getName().equals("Wolf")
                            && c.hasKeyword("This creature gets +1/+1 for each card "
                    + "named Sound the Call in each graveyard.");
                }
            });

            for (int i = 0; i < list.size(); i++) {
                Card c = list.get(i);
                c.setBaseAttack(1 + countSoundTheCalls());
                c.setBaseDefense(c.getBaseAttack());
            }
        }

        private int countSoundTheCalls() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);
            list = list.getName("Sound the Call");
            return list.size();
        }

    }; // Sound_the_Call_Wolf

    /** Constant <code>Tarmogoyf</code>. */
    private static Command tarmogoyf = new Command() {
        private static final long serialVersionUID = 5895665460018262987L;

        public void execute() {
            // get all creatures
            CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Tarmogoyf");

            for (int i = 0; i < list.size(); i++) {
                Card c = list.get(i);
                c.setBaseAttack(countDiffTypes());
                c.setBaseDefense(c.getBaseAttack() + 1);
            }

        } // execute()

        private int countDiffTypes() {
            CardList list = AllZoneUtil.getCardsIn(Zone.Graveyard);

            int count = 0;
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isCreature()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isSorcery()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isInstant()) {
                    count++;
                    break;
                }
            }
            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isArtifact()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isEnchantment()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isLand()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isPlaneswalker()) {
                    count++;
                    break;
                }
            }

            for (int q = 0; q < list.size(); q++) {
                if (list.get(q).isTribal()) {
                    count++;
                    break;
                }
            }
            return count;
        }
    };

    /** Constant <code>Muraganda_Petroglyphs</code>. */
    private static Command muragandaPetroglyphs = new Command() {
        private static final long serialVersionUID = -6715848091817213517L;
        private CardList gloriousAnthemList = new CardList();

        public void execute() {
            CardList list = gloriousAnthemList;
            Card c;
            // reset all cards in list - aka "old" cards
            for (int i = 0; i < list.size(); i++) {
                c = list.get(i);
                c.addSemiPermanentAttackBoost(-2);
                c.addSemiPermanentDefenseBoost(-2);
            }

            // add +2/+2 to vanilla cards
            list.clear();
            PlayerZone[] zone = getZone("Muraganda Petroglyphs");

            // for each zone found add +2/+2 to each vanilla card
            for (int outer = 0; outer < zone.length; outer++) {
                CardList creature = AllZoneUtil.getCreaturesInPlay();

                for (int i = 0; i < creature.size(); i++) {
                    c = creature.get(i);
                    if (((c.getAbilityText().trim().equals("")
                            || c.isFaceDown()) && c.getUnhiddenKeyword().size() == 0)) {
                        c.addSemiPermanentAttackBoost(2);
                        c.addSemiPermanentDefenseBoost(2);

                        gloriousAnthemList.add(c);
                    }

                } // for inner
            } // for outer
        } // execute()
    }; // Muraganda_Petroglyphs

    // returns all PlayerZones that has at least 1 Glorious Anthem
    // if Computer has 2 Glorious Anthems, AllZone.getComputerPlay() will be
    // returned twice
    /**
     * <p>
     * getZone.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     * @return an array of {@link forge.PlayerZone} objects.
     */
    private static PlayerZone[] getZone(final String cardName) {
        CardList all = AllZoneUtil.getCardsIn(Zone.Battlefield);

        ArrayList<PlayerZone> zone = new ArrayList<PlayerZone>();
        for (int i = 0; i < all.size(); i++) {
            Card c = all.get(i);
            if (c.getName().equals(cardName) && !c.isFaceDown()) {
                zone.add(AllZone.getZoneOf(c));
            }
        }

        PlayerZone[] z = new PlayerZone[zone.size()];
        zone.toArray(z);
        return z;
    }

    /** Constant <code>commands</code>. */
    private static HashMap<String, Command> commands = new HashMap<String, Command>();

    static {
        // Please add cards in alphabetical order so they are easier to find

        getCommands().put("Ajani_Avatar_Token", ajaniAvatarToken);
        getCommands().put("Alpha_Status", alphaStatus);
        getCommands().put("Coat_of_Arms", coatOfArms);
        getCommands().put("Elspeth_Emblem", elspethEmblem);
        getCommands().put("Homarid", homarid);

        getCommands().put("Liu_Bei", liuBei);

        getCommands().put("Muraganda_Petroglyphs", muragandaPetroglyphs);

        getCommands().put("Old_Man_of_the_Sea", oldManOfTheSea);

        getCommands().put("Sound_the_Call_Wolf", soundTheCallWolf);
        getCommands().put("Tarmogoyf", tarmogoyf);

        getCommands().put("Umbra_Stalker", umbraStalker);

        // /The commands above are in alphabetical order by cardname.
    }

    /**
     * <p>
     * doPowerSink.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object.
     */
    public static void doPowerSink(final Player p) {
        // get all lands with mana abilities
        CardList lands = AllZoneUtil.getPlayerLandsInPlay(p);
        lands = lands.filter(new CardListFilter() {
            public boolean addCard(final Card c) {
                return c.getManaAbility().size() > 0;
            }
        });
        // tap them
        for (Card c : lands) {
            c.tap();
        }

        // empty mana pool
        p.getManaPool().clearPool();
    }

    /**
     * @return the commands
     */
    public static HashMap<String, Command> getCommands() {
        return commands;
    }

    /**
     * @param commands the commands to set
     */
    public static void setCommands(HashMap<String, Command> commands) {
        GameActionUtil.commands = commands; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the stLandManaAbilities
     */
    public static Command getStLandManaAbilities() {
        return stLandManaAbilities;
    }

    /**
     * @param stLandManaAbilities the stLandManaAbilities to set
     */
    public static void setStLandManaAbilities(Command stLandManaAbilities) {
        GameActionUtil.stLandManaAbilities = stLandManaAbilities; // TODO: Add 0 to parameter's name.
    }

} // end class GameActionUtil
