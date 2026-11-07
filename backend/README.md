# SW Android Backend

Serwer API dla aplikacji SW Android. Instrukcja telefonu: [TERMUX.md](../TERMUX.md).
Node.js 22 lub 24; domyślny adres `127.0.0.1:3000`.

```sh
npm ci
npm run configure
npm start
```

Konfigurator zapisuje `DISCORD_BOT_TOKEN`, `DISCORD_CLIENT_ID`, `DISCORD_GUILD_ID`
i `BACKEND_ACCESS_KEY` w lokalnym ignorowanym `.env`. Sekret OAuth nie jest wymagany.
Token Discord służy tylko serwerowi. Klient używa oddzielnego hasła połączenia
(minimum 12 znaków), przekazywanego przez nagłówek `X-SW-Key`.

Publiczne endpointy: `GET /api/health` i `GET /api/config`. Pozostałe wymagają
hasła połączenia. `GET /api/access` sprawdza samo uwierzytelnienie bez Discorda.
Kanały i wiadomości ograniczone są do skonfigurowanego serwera Discord.
Domyślnie serwer nasłuchuje wyłącznie lokalnie. Zdalne wdrożenie wymaga HTTPS.

Funkcje: `GET /api/officers`, `GET /api/discord/channels`,
`GET|POST /api/discord/messages`, `POST /api/broadcaster/send`,
`GET /api/broadcaster/history`. Historia Broadcastera działa w pamięci procesu.

```sh
npm test
```

Testy używają sztucznych odpowiedzi Discorda i tymczasowych konfiguracji.
Nie wykonują rzeczywistych wysyłek.
