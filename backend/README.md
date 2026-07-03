# InstaDrop backend (Cobalt)

InstaDrop downloads Instagram posts/reels with no server, but **everything else
— YouTube, TikTok, X, Facebook, Reddit, Instagram Stories, ~1000 sites — needs a
backend** that does the extraction. This folder runs one: a self-hosted
[Cobalt](https://github.com/imputnet/cobalt) instance.

The app just POSTs the shared link to your instance and downloads whatever media
URL it returns, so all the brittle per-site logic lives on a server you control
and can update — not baked into the app.

## 1. Run it

```bash
cd backend
# Edit API_URL in docker-compose.yml to how your phone reaches this machine:
#   LAN:      http://<this-machine-LAN-IP>:9000/     e.g. http://192.168.1.50:9000/
#   Domain:   https://cobalt.example.com/
docker compose up -d
```

Check it's alive:

```bash
curl -H "Accept: application/json" http://localhost:9000/
# -> {"cobalt":{"version":"10...","services":[...]}}
```

## 2. Point the app at it

In **InstaDrop → Settings → Downloader server**, paste the **same** URL you set
as `API_URL` (including `http://` and port). Leave the API key blank unless you
enabled one (below). Instagram keeps working with or without this.

- Phone and server on the **same Wi-Fi** → use the machine's LAN IP
  (`http://192.168.x.x:9000/`). The app allows cleartext HTTP for this.
- Phone on **mobile data** / elsewhere → put Cobalt behind a domain with HTTPS
  (reverse proxy such as Caddy/Nginx) and use that `https://…` URL.

## 3. Logged-in sites (Instagram Stories, private posts, age-restricted YouTube)

These aren't public, so the **server** needs a session. Cobalt reads cookies
from a JSON file:

1. Create `cookies.json` next to the compose file:

   ```json
   {
     "instagram": ["cookie_name=value; other=value"],
     "youtube": ["cookie_name=value; ..."]
   }
   ```
   Export cookies from a logged-in browser (e.g. the "Get cookies.txt" extension,
   then convert), using a throwaway account you don't mind exposing.

2. In `docker-compose.yml`, uncomment `COOKIE_PATH` and the `cookies.json`
   volume mount, then `docker compose up -d` again.

See Cobalt's docs for the exact cookie format:
<https://github.com/imputnet/cobalt/blob/main/docs/run-an-instance.md>

## 4. Optional: require an API key

To stop others using your instance:

1. Create `keys.json`:
   ```json
   { "<uuid-key>": { "name": "phone" } }
   ```
2. Uncomment `API_KEY_URL` + the `keys.json` mount in compose, restart.
3. Put `<uuid-key>` in the app's **API key** field.

## Notes & honesty

- Downloading from YouTube violates YouTube's Terms of Service, and other sites
  have their own terms. Only download content you have the right to, for personal
  use. You run this instance at your own risk.
- Sites change constantly; Cobalt ships fixes often — the bundled Watchtower
  container auto-updates the image every 15 min so extraction keeps working.
- Cobalt is MIT-licensed and made by imput — this project just calls it.
