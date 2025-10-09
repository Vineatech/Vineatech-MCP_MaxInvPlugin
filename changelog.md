# 📦 MaxInvPlugin – Changelog  
*(Minecraft 1.21.9 compatible • Java 17 • Paper & Folia support)*

---

## 🇩🇪 Deutsch

### 🟢 **v2.3.1 – AutoStore-Fix für neue Materialien**
- 🧠 **Fehler behoben:** AutoStore speichert nun auch Items, die vorher **nicht** im Lager vorhanden waren  
- 🧰 `/bag` funktioniert wieder korrekt (Plugin blieb zuvor deaktiviert)  
- 📁 Speicherung in YAML-Dateien bleibt erhalten  
- 🌐 Mehrsprachig: `lang_de.yml`, `lang_en.yml`  
- ✅ Voll kompatibel mit **Paper 1.21.x** und **Folia**

---

### 🟢 **v2.3 – LanguageManager + AutoStore**
- 🌍 **LanguageManager** eingeführt – alle Texte aus Sprachdateien (`lang_de.yml`, `lang_en.yml`)  
- 🔁 **AutoStore-Funktion:** Wenn das Spielerinventar voll ist, werden Items automatisch ins Lager verschoben  
- ⚙ Konfiguration über `config.yml` (AutoStore, Nachrichten, Sprache)  
- 💡 Slot 53 zeigt **Restmengen-Zusammenfassung**  
- 🪶 Vorbereitung für Folia-Support

---

### 🟢 **v2.2 – Paper + Folia-Kompatibilität**
- ✅ Plugin vollständig **Folia-kompatibel**  
- 🧵 Nutzung von `runOnEntityThread()` für Thread-sicherheit  
- 🔁 AutoStore arbeitet auch im Folia-Scheduler  
- ⚙ Gleiche Konfiguration wie v2.1, aber stabiler für Multi-Thread-Umgebungen  

---

### 🟢 **v2.1 – Restmengen-Zusammenfassung (Slot 53)**
- ➕ Slot 53 zeigt Restmengen wie „+100 DIAMOND“ an, wenn mehr Items gespeichert sind als in das GUI passen  
- 📦 Anzeige weiterhin in 64er-Stacks, intern jedoch **beliebig große Lagerung** möglich  
- 🪶 Verbesserte Performance beim Speichern großer Mengen  

---

### 🟢 **v2.0 – Erste produktive Version**
- 👜 **`/bag`** öffnet ein virtuelles 54-Slot-Inventar  
- 💾 Inhalte werden **pro Spieler** in YAML-Dateien gespeichert  
- 🔐 **Standard-Stackgrößen (64)**, keine virtuelle Summierung  
- ❌ Noch keine Sprachdateien oder AutoStore-Funktion  

---

### 🟢 **v1.0 – Prototyp**
- 🧰 Erster Befehl: **`/openmaxinv`**  
- 📦 Öffnet leeres Inventar ohne Speicherung  
- 🔬 Nur funktionaler Test der GUI  

---

## 🇬🇧 English

### 🟢 **v2.3.1 – AutoStore Fix for New Materials**
- 🧠 **Fixed:** AutoStore now saves items even if they were **not stored before**  
- 🧰 `/bag` command works correctly again (plugin no longer disabled)  
- 📁 YAML-based player storage retained  
- 🌐 Added language support (`lang_de.yml`, `lang_en.yml`)  
- ✅ Fully compatible with **Paper 1.21.x** and **Folia**

---

### 🟢 **v2.3 – LanguageManager + AutoStore**
- 🌍 Introduced **LanguageManager** for multi-language support  
- 🔁 **AutoStore:** automatically moves items to the virtual bag when the player inventory is full  
- ⚙ Configurable via `config.yml` (AutoStore, messages, language)  
- 💡 Slot 53 shows **remainder summary** if too many items to display  
- 🪶 Added Folia thread-safe scheduler support  

---

### 🟢 **v2.2 – Paper + Folia Compatibility**
- ✅ Fully compatible with **Folia** scheduler (thread-safe)  
- 🧵 Uses `runOnEntityThread()` for safe task execution  
- 🔁 AutoStore works in asynchronous contexts  
- ⚙ Same configuration as v2.1 but improved stability  

---

### 🟢 **v2.1 – Remainder Summary (Slot 53)**
- ➕ Slot 53 displays “+100 DIAMOND” when more items are stored than fit in the GUI  
- 📦 GUI shows 64-stack visuals but stores unlimited quantities internally  
- 🪶 Optimized for larger inventories and save operations  

---

### 🟢 **v2.0 – First Functional Version**
- 👜 **`/bag`** command opens a virtual 54-slot inventory  
- 💾 Per-player YAML storage system  
- 🔐 Standard Minecraft stack sizes (64)  
- ❌ No language support or AutoStore yet  

---

### 🟢 **v1.0 – Prototype**
- 🧰 First command: `/openmaxinv`  
- 📦 Opened empty inventory (no saving)  
- 🔬 Proof-of-concept GUI only  

---

## 🧩 Build Information
```bash
mvn clean package
# → target/MaxInvPlugin-<version>.jar
