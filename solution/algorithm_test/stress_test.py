from locust import HttpUser, task, events, constant_pacing

from util import *


class Client(HttpUser):
    client_data: dict

    wait_time = constant_pacing(1 / 5)

    def on_start(self):
        self.client_data = create_client()

    @task
    def stress_test(self):
        self.client.get(f"/ads?client_id={self.client_data['client_id']}")


@events.init.add_listener
def on_locust_init(environment, **kwargs):
    advance_time(0)

    advertiser = create_advertiser()

    for _ in range(300):
        create_campaign(advertiser['advertiser_id'], get_random_int(1, int(500 * 1.25)),
                        get_random_int(1, int(500 * 0.15)), get_random_float(0.5, 1.5),
                        get_random_float(4, 6), 0, 7)
