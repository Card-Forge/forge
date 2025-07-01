from .. import db
from datetime import datetime

class GameStatistics(db.Model):
    __tablename__ = 'game_statistics'
    
    id = db.Column(db.Integer, primary_key=True)
    game_id = db.Column(db.Integer, db.ForeignKey('game_results.id'), nullable=False)
    player_number = db.Column(db.Integer, nullable=False)
    deck_id = db.Column(db.String(100), nullable=False)
    
    # Resource Management Stats
    total_mana_spent = db.Column(db.Integer, default=0)
    total_mana_available = db.Column(db.Integer, default=0)
    land_drops_made = db.Column(db.Integer, default=0)
    land_drops_missed = db.Column(db.Integer, default=0)
    
    # Card Usage Stats
    cards_drawn = db.Column(db.Integer, default=0)
    cards_played_total = db.Column(db.Integer, default=0)
    creatures_played = db.Column(db.Integer, default=0)
    instants_played = db.Column(db.Integer, default=0)
    sorceries_played = db.Column(db.Integer, default=0)
    artifacts_played = db.Column(db.Integer, default=0)
    enchantments_played = db.Column(db.Integer, default=0)
    planeswalkers_played = db.Column(db.Integer, default=0)
    lands_played = db.Column(db.Integer, default=0)
    
    # Commander Stats
    commander_casts = db.Column(db.Integer, default=0)
    commander_damage_dealt = db.Column(db.Integer, default=0)
    
    # Combat Stats
    creatures_summoned = db.Column(db.Integer, default=0)
    attacks_made = db.Column(db.Integer, default=0)
    blocks_made = db.Column(db.Integer, default=0)
    damage_dealt = db.Column(db.Integer, default=0)
    damage_taken = db.Column(db.Integer, default=0)
    
    # Spell Categories
    removal_spells_cast = db.Column(db.Integer, default=0)
    ramp_spells_cast = db.Column(db.Integer, default=0)
    card_draw_spells_cast = db.Column(db.Integer, default=0)
    counterspells_cast = db.Column(db.Integer, default=0)
    
    # Game State
    final_life_total = db.Column(db.Integer, default=40)
    final_hand_size = db.Column(db.Integer, default=0)
    final_board_state_value = db.Column(db.Integer, default=0)  # Rough estimate of board value
    turns_survived = db.Column(db.Integer, default=0)
    
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<GameStatistics Player {self.player_number}: {self.deck_id}>'

class DeckStatistics(db.Model):
    __tablename__ = 'deck_statistics'
    
    id = db.Column(db.Integer, primary_key=True)
    deck_id = db.Column(db.String(100), unique=True, nullable=False)
    deck_name = db.Column(db.String(200))
    commander_name = db.Column(db.String(100))
    color_identity = db.Column(db.String(10))
    
    # Aggregated Performance Stats
    total_games = db.Column(db.Integer, default=0)
    wins = db.Column(db.Integer, default=0)
    losses = db.Column(db.Integer, default=0)
    avg_game_duration = db.Column(db.Float, default=0.0)
    avg_turns_survived = db.Column(db.Float, default=0.0)
    
    # Aggregated Card Stats
    avg_cards_played = db.Column(db.Float, default=0.0)
    avg_mana_spent = db.Column(db.Float, default=0.0)
    avg_creatures_played = db.Column(db.Float, default=0.0)
    avg_spells_played = db.Column(db.Float, default=0.0)
    avg_commander_casts = db.Column(db.Float, default=0.0)
    
    # Performance Metrics
    win_rate = db.Column(db.Float, default=0.0)
    elimination_rate = db.Column(db.Float, default=0.0)  # Rate of being first eliminated
    
    last_updated = db.Column(db.DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<DeckStatistics {self.deck_name}>'
    
    def update_stats(self):
        """Recalculate aggregated statistics from game results."""
        # This would be called after new games are completed
        # Implementation would query GameStatistics and update aggregated values
        pass

class SimulationSummary(db.Model):
    __tablename__ = 'simulation_summaries'
    
    id = db.Column(db.Integer, primary_key=True)
    simulation_id = db.Column(db.String(36), db.ForeignKey('simulations.id'), unique=True, nullable=False)
    
    # Overall Statistics
    total_games_completed = db.Column(db.Integer, default=0)
    avg_game_duration = db.Column(db.Float, default=0.0)
    avg_turns_per_game = db.Column(db.Float, default=0.0)
    
    # Deck Performance Summary (JSON)
    deck_win_rates = db.Column(db.JSON)  # {deck_id: win_rate}
    deck_elimination_rates = db.Column(db.JSON)  # {deck_id: elimination_rate}
    
    # Player Position Analysis
    position_advantage = db.Column(db.JSON)  # {position: win_rate}
    
    # Meta Analysis
    most_played_cards = db.Column(db.JSON)  # Top cards across all games
    card_type_distribution = db.Column(db.JSON)  # Distribution of card types played
    
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    last_updated = db.Column(db.DateTime, default=datetime.utcnow)
    
    def __repr__(self):
        return f'<SimulationSummary {self.simulation_id}>'