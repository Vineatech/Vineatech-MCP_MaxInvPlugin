# MaxInvPlugin 2.0 (MC 1.21.1) – Virtuelle Stacks & Auto-Lagerung

**/bag** öffnet ein virtuelles 54-Slot-Inventar, das beliebig viele Materialien speichert.

## ✅ Funktionen
- 📦 Unbegrenzte Stackgrößen (intern zusammengefasst)
- 📊 GUI zeigt 64er Stacks, automatisch aufgeteilt
- 🔁 Items bleiben gespeichert (pro Spieler in Datei)
- 🧲 Auto-Lagerung beim Aufheben, wenn Inventar voll (konfigurierbar)
- 🔧 Konfigurierbar über `config.yml`

## 🧱 Kompatibilität

- Minecraft-Version: **1.21.1**
- Server-Software: **PaperMC** (empfohlen)
- API-Version: `api-version: "1.21"` (in plugin.yml)
- Java-Version: **17 oder neuer**

## ⚙️ Installation
1. Lege `MaxInvPlugin-2.0_mc1.21.1.jar` in deinen `plugins/` Ordner
2. Starte deinen Paper-Server (1.21.1 mit Java 17+)
3. Passe `config.yml` an, falls gewünscht
4. Gib im Spiel ein: `/bag`

## 📁 Speicherort
Spielerdaten: `plugins/MaxInvPlugin/data/<UUID>.yml`  
Konfiguration: `plugins/MaxInvPlugin/config.yml`

## 🔧 Beispiel `config.yml`
```yaml
auto-store-if-full: true
auto-store-message: true
show-open-hint: true
show-save-message: true
```

## 📄 Lizenz
MIT License © 2025 Schnitter