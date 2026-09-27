const { test, expect } = require('@playwright/test');
const fs = require('node:fs');
const path = require('node:path');
const { runEncounter } = require('../encounters/run.cjs');

test('UX encounters wait for the participant, accept a different Forest, and retain observations', async () => {
  const result = await runEncounter({ format: 'Commander', mode: 'ux', visible: false, offline: true,
    outputDirectory: test.info().outputPath('encounter'),
    prompt: async ({ step, page }) => {
      if (step.id === 'play-forest') {
        await expect(page.locator('#match-human .lands-row .match-card')).toHaveCount(0);
        // The participant can choose any copy, not the automation's first card.
        const card = page.locator('#match-hand .match-card').last();
        await card.focus(); await card.click();
        return 'I can see the land, but I paused to work out whose turn it was.';
      }
      const land = page.locator('#match-human .lands-row [aria-label="Forest"]');
      await expect(land).toBeVisible(); // Nothing has tapped it on the participant's behalf.
      await land.focus(); await land.click();
      return 'The mana indicator is clear.';
    } });
  expect(result.report.status).toBe('completed');
  expect(result.report.steps.map(step => step.status)).toEqual(['passed', 'passed']);
  expect(result.report.steps[0].notes).toContain('whose turn');
  expect(result.report.artifactWarnings).toEqual([]);
  expect(fs.statSync(path.join(result.directory, 'trace.zip')).size).toBeGreaterThan(0);
  expect(fs.statSync(path.join(result.directory, '02-tap-forest.png')).size).toBeGreaterThan(0);
  const checkpoint = JSON.parse(fs.readFileSync(path.join(result.directory, '02-tap-forest.json'), 'utf8'));
  for (const opponent of checkpoint.players.filter(player => !player.human)) {
    expect(opponent.zones.find(zone => zone.name === 'Hand').cards).toEqual([]);
    expect(opponent.zones.find(zone => zone.name === 'Library').cards).toEqual([]);
  }
});

test('an incomplete UX task fails its outcome check and keeps diagnostic artifacts', async () => {
  const directory = test.info().outputPath('encounter');
  await expect(runEncounter({ mode: 'ux', visible: false, offline: true, outputDirectory: directory,
    prompt: async ({ page }) => {
      await expect(page.locator('#match-human .lands-row .match-card')).toHaveCount(0);
      return 'I could not work out how to play a land.'; // No participant action.
    } })).rejects.toThrow('Encounter artifacts:');
  const report = JSON.parse(fs.readFileSync(path.join(directory, 'report.json'), 'utf8'));
  expect(report.status).toBe('failed');
  expect(report.steps[0].status).toBe('failed');
  expect(report.steps[0].notes).toContain('could not work out');
  expect(fs.existsSync(path.join(directory, 'failure.json'))).toBe(true);
  expect(fs.statSync(path.join(directory, 'trace.zip')).size).toBeGreaterThan(0);
});

test('cancelling a participant session closes the disposable app and preserves its checkpoint', async () => {
  const directory = test.info().outputPath('encounter');
  const controller = new AbortController();
  await expect(runEncounter({ mode: 'ux', visible: false, offline: true, outputDirectory: directory,
    signal: controller.signal, prompt: async ({ page }) => {
      await expect(page.locator('#match-human .lands-row .match-card')).toHaveCount(0);
      controller.abort(new Error('Participant ended the session.'));
      return '';
    } })).rejects.toThrow('Encounter artifacts:');
  const report = JSON.parse(fs.readFileSync(path.join(directory, 'report.json'), 'utf8'));
  expect(report.status).toBe('aborted');
  expect(report.steps[0].status).toBe('aborted');
  expect(fs.existsSync(path.join(directory, '00-ready.json'))).toBe(true);
});
