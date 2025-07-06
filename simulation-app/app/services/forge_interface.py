import subprocess
import os
import threading
import time
import logging
import re
import glob
from datetime import datetime
from flask import current_app
from typing import Dict, List, Optional
from ..models.simulation import Simulation, GameResult
from ..models.statistics import GameStatistics, CardPlay
from .. import db

logger = logging.getLogger(__name__)

class ForgeInterface:
    def __init__(self, app=None):
        """Initialize Forge interface with configuration from environment."""
        self.forge_jar_path = os.environ.get('FORGE_JAR_PATH', '/Users/geromepistre/projects/forge4/forge-gui-desktop/target/forge-gui-desktop-2.0.04-SNAPSHOT-jar-with-dependencies.jar')
        self.forge_languages_path = os.environ.get('FORGE_LANGUAGES_PATH', '/Users/geromepistre/projects/forge4/languages')
        self.llm_endpoint = os.environ.get('LLM_ENDPOINT', 'http://localhost:7861')
        self.max_concurrent = int(os.environ.get('MAX_CONCURRENT_SIMULATIONS', '10'))
        self.batch_size = int(os.environ.get('FORGE_BATCH_SIZE', '10'))  # Games per batch
        self.use_distributed = os.environ.get('FORGE_USE_DISTRIBUTED', 'false').lower() == 'true'
        self.app = app
        
        # Track running simulations
        self.running_simulations = {}
        self.simulation_threads = {}
        
        # Initialize card database for improved parsing
        self.card_database = None
        self._initialize_card_database()
    
    def _initialize_card_database(self):
        """Initialize the card database for improved parsing.""" 
        try:
            logger.info("Loading Forge card database...")
            self.card_database = self._load_card_database()
            logger.info(f"Loaded {len(self.card_database)} cards into database")
        except Exception as e:
            logger.error(f"Failed to load card database: {e}")
            self.card_database = {}
    
    def _load_card_database(self) -> Dict[str, Dict]:
        """Load card database from Forge's card data files."""
        card_db = {}
        
        # Path to Forge card data files
        cardsfolder_path = "/Users/geromepistre/projects/forge4/forge-gui/res/cardsfolder"
        
        if os.path.exists(cardsfolder_path):
            logger.debug(f"Loading card database from: {cardsfolder_path}")
            
            # Find all card files
            card_files = []
            for root, dirs, files in os.walk(cardsfolder_path):
                for file in files:
                    if file.endswith('.txt'):
                        card_files.append(os.path.join(root, file))
            
            logger.debug(f"Found {len(card_files)} card files")
            
            # Load and parse card files
            loaded_count = 0
            for card_file in card_files:
                try:
                    card_data = self._parse_forge_card_file(card_file)
                    if card_data:
                        card_db[card_data['name']] = card_data
                        loaded_count += 1
                        
                        # Progress indicator for large loads
                        if loaded_count % 10000 == 0:
                            logger.debug(f"Loaded {loaded_count} cards...")
                            
                except Exception as e:
                    # Skip problematic files but continue loading
                    continue
            
            logger.debug(f"Successfully loaded {loaded_count} cards from Forge database")
        else:
            logger.warning(f"Forge card database not found at {cardsfolder_path}")
        
        return card_db
    
    def _parse_forge_card_file(self, file_path: str) -> Optional[Dict]:
        """Parse a Forge card file (.txt format) to extract card properties."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            
            card_data = {
                'name': None,
                'types': [],
                'subtypes': [],
                'mana_cost': None,
                'power': None,
                'toughness': None
            }
            
            for line in lines:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                
                if line.startswith('Name:'):
                    card_data['name'] = line[5:].strip()
                elif line.startswith('ManaCost:'):
                    card_data['mana_cost'] = line[9:].strip()
                elif line.startswith('Types:'):
                    types_str = line[6:].strip()
                    # Parse types like "Creature Elder Dragon" or "Instant" or "Artifact Equipment"
                    type_parts = types_str.split()
                    
                    # First part is usually the main type
                    main_types = []
                    subtypes = []
                    
                    # Known main types
                    main_type_keywords = [
                        'Artifact', 'Creature', 'Enchantment', 'Instant', 'Land', 
                        'Planeswalker', 'Sorcery', 'Tribal', 'Legendary', 'Basic', 'Snow'
                    ]
                    
                    for part in type_parts:
                        if part in main_type_keywords:
                            main_types.append(part)
                        else:
                            subtypes.append(part)
                    
                    card_data['types'] = main_types
                    card_data['subtypes'] = subtypes
                    
                elif line.startswith('PT:'):
                    # Power/Toughness like "PT:6/5"
                    pt_str = line[3:].strip()
                    if '/' in pt_str:
                        try:
                            power, toughness = pt_str.split('/')
                            card_data['power'] = power.strip()
                            card_data['toughness'] = toughness.strip()
                        except:
                            pass
            
            return card_data if card_data['name'] else None
            
        except Exception as e:
            return None
    
    def _classify_card_type(self, card_name: str) -> Dict[str, bool]:
        """Classify a card by its types using the loaded card database."""
        classification = {
            'is_creature': False,
            'is_artifact': False,
            'is_enchantment': False,
            'is_instant': False,
            'is_sorcery': False,
            'is_planeswalker': False,
            'is_land': False,
            'is_battle': False  # Add support for Battle cards
        }
        
        if not self.card_database:
            return classification
        
        # Try multiple matching strategies
        card_data = None
        
        # 1. Exact match
        if card_name in self.card_database:
            card_data = self.card_database[card_name]
        else:
            # 2. Remove number suffix (e.g., "Absorb (17)" -> "Absorb")
            base_card_name = re.sub(r'\s*\(\d+\)$', '', card_name)
            if base_card_name != card_name and base_card_name in self.card_database:
                card_data = self.card_database[base_card_name]
            else:
                # 3. Handle tokens (common tokens that aren't in main database)
                if 'token' in card_name.lower():
                    return self._classify_token(card_name)
                
                # 4. Try fuzzy matching for complex names
                # This handles cases where log names don't exactly match database names
                normalized_search = self._normalize_card_name(base_card_name)
                for db_card_name in self.card_database:
                    normalized_db_name = self._normalize_card_name(db_card_name)
                    if normalized_search == normalized_db_name:
                        card_data = self.card_database[db_card_name]
                        break
                
                # 5. If still not found, try prefix matching
                if not card_data:
                    words = base_card_name.lower().split()
                    if len(words) >= 2:
                        search_prefix = ' '.join(words[:2])
                        for db_card_name in self.card_database:
                            if db_card_name.lower().startswith(search_prefix):
                                card_data = self.card_database[db_card_name]
                                break
        
        if card_data:
            types = card_data.get('types', [])
            
            classification['is_creature'] = 'Creature' in types
            classification['is_artifact'] = 'Artifact' in types  
            classification['is_enchantment'] = 'Enchantment' in types
            classification['is_instant'] = 'Instant' in types
            classification['is_sorcery'] = 'Sorcery' in types
            classification['is_planeswalker'] = 'Planeswalker' in types
            classification['is_land'] = 'Land' in types
            classification['is_battle'] = 'Battle' in types
        
        return classification
    
    def _classify_spell_function(self, spell_name: str) -> Dict[str, bool]:
        """Classify spell by its gameplay function."""
        functional_classification = {
            'is_removal': False,
            'is_ramp': False,
            'is_card_draw': False,
            'is_counterspell': False,
            'is_protection': False,
            'is_board_wipe': False,
            'is_tribal': False,
            'is_combo_piece': False,
            'is_value_engine': False,
            'is_other': False
        }
        
        # First check if this is a land - if so, always classify as "other"
        card_type_info = self._classify_card_type(spell_name)
        if card_type_info.get('is_land', False):
            functional_classification['is_other'] = True
            return functional_classification
        
        spell_lower = spell_name.lower()
        
        # Removal spells - destroy, exile, bounce, damage to creatures/planeswalkers
        removal_keywords = [
            'destroy', 'exile', 'murder', 'doom', 'terminate', 'path', 'swords',
            'lightning bolt', 'shock', 'burn', 'damage', 'wrath', 'damnation',
            'board wipe', 'mass destruction', 'apocalypse', 'armageddon',
            'return to hand', 'bounce', 'unsummon', 'capsize', 'cyclonic rift',
            'vandalblast', 'shatter', 'naturalize', 'disenchant', 'vindicate',
            'utter end', 'anguished unmaking', 'generous gift', 'chaos warp',
            'beast within', 'reality shift', 'pongify', 'rapid hybridization'
        ]
        
        # Ramp spells - mana acceleration, land ramp, mana rocks
        ramp_keywords = [
            'rampant growth', 'cultivate', 'kodama\'s reach', 'explosive vegetation',
            'search for forest', 'search for basic land', 'sol ring', 'signets',
            'mana rock', 'mana dork', 'llanowar elves', 'birds of paradise',
            'ramp', 'mana acceleration', 'land to battlefield', 'tapped land',
            'arcane signet', 'fellwar stone', 'mind stone', 'hedron archive',
            'thran dynamo', 'gilded lotus', 'worn powerstone', 'add', 'talisman',
            'mox', 'lotus', 'crypt', 'vault', 'diamond', 'chrome mox', 
            'mox tantalite', 'mox amber', 'mox opal'
        ]
        
        # Card draw spells
        card_draw_keywords = [
            'draw card', 'draw', 'divination', 'ancestral recall', 'brainstorm',
            'ponder', 'preordain', 'rhystic study', 'mystic remora', 'phyrexian arena',
            'sylvan library', 'scroll', 'insight', 'harmonize', 'concentrate',
            'tidings', 'opportunity', 'sphinx\'s revelation', 'blue sun\'s zenith',
            'stroke of genius', 'pull from tomorrow'
        ]
        
        # Counterspells
        counterspell_keywords = [
            'counter', 'counterspell', 'negate', 'dispel', 'spell pierce',
            'force of will', 'mana drain', 'cryptic command', 'dissolve',
            'cancel', 'essence scatter', 'swan song', 'mental misstep',
            'fierce guardianship', 'force of negation', 'arcane denial',
            'no more lies', 'absorb', 'disdainful stroke', 'flusterstorm',
            'dovin\'s veto', 'mystic confluence', 'rewind'
        ]
        
        # Protection spells - protect creatures/permanents, hexproof, indestructible
        protection_keywords = [
            'protection', 'hexproof', 'indestructible', 'shroud', 'ward',
            'lightning greaves', 'swiftfoot boots', 'mother of runes',
            'heroic intervention', 'teferi\'s protection', 'boros charm',
            'rootborn defenses', 'unbreakable formation', 'selfless spirit'
        ]
        
        # Board wipes - mass removal effects
        board_wipe_keywords = [
            'wrath of god', 'damnation', 'board wipe', 'mass destruction',
            'day of judgment', 'fumigate', 'cleansing nova', 'austere command',
            'blasphemous act', 'chain reaction', 'pyroclasm', 'anger of the gods',
            'terminus', 'merciless eviction', 'cyclonic rift', 'evacuation'
        ]
        
        # Tribal spells - support specific creature types
        tribal_keywords = [
            'tribal', 'dragon', 'elf', 'goblin', 'soldier', 'wizard', 'beast',
            'herald\'s horn', 'vanquisher\'s banner', 'door of destinies',
            'coat of arms', 'adaptive automaton', 'metallic mimic',
            'crucible of fire', 'elvish archdruid', 'goblin king', 'warrior',
            'knight', 'zombie', 'vampire', 'angel', 'demon', 'spirit'
        ]
        
        # Combo pieces - infinite combos, synergy engines
        combo_keywords = [
            'infinite', 'combo', 'laboratory maniac', 'thassa\'s oracle',
            'approach of the second sun', 'felidar sovereign', 'test of endurance',
            'dramatic reversal', 'isochron scepter', 'painter\'s servant',
            'grindstone', 'splinter twin', 'pestermite', 'kiki-jiki'
        ]
        
        # Value engines - ongoing card advantage
        value_engine_keywords = [
            'engine', 'value', 'recurring', 'eternal witness', 'archaeomancer',
            'snapcaster mage', 'flashback', 'buyback', 'rebound',
            'the great henge', 'beast whisperer', 'mentor of the meek',
            'skull clamp', 'graveyard', 'graveyard recursion', 'archaeologist',
            'whenever', 'trigger', 'enters the battlefield', 'etb', 'study',
            'library', 'academy', 'research', 'tutor', 'search', 'forge'
        ]
        
        # Check each category
        functional_classification['is_removal'] = any(keyword in spell_lower for keyword in removal_keywords)
        functional_classification['is_ramp'] = any(keyword in spell_lower for keyword in ramp_keywords)
        functional_classification['is_card_draw'] = any(keyword in spell_lower for keyword in card_draw_keywords)
        functional_classification['is_counterspell'] = any(keyword in spell_lower for keyword in counterspell_keywords)
        functional_classification['is_protection'] = any(keyword in spell_lower for keyword in protection_keywords)
        functional_classification['is_board_wipe'] = any(keyword in spell_lower for keyword in board_wipe_keywords)
        functional_classification['is_tribal'] = any(keyword in spell_lower for keyword in tribal_keywords)
        functional_classification['is_combo_piece'] = any(keyword in spell_lower for keyword in combo_keywords)
        functional_classification['is_value_engine'] = any(keyword in spell_lower for keyword in value_engine_keywords)
        
        # Generic fallback classification based on card type and common patterns
        if not any(functional_classification[key] for key in functional_classification if key != 'is_other'):
            # Get card type to help with classification
            card_type_info = self._classify_card_type(spell_name)
            
            # Check if we got valid card type information
            has_valid_type = any(card_type_info.get(key, False) for key in card_type_info)
            
            if has_valid_type:
                # Artifacts are often ramp (mana rocks)
                if card_type_info.get('is_artifact', False):
                    functional_classification['is_ramp'] = True
                
                # Only classify creatures as value engines if they have obvious utility keywords
                elif card_type_info.get('is_creature', False):
                    # Check if creature has utility-suggesting keywords
                    utility_keywords = ['lord', 'master', 'champion', 'sage', 'advisor', 'shaman', 'priest', 'oracle']
                    if any(keyword in spell_lower for keyword in utility_keywords):
                        functional_classification['is_value_engine'] = True
                    # Otherwise leave as "other" - many creatures are just beaters
                    
                # Instants and sorceries that aren't already classified likely have niche effects
                elif card_type_info.get('is_instant', False) or card_type_info.get('is_sorcery', False):
                    # Don't assume these are removal - could be utility spells
                    pass
                    
                # Enchantments are more likely to be value engines due to ongoing effects
                elif card_type_info.get('is_enchantment', False):
                    functional_classification['is_value_engine'] = True
                    
                # Planeswalkers are typically value engines
                elif card_type_info.get('is_planeswalker', False):
                    functional_classification['is_value_engine'] = True
            # If we can't determine card type, leave as "other"
        
        # If no functional category matches, mark as "other"
        has_function = any(functional_classification[key] for key in functional_classification if key != 'is_other')
        functional_classification['is_other'] = not has_function
        if not has_function:
            print('Warning', spell_name, 'failed to be classified')
        return functional_classification
    
    def _record_card_play(self, card_name: str, player_num: int, deck_id: str, turn_number: int, 
                         play_order: int, is_land: bool = False) -> Dict:
        """Record a card play for detailed tracking."""
        # Get card type classification
        card_classification = self._classify_card_type(card_name)
        functional_classification = self._classify_spell_function(card_name)
        
        # Determine primary card type
        primary_type = 'unknown'
        if card_classification['is_land']:
            primary_type = 'land'
        elif card_classification['is_creature']:
            primary_type = 'creature'
        elif card_classification['is_artifact']:
            primary_type = 'artifact'
        elif card_classification['is_enchantment']:
            primary_type = 'enchantment'
        elif card_classification['is_instant']:
            primary_type = 'instant'
        elif card_classification['is_sorcery']:
            primary_type = 'sorcery'
        elif card_classification['is_planeswalker']:
            primary_type = 'planeswalker'
        elif card_classification['is_battle']:
            primary_type = 'battle'
        
        # Determine primary functional category
        functional_category = 'other'
        if functional_classification['is_removal']:
            functional_category = 'removal'
        elif functional_classification['is_ramp']:
            functional_category = 'ramp'
        elif functional_classification['is_card_draw']:
            functional_category = 'card_draw'
        elif functional_classification['is_counterspell']:
            functional_category = 'counterspell'
        elif functional_classification['is_protection']:
            functional_category = 'protection'
        elif functional_classification['is_board_wipe']:
            functional_category = 'board_wipe'
        elif functional_classification['is_tribal']:
            functional_category = 'tribal'
        elif functional_classification['is_combo_piece']:
            functional_category = 'combo_piece'
        elif functional_classification['is_value_engine']:
            functional_category = 'value_engine'
        
        # Check if it's a commander
        is_commander = 'general' in card_name.lower() or any(cmd in card_name.lower() for cmd in ['commander'])
        
        # Get mana cost if available from database
        mana_cost = None
        if card_name in self.card_database:
            mana_cost = self.card_database[card_name].get('mana_cost', None)
        
        return {
            'card_name': card_name,
            'player_number': player_num,
            'deck_id': deck_id,
            'turn_number': turn_number,
            'play_order': play_order,
            'card_type': primary_type,
            'functional_category': functional_category,
            'is_land': is_land,
            'is_commander': is_commander,
            'mana_cost': mana_cost
        }
    
    def _normalize_card_name(self, card_name: str) -> str:
        """Normalize card name for fuzzy matching."""
        # Convert to lowercase and remove punctuation
        normalized = re.sub(r'[^\w\s]', '', card_name.lower())
        # Remove extra whitespace
        normalized = ' '.join(normalized.split())
        return normalized
    
    def _parse_mana_value(self, mana_value: str) -> int:
        """Parse a mana value string and return the total mana amount."""
        if not mana_value:
            return 0
        
        # Handle pure numbers
        if mana_value.isdigit():
            return int(mana_value)
        
        # Handle mana symbols like {2}{R}{G}
        if '{' in mana_value:
            total = 0
            # Find all mana symbols
            symbols = re.findall(r'\{([^}]+)\}', mana_value)
            for symbol in symbols:
                if symbol.isdigit():
                    total += int(symbol)
                else:
                    # Colored mana symbols count as 1 each
                    total += 1
            return total
        
        # Default case
        return 1
    
    def _classify_token(self, token_name: str) -> Dict[str, bool]:
        """Classify token cards based on their name."""
        classification = {
            'is_creature': False,
            'is_artifact': False,
            'is_enchantment': False,
            'is_instant': False,
            'is_sorcery': False,
            'is_planeswalker': False,
            'is_land': False,
            'is_battle': False
        }
        
        token_lower = token_name.lower()
        
        # Most tokens are creatures or artifacts
        if any(keyword in token_lower for keyword in ['creature', 'beast', 'soldier', 'spirit', 'zombie', 'elf', 'goblin']):
            classification['is_creature'] = True
        elif any(keyword in token_lower for keyword in ['treasure', 'clue', 'food', 'artifact']):
            classification['is_artifact'] = True
        else:
            # Default for most tokens is creature
            classification['is_creature'] = True
            
        return classification

    def start_simulation(self, simulation):
        """Start a simulation in a separate thread."""
        if len(self.running_simulations) >= self.max_concurrent:
            raise Exception("Maximum concurrent simulations reached")
        
        # Mark simulation as running
        simulation.status = 'running'
        simulation.started_at = datetime.utcnow()
        db.session.commit()
        
        # Start simulation thread with app context
        thread = threading.Thread(target=self._run_simulation_with_context, args=(simulation.id,))
        thread.daemon = True
        thread.start()
        
        self.running_simulations[simulation.id] = {
            'simulation': simulation,
            'thread': thread,
            'start_time': time.time(),
            'games_completed': 0
        }
        
        logger.info(f"Started simulation {simulation.id}")
    
    def stop_simulation(self, simulation):
        """Stop a running simulation."""
        if simulation.id in self.running_simulations:
            # Mark for stopping (the thread will check this flag)
            self.running_simulations[simulation.id]['stop_requested'] = True
            logger.info(f"Stop requested for simulation {simulation.id}")
        else:
            raise Exception("Simulation not found or not running")
    
    def get_simulation_status(self, simulation):
        """Get current status of a simulation."""
        if simulation.id in self.running_simulations:
            sim_info = self.running_simulations[simulation.id]
            elapsed_time = time.time() - sim_info['start_time']
            
            # Estimate completion time
            if sim_info['games_completed'] > 0:
                avg_time_per_game = elapsed_time / sim_info['games_completed']
                remaining_games = simulation.num_games - sim_info['games_completed']
                estimated_completion = remaining_games * avg_time_per_game
            else:
                estimated_completion = None
            
            return {
                'status': 'running',
                'games_completed': sim_info['games_completed'],
                'elapsed_time': elapsed_time,
                'estimated_completion': estimated_completion,
                'errors': sim_info.get('errors', [])
            }
        else:
            # Get status from database
            return {
                'status': simulation.status,
                'games_completed': simulation.games_completed,
                'elapsed_time': None,
                'estimated_completion': None,
                'errors': [simulation.error_message] if simulation.error_message else []
            }
    
    def get_system_status(self):
        """Get overall system status."""
        return {
            'concurrent_simulations': len(self.running_simulations),
            'max_concurrent': self.max_concurrent,
            'queue_size': 0,  # No queue implemented yet
            'resources': {
                'memory_usage': 'N/A',  # Could implement with psutil
                'cpu_usage': 'N/A'
            }
        }
    
    def _run_simulation_with_context(self, simulation_id):
        """Wrapper to run simulation with Flask application context."""
        from flask import current_app
        
        # Get the app instance - either from self.app or current_app
        app = self.app or current_app._get_current_object()
        
        with app.app_context():
            # Re-fetch the simulation object within the app context
            simulation = Simulation.query.get(simulation_id)
            if simulation:
                self._run_simulation(simulation)
    
    def _run_simulation(self, simulation):
        """Run a simulation (called in separate thread)."""
        sim_info = self.running_simulations.get(simulation.id)
        if not sim_info:
            return
        
        try:
            # Get deck IDs from simulation players
            deck_ids = [player.deck_id for player in simulation.players]
            
            # Choose execution strategy
            if self.use_distributed or simulation.num_games <= self.batch_size:
                # Use single-game execution for distributed mode or small simulations
                self._run_games_sequential(simulation, deck_ids, sim_info)
            else:
                # Use batch execution for better performance
                self._run_games_batch(simulation, deck_ids, sim_info)
            
            # Mark simulation as completed
            if simulation.status != 'stopped':
                simulation.status = 'completed'
            
            simulation.completed_at = datetime.utcnow()
            db.session.commit()
            
            logger.info(f"Simulation {simulation.id} completed with status: {simulation.status}")
            
        except Exception as e:
            logger.error(f"Fatal error in simulation {simulation.id}: {e}")
            simulation.status = 'failed'
            simulation.error_message = str(e)
            simulation.completed_at = datetime.utcnow()
            db.session.commit()
        
        finally:
            # Clean up
            if simulation.id in self.running_simulations:
                del self.running_simulations[simulation.id]
    
    def _run_single_game(self, simulation, game_num, deck_ids):
        """Run a single game and return the result."""
        # Build command
        cmd = [
            'java',
            f'-Dllm.endpoint={self.llm_endpoint}',
            f'-Dlang.dir={self.forge_languages_path}',
            '-jar', self.forge_jar_path,
            'sim',
            '-f', 'Commander',
            '-d'
        ]
        
        # Add deck IDs
        cmd.extend(deck_ids)
        
        # Add controller types (all AI for now)
        cmd.extend(['-c', ','.join(['ai'] * len(deck_ids))])
        
        # Set timeout
        timeout = simulation.game_timeout
        
        try:
            # Log the command being executed
            logger.info(f"Executing command: {' '.join(cmd)}")
            logger.info(f"Working directory: {os.path.dirname(self.forge_jar_path) if self.forge_jar_path else 'None'}")
            logger.info(f"Language path: {self.forge_languages_path}")
            
            # Check prerequisites
            if not self._check_forge_prerequisites():
                return None
            
            # Run the command from the main forge directory instead of the target directory
            forge_main_dir = '/Users/geromepistre/projects/forge4'
            start_time = time.time()
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                cwd=forge_main_dir
            )
            
            duration = int(time.time() - start_time)
            
            # Parse result from Forge output
            if result.returncode == 0:
                # Store the full output log
                full_log = result.stdout + "\n" + result.stderr if result.stderr else result.stdout
                
                # Parse the game results from the log
                parsed_results = self._parse_forge_output(full_log, deck_ids)
                
                # Create game result with parsed data
                game_result = GameResult(
                    simulation_id=simulation.id,
                    game_number=game_num,
                    game_duration_seconds=duration,
                    game_log=full_log,
                    winner_player_number=parsed_results['winner_player_number'],
                    winner_deck_id=parsed_results['winner_deck_id'],
                    total_turns=parsed_results['total_turns'],
                    elimination_order=parsed_results['elimination_order']
                )
                
                # Create statistics for each player based on parsed data
                for player_data in parsed_results['player_statistics']:
                    stats = GameStatistics(
                        player_number=player_data['player_number'],
                        deck_id=player_data['deck_id'],
                        total_mana_spent=player_data['total_mana_spent'],
                        cards_played_total=player_data['cards_played_total'],
                        creatures_played=player_data['creatures_played'],
                        commander_casts=player_data['commander_casts'],
                        turns_survived=player_data['turns_survived'],
                        # Add the new fields from improved parser
                        artifacts_played=player_data.get('artifacts_played', 0),
                        enchantments_played=player_data.get('enchantments_played', 0),
                        instants_played=player_data.get('instants_played', 0),
                        sorceries_played=player_data.get('sorceries_played', 0),
                        planeswalkers_played=player_data.get('planeswalkers_played', 0),
                        lands_played=player_data.get('lands_played', 0),
                        cards_drawn=player_data.get('cards_drawn', 0),
                        total_mana_available=player_data.get('total_mana_available', 0),
                        land_drops_made=player_data.get('land_drops_made', 0),
                        land_drops_missed=player_data.get('land_drops_missed', 0),
                        damage_dealt=player_data.get('life_lost', 0),  # Approximate
                        final_life_total=40 - player_data.get('life_lost', 0),  # Estimate
                        # Functional categories
                        removal_spells_cast=player_data.get('removal_spells_cast', 0),
                        ramp_spells_cast=player_data.get('ramp_spells_cast', 0),
                        card_draw_spells_cast=player_data.get('card_draw_spells_cast', 0),
                        counterspells_cast=player_data.get('counterspells_cast', 0),
                        protection_spells_cast=player_data.get('protection_spells_cast', 0),
                        board_wipes_cast=player_data.get('board_wipes_cast', 0),
                        tribal_spells_cast=player_data.get('tribal_spells_cast', 0),
                        combo_pieces_cast=player_data.get('combo_pieces_cast', 0),
                        value_engines_cast=player_data.get('value_engines_cast', 0),
                        other_spells_cast=player_data.get('other_spells_cast', 0)
                    )
                    game_result.statistics.append(stats)
                
                return game_result, parsed_results
            else:
                logger.error(f"Game failed with return code {result.returncode}: {result.stderr}")
                return None, None
                
        except subprocess.TimeoutExpired:
            logger.error(f"Game {game_num} timed out after {timeout} seconds")
            return None, None
        except Exception as e:
            logger.error(f"Error running game {game_num}: {e}")
            return None, None
    
    def _run_games_sequential(self, simulation, deck_ids, sim_info):
        """Run games one by one (for distributed execution or small batches)."""
        for game_num in range(1, simulation.num_games + 1):
            # Check if stop was requested
            if sim_info.get('stop_requested', False):
                simulation.status = 'stopped'
                break
            
            try:
                # Run single game
                game_result, parsed_results = self._run_single_game(simulation, game_num, deck_ids)
                
                if game_result:
                    # Update progress
                    simulation.games_completed += 1
                    sim_info['games_completed'] = simulation.games_completed
                    
                    # Save game result
                    db.session.add(game_result)
                    db.session.flush()  # Flush to get the game_result.id
                    
                    # Save individual card plays
                    if parsed_results and 'card_plays' in parsed_results:
                        for card_play_data in parsed_results['card_plays']:
                            card_play = CardPlay(
                                game_id=game_result.id,
                                player_number=card_play_data['player_number'],
                                deck_id=card_play_data['deck_id'],
                                turn_number=card_play_data['turn_number'],
                                card_name=card_play_data['card_name'],
                                card_type=card_play_data['card_type'],
                                functional_category=card_play_data['functional_category'],
                                is_land=card_play_data['is_land'],
                                is_commander=card_play_data['is_commander'],
                                play_order=card_play_data['play_order'],
                                mana_cost=card_play_data['mana_cost']
                            )
                            db.session.add(card_play)
                    
                    db.session.commit()
                    
                    logger.info(f"Completed game {game_num}/{simulation.num_games} for simulation {simulation.id}")
                
            except Exception as e:
                logger.error(f"Error in game {game_num} of simulation {simulation.id}: {e}")
                if 'errors' not in sim_info:
                    sim_info['errors'] = []
                sim_info['errors'].append(str(e))
    
    def _run_games_batch(self, simulation, deck_ids, sim_info):
        """Run games in batches using the -n parameter for better performance."""
        remaining_games = simulation.num_games
        games_completed = 0
        
        while remaining_games > 0:
            # Check if stop was requested
            if sim_info.get('stop_requested', False):
                simulation.status = 'stopped'
                break
            
            # Determine batch size for this iteration
            current_batch_size = min(remaining_games, self.batch_size)
            
            try:
                # Run batch of games
                batch_results = self._run_game_batch(simulation, games_completed + 1, current_batch_size, deck_ids)
                
                if batch_results:
                    # Set simulation_id and save all results from this batch
                    for game_result in batch_results:
                        game_result.simulation_id = simulation.id
                        db.session.add(game_result)
                    
                    # Update progress
                    games_completed += len(batch_results)
                    simulation.games_completed = games_completed
                    sim_info['games_completed'] = games_completed
                    
                    db.session.commit()
                    
                    logger.info(f"Completed batch of {len(batch_results)} games ({games_completed}/{simulation.num_games}) for simulation {simulation.id}")
                    
                    remaining_games -= current_batch_size
                else:
                    # Batch failed, stop execution
                    logger.error(f"Batch execution failed for simulation {simulation.id}")
                    break
                
            except Exception as e:
                logger.error(f"Error in batch execution for simulation {simulation.id}: {e}")
                if 'errors' not in sim_info:
                    sim_info['errors'] = []
                sim_info['errors'].append(str(e))
                break
    
    def _run_game_batch(self, simulation, start_game_num, num_games, deck_ids):
        """Run a batch of games using the -n parameter."""
        # Build command with -n parameter
        cmd = [
            'java',
            f'-Dllm.endpoint={self.llm_endpoint}',
            f'-Dlang.dir={self.forge_languages_path}',
            '-jar', self.forge_jar_path,
            'sim',
            '-f', 'Commander',
            '-n', str(num_games),  # Number of games to run
            '-d'
        ]
        
        # Add deck IDs
        cmd.extend(deck_ids)
        
        # Add controller types (all AI for now)
        cmd.extend(['-c', ','.join(['ai'] * len(deck_ids))])
        
        # Set timeout (multiply by number of games with some buffer)
        timeout = simulation.game_timeout * num_games * 1.2  # 20% buffer
        
        try:
            # Log the command being executed
            logger.info(f"Executing batch command: {' '.join(cmd)}")
            logger.info(f"Batch size: {num_games} games")
            
            # Check prerequisites
            if not self._check_forge_prerequisites():
                return None
            
            # Run the batch command
            forge_main_dir = '/Users/geromepistre/projects/forge4'
            start_time = time.time()
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                cwd=forge_main_dir
            )
            
            duration = int(time.time() - start_time)
            
            # Parse results from batch output
            if result.returncode == 0:
                # Store the full output log
                full_log = result.stdout + "\n" + result.stderr if result.stderr else result.stdout
                
                # Parse multiple game results from the batch log
                batch_results = self._parse_batch_forge_output(full_log, deck_ids, start_game_num, num_games, duration)
                
                return batch_results
            else:
                logger.error(f"Batch execution failed with return code {result.returncode}: {result.stderr}")
                return None
                
        except subprocess.TimeoutExpired:
            logger.error(f"Batch execution timed out after {timeout} seconds")
            return None
        except Exception as e:
            logger.error(f"Error running batch: {e}")
            return None
    
    def _check_forge_prerequisites(self):
        """Check if Forge prerequisites are met."""
        # Check if jar file exists
        if not os.path.exists(self.forge_jar_path):
            logger.error(f"Forge JAR not found at: {self.forge_jar_path}")
            return False
        
        # Check if language directory exists
        if not os.path.exists(self.forge_languages_path):
            logger.error(f"Language directory not found at: {self.forge_languages_path}")
            return False
        
        # Check if en-US.properties exists
        en_us_file = os.path.join(self.forge_languages_path, 'en-US.properties')
        if not os.path.exists(en_us_file):
            logger.error(f"en-US.properties not found at: {en_us_file}")
            return False
        
        return True
    
    def _parse_batch_forge_output(self, output, deck_ids, start_game_num, num_games, total_duration):
        """Parse Forge batch output to extract multiple game results."""
        game_results = []
        
        # Split the log by game separators or use a different parsing strategy
        # For now, we'll try to identify individual games in the batch output
        lines = output.split('\n')
        
        current_game_lines = []
        game_count = 0
        
        for line in lines:
            current_game_lines.append(line)
            
            # Look for game completion indicators
            if 'Game outcome:' in line and 'has won' in line:
                # End of a game, process it
                game_log = '\n'.join(current_game_lines)
                parsed_results = self._parse_forge_output(game_log, deck_ids)
                
                # Estimate duration per game (rough approximation)
                game_duration = total_duration // num_games
                
                # Create game result
                game_result = GameResult(
                    simulation_id=None,  # Will be set by caller
                    game_number=start_game_num + game_count,
                    game_duration_seconds=game_duration,
                    game_log=game_log,
                    winner_player_number=parsed_results['winner_player_number'],
                    winner_deck_id=parsed_results['winner_deck_id'],
                    total_turns=parsed_results['total_turns'],
                    elimination_order=parsed_results['elimination_order']
                )
                
                # Create statistics for each player
                for player_data in parsed_results['player_statistics']:
                    stats = GameStatistics(
                        player_number=player_data['player_number'],
                        deck_id=player_data['deck_id'],
                        total_mana_spent=player_data['total_mana_spent'],
                        cards_played_total=player_data['cards_played_total'],
                        creatures_played=player_data['creatures_played'],
                        commander_casts=player_data['commander_casts'],
                        turns_survived=player_data['turns_survived'],
                        # Add the new fields from improved parser
                        artifacts_played=player_data.get('artifacts_played', 0),
                        enchantments_played=player_data.get('enchantments_played', 0),
                        instants_played=player_data.get('instants_played', 0),
                        sorceries_played=player_data.get('sorceries_played', 0),
                        planeswalkers_played=player_data.get('planeswalkers_played', 0),
                        lands_played=player_data.get('lands_played', 0),
                        cards_drawn=player_data.get('cards_drawn', 0),
                        total_mana_available=player_data.get('total_mana_available', 0),
                        land_drops_made=player_data.get('land_drops_made', 0),
                        land_drops_missed=player_data.get('land_drops_missed', 0),
                        damage_dealt=player_data.get('life_lost', 0),  # Approximate
                        final_life_total=40 - player_data.get('life_lost', 0),  # Estimate
                        # Functional categories
                        removal_spells_cast=player_data.get('removal_spells_cast', 0),
                        ramp_spells_cast=player_data.get('ramp_spells_cast', 0),
                        card_draw_spells_cast=player_data.get('card_draw_spells_cast', 0),
                        counterspells_cast=player_data.get('counterspells_cast', 0),
                        protection_spells_cast=player_data.get('protection_spells_cast', 0),
                        board_wipes_cast=player_data.get('board_wipes_cast', 0),
                        tribal_spells_cast=player_data.get('tribal_spells_cast', 0),
                        combo_pieces_cast=player_data.get('combo_pieces_cast', 0),
                        value_engines_cast=player_data.get('value_engines_cast', 0),
                        other_spells_cast=player_data.get('other_spells_cast', 0)
                    )
                    game_result.statistics.append(stats)
                
                game_results.append(game_result)
                game_count += 1
                current_game_lines = []  # Reset for next game
                
                # Stop if we've found all expected games
                if game_count >= num_games:
                    break
        
        # Set simulation_id for all results
        for result in game_results:
            result.simulation_id = None  # Will be set by caller
        
        logger.info(f"Parsed {len(game_results)} games from batch output")
        return game_results
    
    def _parse_forge_output(self, output, deck_ids):
        """Parse Forge simulation output to extract game statistics with improved robustness."""
        import re
        from collections import defaultdict
        
        logger.debug(f"Parsing Forge output for deck IDs: {deck_ids}")
        
        # Initialize tracking variables
        winner_player_number = None
        winner_deck_id = None
        total_turns = 0
        elimination_order = []
        lines = output.split('\n')
        
        # Player statistics tracking
        player_stats = defaultdict(lambda: {
            'mana_spent': 0,
            'mana_produced': 0,
            'total_mana_available': 0,
            'land_drops_made': 0,
            'land_drops_missed': 0,
            'cards_played': 0,
            'cards_drawn': 0,
            'creatures_played': 0,
            'artifacts_played': 0,
            'enchantments_played': 0,
            'planeswalkers_played': 0,
            'instants_played': 0,
            'sorceries_played': 0,
            'lands_played': 0,
            'commander_casts': 0,
            # Functional categories
            'removal_spells_cast': 0,
            'ramp_spells_cast': 0,
            'card_draw_spells_cast': 0,
            'counterspells_cast': 0,
            'protection_spells_cast': 0,
            'board_wipes_cast': 0,
            'tribal_spells_cast': 0,
            'combo_pieces_cast': 0,
            'value_engines_cast': 0,
            'other_spells_cast': 0,
            'life_gained': 0,
            'life_lost': 0,
            'turn_count': 0,
            'turns_survived': 0,
            'lands_per_turn': {},  # Track land drops per turn
            'mana_per_turn': {}    # Track mana usage per turn
        })
        
        current_turn = 0
        current_turn_player = None
        players_alive = set(range(1, len(deck_ids) + 1))
        players_eliminated = []
        
        # Track card plays for detailed recording
        card_plays = []
        play_order_counter = 0
        
        # More comprehensive parsing patterns
        patterns = {
            'winner': [
                r'Game outcome: Ai\((\d+)\)-(.*?) has won',
                r'Game Result:.*Ai\((\d+)\)-(.*?) has won!',
                r'Ai\((\d+)\)-.* wins the game',
                r'Player (\d+) \((.*?)\) has won'
            ],
            'turn_start': [
                r'Turn: Turn (\d+) \(Ai\((\d+)\)',
                r'Turn (\d+) \(Ai\((\d+)\)',
                r'Turn (\d+): Ai\((\d+)\)',
                r'--- Turn (\d+) - Player (\d+)'
            ],
            'total_turns': [
                r'Game outcome: Turn (\d+)',
                r'Game ended on turn (\d+)',
                r'Total turns: (\d+)'
            ],
            'spell_cast': [
                r'Add to stack: Ai\((\d+)\)-.* cast (.+?) \(\d+\)$',
                r'Add to stack: Ai\((\d+)\)-.* cast (.+?)(?:\s+targeting|$)',
                r'Ai\((\d+)\)-.* casts (.+?) \(\d+\)$',
                r'Ai\((\d+)\)-.* casts (.+?)(?:\s+targeting|$)',
                r'Player (\d+) casts (.+?) \(\d+\)$',
                r'Player (\d+) casts (.+?)(?:\s+targeting|$)'
            ],
            'land_play': [
                r'Land: Ai\((\d+)\)-.* played (.+) \(\d+\)$',
                r'Land: Player (\d+) played (.+) \(\d+\)$'
            ],
            'mana_ability': [
                r'Mana: (.+) - \{T\}: Add (\{[^}]+\}|\{[^}]+\}\{[^}]+\})',
                r'Mana: (.+) - Add (\{[^}]+\})',
                r'Ai\((\d+)\)-.* adds? (\{[^}]+\}|\d+) to (?:their )?mana pool',
                r'Player (\d+) adds? (\{[^}]+\}|\d+)',
                r'Ai\((\d+)\)-.* taps .* for (\{[^}]+\}|\d+)',
                r'Ai\((\d+)\)-.* activates? .* Add (\{[^}]+\}|\d+)'
            ],
            'mana_spent': [
                r'Ai\((\d+)\)-.* pays? (\{[^}]+\}|\d+)',
                r'Ai\((\d+)\)-.* spends? (\{[^}]+\}|\d+)',
                r'Cost: (\{[^}]+\}|\d+) .*Ai\((\d+)\)'
            ],
            'card_draw': [
                r'Ai\((\d+)\)-.* draws? (\d+) cards?',
                r'Player (\d+) draws? (\d+) cards?',
                r'Draw: Ai\((\d+)\)-.* draws? (\d+)?',
                r'Ai\((\d+)\)-.* draw(?:s)? a card',
                r'Draw phase: Ai\((\d+)\)-.* draws? (\d+)?',
                r'Phase: Ai\((\d+)\)-.*\'s Draw step'
            ],
            'land_drop': [
                r'Main Phase: Ai\((\d+)\)-.* played (.+) \(\d+\)$',
                r'Land drop: Ai\((\d+)\)-.* plays (.+)',
                r'Ai\((\d+)\)-.* plays (.+) as land drop'
            ],
            'life_change': [
                r'Ai\((\d+)\)-.* gains? (\d+) life',
                r'Ai\((\d+)\)-.* loses? (\d+) life',
                r'Player (\d+) (?:gains?|loses?) (\d+) life'
            ],
            'player_loss': [
                r'Ai\((\d+)\)-.* has lost',
                r'Ai\((\d+)\)-.* loses the game',
                r'Player (\d+) has been eliminated',
                r'Ai\((\d+)\)-.* life total reached 0'
            ]
        }
        
        # Process each line
        for line_num, line in enumerate(lines):
            line = line.strip()
            if not line:
                continue
            
            # Track current turn and player
            for pattern in patterns['turn_start']:
                match = re.search(pattern, line)
                if match:
                    turn_num = int(match.group(1))
                    player_num = int(match.group(2))
                    
                    # Update current tracking
                    current_turn = turn_num
                    current_turn_player = player_num
                    
                    # Calculate the actual "round" number
                    # In MTG: Turn 1 = Player 1's first turn, Turn 2 = Player 2's first turn, etc.
                    # Round 1 = Turns 1-N (where N = number of players)
                    # Round 2 = Turns (N+1)-(2N), etc.
                    num_players = len(deck_ids)
                    actual_round = ((turn_num - 1) // num_players) + 1
                    
                    # Set the turn count for this player to the current round
                    if player_num in player_stats:
                        player_stats[player_num]['turn_count'] = actual_round
                    
                    break
            
            # Check for winner
            for pattern in patterns['winner']:
                match = re.search(pattern, line)
                if match:
                    winner_player_number = int(match.group(1))
                    if len(match.groups()) > 1:
                        winner_deck_name = match.group(2).strip()
                        # Map to deck_id
                        winner_deck_id = self._find_deck_id_by_name(winner_deck_name, deck_ids)
                    if not winner_deck_id:
                        winner_deck_id = deck_ids[winner_player_number - 1] if winner_player_number <= len(deck_ids) else deck_ids[0]
                    logger.debug(f"Found winner: Player {winner_player_number}, Deck: {winner_deck_id}")
                    break
            
            # Check for total turns
            for pattern in patterns['total_turns']:
                match = re.search(pattern, line)
                if match:
                    total_turns = int(match.group(1))
                    break
            
            # Track player eliminations
            for pattern in patterns['player_loss']:
                match = re.search(pattern, line)
                if match:
                    eliminated_player = int(match.group(1))
                    if eliminated_player in players_alive:
                        players_alive.remove(eliminated_player)
                        players_eliminated.append(eliminated_player)
                        player_stats[eliminated_player]['turns_survived'] = current_turn
                        logger.debug(f"Player {eliminated_player} eliminated on turn {current_turn}")
                    break
            
            # Track spell casts
            for pattern in patterns['spell_cast']:
                match = re.search(pattern, line)
                if match:
                    player_num = int(match.group(1))
                    spell_name = match.group(2).strip() if len(match.groups()) > 1 else ""
                    
                    player_stats[player_num]['cards_played'] += 1
                    
                    # Use improved card classification
                    card_classification = self._classify_card_type(spell_name)
                    
                    # Count cards in specific categories (note: some cards may have multiple types)
                    card_categorized = False
                    
                    if card_classification['is_creature']:
                        player_stats[player_num]['creatures_played'] += 1
                        card_categorized = True
                    
                    if card_classification['is_artifact']:
                        player_stats[player_num]['artifacts_played'] += 1
                        card_categorized = True
                    
                    if card_classification['is_enchantment']:
                        player_stats[player_num]['enchantments_played'] += 1
                        card_categorized = True
                    
                    if card_classification['is_planeswalker']:
                        player_stats[player_num]['planeswalkers_played'] += 1
                        card_categorized = True
                    
                    if card_classification['is_instant']:
                        player_stats[player_num]['instants_played'] += 1
                        card_categorized = True
                    
                    if card_classification['is_sorcery']:
                        player_stats[player_num]['sorceries_played'] += 1
                        card_categorized = True
                    
                    # Handle battles and other card types
                    if card_classification['is_battle']:
                        # Battles are a newer card type - count them as spells for now
                        player_stats[player_num]['sorceries_played'] += 1
                        card_categorized = True
                    
                    # If card wasn't categorized at all, log it for debugging
                    if not card_categorized:
                        logger.debug(f"Uncategorized spell cast: {spell_name} by player {player_num}")
                    
                    # Classify by functional category
                    functional_classification = self._classify_spell_function(spell_name)
                    
                    if functional_classification['is_removal']:
                        player_stats[player_num]['removal_spells_cast'] += 1
                    
                    if functional_classification['is_ramp']:
                        player_stats[player_num]['ramp_spells_cast'] += 1
                    
                    if functional_classification['is_card_draw']:
                        player_stats[player_num]['card_draw_spells_cast'] += 1
                    
                    if functional_classification['is_counterspell']:
                        player_stats[player_num]['counterspells_cast'] += 1
                    
                    if functional_classification['is_protection']:
                        player_stats[player_num]['protection_spells_cast'] += 1
                    
                    if functional_classification['is_board_wipe']:
                        player_stats[player_num]['board_wipes_cast'] += 1
                    
                    if functional_classification['is_tribal']:
                        player_stats[player_num]['tribal_spells_cast'] += 1
                    
                    if functional_classification['is_combo_piece']:
                        player_stats[player_num]['combo_pieces_cast'] += 1
                    
                    if functional_classification['is_value_engine']:
                        player_stats[player_num]['value_engines_cast'] += 1
                    
                    if functional_classification['is_other']:
                        player_stats[player_num]['other_spells_cast'] += 1
                    
                    # Record individual card play
                    play_order_counter += 1
                    card_play_record = self._record_card_play(
                        spell_name, player_num, deck_ids[player_num - 1],
                        current_turn, play_order_counter, is_land=False
                    )
                    card_plays.append(card_play_record)
                    
                    # Check if it's a commander cast
                    spell_lower = spell_name.lower()
                    if 'general' in spell_lower or any(cmd in spell_lower for cmd in ['chatterfang', 'commander']):
                        player_stats[player_num]['commander_casts'] += 1
                    
                    break
            
            # Track land plays
            for pattern in patterns['land_play']:
                match = re.search(pattern, line)
                if match:
                    player_num = int(match.group(1))
                    land_name = match.group(2).strip() if len(match.groups()) > 1 else ""
                    
                    # Remove the number in parentheses if present
                    land_name = re.sub(r'\s*\(\d+\)$', '', land_name)
                    
                    if player_num and player_num in player_stats:
                        # Always count land plays since the log explicitly says "Land: ... played ..."
                        player_stats[player_num]['lands_played'] += 1
                        player_stats[player_num]['cards_played'] += 1
                        
                        # Optionally verify it's a land using the card database for validation
                        card_classification = self._classify_card_type(land_name)
                        if not card_classification['is_land']:
                            # Log unexpected non-land in land play (but still count it)
                            logger.debug(f"Unexpected non-land in land play: {land_name}")
                        
                        # Record individual card play
                        play_order_counter += 1
                        card_play_record = self._record_card_play(
                            land_name, player_num, deck_ids[player_num - 1],
                            current_turn, play_order_counter, is_land=True
                        )
                        card_plays.append(card_play_record)
                    
                    break
            
            # Track mana abilities (mana production)
            for pattern in patterns['mana_ability']:
                match = re.search(pattern, line)
                if match:
                    # For new format "Mana: Card Name - {T}: Add {C}{C}"
                    if pattern.startswith(r'Mana: (.+) -'):
                        card_name = match.group(1)
                        mana_value = match.group(2) if len(match.groups()) > 1 else "1"
                        # Determine player from current turn or context
                        player_num = current_turn_player if current_turn_player else 1
                    else:
                        # For older patterns with explicit player numbers
                        player_num = int(match.group(1))
                        mana_value = match.group(2) if len(match.groups()) > 1 else "1"
                    
                    mana_amount = self._parse_mana_value(mana_value)
                    
                    player_stats[player_num]['mana_produced'] += mana_amount
                    player_stats[player_num]['total_mana_available'] += mana_amount
                    
                    # Track mana per turn
                    if current_turn not in player_stats[player_num]['mana_per_turn']:
                        player_stats[player_num]['mana_per_turn'][current_turn] = 0
                    player_stats[player_num]['mana_per_turn'][current_turn] += mana_amount
                    break
            
            # Track mana spent
            for pattern in patterns['mana_spent']:
                match = re.search(pattern, line)
                if match:
                    if len(match.groups()) == 2:
                        player_num = int(match.group(1))
                        mana_value = match.group(2)
                    else:
                        # Pattern where cost comes first
                        mana_value = match.group(1)
                        player_num = int(match.group(2))
                    
                    mana_amount = self._parse_mana_value(mana_value)
                    player_stats[player_num]['mana_spent'] += mana_amount
                    break
            
            # Track card draws
            for pattern in patterns['card_draw']:
                match = re.search(pattern, line)
                if match:
                    player_num = int(match.group(1))
                    cards_drawn = 1
                    
                    # Handle different patterns
                    if pattern.endswith("Draw step"):
                        # Draw step - assume 1 card drawn
                        cards_drawn = 1
                    elif len(match.groups()) > 1 and match.group(2):
                        if match.group(2).isdigit():
                            cards_drawn = int(match.group(2))
                        # Handle phrases like "a card"
                        elif "card" in match.group(2).lower():
                            cards_drawn = 1
                    
                    player_stats[player_num]['cards_drawn'] += cards_drawn
                    break
            
            # Track land drops
            for pattern in patterns['land_drop']:
                match = re.search(pattern, line)
                if match:
                    player_num = int(match.group(1))
                    land_name = match.group(2).strip() if len(match.groups()) > 1 else ""
                    
                    # Track land drops per turn
                    if current_turn not in player_stats[player_num]['lands_per_turn']:
                        player_stats[player_num]['lands_per_turn'][current_turn] = 0
                    
                    player_stats[player_num]['lands_per_turn'][current_turn] += 1
                    player_stats[player_num]['land_drops_made'] += 1
                    break
            
            # Track life changes
            for pattern in patterns['life_change']:
                match = re.search(pattern, line)
                if match:
                    player_num = int(match.group(1))
                    life_amount = int(match.group(2)) if len(match.groups()) > 1 else 1
                    
                    if 'gains' in line.lower():
                        player_stats[player_num]['life_gained'] += life_amount
                    elif 'loses' in line.lower():
                        player_stats[player_num]['life_lost'] += life_amount
                    break
        
        # Set turns survived for winner and any remaining players
        for player_num in players_alive:
            player_stats[player_num]['turns_survived'] = total_turns or current_turn
        
        # Calculate missed land drops (estimate: 1 land drop per turn - lands played)
        for player_num in range(1, len(deck_ids) + 1):
            turns_played = player_stats[player_num]['turns_survived']
            lands_dropped = len(player_stats[player_num]['lands_per_turn'])
            
            # Estimate missed land drops (conservative: turns - actual land drops)
            estimated_missed = max(0, turns_played - lands_dropped) if turns_played > 0 else 0
            player_stats[player_num]['land_drops_missed'] = estimated_missed
        
        # Build elimination order (first eliminated to last)
        elimination_order = players_eliminated.copy()
        if winner_player_number and winner_player_number not in elimination_order:
            # Add any remaining players, with winner last
            for player_num in range(1, len(deck_ids) + 1):
                if player_num not in elimination_order and player_num != winner_player_number:
                    elimination_order.append(player_num)
            elimination_order.append(winner_player_number)
        
        # Convert to expected format
        player_statistics = []
        for i, deck_id in enumerate(deck_ids):
            player_number = i + 1
            stats = player_stats[player_number]
            
            player_statistics.append({
                'player_number': player_number,
                'deck_id': deck_id,
                'total_mana_spent': stats['mana_spent'],
                'total_mana_available': stats['total_mana_available'],
                'land_drops_made': stats['land_drops_made'],
                'land_drops_missed': stats['land_drops_missed'],
                'cards_drawn': stats['cards_drawn'],
                'cards_played_total': stats['cards_played'],
                'creatures_played': stats['creatures_played'],
                'commander_casts': max(1, stats['commander_casts']),  # Assume at least 1
                'turns_survived': max(1, stats['turns_survived']),
                # Additional stats that are now saved to database
                'artifacts_played': stats['artifacts_played'],
                'enchantments_played': stats['enchantments_played'],
                'instants_played': stats['instants_played'],
                'sorceries_played': stats['sorceries_played'],
                'planeswalkers_played': stats['planeswalkers_played'],
                'lands_played': stats['lands_played'],
                'mana_produced': stats['mana_produced'],
                'life_gained': stats['life_gained'],
                'life_lost': stats['life_lost'],
                # Functional categories
                'removal_spells_cast': stats['removal_spells_cast'],
                'ramp_spells_cast': stats['ramp_spells_cast'],
                'card_draw_spells_cast': stats['card_draw_spells_cast'],
                'counterspells_cast': stats['counterspells_cast'],
                'protection_spells_cast': stats['protection_spells_cast'],
                'board_wipes_cast': stats['board_wipes_cast'],
                'tribal_spells_cast': stats['tribal_spells_cast'],
                'combo_pieces_cast': stats['combo_pieces_cast'],
                'value_engines_cast': stats['value_engines_cast'],
                'other_spells_cast': stats['other_spells_cast']
            })
        
        result = {
            'winner_player_number': winner_player_number or 1,
            'winner_deck_id': winner_deck_id or deck_ids[0],
            'total_turns': max(1, total_turns or current_turn),
            'elimination_order': elimination_order or list(range(1, len(deck_ids) + 1)),
            'player_statistics': player_statistics,
            'card_plays': card_plays  # Add card plays for detailed recording
        }
        
        logger.debug(f"Parsed result: Winner P{result['winner_player_number']}, Turns: {result['total_turns']}")
        return result
    
    def _find_deck_id_by_name(self, deck_name, deck_ids):
        """Helper to find deck_id by matching deck name."""
        deck_name_clean = deck_name.lower().strip()
        
        # Try exact matches first
        for deck_id in deck_ids:
            if deck_name_clean in deck_id.lower() or deck_id.lower() in deck_name_clean:
                return deck_id
        
        # Try partial matches
        for deck_id in deck_ids:
            deck_parts = deck_id.lower().split('-')
            if any(part in deck_name_clean for part in deck_parts if len(part) > 3):
                return deck_id
        
        return None