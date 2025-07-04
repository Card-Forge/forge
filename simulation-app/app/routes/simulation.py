from flask import Blueprint, render_template, request, redirect, url_for, flash, jsonify, current_app
from flask_login import login_required, current_user
from ..models.simulation import Simulation, SimulationPlayer
from ..services.forge_interface import ForgeInterface
from ..services.bigquery_client import BigQueryClient
from .. import db
import uuid

simulation_bp = Blueprint('simulation', __name__)

@simulation_bp.route('/')
@login_required
def list_simulations():
    """List all simulations for the current user."""
    simulations = Simulation.query.filter_by(user_id=current_user.id).order_by(Simulation.created_at.desc()).all()
    return render_template('simulation/list.html', simulations=simulations)

@simulation_bp.route('/new', methods=['GET', 'POST'])
@login_required
def new_simulation():
    """Create a new simulation configuration."""
    if request.method == 'POST':
        simulation_name = request.form['name']
        num_games = int(request.form['num_games'])
        game_timeout = int(request.form.get('game_timeout', 600))  # 10 minutes default
        
        # Validate num_games limit
        if num_games > 100:
            flash('Maximum 100 games per simulation')
            return render_template('simulation/new.html')
        
        # Create simulation
        simulation = Simulation(
            id=str(uuid.uuid4()),
            name=simulation_name,
            user_id=current_user.id,
            num_games=num_games,
            game_timeout=game_timeout,
            status='pending'
        )
        db.session.add(simulation)
        
        # Add players
        for i in range(4):  # Support up to 4 players
            deck_id = request.form.get(f'player_{i}_deck')
            if deck_id:  # Only add players with selected decks
                player = SimulationPlayer(
                    simulation_id=simulation.id,
                    player_number=i + 1,
                    deck_id=deck_id
                )
                db.session.add(player)
        
        db.session.commit()
        flash('Simulation created successfully')
        return redirect(url_for('simulation.list_simulations'))
    
    return render_template('simulation/new.html')

@simulation_bp.route('/<simulation_id>/start', methods=['POST'])
@login_required
def start_simulation(simulation_id):
    """Start a simulation."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        flash('Unauthorized')
        return redirect(url_for('simulation.list_simulations'))
    
    # Start simulation via Forge interface
    forge_interface = ForgeInterface(current_app._get_current_object())
    try:
        forge_interface.start_simulation(simulation)
        simulation.status = 'running'
        db.session.commit()
        flash('Simulation started')
    except Exception as e:
        flash(f'Failed to start simulation: {str(e)}')
    
    return redirect(url_for('monitoring.simulation_status', simulation_id=simulation_id))

@simulation_bp.route('/<simulation_id>/stop', methods=['POST'])
@login_required
def stop_simulation(simulation_id):
    """Stop a running simulation."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        flash('Unauthorized')
        return redirect(url_for('simulation.list_simulations'))
    
    # Stop simulation via Forge interface
    forge_interface = ForgeInterface(current_app._get_current_object())
    try:
        forge_interface.stop_simulation(simulation)
        simulation.status = 'stopped'
        db.session.commit()
        flash('Simulation stopped')
    except Exception as e:
        flash(f'Failed to stop simulation: {str(e)}')
    
    return redirect(url_for('simulation.list_simulations'))

@simulation_bp.route('/<simulation_id>/delete', methods=['POST'])
@login_required
def delete_simulation(simulation_id):
    """Delete a simulation."""
    simulation = Simulation.query.get_or_404(simulation_id)
    
    if simulation.user_id != current_user.id:
        flash('Unauthorized')
        return redirect(url_for('simulation.list_simulations'))
    
    if simulation.status == 'running':
        flash('Cannot delete a running simulation')
        return redirect(url_for('simulation.list_simulations'))
    
    try:
        db.session.delete(simulation)
        db.session.commit()
        flash('Simulation deleted successfully')
    except Exception as e:
        flash(f'Failed to delete simulation: {str(e)}')
    
    return redirect(url_for('simulation.list_simulations'))