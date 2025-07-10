from flask import Flask, jsonify
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager
import os

db = SQLAlchemy()
login_manager = LoginManager()

def create_app(config=None):
    app = Flask(__name__)
    
    # Configuration
    app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
    app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL', 'sqlite:///simulation.db')
    app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
    
    # Apply test configuration if provided
    if config:
        app.config.update(config)
    
    # Initialize extensions
    db.init_app(app)
    login_manager.init_app(app)
    login_manager.login_view = 'auth.login'
    
    # User loader for Flask-Login
    @login_manager.user_loader
    def load_user(user_id):
        from .models.user import User
        return User.query.get(int(user_id))
    
    # Health check endpoints for container deployment
    @app.route('/health')
    def health_check():
        """Basic health check endpoint for load balancers."""
        return jsonify({
            'status': 'healthy',
            'version': '1.0.0',
            'service': 'forge-simulation-app'
        }), 200
    
    @app.route('/ready')
    def readiness_check():
        """Readiness check endpoint for container orchestration."""
        try:
            # Check database connection
            db.session.execute('SELECT 1')
            return jsonify({'status': 'ready'}), 200
        except Exception as e:
            return jsonify({'status': 'not ready', 'error': str(e)}), 503
    
    # Register blueprints
    from .routes.main import main_bp
    from .routes.simulation import simulation_bp
    from .routes.monitoring import monitoring_bp
    from .routes.statistics import statistics_bp
    from .routes.decks import decks_bp
    from .routes.auth import auth_bp
    from .routes.deck_import import deck_import_bp
    from .routes.api import api_bp
    
    app.register_blueprint(main_bp)
    app.register_blueprint(simulation_bp, url_prefix='/simulation')
    app.register_blueprint(monitoring_bp, url_prefix='/monitoring')
    app.register_blueprint(statistics_bp, url_prefix='/statistics')
    app.register_blueprint(decks_bp, url_prefix='/decks')
    app.register_blueprint(auth_bp, url_prefix='/auth')
    app.register_blueprint(deck_import_bp, url_prefix='/deck_import')
    app.register_blueprint(api_bp, url_prefix='/api')
    
    # Create tables
    with app.app_context():
        db.create_all()
    
    return app