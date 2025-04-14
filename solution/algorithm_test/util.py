import random
import requests
import time
import uuid

BASE_URL = 'http://localhost:8080'


def get_random_string(length: int = 8) -> str:
    letters = 'abcdefghijklmnopqrstuvwxyz'
    return ''.join(random.choice(letters) for i in range(length))


def get_random_int(a: int = 0, b: int = 100) -> int:
    return random.randint(a, b)


def get_random_float(a: float = 0, b: float = 100) -> float:
    return random.uniform(a, b)


def get_random_gender() -> str:
    return random.choice(['MALE', 'FEMALE'])


def create_client(location: str = get_random_string(), age: int = get_random_int(),
                  gender: str = get_random_gender()) -> dict:
    return requests.post(f'{BASE_URL}/clients/bulk', json=[{
        'client_id': str(uuid.uuid4()),
        'login': get_random_string(),
        'age': age,
        'location': location,
        'gender': gender
    }]).json()[0]


def create_advertiser() -> dict:
    return requests.post(f'{BASE_URL}/advertisers/bulk', json=[{
        'advertiser_id': str(uuid.uuid4()),
        'name': get_random_string()
    }]).json()[0]


def put_ml_scores(client_id: str, advertiser_id: str, score: int) -> dict:
    return requests.put(f'{BASE_URL}/ml_scores', json={
        'client_id': client_id,
        'advertiser_id': advertiser_id,
        'score': score
    }).json()


def create_campaign(advertiser_id: str, impressions_limit: int, clicks_limit: int, cost_per_impression: float,
                    cost_per_click: float, start_date: int, end_date: int, gender: str = None, age_from: int = None,
                    age_to: int = None, location: str = None) -> dict:
    return requests.post(f'{BASE_URL}/advertisers/{advertiser_id}/campaigns', json={
        'impressions_limit': impressions_limit,
        'clicks_limit': clicks_limit,
        'cost_per_impression': cost_per_impression,
        'cost_per_click': cost_per_click,
        'ad_title': get_random_string(),
        'ad_text': get_random_string(),
        'start_date': start_date,
        'end_date': end_date,
        'targeting': {
            'gender': gender,
            'age_from': age_from,
            'age_to': age_to,
            'location': location
        }
    }).json()


def get_relevant_ad(client_id: str) -> dict:
    return requests.get(f'{BASE_URL}/ads?client_id={client_id}').json()


def time_get_relevant_ad(client_id: str) -> (dict, float):
    start = time.time()
    response = get_relevant_ad(client_id)
    return response, time.time() - start


def click_ad(client_id: str, ad_id: str) -> None:
    requests.post(f'{BASE_URL}/ads/{ad_id}/click', json={'client_id': client_id})


def advance_time(date: int) -> None:
    requests.post(f'{BASE_URL}/time/advance', json={'current_date': date})
