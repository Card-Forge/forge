/* Guidance describes the engine's current stop; it never advances the game. */
const turnGuide = (() => {
  const steps = [
    { key: 'UNTAP', name: 'Untap', next: 'Upkeep', button: 'Finish untap',
      text: 'The active player’s eligible cards untap automatically. There is normally no chance to cast spells here.' },
    { key: 'UPKEEP', name: 'Upkeep', next: 'Draw', button: 'Finish upkeep',
      text: 'This is the step before drawing. “At the beginning of upkeep” abilities happen here. There is no upkeep cost unless a card says so.' },
    { key: 'DRAW', name: 'Draw', next: 'Main phase 1', button: 'Finish draw step',
      text: 'The active player draws for the turn automatically. Card effects or the first-turn rules can change or skip that draw.' },
    { key: 'MAIN1', name: 'Main phase 1', next: 'Beginning of combat', button: 'Go to combat',
      text: 'The active player can normally play a land and cast creatures and other spells while nothing is waiting to resolve.' },
    { key: 'COMBAT_BEGIN', name: 'Beginning of combat', next: 'Declare attackers', button: 'Continue to attackers',
      text: 'The last chance to act before attackers are chosen. Beginning-of-combat abilities happen here.' },
    { key: 'COMBAT_DECLARE_ATTACKERS', name: 'Declare attackers', next: 'Declare blockers, if attacking', button: 'Continue to blockers',
      text: 'The active player chooses attackers and who or what they attack. After they are declared, players can act before blockers are chosen.' },
    { key: 'COMBAT_DECLARE_BLOCKERS', name: 'Declare blockers', next: 'Combat damage (first strike, if needed)', button: 'Continue to damage',
      text: 'Defending players assign their blockers. After blocks are declared, this is a chance to use spells or abilities before combat damage.' },
    { key: 'COMBAT_FIRST_STRIKE_DAMAGE', name: 'First-strike damage', next: 'Regular combat damage', button: 'Continue to regular damage',
      text: 'Creatures with first strike or double strike deal damage in this extra step. Players can then act before regular combat damage.' },
    { key: 'COMBAT_DAMAGE', name: 'Combat damage', next: 'End of combat', button: 'Finish combat damage',
      text: 'Attacking and blocking creatures deal combat damage. Life, damage, and creatures that die update automatically.' },
    { key: 'COMBAT_END', name: 'End of combat', next: 'Main phase 2', button: 'Go to second main',
      text: 'Combat is wrapping up. End-of-combat abilities happen here; attacking and blocking creatures remain in combat until this step ends.' },
    { key: 'MAIN2', name: 'Main phase 2', next: 'End step', button: 'Go to end step',
      text: 'Another chance to cast spells after combat. The active player can play a land if they still have a land play available.' },
    { key: 'END_OF_TURN', name: 'End step', next: 'Cleanup', button: 'Finish end step',
      text: '“At the beginning of the end step” abilities happen here. Players can still use instants and abilities before cleanup.' },
    { key: 'CLEANUP', name: 'Cleanup', next: 'Next player’s turn', button: 'Finish cleanup',
      text: 'The active player discards down to their maximum hand size if needed. Damage clears and “until end of turn” effects end. Usually this is automatic; new triggers can create another pause.' }
  ];

  function describe(state, yours) {
    const step = steps.find(value => value.key === state.phaseKey);
    const prompt = state.prompt;
    const main = ['MAIN1', 'MAIN2'].includes(state.phaseKey);
    const priority = prompt?.inputType === 'InputPassPriority';
    const optionalResponse = priority && (!yours || !main || Boolean(state.stack?.length));
    const context = step ? { ...step } : null;
    let title = '', instruction = '', passLabel = '', passHint = '', responseText = '';
    if (priority) {
      if (state.stack?.length) {
        title = `${state.stack[0].name} is waiting.`;
        instruction = 'Choose Let it resolve if you do not want to play anything first. It takes effect after every remaining player passes.\n\nTo respond, select a highlighted card or ability.';
        passLabel = 'Let it resolve';
        passHint = 'Play nothing in response to this spell or ability.';
        responseText = state.stack[0].text || '';
      } else {
        passLabel = step?.button || 'Continue';
        title = main ? (yours ? 'Play a card or continue.' : 'Let your opponent continue.')
          : state.phaseKey === 'COMBAT_DECLARE_ATTACKERS' ? 'Attackers are declared.'
            : state.phaseKey === 'COMBAT_DECLARE_BLOCKERS' ? 'Blocks are declared.'
              : `Finish ${step?.name.toLowerCase() || 'this step'}?`;
        instruction = main && yours
          ? `Play a highlighted card or use an ability. When you’re done, choose ${passLabel}.`
          : `Nothing is waiting to resolve. You do not have to play anything. Choose ${passLabel} to continue.\n\nTo act first, select a highlighted instant or ability.`;
        passHint = 'Pass this chance to act. The step ends after every remaining player passes without playing anything.';
        if (context && ['DRAW', 'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE'].includes(state.phaseKey)) {
          context.text = state.phaseKey === 'DRAW'
            ? 'The turn’s draw has already been handled. You can act now, before the main phase.'
            : 'Combat damage for this step has already been handled. You can act now, before moving on.';
        }
      }
    } else if (prompt?.playerChoices?.length) {
      title = 'Choose a player.';
      instruction = 'Click one of the highlighted life totals to choose that player.';
    } else if (prompt?.inputType === 'InputAttack') {
      title = 'Choose your attackers.';
      instruction = 'Click creatures on your battlefield to attack the highlighted defender, or drag a creature onto an opponent. Click a life total to change defenders. Confirm attackers when ready.';
      passLabel = 'Confirm attackers';
    } else if (prompt?.inputType === 'InputBlock') {
      title = 'Choose your blockers.';
      instruction = 'Click an attacking creature, then one of your highlighted blockers, or drag your creature onto an attacker. Blue lines show blocks. Select that attacker and click its blocker again to remove the block. Confirm blocks when ready.';
      passLabel = 'Confirm blocks';
    } else if (prompt?.inputType?.startsWith('InputPayMana')) {
      title = 'Pay the requested cost.';
      instruction = 'Select mana sources or use Auto-pay mana when offered. The cost and any remaining mana are shown below.';
    } else if (prompt?.inputType === 'InputSelectCardsFromList') {
      title = 'Select the requested cards.';
      // Selecting the maximum number may submit immediately in the engine.
      instruction = 'Select the highlighted cards requested below. A full selection continues automatically; otherwise, choose Confirm selection when ready.';
      passLabel = 'Confirm selection';
    }
    return { context, optionalResponse, title, instruction, passLabel, passHint, responseText };
  }
  return { steps, describe };
})();
if (typeof module !== 'undefined') module.exports = turnGuide;
