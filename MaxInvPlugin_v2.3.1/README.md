# MaxInvPlugin v2.3.1

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)](https://jdk.java.net/)
[![Minecraft 1.21.x](https://img.shields.io/badge/Minecraft-1.21.1–1.21.9-blueviolet)](https://papermc.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

👜 **Virtuelles Inventar für Minecraft Paper/Folia (getestet mit 1.21.1–1.21.9)**  
Ein Plugin, das jedem Spieler ein eigenes Lager mit unbegrenztem Speicherplatz je Itemtyp bietet.

---

## 📝 Changelog

### 🔧 v2.3.1 (Aktuell)
- ✅ FIX: Auto-Speicherung funktioniert jetzt auch für neue Items, die noch nie zuvor gelagert wurden
- 📦 Items werden zuverlässig ins Lager übertragen, auch wenn sie beim ersten Mal aufgenommen werden
- 🔄 Kein Filter mehr auf `containsKey()` im AutoStore

### 🌍 v2.3 – Sprachdateien & Folia-Kompatibilität
- ➕ Sprachdateien: `lang/lang_de.yml`, `lang/lang_en.yml`
- 🛠 Texte (GUI-Titel, Chat-Meldungen) nicht mehr hardcodiert
- 🗨️ Mehrsprachigkeit per `config.yml: language: de/en/...`
- ✅ Folia-kompatibel: sichere Entity-Thread-Ausführung mit Reflection-Fallback
- 📩 Slot 53 zeigt zusammengefasste Restmengen („Weitere eingelagert...“)

### 📦 v2.2 – Erste stabile Folia-Version
- 🧠 Automatische Speicherfunktion bei vollem Inventar
- ♻️ Stackgrößen werden intern zusammengefasst (virtuelle Summen)
- 🔎 GUI zeigt realistische 64er-Stacks + Mengen
- 💡 Hinweistexte beim Öffnen/Speichern (abschaltbar)

### 🧪 v2.0 – Basisfunktionen
- 🚪 `/bag` öffnet ein 54-Slot-Inventar
- 📁 Persönlicher Speicher pro Spieler (Datei: `UUID.yml`)
- 📊 Inhalte bleiben zwischen Sessions erhalten
- ✅ Funktioniert auf Paper 1.21.1 mit Java 17+

---

## 🧪 Compatibility

| Feature   | Supported             |
|-----------|-----------------------|
| Minecraft | 1.21.1 – 1.21.9 ✅     |
| Java      | 17+                   |
| Paper     | ✅                    |
| Folia     | ✅                    |

---

## 🚀 Installation

1. `.jar` in den `/plugins/` Ordner legen  
2. Server starten  
3. `/bag` im Spiel verwenden

---

## 📄 License

MIT License © 2025 Schnitter