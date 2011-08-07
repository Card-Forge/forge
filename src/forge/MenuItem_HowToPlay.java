package forge;


import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * <p>MenuItem_HowToPlay class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class MenuItem_HowToPlay extends JMenuItem implements NewConstants.LANG.HowTo {
    /** Constant <code>serialVersionUID=5552000208438248428L</code> */
    private static final long serialVersionUID = 5552000208438248428L;

    /**
     * <p>Constructor for MenuItem_HowToPlay.</p>
     */
    public MenuItem_HowToPlay() {
        super(ForgeProps.getLocalized(TITLE));

        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String text = ForgeProps.getLocalized(MESSAGE);

                JTextArea area = new JTextArea(text, 25, 40);
                area.setWrapStyleWord(true);
                area.setLineWrap(true);
                area.setEditable(false);

                area.setOpaque(false);

                JOptionPane.showMessageDialog(null, new JScrollPane(area), ForgeProps.getLocalized(TITLE),
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }//constructor

    /**
     * <p>getString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @SuppressWarnings("unused")
    private String getString() {
        String newLine = "\r\n\r\n";
        StringBuffer s = new StringBuffer();

        s.append("How to Play  -  (Revised: September 2009.)\r\n\r\n\r\n");

        s.append("Introduction").append(newLine);
        s.append("1.  This game is similar to many other trading card games.  You start out with 20 life and your goal is to reduce your opponents life to zero by attacking with your creatures.  The game will end when your life or the computer's life is reduced to zero.  You play \"matches\" against the computer as a set of \"best two out of three\" games.")
                .append(newLine);
        s.append("2.  You use land to pay for spells.  You can play one land a turn.").append(newLine);
        s.append("3.  Each land produces a different magical energy.  This magical energy is shortened to one letter on cards.")
                .append(newLine);
        s.append("    Forests make G\r\n");
        s.append("    Swamps make B\r\n");
        s.append("    Plains make W\r\n");
        s.append("    Islands make U\r\n");
        s.append("    Mountains make R").append(newLine);
        s.append("4.  Each nonland card has a name and a cost.  The cost looks like this \"2GG\" A cost like that would require two Forest lands and two other lands.  The number 2 can be paid for by any land.  A cost like \"R\", would require a Mountain land.")
                .append(newLine);
        s.append("5.  Creature cards stay in play and can attack on the turn AFTER they are played.  A creature's attack and defense is shown like 2/4 meaning that the creature has an attack power of 2 and a defense of 4.  If this creature receives 4 damage it is put into the graveyard.")
                .append(newLine);
        s.append("6.  When you attack with your creatures the computer has a chance to block with his creatures.  When you attack you \"tap\" your creatures by turning them sideways.  Your creatures will untap during your next turn.  When you block, only untapped creatures can block.  Usually a creature cannot attack and block during the same turn.")
                .append(newLine);
        s.append("7.  Sorcery and Instant cards have an effect on the game. After you play any card it goes on the stack to the left, click OK and the stack will clear.  Sorcery cards can only be played during your turn and when the stack is empty.  Instant cards can be played at any time and are more versatile.")
                .append(newLine);
        s.append("8.  If a card has a target, you get to choose that target.  If the target is a player, click on that player's life points.")
                .append(newLine);
        s.append("9.  When you mulligan, the cards in your hand are shuffled into you deck and you are given 1 less card.\r\n\r\n\r\n");

        s.append("Legendary Cards").append(newLine);
        s.append("Legendary cards are powerful cards that can be either creatures, enchantments, lands, artifacts or planeswalkers.  These cards can only exist once in the battlefield, and if another copy of the legendary card is played, all copies of the card are destroyed and sent to the graveyard.\r\n\r\n\r\n");

        s.append("Planeswalkers").append(newLine);
        s.append("There are 6 planeswalkers (Ajani Goldmane; Liliana Vess; Garruk Wildspeaker; Chandra Nalaar; Nicol Bolas; Elspeth, Knight-Errant) and they have specific rules for their special abilities.")
                .append(newLine);
        s.append("You can only use one ability a turn.  A planeswalker can be attacked, but you can also block with your creatures.  For each 1 damage a planeswalker receives, you remove 1 counter.  When a planeswalker doesn't have any counters, it goes to the graveyard.\r\n\r\n\r\n");

        s.append("Hybrid Mana and Multicolored Cards").append(newLine);
        s.append("1.  Hybrid Mana Cards are unique in their casting cost as seen in the card.  Instead of seeing a single color per mana symbol, these cards have 2 colors per mana symbol indicating that the player has the choice of playing either of the two.  There are also cases where numbers and colors are combined together in one mana symbol, which indicates that either colorless or colored mana can be paid for each symbol.  Hybrid cards are treated as two colors and as such can be said to be multicolored.")
                .append(newLine);
        s.append("2.  Multicolored Cards are slightly different than hybrid mana cards, as they require two or more different colors of mana, which may or not be in a single mana symbol.  An example would be a card like Lightning Helix which requires one red and one white mana to be played.\r\n\r\n\r\n");

        s.append("Game Types").append(newLine);
        s.append("1.  In Constructed Deck mode you can use any of the cards to make your deck.  To make a constructed deck, from the Deck Editor select \"New Deck � Constructed\".  A list of all the cards will be displayed.")
                .append(newLine);
        s.append("2.  In Sealed Deck mode you are given 75 cards and you have to make your deck from just those cards.")
                .append(newLine);
        s.append("3.  In Booster Draft mode you select 1 card at a time and then make your deck from just those cards.  After you are done drafting you have to type in a filename, then go to the Deck Editor and from the menu select \"Open Deck - Draft\" and find the filename.  This will allow you to construct your deck.  You can then play against the other 7 computer opponents that were drafting with you.")
                .append(newLine);
        s.append("4.  In Quest Mode you start out with 275 cards, 200 are Basic Lands. As you complete matches in your quest you will win more cards.  In easy mode you get more cards after every game, whether you win or lose.  Your goal is to become world champion.  Once you reach the end of your quest you can continue to play additional matches and win even more cards or you can start a new quest at anytime.")
                .append(newLine);
        s.append("At the Quest Options screen you will be given a choice of four different difficulty levels.  These difficulty levels control:")
                .append(newLine);
        s.append("1)   the length of the quest in matches,\r\n");
        s.append("2)   the hardness of the AI deck that you will face as an opponent,\r\n");
        s.append("3)   the number of wins or loses needed to get more cards,\r\n");
        s.append("4)   the number of wins needed to advance a player to the next rank.\r\n\r\n\r\n");

        s.append("Quick Games").append(newLine);
        s.append("There may be occasions where you only have a few minutes to play a quick game or two.  At the top of the New Game window you will see the three different game types with radio buttons.  Click on the Constructed (Easy) button and it will become highlighted.")
                .append(newLine);
        s.append("In the middle area of the New Game window you will see two menus, one labeled \"Your Deck\" and the other \"Opponent\".  For a quick game you should select the \"Generate Deck\" or the \"Random\" option for both you and the computer.")
                .append(newLine);
        s.append("1.  The \"Generate Deck\" option creates a 2 color deck.  This option randomly picks cards and sometimes your mana curve may be too high.")
                .append(newLine);
        s.append("2.  The \"Random\" option will randomly select one of the constructed decks that appear in the two deck menus.  You either construct these decks in the Deck Editor or you imported a .deck file from the Deck Editor.")
                .append(newLine);
        s.append("If you select the \"Random\" option and click on the \"Start Game\" button and the match fails to begin � well, this happens if you fail to have any constructed decks saved to your all-decks2 file.  You should choose the \"Generate Deck\" option instead.\r\n\r\n\r\n");

        s.append("Resizable Game Area & Stack AI Land").append(newLine);
        s.append("1.  The \"Resizable Game Area\" check box should be set to on if your monitor will display more than 1024 x 768 pixels.  The window containing the Battlefield and the informational displays will fill the entire screen.")
                .append(newLine);
        s.append("2.  The \"Stack AI Land\" option will make the computer a more difficult opponent to beat.  The computer will draw nonland cards from it's library after it has drawn enough lands to cover the cost of it's spells.  Set the check box to on for a stronger opponent and set the check box to off for a weaker opponent.")
                .append(newLine);
        s.append("At times, you may notice that when you click the \"Start Game\" button that the match fails to begin.  In this case you should turn the \"Stack AI Land\" option to off.\r\n\r\n\r\n");

        s.append("Abilities").append(newLine);
        s.append("There are three kinds of abilities: Activated, Triggered, and Static.").append(newLine);
        s.append("1.  Activated abilities contain a colon that separates cost and effect, these can be played any time you could play an instant.  An example is Elvish Piper's ability.  That cost also contains the tap symbol.  For creatures only, abilities containing the tap- or untap symbol can be played starting the turn after the creature entered the battlefield.  Another common cost for an activated ability is sacrificing the card.  You do that by putting it into your graveyard.  Such abilities can only be played once.")
                .append(newLine);
        s.append("2.  Triggered abilities aren't played, they simply trigger when their condition occurs.  An example is Angel of Mercy: You don't play the ability, but gain 3 life when it enters the battlefield.")
                .append(newLine);
        s.append("3.  Static abilities are neither played, nor do they trigger.  They still have an effect for as long as they are in play.  An example is Glorious Anthem.  There is no condition or cost, your creatures are just stronger.\r\n\r\n\r\n");

        s.append("Keyword Abilities").append(newLine);
        s.append("1.  Flying:  Creatures with flying are harder to block.  Only creatures with flying or reach can block other flyers.")
                .append(newLine);
        s.append("2.  Haste:  Haste lets a creature attack or use any abilities immediately during this turn.")
                .append(newLine);
        s.append("3.  Fear:  Creatures with fear can only be blocked by artifact or black creatures.  Creatures with fear are harder to block.")
                .append(newLine);
        s.append("4.  Cycling:  When you cycle a card you pay some cost like 2 and then you discard that card, and then draw a new card.  Cycling helps make your deck more versatile.")
                .append(newLine);
        s.append("5.  Vigilance:  This means that the creature will not tap when attacking.  This creature can both attack and block during the same turn.")
                .append(newLine);
        s.append("6.  Trample:  If you use 2/1 creature to block an attacking 3/4 creature with trample, you will still receive 2 damage because the 3/4 trampled over your 2/1 creature.  Trample damage is calculated by (attack - blocker's defense), in this case 3-1 which is 2.")
                .append(newLine);
        s.append("7.  Deathtouch:  When a creatures with deathtouch deals damage to a creature, that creature is destroyed.")
                .append(newLine);
        s.append("8.  Defender:  Creatures with defender can not attack, they can only block another attacker.")
                .append(newLine);
        s.append("9.  First Strike and Double Strike:  Creatures with first strike deals their combat damage first.  Creatures with double strike deals their combat damage first, and then deals their combat damage a second time during the combat damage step.")
                .append(newLine);
        s.append("10.  Flash:  You can play a creature with flash anytime that you can play an Instant.").append(newLine);
        s.append("11.  Landwalk:  Allows your creature to attack without being blocked if your opponent controls the appropriate land type.")
                .append(newLine);
        s.append("12.  Lifelink:  With lifeline you gain life equal to the amount of damage dealt.").append(newLine);
        s.append("13.  Protection:  Can not be damaged, blocked or targeted by sources that match the protection type.")
                .append(newLine);
        s.append("14.  Reach:  Creatures with reach can block flying creatures.").append(newLine);
        s.append("15.  Shroud:  Permanents with shroud can not be targeted by abilities or spells.").append(newLine);
        s.append("16.  Regenerate:  Regenerate is an ability that some creatures have which prevents them from being destroyed and put into the graveyard.  When you regenerate a creature, it acts like a shield until end of turn.")
                .append(newLine);
        s.append("17.  Morph:  A creature with morph can be played by usually paying 3 mana of any color and be treated as a 2/2 creature with no abilities rather than playing the creature's actual cost.  This creature is placed face down in the battlefield and can be flipped face up anytime as long as you pay its morph cost which is indicated on the card.  Once flipped face up the card is treated as the original card, the one that you would normally play with its original-full casting cost.");


        return s.toString();
    }
}//MenuItem_HowToPlay

