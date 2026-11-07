# SW Android — aplikacja i serwer w Termuxie

Aplikacja Android współpracuje z serwerem Node.js uruchomionym na tym samym telefonie.
Nie wymaga hostingu Render. Termux uruchamia serwer; aplikację instaluje się z APK.

Instrukcja krok po kroku: **[TERMUX.md](TERMUX.md)**.

- [APK do zainstalowania na telefonie](downloads/SW-Android.apk)
- Konfiguracja lokalna: `bash termux.sh --setup`
- Kolejne uruchomienie: `bash termux.sh`
- API domyślnie: `http://127.0.0.1:3000`

W aplikacji otwórz **Ustawienia połączenia**, pozostaw lokalny adres serwera
oraz wpisz **hasło połączenia**, które ustawiłeś w Termuxie. Token Discord
pozostaje w `backend/.env`; aplikacja nie wymaga tokena ani sekretu OAuth.

Działające moduły API obejmują listę funkcjonariuszy, kanały i wiadomości oraz
Broadcaster. Pozostałe ekrany informacyjne aplikacji nie są pełnymi rejestrami
kadrowymi, raportowymi ani systemem grafiku.

Telefon musi mieć dostęp do internetu, a Termux pozostawać uruchomiony.
Android może zakończyć proces w tle; lokalny telefon nie zapewnia pracy 24/7.
Aplikacja domyślnie łączy się z serwerem lokalnym. Własny zdalny serwer wymaga HTTPS.

## Rozwój

```sh
cd backend
npm ci
npm test
```

Android: Gradle 8.7, Android SDK 35 i JDK 17. Workflow GitHub Actions buduje debug APK;
jego artefakt nazywa się `SW-Android-APK`. Testy serwera nie wymagają tokenów ani
wykonywania prawdziwych operacji na Discordzie.

Tokenów, haseł i lokalnej konfiguracji nie dodawaj do Gita. Zachowaj ustawienia
oraz własne zmiany przed aktualizacją repozytorium.
