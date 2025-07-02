"""
REST API Routes
Simple REST API for core server functions.
"""

from flask import Blueprint, request, jsonify
from flask_login import login_required, current_user
import logging
from datetime import datetime
from ..models.simulation import Simulation, SimulationPlayer, GameResult
from ..models.statistics import GameStatistics
from ..services.deck_scraper import DeckScraper
from ..services.bigquery_client import BigQueryClient
from ..services.forge_interface import ForgeInterface
from ..services.statistics_engine import StatisticsEngine
from .. import db

logger = logging.getLogger(__name__)

api_bp = Blueprint('api', __name__)

# Initialize services (lazy-loaded with current app)
def get_forge_interface():
    """Get ForgeInterface instance with current app context."""
    from flask import current_app
    return ForgeInterface(current_app._get_current_object())

def get_deck_scraper():
    """Get DeckScraper instance."""
    return DeckScraper()

def get_bq_client():
    """Get BigQueryClient instance."""
    return BigQueryClient()

def get_stats_engine():
    """Get StatisticsEngine instance."""
    return StatisticsEngine()

@api_bp.route('/decks', methods=['GET'])
@login_required
def list_decks():
    """List available decks with optional filtering."""
    try:
        query = request.args.get('q', '')
        commander = request.args.get('commander', '')
        colors = request.args.get('colors', '')
        limit = int(request.args.get('limit', 20))
        offset = int(request.args.get('offset', 0))
        
        decks = get_bq_client().search_decks(
            query=query,
            commander=commander,
            colors=colors,
            limit=limit,
            offset=offset
        )
        
        return jsonify({
            'success': True,
            'decks': decks,
            'count': len(decks),
            'limit': limit,
            'offset': offset
        })
        
    except Exception as e:
        logger.error(f"Error listing decks: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/decks/<deck_id>', methods=['GET'])
@login_required
def get_deck(deck_id):
    """Get detailed information about a specific deck."""
    try:
        deck = get_bq_client().get_deck(deck_id)
        
        if not deck:
            return jsonify({
                'success': False,
                'error': 'Deck not found'
            }), 404
        
        return jsonify({
            'success': True,
            'deck': deck
        })
        
    except Exception as e:
        logger.error(f"Error getting deck {deck_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/decks', methods=['POST'])
@login_required
def add_deck():
    """Add a new deck by URL or deck data."""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({
                'success': False,
                'error': 'No data provided'
            }), 400
        
        # Check if URL is provided (import from external site)
        if 'url' in data:
            url = data['url']
            result = get_deck_scraper().scrape_and_upload(url)
            
            return jsonify({
                'success': result['success'],
                'deck_id': result.get('deck_id'),
                'deck_name': result.get('deck_name'),
                'error': result.get('error')
            })
        
        # TODO: Handle direct deck data upload
        else:
            return jsonify({
                'success': False,
                'error': 'Direct deck upload not implemented yet'
            }), 501
            
    except Exception as e:
        logger.error(f"Error adding deck: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations', methods=['GET'])
@login_required
def list_simulations():
    """List user's simulations."""
    try:
        limit = int(request.args.get('limit', 50))
        offset = int(request.args.get('offset', 0))
        status = request.args.get('status', '')
        
        query = Simulation.query.filter_by(user_id=current_user.id)
        
        if status:
            query = query.filter_by(status=status)
        
        simulations = query.order_by(Simulation.created_at.desc()).offset(offset).limit(limit).all()
        
        result = []
        for sim in simulations:
            sim_data = {
                'id': sim.id,
                'name': sim.name,
                'num_games': sim.num_games,
                'games_completed': sim.games_completed,
                'status': sim.status,
                'created_at': sim.created_at.isoformat() if sim.created_at else None,
                'started_at': sim.started_at.isoformat() if sim.started_at else None,
                'completed_at': sim.completed_at.isoformat() if sim.completed_at else None,
                'progress_percent': sim.progress_percent,
                'players': [
                    {
                        'player_number': p.player_number,
                        'deck_id': p.deck_id
                    } for p in sim.players
                ]
            }
            result.append(sim_data)
        
        return jsonify({
            'success': True,
            'simulations': result,
            'count': len(result),
            'limit': limit,
            'offset': offset
        })
        
    except Exception as e:
        logger.error(f"Error listing simulations: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations', methods=['POST'])
@login_required
def create_simulation():
    """Create a new simulation."""
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({
                'success': False,
                'error': 'No data provided'
            }), 400
        
        # Validate required fields
        required_fields = ['name', 'num_games', 'players']
        for field in required_fields:
            if field not in data:
                return jsonify({
                    'success': False,
                    'error': f'Missing required field: {field}'
                }), 400
        
        # Validate players
        players = data['players']
        if len(players) < 2:
            return jsonify({
                'success': False,
                'error': 'At least 2 players required'
            }), 400
        
        if len(players) > 4:
            return jsonify({
                'success': False,
                'error': 'Maximum 4 players allowed'
            }), 400
        
        # Create simulation
        simulation = Simulation(
            name=data['name'],
            user_id=current_user.id,
            num_games=data['num_games'],
            game_timeout=data.get('game_timeout', 600),
            status='pending'
        )
        
        db.session.add(simulation)
        db.session.flush()  # Get simulation ID
        
        # Add players
        for i, player_data in enumerate(players):
            if 'deck_id' not in player_data:
                return jsonify({
                    'success': False,
                    'error': f'Missing deck_id for player {i+1}'
                }), 400
            
            player = SimulationPlayer(
                simulation_id=simulation.id,
                player_number=i + 1,
                deck_id=player_data['deck_id']
            )
            db.session.add(player)
        
        db.session.commit()
        
        # Optionally start the simulation immediately
        if data.get('start_immediately', False):
            get_forge_interface().start_simulation(simulation)
        
        return jsonify({
            'success': True,
            'simulation_id': simulation.id,
            'status': simulation.status,
            'message': 'Simulation created successfully'
        })
        
    except Exception as e:
        logger.error(f"Error creating simulation: {e}")
        db.session.rollback()
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>', methods=['GET'])
@login_required
def get_simulation(simulation_id):
    """Get detailed simulation information."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        sim_data = {
            'id': simulation.id,
            'name': simulation.name,
            'num_games': simulation.num_games,
            'games_completed': simulation.games_completed,
            'status': simulation.status,
            'created_at': simulation.created_at.isoformat() if simulation.created_at else None,
            'started_at': simulation.started_at.isoformat() if simulation.started_at else None,
            'completed_at': simulation.completed_at.isoformat() if simulation.completed_at else None,
            'game_timeout': simulation.game_timeout,
            'progress_percent': simulation.progress_percent,
            'error_message': simulation.error_message,
            'players': [
                {
                    'player_number': p.player_number,
                    'deck_id': p.deck_id
                } for p in simulation.players
            ]
        }
        
        return jsonify({
            'success': True,
            'simulation': sim_data
        })
        
    except Exception as e:
        logger.error(f"Error getting simulation {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>/status', methods=['GET'])
@login_required
def get_simulation_status(simulation_id):
    """Get current simulation status."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        # Get status from ForgeInterface
        status_info = get_forge_interface().get_simulation_status(simulation)
        
        return jsonify({
            'success': True,
            'simulation_id': simulation_id,
            'status': status_info
        })
        
    except Exception as e:
        logger.error(f"Error getting simulation status {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>/start', methods=['POST'])
@login_required
def start_simulation(simulation_id):
    """Start a simulation."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        if simulation.status != 'pending':
            return jsonify({
                'success': False,
                'error': f'Cannot start simulation with status: {simulation.status}'
            }), 400
        
        get_forge_interface().start_simulation(simulation)
        
        return jsonify({
            'success': True,
            'simulation_id': simulation_id,
            'status': 'running',
            'message': 'Simulation started successfully'
        })
        
    except Exception as e:
        logger.error(f"Error starting simulation {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>/stop', methods=['POST'])
@login_required
def stop_simulation(simulation_id):
    """Stop a running simulation."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        if simulation.status != 'running':
            return jsonify({
                'success': False,
                'error': f'Cannot stop simulation with status: {simulation.status}'
            }), 400
        
        get_forge_interface().stop_simulation(simulation)
        
        return jsonify({
            'success': True,
            'simulation_id': simulation_id,
            'message': 'Simulation stop requested'
        })
        
    except Exception as e:
        logger.error(f"Error stopping simulation {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>/stats', methods=['GET'])
@login_required
def get_simulation_stats(simulation_id):
    """Get detailed simulation statistics."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        # Get comprehensive statistics
        stats = get_stats_engine().get_simulation_statistics(simulation_id)
        
        if not stats:
            return jsonify({
                'success': False,
                'error': 'No statistics available for this simulation'
            }), 404
        
        return jsonify({
            'success': True,
            'simulation_id': simulation_id,
            'statistics': stats
        })
        
    except Exception as e:
        logger.error(f"Error getting simulation stats {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/simulations/<simulation_id>/charts', methods=['GET'])
@login_required
def get_simulation_charts(simulation_id):
    """Get chart data for simulation visualization."""
    try:
        simulation = Simulation.query.filter_by(
            id=simulation_id,
            user_id=current_user.id
        ).first()
        
        if not simulation:
            return jsonify({
                'success': False,
                'error': 'Simulation not found'
            }), 404
        
        # Get chart data
        chart_data = get_stats_engine().get_chart_data(simulation_id)
        
        return jsonify({
            'success': True,
            'simulation_id': simulation_id,
            'charts': chart_data
        })
        
    except Exception as e:
        logger.error(f"Error getting simulation charts {simulation_id}: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@api_bp.route('/system/status', methods=['GET'])
@login_required
def get_system_status():
    """Get overall system status."""
    try:
        status = get_forge_interface().get_system_status()
        
        return jsonify({
            'success': True,
            'system_status': status
        })
        
    except Exception as e:
        logger.error(f"Error getting system status: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

# Error handlers for the API
@api_bp.errorhandler(404)
def api_not_found(error):
    return jsonify({
        'success': False,
        'error': 'API endpoint not found'
    }), 404

@api_bp.errorhandler(405)
def api_method_not_allowed(error):
    return jsonify({
        'success': False,
        'error': 'Method not allowed'
    }), 405

@api_bp.errorhandler(500)
def api_internal_error(error):
    return jsonify({
        'success': False,
        'error': 'Internal server error'
    }), 500