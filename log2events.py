import glob
import gzip
import hashlib
import hmac
import json
import os
from collections import defaultdict
from datetime import datetime, timedelta

from dateutil.parser import parse as parse_date

import requests

SECRET = os.environ['CLUBHOUSE_WEBHOOK_SECRET']
TOKEN = os.environ['CLUBHOUSE_TOKEN']

SOURCE_GLOB = '/var/log/nginx/clubhouse.*'
TARGET_DIR = os.path.join(
    os.path.abspath(os.path.dirname(__file__)),
    'resources', 'public', 'api',
)

story_url = 'https://api.clubhouse.io/api/v2/stories/{id}'


def ch_meta(events):
    headers = {"Shortcut-Token": TOKEN,
               "Clubhouse-Token": TOKEN}

    stories = {}
    for event in events:
        for action in event['actions']:
            if (
                action['entity_type'] == 'story'
                and action['id'] not in stories
            ):
                print("Fetching story", action['id'])
                response = requests.get(story_url.format(**action),
                                        headers=headers)
                if response.status_code == 200:
                    stories[action['id']] = response.json()
                else:
                    print("Story response HTTP", response.status_code)

    print("Fetching members")
    response = requests.get("https://api.clubhouse.io/api/v2/members",
                            headers=headers)
    response.raise_for_status()
    members = {m['id']: m for m in response.json()}

    print("Fetching projects")
    response = requests.get("https://api.clubhouse.io/api/v2/projects",
                            headers=headers)
    response.raise_for_status()
    projects = {p['id']: p for p in response.json()}

    return {'stories': stories,
            'members': members,
            'projects': projects}


def main():
    limit = (datetime.now() - timedelta(hours=3)).date()

    by_day = defaultdict(list)

    for log_file in glob.glob(SOURCE_GLOB):
        if log_file.endswith('.gz'):
            with gzip.open(log_file, 'rt') as f:
                contents = f.read()
        else:
            with open(log_file, 'rt') as f:
                contents = f.read()

        for line in contents.splitlines():
            line = line.strip()
            if not line:
                continue

            try:
                date, ts, signature, payload = line.split(' ', 3)
            except ValueError:
                print(line)
                continue
            parsed = parse_date(f'{date[:11]} {date[12:]} {ts}').date()
            if parsed >= limit:
                continue
            day = date.split(':', 1)[0]
            payload = payload.strip()
            try:
                event = json.loads(payload)
            except json.decoder.JSONDecodeError:
                payload = json.loads(f'"{payload}"')
                event = json.loads(payload)

            hasher = hmac.new(SECRET.encode(), digestmod=hashlib.sha256)
            hasher.update(payload.encode('utf-8'))
            if hasher.hexdigest() != signature:
                print("Invalid sig, discarding", payload)
                continue
            if not event:
                continue
            by_day[day].append(event)

    for day, events in by_day.items():
        events = list(sorted(events, key=lambda e: e["changed_at"]))
        day_date = parse_date(day)
        file_name = os.path.join(
            TARGET_DIR,
            '{}.json'.format(day_date.strftime('%Y-%m-%d')))
        os.makedirs(TARGET_DIR, exist_ok=True)
        if os.path.exists(file_name):
            print(f"Skipping existing {file_name}")
            continue

        meta = ch_meta(events)
        meta['events'] = events

        print("Writing", file_name)
        with open(file_name, 'w') as f:
            f.write(json.dumps(meta))


if __name__ == '__main__':
    main()
