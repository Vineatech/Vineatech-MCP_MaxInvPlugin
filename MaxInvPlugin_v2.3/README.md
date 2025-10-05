
# MaxInvPlugin v2.3 – Language Support (DE/EN), Paper+Folia, /bag

- Mehrsprachig via `lang/lang_<code>.yml`
- Standard: Deutsch (`language: de`) → Titel „Tasche“
- Befehl bleibt `/bag`
- Virtuelle Summen pro Material, Anzeige in 64er-Stacks
- Slot 53 zeigt Restmengen-Liste
- Auto-Store bei vollem Inventar
- Folia-kompatibel (entity-thread-sichere Ausführung)

## Build
```bash
mvn clean package
# JAR: target/MaxInvPlugin-2.3.jar
```
