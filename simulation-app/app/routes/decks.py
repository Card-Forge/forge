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
    """Enhanced API endpoint for deck search with advanced filtering."""
    # Basic search parameters
    name = request.args.get('name', '')
    commander = request.args.get('commander', '')
    source = request.args.get('source', '')
    
    # Color filtering
    colors = request.args.get('colors', '')
    color_match_mode = request.args.get('colorMatchMode', 'any')
    
    # Pagination with validation
    try:
        page = int(request.args.get('page', 1))
    except (ValueError, TypeError):
        page = 1
    
    try:
        limit = int(request.args.get('limit', 20))
    except (ValueError, TypeError):
        limit = 20
    
    # Ensure valid ranges
    page = max(1, page)
    limit = max(1, min(100, limit))  # Cap at 100 to prevent abuse
    offset = (page - 1) * limit
    
    # Legacy support for 'q' parameter
    if not name and request.args.get('q'):
        name = request.args.get('q')
    
    bigquery_client = BigQueryClient()
    
    try:
        results = bigquery_client.search_decks(
            query=name,
            commander=commander,
            colors=colors,
            source=source,
            color_match_mode=color_match_mode,
            limit=limit,
            offset=offset
        )
        
        # Get total count for pagination
        total_count = bigquery_client.count_decks(
            query=name,
            commander=commander,
            colors=colors,
            source=source,
            color_match_mode=color_match_mode
        )
        
        return jsonify({
            'decks': results,
            'total': total_count,
            'page': page,
            'limit': limit,
            'has_more': (page * limit) < total_count
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