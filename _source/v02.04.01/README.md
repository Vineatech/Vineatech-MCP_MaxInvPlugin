# MaxInvPlugin v2.4.1 – Advanced (Paging, Search, Favorites, Totals, Sort)

Features:
- Paging: `/bag [page]` + GUI arrows (◀ 45 / ▶ 53)
- Search: `/bag search <query>` / `/bag clearsearch`
- Favorites: `/bag fav`, `/bag fav add <MATERIAL>`, `/bag fav remove <MATERIAL>`
- Total items counter (slot 48)
- Sort toggle (slot 50) – `alphabetical` / `amount-desc`
- AutoStore when inventory is full
- Paper + Folia compatible (Java 17, MC 1.21.x)

Build:
```bash
mvn clean package
# → target/MaxInvPlugin-2.4.1.jar
```