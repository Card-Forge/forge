// Every message and field comes from protocol.gen.ts. This file only adds the view types the client uses to read objects

import type { ClientMessage, Ref, TrackedProps, ZoneType } from './protocol.gen';

export type * from './protocol.gen';

/** Any object in the game's object table. The table does not record each object's kind. */
export type TrackedObject = TrackedProps & { $key: number };

// Each view is picked from TrackedProps, so a property Forge renames or retypes breaks the build here
type View<K extends keyof TrackedProps> = Pick<TrackedProps, K> & { $key: number };

export type CardView = View<'CurrentState' | 'Owner' | 'Controller' | 'Tapped' | 'Sickness' | 'Attacking' | 'Blocking'
  | 'PhasedOut' | 'Detained' | 'Token' | 'Cloned' | 'Damage' | 'IsRingBearer' | 'Counters' | 'EntityAttachedTo' | 'Zone'
  | 'ShieldCount' | 'MustBlockCards' | 'BlockAdditional' | 'BlockAny'
  | 'ClassLevel' | 'CurrentRoom' | 'RingLevel' | 'Sprocket' | 'AttractionLights' | 'Intensity' | 'PlayerMayLook'
  | 'IsCommander' | 'UntilLeavesBattlefield' | 'PreparedSpell' | 'PairedWith' | 'Facedown' | 'FacedownImageKey' | 'OverlayText' | 'ChangedTypes' | 'ChangedColorWords' | 'PaperFoil' | 'Stickers' | 'IsEmblem' | 'GamePieceType'>;

export type CardStateView = View<'Name' | 'ImageKey' | 'Type' | 'ManaCost' | 'OriginalManaCost' | 'Power' | 'Toughness' | 'BasePower' | 'BaseToughness' | 'Loyalty'
  | 'Defense' | 'Keywords' | 'Colors' | 'RulesText' | 'OracleText' | 'AbilityText' | 'FoilIndex'>;

export type PlayerView = View<'Name' | 'Life' | 'IsAI' | 'HasPriority' | 'AvatarIndex' | 'AvatarCardImageKey'
  | 'SleeveIndex' | 'Counters' | 'Commander' | 'CommanderDamage' | 'CommanderCast' | 'Mana' | 'HasLost' | 'Opponents' | 'SharedTeam' | PlayerZone>;

export type GameView = View<'Players' | 'PlayerTurn' | 'Turn' | 'Phase' | 'Stack' | 'CombatView' | 'GameOver' | 'MatchOver'
  | 'WinningPlayerName' | 'NumGamesInMatch' | 'NumPlayedGamesInMatch' | 'PlanarPlayer' | 'StormCount'>;

export type StackItemView = View<'SourceCard' | 'ActivatingPlayer' | 'Ability' | 'Description' | 'SubInstance'
  | 'TargetCards' | 'TargetPlayers'>;

/** The zones a player holds cards in. Each is a Forge zone and also a player property that lists its cards. The stack is not one. */
export type PlayerZone = Exclude<Extract<ZoneType, keyof TrackedProps>, 'Stack'>;

/** A list of references as the table holds them. An object Forge could not name is null. */
export type Refs = readonly (Ref | null)[];

export type Send = (msg: ClientMessage) => void;
