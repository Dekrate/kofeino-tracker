# KofeinoTracker - Aplikacja Wear OS + Android

**KofeinoTracker** to nowoczesna aplikacja do monitorowania spożycia kofeiny zaprojektowana dla **Wear OS** (smart zegarek Google). Aplikacja jest w pelni po polsku i wykorzystuje najnowsze wzorce UI oraz architektoniczne.

---

## Funkcjonalność

- **Ekran główny** – pokazuje dzienny bilans kofeiny (mg) z okrągłym wskaźnikiem postępu, listą dzisiejszych napojów i przyciskiem dodawania.
- **Dodawanie napoju** – szybki wybór z 9 predefiniowanych napojów (espresso, cappuccino, czarna kawa, latte, herbata, zielona herbata, energy drink, cola) z automatyczną konwersją na mg kofeiny.
- **Historia** – pełna lista spożyć z dzisiaj wraz z godziną.
- **Ostrzeżenie o limicie** – przy przekroczeniu 400 mg (bezpieczny dzienny limit) wyświetla się czerwone ostrzeżenie.

---

## Technologie & Wzorce

| Warstwa | Technologia |
|---------|-------------|
| UI | **Jetpack Compose** + **Wear Compose Material3** (najnowszy design system dla Wear OS) |
| Nawigacja | **SwipeDismissableNavHost** (gest swipe-to-dismiss natywny dla zegarka) |
| Architektura | **MVVM** + **Repository Pattern** |
| DI | **Hilt** |
| Baza danych | **Room** (lokalna baza SQLite) |
| Reaktywność | **Kotlin Coroutines** + **StateFlow** |
| Testy jednostkowe | JUnit4 + **MockK** + **Turbine** + **Robolectric** |
| Testy UI/E2E | Compose UI Test + **Espresso** + **Hilt Android Testing** |

---

## Struktura projektu

```
KofeinoTracker/
├── app/                    # Moduł smartfona (placeholder, gotowy do rozbudowy)
├── wear/                   # Moduł Wear OS (główna aplikacja zegarka)
│   ├── src/main/java/...
│   │   ├── data/           # Room DAO, Database, Repository
│   │   ├── domain/         # Modele (CaffeineDrink, CaffeineIntake)
│   │   ├── di/             # Hilt modules
│   │   └── presentation/   # UI, ViewModel, Navigation, Theme
│   ├── src/test/           # Testy jednostkowe (Repository, ViewModel)
│   └── src/androidTest/    # Testy UI & E2E
├── gradle/libs.versions.toml
└── README.md
```

---

## Konfiguracja emulatora Wear OS

### Wymagania
- **Android Studio Narwhal** (lub nowsza) – wymagana dla emulatora Wear OS 6
- SDK API 36 (Wear OS 6) lub API 33+ (Wear OS 4/5)

### Krok po kroku

1. **Otwórz Android Studio** → `Tools` → `SDK Manager`
2. Przejdź do zakładki **SDK Tools** i upewnij się, że masz zainstalowane:
   - `Android Emulator` (najnowsza wersja)
   - `Android SDK Platform-Tools`
3. Przejdź do zakładki **SDK Platforms** i zainstaluj:
   - **Android 16.0 ("Baklava")** – API 36 (Wear OS 6.0)
4. Otwórz **Device Manager** (`Tools` → `Device Manager`)
5. Kliknij **Create Device** → w kategorii wybierz **Wear OS**
6. Wybierz profil sprzętowy (np. **Wear OS Small Round** lub **Wear OS Large Round**)
7. Wybierz obraz systemu **Wear OS 6 (API 36)** i pobierz go jeśli trzeba
8. Zakończ konfigurację kreatora i uruchom emulator przyciskiem **Play**

> **Wskazówka:** Emulator Wear OS 6 używa podpisanego builda – nie można uzyskać root access. Dla testów deweloperskich wystarczy zwykły tryb debug.

---

## Uruchomienie aplikacji

```bash
# Zbuduj i zainstaluj na emulatorze Wear OS
./gradlew :wear:installDebug

# Lub z Android Studio: wybierz konfigurację 'wear' i kliknij Run
```

Aplikacja instaluje się jako **standalone** (`com.google.android.wearable.standalone = true`) – działa bez konieczności parowania z telefonem.

---

## Testy

### Testy jednostkowe (JVM + Robolectric)
```bash
./gradlew :wear:testDebugUnitTest
```
- `CaffeineRepositoryImplTest` – testy Room z in-memory DB
- `CaffeineViewModelTest` – testy StateFlow z MockK i Turbine

### Testy UI / E2E (instrumentowane)
```bash
./gradlew :wear:connectedDebugAndroidTest
```
- `HomeScreenTest` – wyświetlanie, nawigacja, puste stany
- `AddDrinkScreenTest` – wybór napojów, callbacki
- `HistoryScreenTest` – lista historii, sumy
- `CaffeineTrackerE2ETest` – pełny end-to-end flow (Hilt + prawdziwa aktywność)

---

## Najlepsze praktyki zastosowane w projekcie

1. **Wear Compose Material3** zamiast Material2 – nowy design system, `AppScaffold` + `ScreenScaffold`
2. **TransformingLazyColumn** zamiast `ScalingLazyColumn` – nowoczesna lista z animacjami
3. **EdgeButton** dla głównej akcji – natywny wzorzec Wear OS
4. **Dynamic color scheme** – automatyczne dopasowanie kolorystyki do ustawień zegarka (Android 14+)
5. **Swipe-to-dismiss** w nawigacji – gest właściwy dla zegarka
6. **Flow + StateFlow** – jednokierunkowy przepływ danych, reaktywne UI
7. **Room + Coroutines** – bezpieczne operacje IO na wątkach w tle
8. **Hilt** – deklaratywne wstrzykiwanie zależności, testowalność
9. **Agresywne testy** – pokrycie repository, viewmodelu, wszystkich ekranów i pełnego E2E

---

## Licencja

Projekt edukacyjny – dowolne wykorzystanie.
