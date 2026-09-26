# CustomAbilities

Minecraft 1.21.11 plugin (Spigot/Paper) with five ability items: Web Launcher, Energy Cookie, Switcher Ball, Landmine and Evade.

## Building
Push this folder to a GitHub repo. GitHub Actions builds it automatically:
**Actions tab → latest "Build Plugin" run → Artifacts → CustomAbilities**. Unzip it and drop the `.jar` into the server's `plugins` folder.

## Commands (OP only)
- `/abilities give <player> <item|all> [amount]`
  items: `web-launcher`, `energy-cookie`, `switcher-ball`, `landmine`, `evade`
- `/abilities reload` - reload `config.yml`

## Config
`plugins/CustomAbilities/config.yml` has the cooldowns, strength level/duration, evade range, landmine hole size and more.
