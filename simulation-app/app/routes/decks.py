from flask import Blueprint, render_template, request, jsonify
from flask_login import login_required
from ..services.bigquery_client import BigQueryClient

decks_bp = Blueprint('decks', __name__)

@decks_bp.route('/')
@login_required
def deck_browser():
    """Browse available decks from BigQuery."""
    return render_template('decks/browser.html')

@decks_bp.route('/api/search')
@login_required
def api_deck_search():
    """API endpoint for deck search."""
    query = request.args.get('q', '')
    commander = request.args.get('commander', '')
    colors = request.args.get('colors', '')
    limit = int(request.args.get('limit', 20))
    offset = int(request.args.get('offset', 0))
    
    bigquery_client = BigQueryClient()
    
    try:
        results = bigquery_client.search_decks(
            query=query,
            commander=commander,
            colors=colors,
            limit=limit,
            offset=offset
        )
        
        return jsonify({
            'decks': results,
            'total': len(results),
            'has_more': len(results) == limit
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@decks_bp.route('/api/<deck_id>')
@login_required
def api_deck_details(deck_id):
    """API endpoint for deck details."""
    bigquery_client = BigQueryClient()
    
    try:
        deck = bigquery_client.get_deck(deck_id)
        if not deck:
            return jsonify({'error': 'Deck not found'}), 404
        
        return jsonify(deck)
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@decks_bp.route('/api/commanders')
@login_required
def api_commanders():
    """API endpoint for getting list of commanders."""
    bigquery_client = BigQueryClient()
    
    try:
        commanders = bigquery_client.get_commanders()
        return jsonify(commanders)
    except Exception as e:
        return jsonify({'error': str(e)}), 500