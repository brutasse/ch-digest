## ClubHouse digest

A variation of ClubHouse's "daily summay" emails.

This relies on the ClubHouse webhook and a simple nginx setup to receive the
events.

### Set up

* Set up a nginx vhost that logs POST requests' JSON body:

```nginx
log_format clubhouse escape=json '$time_local $http_clubhouse_signature $request_body';

server {
	listen 80 default_server;
	listen [::]:80 default_server;

        location /an-opaque-url-that-only-clubhouse-knows {
                if ($request_method != POST) {
                        return 404;
                }
                access_log /var/log/nginx/clubhouse.log clubhouse;
                echo_read_request_body;
}
```

* Set up the ClubHouse webhook to post to the specific location.

* On the server, run `log2events.py` every day after midnight with the
  following environment variables:

  * `CLUBHOUSE_WEBHOOK_SECRET`: the secret you've set in the ClubHouse webhook
    config.
  * `CLUBHOUSE_TOKEN`: a valid token for the ClubHouse API.

  `log2events.py` requires the libraries defined in `requirements.txt`.

* Serve `resources/public` over HTTP.
