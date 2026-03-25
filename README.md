# Vengeance Live (RuneLite)

RuneLite-plugin die collection log-unlocks, waardevolle loot (standaard ≥ 1M GP) en PK / wilderness loot key-inhoud naar je **eigen Vengeance-website** stuurt (`POST /api/drops`). Geschikt voor spelers die via de **Jagex Launcher** met een **Jagex-account** inloggen.

## Installeren (optie A: Plugin Hub + Jagex Launcher)

1. Start **Old School RuneScape** via de **Jagex Launcher** en kies **RuneLite** als client.
2. Open in RuneLite het **configuratiescherm** (tandwiel) → **Plugin Hub** (stekker-icoon rechtsboven).
3. Zoek op **Vengeance Live** (of tags zoals `pvp`, `drops`, `website`) en klik **Installeren**.
4. Ga naar **Configuratie** → **Vengeance Live**:
   - **API basis-URL**: je live site, bv. `https://jouwdomein.nl` (geen `/` aan het eind).
   - **Min. loot (GP)**: drempel voor monster-/PK-loot (standaard 1.000.000). Collection log wordt altijd meegestuurd (als de chatregel herkend wordt).

Zonder Plugin Hub-inschijving verschijnt de plugin **niet** in de Hub. Zie onderstaand hoofdstuk als je de plugin nog moet **inzenden**.

## Privacy

De plugin stuurt o.a. je **RS-gebruikersnaam**, **item-id’s**, **hoeveelheden** en (indien van toepassing) **berekende GP-waarden** naar de door jou ingestelde URL. Alleen gebruiken op een server die jij vertrouwt.

## Ontwikkelaars

```bash
./gradlew run          # RuneLite met plugin ingebouwd (dev)
./gradlew shadowJar    # fat jar in build/libs/
```

## Plugin Hub: zelf inzenden

Tot de plugin in de Hub staat, kunnen anderen hem daar niet installeren. Stappen (officieel RuneLite-proces):

1. Zet deze code in een **publieke** GitHub-repository (bijv. `vengeance-live`).
2. Voeg/commit alles, push naar `main`/`master`.
3. **Fork** [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
4. Maak een branch en voeg een bestand toe: `plugins/vengeance-live` (bestandsnaam zonder extensie, inhoud):

   ```text
   repository=https://github.com/JOUW-USER/vengeance-live.git
   commit=VOLLEDIGE_40_TEKENS_COMMIT_HASH
   ```

   De hash kopieer je van de laatste commit op GitHub (commits → commit → hash rechtsboven).

5. Open een **Pull Request** naar `runelite/plugin-hub` en wacht op review (CI moet groen zijn).

Meer detail: [plugin-hub README](https://github.com/runelite/plugin-hub/blob/master/README.md) en [Jagex Accounts + dev-login](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) als je lokaal wilt testen met een Jagex-account.

## Licentie

BSD 2-Clause — zie [LICENSE](LICENSE).
