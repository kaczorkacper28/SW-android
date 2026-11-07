# SW Android na telefonie przez Termux

To osobny projekt od `SW-bot`. Serwer SW Android używa domyślnie portu **3000**.

## 1. Pobranie i konfiguracja serwera

Otwórz nową sesję Termuxa. Wklej tylko polecenia z ramki:

```sh
pkg update -y
pkg install -y git nodejs-lts npm openssl
cd ~
git clone https://github.com/kaczorkacper28/SW-android.git
cd ~/SW-android
bash termux.sh --setup
```

Konfigurator zapyta o:

1. Token bota Discord. Wpisz go niewidocznie w Termuxie. Enter pozwala pominąć
   token i uruchomić sam serwer; moduły Discord wtedy nie pobiorą ani nie wyślą danych.
2. ID aplikacji i ID serwera. Jeżeli proponowane wartości są Twoje, naciśnij Enter.
3. Hasło połączenia z aplikacją — minimum 12 znaków. Powtórz je na kolejne pytanie.

Przy tokenie i haśle nie widać liter ani gwiazdek. To normalne. Nie wpisuj ich
w czacie ani w poleceniach powłoki. Jeśli token był wcześniej ujawniony, zresetuj
go w Discord Developer Portal i użyj nowego wyłącznie w Termuxie.

Skrypt zapisuje prywatny plik `backend/.env` i uruchamia serwer. Domyślny adres
to `http://127.0.0.1:3000`. Zostaw Termux uruchomiony. Poleceń nie wpisuje się
w sesji, w której trwa praca serwera; do innych poleceń otwórz NEW SESSION.

## 2. Instalacja APK i połączenie

Na telefonie otwórz [pobieranie APK](https://github.com/kaczorkacper28/SW-android/raw/refs/heads/main/downloads/SW-Android.apk).
Pobierz plik i otwórz go, aby zainstalować aplikację. To pakiet debug do ręcznej
instalacji. Android może poprosić o zezwolenie na instalację z użytej przeglądarki.

W aplikacji:

1. Otwórz **Ustawienia połączenia**.
2. Wpisz adres `http://127.0.0.1:3000`.
3. W polu **Hasło połączenia** wpisz to samo hasło, które ustawiłeś w Termuxie.
4. Zapisz ustawienia i sprawdź połączenie.

Adres 127.0.0.1 działa tylko na urządzeniu z uruchomionym serwerem. Status
połączenia oznacza dostęp do serwera, a nie potwierdzenie uprawnień Discorda.
Do pobierania kadry bot musi być na wybranym serwerze; lista członków może wymagać
włączenia **Server Members Intent** w Discord Developer Portal. Wysyłanie wymaga
uprawnień bota do docelowego kanału. Token Discord nie trafia do aplikacji APK.

## 3. Następny start

W nowej sesji Termuxa:

```sh
cd ~/SW-android
bash termux.sh
```

Skrypt ponownie wykorzystuje zapisaną konfigurację. Jeśli narzędzia lub zależności
nie są zainstalowane, poinformuje o wymaganym kroku lub zainstaluje zależności npm.
Nie wyłączaj aplikacji Termux. Ustawienie pracy bez ograniczeń baterii oraz blokada
uśpienia pomagają, ale Android nadal może zakończyć proces.

## 4. Zmiana hasła lub tokena

W sesji z serwerem dotknij CTRL, potem C. Następnie:

```sh
cd ~/SW-android
bash termux.sh --setup
```

Puste pole zachowuje istniejący token lub hasło. Nowe hasło trzeba również wpisać
w Ustawieniach połączenia aplikacji. Nie jest potrzebny login `kilerkomand`:
SW Android używa hasła połączenia ustawionego dla tego osobnego serwera.

## 5. Sprawdzanie błędów

Jeżeli aplikacja nie widzi serwera, w Chrome na tym samym telefonie otwórz
`http://127.0.0.1:3000/api/health`. Poprawna odpowiedź zawiera `status: ok`.
Jeśli strona nie działa, sprawdź, czy Termux nadal uruchamia serwer. Błędne hasło
połączenia i brak tokena Discord mają osobne komunikaty.

Prześlij treść błędu lub zrzut ekranu bez tokenów i haseł. Nie przesyłaj `.env`.
Zmiennych z `SW-bot/.env` w chmurze nie ma automatycznie na telefonie — każdy
serwer ma własną lokalną konfigurację.

Po zakończeniu pracy zatrzymaj serwer przez CTRL+C. Jeśli nie będziesz go teraz
używać, wykonaj `termux-wake-unlock`, aby zwolnić blokadę uśpienia telefonu.
