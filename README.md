# MaxInvPlugin v2.3

👜 `/bag` öffnet ein 54-Slot-Inventar ("Tasche") zur virtuellen Lagerung unbegrenzter Materialmengen.

## ✨ Features
- Unbegrenzte Stackgrößen (pro Material intern gespeichert)
- GUI zeigt 64er-Stacks (automatisch aufgeteilt)
- Slot 53 zeigt Restmengen-Zusammenfassung
- Auto-Einlagerung bei vollem Inventar (konfigurierbar)
- Mehrsprachigkeit via Sprachdatei (`lang/lang_de.yml`, `lang/lang_en.yml`)
- Kompatibel mit **Paper** und **Folia**
- Konfiguration in `config.yml`

## 🧱 Kompatibilität
- Minecraft-Version: 1.21.1
- Java 17+
- Server: Paper oder Folia

## 🔧 Konfiguration (`config.yml`)
```yaml
language: de
auto-store-if-full: true
auto-store-message: true
show-open-hint: true
show-save-message: true
```

## 📁 Speicherorte im Betrieb
- Konfiguration: `plugins/MaxInvPlugin/config.yml`
- Sprachdateien: `plugins/MaxInvPlugin/lang/lang_*.yml`
- Spielerdaten: `plugins/MaxInvPlugin/data/<UUID>.yml`

## 🛠 Installation
1. Lege `MaxInvPlugin-2.3.jar` in deinen `plugins/` Ordner
2. Starte den Server
3. Passe `config.yml` + Sprachdateien ggf. an
4. Im Spiel: `/bag`