import subprocess
import os
import threading
import time
import logging
from datetime import datetime
from flask import current_app
from ..models.simulation import Simulation, GameResult
from ..models.statistics import GameStatistics
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
        if self.app:
            with self.app.app_context():
                # Re-fetch the simulation object within the app context
                simulation = Simulation.query.get(simulation_id)
                if simulation:
                    self._run_simulation(simulation)
        else:
            # Fallback to current_app if app not provided
            with current_app.app_context():
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
                        turns_survived=player_data['turns_survived']
                    )
                    game_result.statistics.append(stats)
                
                return game_result
            else:
                logger.error(f"Game failed with return code {result.returncode}: {result.stderr}")
                return None
                
        except subprocess.TimeoutExpired:
            logger.error(f"Game {game_num} timed out after {timeout} seconds")
            return None
        except Exception as e:
            logger.error(f"Error running game {game_num}: {e}")
            return None
    
    def _run_games_sequential(self, simulation, deck_ids, sim_info):
        """Run games one by one (for distributed execution or small batches)."""
        for game_num in range(1, simulation.num_games + 1):
            # Check if stop was requested
            if sim_info.get('stop_requested', False):
                simulation.status = 'stopped'
                break
            
            try:
                # Run single game
                game_result = self._run_single_game(simulation, game_num, deck_ids)
                
                if game_result:
                    # Update progress
                    simulation.games_completed += 1
                    sim_info['games_completed'] = simulation.games_completed
                    
                    # Save game result
                    db.session.add(game_result)
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
                        turns_survived=player_data['turns_survived']
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
        """Parse Forge simulation output to extract game statistics."""
        import re
        
        # Initialize defaults
        winner_player_number = None
        winner_deck_id = None
        total_turns = 0
        elimination_order = []
        
        # Parse the log line by line
        lines = output.split('\n')
        
        # Extract winner information
        for line in lines:
            # Look for winner declaration: "Game outcome: Ai(1)-deck_name has won because all opponents have lost"
            winner_match = re.search(r'Game outcome: Ai\((\d+)\)-(.*?) has won', line)
            if winner_match:
                winner_player_number = int(winner_match.group(1))
                winner_deck_name = winner_match.group(2).strip()
                # Find the corresponding deck_id
                for i, deck_id in enumerate(deck_ids):
                    if deck_id in winner_deck_name or winner_deck_name in deck_id:
                        winner_deck_id = deck_id
                        break
                if not winner_deck_id:
                    winner_deck_id = deck_ids[winner_player_number - 1] if winner_player_number <= len(deck_ids) else deck_ids[0]
                continue
            
            # Alternative winner pattern: "Game Result: Game X ended in Y ms. Ai(Z)-deck_name has won!"
            winner_match2 = re.search(r'Game Result:.*Ai\((\d+)\)-(.*?) has won!', line)
            if winner_match2:
                winner_player_number = int(winner_match2.group(1))
                winner_deck_name = winner_match2.group(2).strip()
                # Find the corresponding deck_id
                for i, deck_id in enumerate(deck_ids):
                    if deck_id in winner_deck_name or winner_deck_name in deck_id:
                        winner_deck_id = deck_id
                        break
                if not winner_deck_id:
                    winner_deck_id = deck_ids[winner_player_number - 1] if winner_player_number <= len(deck_ids) else deck_ids[0]
                continue
            
            # Extract turn count: "Game outcome: Turn X"
            turn_match = re.search(r'Game outcome: Turn (\d+)', line)
            if turn_match:
                total_turns = int(turn_match.group(1))
                continue
        
        # Count various statistics from the log
        player_statistics = []
        for i, deck_id in enumerate(deck_ids):
            player_number = i + 1
            
            # Count spells cast by this player
            spells_pattern = rf'Add to stack: Ai\({player_number}\)-.*? cast '
            cards_played = len(re.findall(spells_pattern, output))
            
            # Count creatures played (this is approximate - looking for creature types in cast spells)
            # We'll use a simple heuristic based on common creature names
            creatures_pattern = rf'Add to stack: Ai\({player_number}\)-.*? cast .*?(Creature|Elf|Shaman|General|Dragon|Beast|Token)'
            creatures_played = len(re.findall(creatures_pattern, output))
            
            # Count commander casts (looking for "General" or the specific commander name in cast spells)
            commander_pattern = rf'Add to stack: Ai\({player_number}\)-.*? cast .*?(General|Chatterfang)'
            commander_casts = len(re.findall(commander_pattern, output))
            
            # Count mana activations (rough estimate of mana spent)
            mana_pattern = rf'Mana: .* - .*Add.*\.'
            # This is a rough estimate - count all mana activations and divide by number of players
            total_mana_activations = len(re.findall(mana_pattern, output))
            estimated_mana_spent = total_mana_activations // len(deck_ids)  # Rough estimate
            
            # Calculate turns survived - if this player won, they survived all turns
            if winner_player_number == player_number:
                turns_survived = total_turns
            else:
                # Look for elimination/loss messages for this player
                loss_patterns = [
                    rf'Ai\({player_number}\)-[^h]+ has lost',
                    rf'Ai\({player_number}\)-[^h]+ life total reached 0'
                ]
                # For now, assume non-winners survived until the last turn
                turns_survived = max(1, total_turns - 1)
            
            player_statistics.append({
                'player_number': player_number,
                'deck_id': deck_id,
                'total_mana_spent': estimated_mana_spent,
                'cards_played_total': cards_played,
                'creatures_played': creatures_played,
                'commander_casts': max(1, commander_casts),  # Assume at least 1 cast
                'turns_survived': turns_survived
            })
        
        # Build elimination order (this is a simplified approach)
        if winner_player_number:
            elimination_order = list(range(1, len(deck_ids) + 1))
            elimination_order.remove(winner_player_number)
            elimination_order.append(winner_player_number)  # Winner is last to be "eliminated"
        else:
            elimination_order = list(range(1, len(deck_ids) + 1))
        
        return {
            'winner_player_number': winner_player_number or 1,  # Default to player 1 if not found
            'winner_deck_id': winner_deck_id or deck_ids[0],    # Default to first deck if not found
            'total_turns': max(1, total_turns),                 # Ensure at least 1 turn
            'elimination_order': elimination_order,
            'player_statistics': player_statistics
        }