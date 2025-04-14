import json
import logging
import os
import random
from typing import Any, Dict, List, Tuple

from util import (
    create_client,
    create_advertiser,
    create_campaign,
    advance_time,
    time_get_relevant_ad,
    click_ad,
    get_random_int,
    get_random_float,
)

# Configuration constants
TARGET_CTR = 0.1
CLIENTS_COUNT = 100
CAMPAIGNS_COUNT = 50  # Not directly used here but kept for clarity
SIMULATION_DAYS = 7


def setup_logging() -> None:
    """Configure logging for the simulation."""
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )


def load_json_file(filepath: str, default: Any = None) -> Any:
    """Load JSON data from a file, returning a default if file does not exist or fails."""
    if os.path.exists(filepath):
        try:
            with open(filepath, 'r') as f:
                return json.load(f)
        except Exception as e:
            logging.error("Failed to load %s: %s", filepath, str(e))
            return default
    return default


def save_json_file(data: Any, filepath: str) -> None:
    """Save JSON data to a file."""
    try:
        with open(filepath, 'w') as f:
            json.dump(data, f)
    except Exception as e:
        logging.error("Failed to save %s: %s", filepath, str(e))


def initialize_campaigns_creation_data(filepath: str, clients_count: int, num_campaigns: int = 100) -> List[Tuple]:
    """
    Load or generate campaigns creation data.

    Each tuple is built with randomized parameters based on the number of clients.
    """
    data = load_json_file(filepath, default=[])
    if not data:
        for _ in range(num_campaigns):
            data.append((
                get_random_int(1, int(clients_count * 1.25)),
                get_random_int(1, int(clients_count * 0.15)),
                get_random_float(0.5, 1.5),
                get_random_float(4, 6),
                get_random_int(1, 3),
                get_random_int(3, 7)
            ))
    save_json_file(data, filepath)
    return data


def initialize_actions_data(filepath: str, clients_count: int, campaigns_count: int, days: int) -> List[int]:
    """
    Load or generate actions data for each simulation day.

    Each day gets a random number of actions based on the total number of client/campaign pairs.
    """
    data = load_json_file(filepath, default=[])
    if not data:
        base_actions = (clients_count * campaigns_count) // days
        data = [
            get_random_int(int(base_actions * 0.75), int(base_actions * 1.33))
            for _ in range(days)
        ]
    save_json_file(data, filepath)
    return data


def simulate_day(day_index: int, actions_count: int, clients: List[Dict], campaigns: Dict, total_profit: float,
                 response_times: List[float]) -> float:
    """
    Simulate one day of actions.

    For each action, select a random client, get an ad response, update campaign statistics,
    and accumulate profit. Returns the updated total profit.
    """
    logging.info("Simulating Day %d with %d actions", day_index + 1, actions_count)
    advance_time(day_index + 1)

    for _ in range(actions_count):
        client = random.choice(clients)
        response, elapsed_time = time_get_relevant_ad(client['client_id'])

        # No ad available or unexpected response format
        if 'status' in response:
            logging.debug("No ads available for client %s", client['client_id'])
            continue
        if 'ad_id' not in response:
            logging.warning("Unexpected response for client %s: %s", client['client_id'], response)
            continue

        ad_id = response['ad_id']
        if ad_id not in campaigns:
            logging.error("Campaign %s not found for client %s", ad_id, client['client_id'])
            continue

        campaign = campaigns[ad_id]

        # Ensure a client only interacts with a given ad once
        if ad_id in client:
            continue
        client[ad_id] = True

        # Update impressions and record response time
        campaign.setdefault('impressions', 0)
        campaign['impressions'] += 1
        response_times.append(elapsed_time)

        try:
            total_profit += campaign['cost_per_impression']
        except KeyError:
            logging.exception("Campaign missing cost_per_impression: %s", campaign)
            continue

        # Simulate a click based on target CTR
        if get_random_int(0, 100) <= TARGET_CTR * 100:
            total_profit += campaign.get('cost_per_click', 0)
            campaign.setdefault('clicks', 0)
            campaign['clicks'] += 1
            click_ad(client['client_id'], ad_id)

    return total_profit


def compute_statistics(values: List[float]) -> Dict[str, float]:
    """Compute basic statistics (total, average, min, max, median) for a list of numbers."""
    if not values:
        return {}
    sorted_vals = sorted(values)
    count = len(values)
    if count % 2 == 1:
        median = sorted_vals[count // 2]
    else:
        median = (sorted_vals[count // 2 - 1] + sorted_vals[count // 2]) / 2
    return {
        'total': sum(values),
        'average': sum(values) / count,
        'min': min(values),
        'max': max(values),
        'median': median
    }


def print_statistics(label: str, stats: Dict[str, float]) -> None:
    """Log the statistics with a label."""
    logging.info(
        "%s - Total: %.2f, Average: %.2f, Min: %.2f, Max: %.2f, Median: %.2f",
        label,
        stats.get('total', 0),
        stats.get('average', 0),
        stats.get('min', 0),
        stats.get('max', 0),
        stats.get('median', 0)
    )


def main() -> None:
    setup_logging()
    logging.info("Starting simulation")

    # Initialize simulation entities
    clients = [create_client() for _ in range(CLIENTS_COUNT)]
    advertiser = create_advertiser()
    logging.info("Advertiser created: %s", advertiser)

    # Set initial time
    advance_time(0)

    # Load or generate campaigns creation data and create campaigns
    campaigns_data = initialize_campaigns_creation_data('campaigns_creation_data.json', CLIENTS_COUNT)
    campaigns = {}
    for data in campaigns_data:
        campaign = create_campaign(advertiser['advertiser_id'], *data)
        logging.info("Campaign created: %s", campaign)
        campaigns[campaign['campaign_id']] = campaign

    # Load or generate actions data for each simulation day
    actions_data = initialize_actions_data('actions_data.json', CLIENTS_COUNT, CAMPAIGNS_COUNT, SIMULATION_DAYS)

    total_profit = 0.0
    response_times: List[float] = []

    # Run simulation for each day
    for day_index, actions_count in enumerate(actions_data):
        total_profit = simulate_day(day_index, actions_count, clients, campaigns, total_profit, response_times)

    # Calculate fulfillment metrics for campaigns
    fulfillment_clicks = []
    fulfillment_impressions = []
    clicks_mre = []
    impressions_mre = []

    for campaign in campaigns.values():
        clicks = campaign.get('clicks', 0)
        impressions = campaign.get('impressions', 0)
        clicks_limit = campaign.get('clicks_limit', 1)
        impressions_limit = campaign.get('impressions_limit', 1)
        fc = clicks / clicks_limit
        fi = impressions / impressions_limit
        fulfillment_clicks.append(fc)
        fulfillment_impressions.append(fi)
        clicks_mre.append(abs(fc - 1))
        impressions_mre.append(abs(fi - 1))

        if fc > 1:
            logging.warning("Campaign %s exceeded clicks limit: %.2f", campaign['campaign_id'], fc)
        if fi > 1:
            logging.warning("Campaign %s exceeded impressions limit: %.2f", campaign['campaign_id'], fi)

    logging.info("Total profit: %.2f", total_profit)

    # Log statistics for fulfillment and response times
    print_statistics("Fulfillment Clicks", compute_statistics(fulfillment_clicks))
    print_statistics("Fulfillment Impressions", compute_statistics(fulfillment_impressions))
    print_statistics("Impressions MRE", compute_statistics(impressions_mre))
    print_statistics("Clicks MRE", compute_statistics(clicks_mre))
    print_statistics("Response Times", compute_statistics(response_times))


if __name__ == '__main__':
    main()
