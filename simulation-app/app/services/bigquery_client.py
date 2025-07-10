from google.cloud import bigquery
from google.oauth2 import service_account
import google.auth
import os
import json
import logging

logger = logging.getLogger(__name__)

class BigQueryClient:
    def __init__(self):
        """Initialize BigQuery client with proper authentication."""
        credentials_path = os.environ.get('BIGQUERY_CREDENTIALS_PATH')
        project_id = os.environ.get('GOOGLE_CLOUD_PROJECT')
        
        logger.info(f"Initializing BigQueryClient with credentials_path: {credentials_path}")
        logger.info(f"Environment GOOGLE_CLOUD_PROJECT: {project_id}")
        
        credentials = None
        
        # Try service account file first if provided
        if credentials_path and os.path.exists(credentials_path):
            try:
                logger.info(f"Loading service account credentials from: {credentials_path}")
                credentials = service_account.Credentials.from_service_account_file(
                    credentials_path,
                    scopes=['https://www.googleapis.com/auth/bigquery']
                )
                
                # Extract project ID from service account file if not provided
                if not project_id:
                    with open(credentials_path, 'r') as f:
                        credentials_data = json.load(f)
                        project_id = credentials_data.get('project_id')
                        logger.info(f"Extracted project_id from service account: {project_id}")
                
                logger.info("Service account credentials loaded successfully")
                
            except Exception as e:
                logger.error(f"Error loading service account credentials: {e}")
                credentials = None
        
        # Fall back to Application Default Credentials if service account failed
        if credentials is None:
            try:
                logger.info("Attempting to use Application Default Credentials (ADC)")
                credentials, adc_project = google.auth.default(
                    scopes=['https://www.googleapis.com/auth/bigquery']
                )
                
                # Use project from ADC if not specified elsewhere
                if not project_id:
                    project_id = adc_project
                    logger.info(f"Using project from ADC: {project_id}")
                
                logger.info("Application Default Credentials loaded successfully")
                
            except Exception as e:
                logger.error(f"Error loading Application Default Credentials: {e}")
                
                # Final fallback - try without explicit credentials (environment-based)
                logger.warning("Falling back to environment-based authentication")
                credentials = None
        
        # Set fallback project ID if still not found
        if not project_id:
            project_id = "elated-liberty-100303"
            logger.info(f"Using fallback project_id: {project_id}")
        
        if not project_id:
            raise ValueError("Project ID must be provided via GOOGLE_CLOUD_PROJECT environment variable, service account credentials, or ADC")
        
        try:
            # Create BigQuery client with explicit credentials if available
            if credentials:
                self.client = bigquery.Client(credentials=credentials, project=project_id)
                logger.info(f"BigQuery client created with explicit credentials")
            else:
                self.client = bigquery.Client(project=project_id)
                logger.info(f"BigQuery client created with environment credentials")
            
            self.project_id = project_id
            self.dataset_id = 'mtg'
            
            # Test the connection
            try:
                # Simple test query to verify authentication works
                test_query = f"SELECT 1 as test_value"
                test_job = self.client.query(test_query)
                list(test_job.result())  # Force execution
                logger.info(f"BigQuery client initialized and tested successfully with project: {self.project_id}")
            except Exception as test_error:
                logger.warning(f"BigQuery client created but test query failed: {test_error}")
                # Don't fail initialization, just warn
            
        except Exception as e:
            logger.error(f"Error initializing BigQuery client: {e}")
            logger.error("Troubleshooting tips:")
            logger.error("1. Ensure BIGQUERY_CREDENTIALS_PATH points to a valid service account key")
            logger.error("2. Or run 'gcloud auth application-default login' for ADC")
            logger.error("3. Or set GOOGLE_APPLICATION_CREDENTIALS environment variable")
            raise
    
    def search_decks(self, query='', commander='', colors='', source='', color_match_mode='any', limit=20, offset=0):
        """Search for decks in BigQuery based on filters."""
        base_sql = f"""
        SELECT 
            deck_id,
            deck_name,
            deck_url,
            commander_1,
            commander_2,
            is_W,
            is_U,
            is_B,
            is_R,
            is_G,
            source,
            CASE 
                WHEN is_W THEN 'W' ELSE '' 
            END ||
            CASE 
                WHEN is_U THEN 'U' ELSE '' 
            END ||
            CASE 
                WHEN is_B THEN 'B' ELSE '' 
            END ||
            CASE 
                WHEN is_R THEN 'R' ELSE '' 
            END ||
            CASE 
                WHEN is_G THEN 'G' ELSE '' 
            END AS color_identity
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE 1=1
        """
        
        params = []
        
        if query:
            base_sql += " AND (deck_name LIKE @query OR deck_id LIKE @query)"
            params.append(bigquery.ScalarQueryParameter("query", "STRING", f"%{query}%"))
        
        if commander:
            base_sql += " AND (commander_1 LIKE @commander OR commander_2 LIKE @commander)"
            params.append(bigquery.ScalarQueryParameter("commander", "STRING", f"%{commander}%"))
        
        if source:
            base_sql += " AND source = @source"
            params.append(bigquery.ScalarQueryParameter("source", "STRING", source))
        
        if colors:
            # Handle color filtering based on the boolean columns and match mode
            color_conditions = []
            if 'W' in colors:
                color_conditions.append("is_W = true")
            if 'U' in colors:
                color_conditions.append("is_U = true")
            if 'B' in colors:
                color_conditions.append("is_B = true")
            if 'R' in colors:
                color_conditions.append("is_R = true")
            if 'G' in colors:
                color_conditions.append("is_G = true")
            
            if color_conditions:
                if color_match_mode == 'exact':
                    # Exact match: must have exactly these colors
                    all_colors = ['W', 'U', 'B', 'R', 'G']
                    exact_conditions = []
                    for color in all_colors:
                        if color in colors:
                            exact_conditions.append(f"is_{color} = true")
                        else:
                            exact_conditions.append(f"is_{color} = false")
                    base_sql += f" AND ({' AND '.join(exact_conditions)})"
                elif color_match_mode == 'subset':
                    # Subset: must have only these colors or fewer
                    subset_conditions = []
                    for color in ['W', 'U', 'B', 'R', 'G']:
                        if color not in colors:
                            subset_conditions.append(f"is_{color} = false")
                    if subset_conditions:
                        base_sql += f" AND ({' AND '.join(subset_conditions)})"
                else:  # 'any' mode (default)
                    # Contains any: must have at least one of these colors
                    base_sql += f" AND ({' OR '.join(color_conditions)})"
        
        base_sql += " ORDER BY deck_name LIMIT @limit OFFSET @offset"
        params.extend([
            bigquery.ScalarQueryParameter("limit", "INT64", limit),
            bigquery.ScalarQueryParameter("offset", "INT64", offset)
        ])
        
        job_config = bigquery.QueryJobConfig(query_parameters=params)
        
        try:
            results = self.client.query(base_sql, job_config=job_config)
            return [dict(row) for row in results]
        except Exception as e:
            logger.error(f"Error searching decks: {e}")
            return []
    
    def get_deck(self, deck_id):
        """Get detailed information about a specific deck."""
        sql = f"""
        SELECT 
            deck_id,
            deck_name,
            deck_url,
            commander_1,
            commander_2,
            is_W,
            is_U,
            is_B,
            is_R,
            is_G,
            source,
            CASE 
                WHEN is_W THEN 'W' ELSE '' 
            END ||
            CASE 
                WHEN is_U THEN 'U' ELSE '' 
            END ||
            CASE 
                WHEN is_B THEN 'B' ELSE '' 
            END ||
            CASE 
                WHEN is_R THEN 'R' ELSE '' 
            END ||
            CASE 
                WHEN is_G THEN 'G' ELSE '' 
            END AS color_identity
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE deck_id = @deck_id
        """
        
        job_config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("deck_id", "STRING", deck_id)
            ]
        )
        
        try:
            results = self.client.query(sql, job_config=job_config)
            rows = list(results)
            if rows:
                return dict(rows[0])
            return None
        except Exception as e:
            logger.error(f"Error getting deck {deck_id}: {e}")
            return None
    
    def count_decks(self, query='', commander='', colors='', source='', color_match_mode='any'):
        """Count decks matching the specified filters."""
        base_sql = f"""
        SELECT COUNT(*) as total
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE 1=1
        """
        
        params = []
        
        if query:
            base_sql += " AND (deck_name LIKE @query OR deck_id LIKE @query)"
            params.append(bigquery.ScalarQueryParameter("query", "STRING", f"%{query}%"))
        
        if commander:
            base_sql += " AND (commander_1 LIKE @commander OR commander_2 LIKE @commander)"
            params.append(bigquery.ScalarQueryParameter("commander", "STRING", f"%{commander}%"))
        
        if source:
            base_sql += " AND source = @source"
            params.append(bigquery.ScalarQueryParameter("source", "STRING", source))
        
        if colors:
            # Same color filtering logic as search_decks
            color_conditions = []
            if 'W' in colors:
                color_conditions.append("is_W = true")
            if 'U' in colors:
                color_conditions.append("is_U = true")
            if 'B' in colors:
                color_conditions.append("is_B = true")
            if 'R' in colors:
                color_conditions.append("is_R = true")
            if 'G' in colors:
                color_conditions.append("is_G = true")
            
            if color_conditions:
                if color_match_mode == 'exact':
                    all_colors = ['W', 'U', 'B', 'R', 'G']
                    exact_conditions = []
                    for color in all_colors:
                        if color in colors:
                            exact_conditions.append(f"is_{color} = true")
                        else:
                            exact_conditions.append(f"is_{color} = false")
                    base_sql += f" AND ({' AND '.join(exact_conditions)})"
                elif color_match_mode == 'subset':
                    subset_conditions = []
                    for color in ['W', 'U', 'B', 'R', 'G']:
                        if color not in colors:
                            subset_conditions.append(f"is_{color} = false")
                    if subset_conditions:
                        base_sql += f" AND ({' AND '.join(subset_conditions)})"
                else:  # 'any' mode (default)
                    base_sql += f" AND ({' OR '.join(color_conditions)})"
        
        job_config = bigquery.QueryJobConfig(query_parameters=params)
        
        try:
            results = self.client.query(base_sql, job_config=job_config)
            for row in results:
                return row.total
            return 0
        except Exception as e:
            logger.error(f"Error counting decks: {e}")
            return 0

    def get_commanders(self):
        """Get list of unique commanders."""
        sql = f"""
        SELECT DISTINCT commander_1 as commander
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE commander_1 IS NOT NULL
        UNION DISTINCT
        SELECT DISTINCT commander_2 as commander
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE commander_2 IS NOT NULL
        ORDER BY commander
        """
        
        try:
            results = self.client.query(sql)
            return [row.commander for row in results]
        except Exception as e:
            logger.error(f"Error getting commanders: {e}")
            return []
    
    def get_color_identities(self):
        """Get list of unique color identities."""
        sql = f"""
        SELECT DISTINCT 
            CASE 
                WHEN is_W THEN 'W' ELSE '' 
            END ||
            CASE 
                WHEN is_U THEN 'U' ELSE '' 
            END ||
            CASE 
                WHEN is_B THEN 'B' ELSE '' 
            END ||
            CASE 
                WHEN is_R THEN 'R' ELSE '' 
            END ||
            CASE 
                WHEN is_G THEN 'G' ELSE '' 
            END AS color_identity
        FROM `{self.project_id}.{self.dataset_id}.decks`
        ORDER BY color_identity
        """
        
        try:
            results = self.client.query(sql)
            return [row.color_identity for row in results if row.color_identity]
        except Exception as e:
            logger.error(f"Error getting color identities: {e}")
            return []
    
    def deck_exists(self, deck_id):
        """Check if a deck exists in the database."""
        sql = f"""
        SELECT COUNT(*) as count
        FROM `{self.project_id}.{self.dataset_id}.decks`
        WHERE deck_id = @deck_id
        """
        
        job_config = bigquery.QueryJobConfig(
            query_parameters=[
                bigquery.ScalarQueryParameter("deck_id", "STRING", deck_id)
            ]
        )
        
        try:
            results = self.client.query(sql, job_config=job_config)
            row = next(iter(results))
            return row.count > 0
        except Exception as e:
            logger.error(f"Error checking if deck exists {deck_id}: {e}")
            return False
    
    def save_simulation_results(self, simulation_results):
        """Save simulation results to BigQuery for long-term storage."""
        # This would insert into a results table for backup/analytics
        # Implementation depends on desired schema for results storage
        table_id = f"{self.project_id}.{self.dataset_id}.simulation_results"
        
        try:
            # Convert simulation results to BigQuery format
            rows_to_insert = self._format_results_for_bigquery(simulation_results)
            
            table = self.client.get_table(table_id)
            errors = self.client.insert_rows_json(table, rows_to_insert)
            
            if errors:
                logger.error(f"Error inserting simulation results: {errors}")
                return False
            
            logger.info(f"Successfully saved {len(rows_to_insert)} simulation results to BigQuery")
            return True
            
        except Exception as e:
            logger.error(f"Error saving simulation results: {e}")
            return False
    
    def _format_results_for_bigquery(self, simulation_results):
        """Format simulation results for BigQuery insertion."""
        # Implementation would depend on the exact format of simulation_results
        # and the desired BigQuery schema
        formatted_rows = []
        
        for result in simulation_results:
            formatted_rows.append({
                'simulation_id': result.get('simulation_id'),
                'game_number': result.get('game_number'),
                'winner': result.get('winner'),
                'duration': result.get('duration'),
                'statistics': result.get('statistics'),
                'timestamp': result.get('timestamp')
            })
        
        return formatted_rows