from flask import Blueprint, render_template, jsonify, current_app
from flask_login import login_required, current_user
from ..models.simulation import Simulation
from ..services.forge_interface import ForgeInterface

monitoring_bp = Blueprint('monitoring', __name__)

@monitoring_bp.route('/')
@login_required
def monitoring_dashboard():
    """Main monitoring dashboard."""
    running_simulations = Simulation.query.filter_by(status='running').all()
    user_simulations = [s for s in running_simulations if s.user_id == current_user.id]
    
    return render_template('monitoring/dashboard.html', 
                         running_simulations=running_simulations,
                         user_simulations=user_simulations)

@monitoring_bp.route('/simulation/<simulation_id>')
@login_required
def simulation_status(simulation_id):
    """Detailed status for a specific simulation."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        return "Unauthorized", 403
    
    return render_template('monitoring/simulation_detail.html', simulation=simulation)

@monitoring_bp.route('/api/simulation/<simulation_id>/status')
@login_required
def api_simulation_status(simulation_id):
    """API endpoint for simulation status updates."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        return jsonify({'error': 'Unauthorized'}), 403
    
    # Get current status from Forge interface
    forge_interface = ForgeInterface(current_app._get_current_object())
    status = forge_interface.get_simulation_status(simulation)
    
    return jsonify({
        'id': simulation.id,
        'name': simulation.name,
        'status': simulation.status,
        'games_completed': status.get('games_completed', 0),
        'total_games': simulation.num_games,
        'progress_percent': (status.get('games_completed', 0) / simulation.num_games) * 100,
        'estimated_completion': status.get('estimated_completion'),
        'errors': status.get('errors', [])
    })

@monitoring_bp.route('/api/system/status')
@login_required
def api_system_status():
    """API endpoint for overall system status."""
    forge_interface = ForgeInterface(current_app._get_current_object())
    system_status = forge_interface.get_system_status()
    
    return jsonify({
        'concurrent_simulations': system_status.get('concurrent_simulations', 0),
        'max_concurrent': 10,  # From specs
        'queue_size': system_status.get('queue_size', 0),
        'system_resources': system_status.get('resources', {})
    })