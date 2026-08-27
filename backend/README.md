# SW Android Backend

Backend API dla aplikacji Służby Więziennej RP.

## Konfiguracja

1. Skopiuj `.env.example` do `.env`.
2. Uzupełnij `DISCORD_BOT_TOKEN` oraz `DISCORD_CLIENT_SECRET` lokalnie lub w zmiennych środowiskowych hostingu.
3. Nie dodawaj pliku `.env` do GitHuba.

## Uruchomienie

```bash
npm install
npm start
```

## Endpointy

- `GET /api/health` — sprawdzenie działania backendu.
- `GET /api/config` — publiczna konfiguracja aplikacji.
- `GET /api/officers` — pobranie członków serwera Discord po skonfigurowaniu tokena bota.

Serwer Discord: `1506076988564050081`.
