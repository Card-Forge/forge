const { test } = require('node:test');
const assert = require('node:assert/strict');
const guide = require('../renderer/turn-guide.js');

test('every engine step explains itself during automatic progress and priority pauses for both players', () => {
  const phases = ['UNTAP', 'UPKEEP', 'DRAW', 'MAIN1', 'COMBAT_BEGIN', 'COMBAT_DECLARE_ATTACKERS',
    'COMBAT_DECLARE_BLOCKERS', 'COMBAT_FIRST_STRIKE_DAMAGE', 'COMBAT_DAMAGE', 'COMBAT_END', 'MAIN2', 'END_OF_TURN', 'CLEANUP'];
  assert.deepEqual(guide.steps.map(step => step.key), phases);
  for (const phaseKey of phases) for (const yours of [true, false]) {
    const automatic = guide.describe({ phaseKey }, yours);
    assert.ok(automatic.context.text);
    assert.ok(automatic.context.next);
    assert.equal(automatic.passLabel, '');
    const pause = guide.describe({ phaseKey, prompt: { inputType: 'InputPassPriority' } }, yours);
    assert.ok(pause.passLabel);
    assert.ok(pause.instruction.includes(pause.passLabel));
    assert.equal(pause.optionalResponse, !yours || !['MAIN1', 'MAIN2'].includes(phaseKey));
  }
});

test('pending triggers and spells take precedence over moving to the next step', () => {
  for (const step of guide.steps) {
    const result = guide.describe({ phaseKey: step.key, stack: [{ name: 'Upkeep trigger', text: 'Pay a cost.' }],
      prompt: { inputType: 'InputPassPriority' } }, true);
    assert.equal(result.title, 'Upkeep trigger is waiting.');
    assert.equal(result.passLabel, 'Let it resolve');
    assert.equal(result.responseText, 'Pay a cost.');
    assert.equal(result.optionalResponse, true);
  }
});

test('required combat, costs, targets, cleanup, and card choices never become an optional phase skip', () => {
  for (const inputType of ['InputAttack', 'InputBlock', 'InputPayManaSimple', 'InputSelectCardsFromList', 'InputSelectTargets', undefined]) {
    for (const step of guide.steps) {
      const result = guide.describe({ phaseKey: step.key, prompt: { inputType, message: 'Required engine choice.' } }, true);
      assert.equal(result.optionalResponse, false);
      assert.notEqual(result.passLabel, step.button);
      assert.equal(result.passHint, '');
      if (inputType === 'InputAttack') assert.equal(result.passLabel, 'Confirm attackers');
      if (inputType === 'InputBlock') assert.equal(result.passLabel, 'Confirm blocks');
    }
  }
});
