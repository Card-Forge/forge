import { describe, expect, it } from 'vitest';
import { PLAYMATS, SETTINGS, stored, type SettingDef } from '../../main/ts/settings';

const def = (key: string): SettingDef => SETTINGS.find(d => d.key === key)!;
const ofType = (type: SettingDef['type']): SettingDef => SETTINGS.find(d => d.type === type)!;

describe('a stored setting', () => {
  // Fails if a playmat that was removed, or a choice value outside the current options, is used
  it('drops a choice that is no longer offered, and keeps one that is', () => {
    const choice = ofType('choice') as SettingDef & { type: 'choice' };
    const tiles = ofType('tiles') as SettingDef & { type: 'tiles' };
    const cases: [SettingDef, string][] = [[choice, choice.options[0][0]], [tiles, tiles.options[0].value], [def('playmat'), PLAYMATS[0].id]];
    for (const [setting, offered] of cases) {
      expect(stored(setting, 'gone'), setting.key).toBeUndefined();
      expect(stored(setting, offered), setting.key).toBe(offered);
    }
  });
  // Fails if a value of the wrong type is used, as after a setting changes type
  it('drops a value of the wrong type', () => {
    expect(stored(ofType('toggle'), 'true')).toBeUndefined();
    expect(stored(ofType('slider'), '3')).toBeUndefined();
  });
  // Fails if a number outside a slider's range is used as it is
  it("keeps a slider's value inside its range", () => {
    const slider = ofType('slider') as SettingDef & { type: 'slider' };
    expect(stored(slider, slider.max + 100)).toBe(slider.max);
    expect(stored(slider, slider.min - 100)).toBe(slider.min);
  });
});
