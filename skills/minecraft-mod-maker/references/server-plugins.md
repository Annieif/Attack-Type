# 服务器插件开发教程

服务器插件运行在 Bukkit/Spigot/Paper 服务端上，与 Mod 不同，插件不需要客户端安装任何 Mod。

## 1. 服务器平台对比

| 平台 | 基于 | 特点 | 推荐度 |
|------|------|------|:---:|
| **Bukkit** | 原始 CraftBukkit | 基础 API，不再维护 | ⭐ |
| **Spigot** | Bukkit 增强 | 性能优化，更多 API | ⭐⭐⭐ |
| **Paper** | Spigot 增强 | 最佳性能，扩展 API，活跃维护 | ⭐⭐⭐⭐⭐ |
| **Purpur** | Paper 增强 | 更多配置选项，自定义功能 | ⭐⭐⭐⭐ |
| **Folia** | Paper 分支 | 多线程区域化服务器 | ⭐⭐⭐ |
| **Velocity** | 独立代理 | 跨服代理（BungeeCord 替代） | ⭐⭐⭐⭐ |

> **推荐使用 Paper 1.21+** 作为开发目标，它向下兼容 Spigot/Bukkit 插件。

## 2. 项目搭建

### Gradle 配置（推荐）

`build.gradle`：
```groovy
plugins {
    id 'java'
    id 'io.papermc.paperweight.userdev' version '1.7.7'
}

group = 'com.example'
version = '1.0.0'

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}
```

`settings.gradle`：
```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    }
}
```

### Maven 配置

`pom.xml`：
```xml
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 3. plugin.yml

`src/main/resources/plugin.yml`：
```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: '1.21'
description: 我的第一个 Paper 插件
author: YourName
website: https://example.com

# 依赖（可选）
depend: [Vault]
softdepend: [LuckPerms, PlaceholderAPI]

# 命令
commands:
  mycommand:
    description: 我的自定义命令
    usage: /<command> [参数]
    aliases: [mc, mycmd]
    permission: myplugin.mycommand
    permission-message: §c你没有权限使用此命令！

  heal:
    description: 治疗玩家
    usage: /<command> [玩家]
    permission: myplugin.heal

# 权限
permissions:
  myplugin.*:
    description: 所有权限
    default: op
    children:
      myplugin.mycommand: true
      myplugin.heal: true

  myplugin.mycommand:
    description: 使用 mycommand 命令
    default: true

  myplugin.heal:
    description: 使用治疗命令
    default: op
```

## 4. 主类结构

```java
package com.example.myplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    private static MyPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("MyPlugin 已启用！");

        // 保存默认配置
        saveDefaultConfig();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MyListener(this), this);

        // 注册命令
        getCommand("mycommand").setExecutor(new MyCommand(this));
        getCommand("heal").setExecutor(new HealCommand());

        getLogger().info("MyPlugin 启动完成！");
    }

    @Override
    public void onDisable() {
        getLogger().info("MyPlugin 已关闭！");
    }

    public static MyPlugin getInstance() {
        return instance;
    }
}
```

## 5. 事件系统

```java
package com.example.myplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.ChatColor;

public class MyListener implements Listener {

    private final MyPlugin plugin;

    public MyListener(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 自定义加入消息
        event.setJoinMessage(ChatColor.GREEN + "欢迎 " +
            event.getPlayer().getName() + " 加入服务器！");

        // 给玩家发送欢迎信息
        event.getPlayer().sendMessage(ChatColor.GOLD + "欢迎来到服务器！");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(ChatColor.RED + event.getPlayer().getName() + " 离开了服务器");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 检查玩家是否有权限破坏方块
        if (!event.getPlayer().hasPermission("myplugin.break")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "你没有权限破坏方块！");
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // 取消所有摔落伤害
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // 防止玩家移动特定物品
        if (event.getCurrentItem() != null &&
            event.getCurrentItem().hasItemMeta() &&
            event.getCurrentItem().getItemMeta().hasLore()) {
            // 取消移动带有 lore 的物品
            // event.setCancelled(true);
        }
    }
}
```

### 事件优先级

```java
@EventHandler(priority = EventPriority.LOWEST)   // 最先执行
@EventHandler(priority = EventPriority.LOW)
@EventHandler(priority = EventPriority.NORMAL)    // 默认
@EventHandler(priority = EventPriority.HIGH)
@EventHandler(priority = EventPriority.HIGHEST)   // 最后执行
@EventHandler(priority = EventPriority.MONITOR)   // 监控（不应修改事件）
```

### 忽略已取消事件

```java
@EventHandler(ignoreCancelled = true)
public void onBlockBreak(BlockBreakEvent event) {
    // 仅在事件未被取消时执行
}
```

## 6. 命令系统

### 基础命令

```java
package com.example.myplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MyCommand implements CommandExecutor, TabCompleter {

    private final MyPlugin plugin;

    public MyCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/mycommand help - 查看帮助");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sender.sendMessage("§e=== MyPlugin 帮助 ===");
                sender.sendMessage("§e/mycommand info - 查看信息");
                sender.sendMessage("§e/mycommand reload - 重载配置");
                break;

            case "info":
                if (sender instanceof Player player) {
                    player.sendMessage("§a你的名字: " + player.getName());
                    player.sendMessage("§a你的位置: " +
                        player.getLocation().getBlockX() + ", " +
                        player.getLocation().getBlockY() + ", " +
                        player.getLocation().getBlockZ());
                    player.sendMessage("§a你的血量: " + player.getHealth() + "/" +
                        player.getMaxHealth());
                } else {
                    sender.sendMessage("§c此命令只能由玩家执行！");
                }
                break;

            case "reload":
                if (!sender.hasPermission("myplugin.reload")) {
                    sender.sendMessage("§c你没有权限！");
                    return true;
                }
                plugin.reloadConfig();
                sender.sendMessage("§a配置已重载！");
                break;

            default:
                sender.sendMessage("§c未知子命令！使用 /mycommand help 查看帮助");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
            String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("help");
            completions.add("info");
            if (sender.hasPermission("myplugin.reload")) {
                completions.add("reload");
            }
            return completions.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }
        return Collections.emptyList();
    }
}
```

### 治疗命令

```java
public class HealCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }

        if (args.length > 0) {
            // 治疗其他玩家
            Player target = player.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§c玩家 " + args[0] + " 不在线！");
                return true;
            }
            target.setHealth(target.getMaxHealth());
            target.setFoodLevel(20);
            target.setSaturation(20);
            target.sendMessage("§a你已被 " + player.getName() + " 治疗！");
            player.sendMessage("§a你治疗了 " + target.getName());
        } else {
            // 治疗自己
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.sendMessage("§a你已被治疗！");
        }
        return true;
    }
}
```

### Paper Brigadier 命令（推荐，1.19+）

```java
// 使用 Paper 的 Brigadier 命令框架（更现代、更强大）
// 在 plugin.yml 中注册：
// commands:
//   mycommand:
//     description: 我的命令

// 在 onEnable 中注册：
LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
    Commands commands = event.registrar();
    commands.register(
        Commands.literal("mycommand")
            .requires(src -> src.getSender().hasPermission("myplugin.mycommand"))
            .then(Commands.literal("info")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage("Info!");
                    return 1;
                })
            )
            .then(Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("myplugin.reload"))
                .executes(ctx -> {
                    reloadConfig();
                    ctx.getSource().getSender().sendMessage("Reloaded!");
                    return 1;
                })
            )
            .build()
    );
});
```

## 7. 配置管理

### config.yml

```yaml
# MyPlugin 配置文件
settings:
  welcome-message: "欢迎 {player} 加入服务器！"
  auto-save-interval: 300  # 秒

features:
  anti-fall-damage: true
  join-message: true
  quit-message: true

database:
  type: sqlite  # sqlite 或 mysql
  mysql:
    host: localhost
    port: 3306
    database: myplugin
    username: root
    password: ""
```

### 读取配置

```java
public class ConfigManager {
    private final MyPlugin plugin;

    public ConfigManager(MyPlugin plugin) {
        this.plugin = plugin;
    }

    public String getWelcomeMessage() {
        return plugin.getConfig().getString("settings.welcome-message",
            "欢迎 {player} 加入服务器！");
    }

    public boolean isAntiFallDamageEnabled() {
        return plugin.getConfig().getBoolean("features.anti-fall-damage", false);
    }

    public int getAutoSaveInterval() {
        return plugin.getConfig().getInt("settings.auto-save-interval", 300);
    }

    public void reload() {
        plugin.reloadConfig();
    }
}
```

### 自定义 config.yml

```java
// 在 onEnable 中
saveDefaultConfig(); // 如果不存在则从 jar 中复制默认 config.yml

// 获取 config.yml 中的值
String message = getConfig().getString("settings.welcome-message");
int interval = getConfig().getInt("settings.auto-save-interval", 300);

// 修改并保存
getConfig().set("settings.auto-save-interval", 600);
saveConfig();
```

## 8. 权限系统

```java
// 检查权限
if (player.hasPermission("myplugin.admin")) {
    // 管理员操作
}

// 带默认值的权限检查
if (player.hasPermission("myplugin.use")) {
    // 需要 myplugin.use 权限
} else {
    player.sendMessage("§c你没有权限！");
}

// 权限继承（在 plugin.yml 中定义）
// myplugin.admin 自动包含 myplugin.use 的所有权限

// 动态注册权限（使用 Vault/LuckPerms API）
// 推荐使用 LuckPerms API 进行高级权限管理
```

## 9. 调度器

```java
public class SchedulerExample {
    private final MyPlugin plugin;

    public SchedulerExample(MyPlugin plugin) {
        this.plugin = plugin;
    }

    public void startAutoSave() {
        int interval = plugin.getConfig().getInt("settings.auto-save-interval", 300);

        // 延迟执行（tick 为单位，20 ticks = 1 秒）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("延迟任务执行！");
        }, 20 * 5); // 5 秒后执行

        // 定时重复执行
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("自动保存...");
            // 保存数据
        }, 0, 20L * interval); // 立即开始，每 interval 秒执行一次

        // 异步执行（不阻塞主线程）
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // 注意：异步任务中不能调用 Bukkit API！
            // 只能进行文件 I/O、网络请求、数据库查询等
            plugin.getLogger().info("异步任务执行中...");
        });

        // 同步执行（确保在主线程）
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // 可以安全调用 Bukkit API
            plugin.getServer().broadcastMessage("服务器公告！");
        });
    }
}
```

## 10. 数据库（SQLite/MySQL）

### SQLite 连接

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DatabaseManager {
    private final MyPlugin plugin;
    private Connection connection;

    public DatabaseManager(MyPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        // SQLite 数据库文件存放在插件目录
        String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/database.db";
        connection = DriverManager.getConnection(url);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    balance REAL DEFAULT 0.0,
                    play_time INTEGER DEFAULT 0,
                    last_login INTEGER DEFAULT 0
                )
            """);
        }
    }

    public void savePlayerData(UUID uuid, String name, double balance) {
        String sql = "INSERT OR REPLACE INTO players (uuid, name, balance, last_login) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, balance);
            ps.setLong(4, System.currentTimeMillis() / 1000);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }

    public double getPlayerBalance(UUID uuid) {
        String sql = "SELECT balance FROM players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("读取玩家数据失败: " + e.getMessage());
        }
        return 0.0;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
        }
    }
}
```

### MySQL 连接

```java
public void connectMySQL() throws SQLException {
    String host = plugin.getConfig().getString("database.mysql.host", "localhost");
    int port = plugin.getConfig().getInt("database.mysql.port", 3306);
    String database = plugin.getConfig().getString("database.mysql.database", "myplugin");
    String username = plugin.getConfig().getString("database.mysql.username", "root");
    String password = plugin.getConfig().getString("database.mysql.password", "");

    String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true",
        host, port, database);
    connection = DriverManager.getConnection(url, username, password);
}
```

## 11. GUI 菜单

```java
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager {

    public static Inventory createMainMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, "§l主菜单");

        // 添加物品
        inv.setItem(11, createMenuItem(Material.DIAMOND_SWORD, "§a战斗", "§7点击进入战斗设置"));
        inv.setItem(13, createMenuItem(Material.CHEST, "§e背包", "§7点击打开背包"));
        inv.setItem(15, createMenuItem(Material.CRAFTING_TABLE, "§b合成", "§7点击打开合成台"));

        // 填充边框
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }

        return inv;
    }

    private static ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    // 在 InventoryClickEvent 中处理点击
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§l主菜单")) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        switch (clicked.getType()) {
            case DIAMOND_SWORD:
                player.sendMessage("§a打开战斗设置...");
                player.closeInventory();
                break;
            case CHEST:
                player.openInventory(player.getInventory());
                break;
            case CRAFTING_TABLE:
                player.openWorkbench(null, true);
                break;
        }
    }
}
```

## 12. 发送数据包（Packet）

```java
// Paper 允许直接操作 NMS 数据包
// 发送标题
player.sendTitle("§6标题", "§7副标题", 10, 70, 20); // 淡入、停留、淡出

// 发送 Action Bar
player.sendActionBar("§e这是一条 Action Bar 消息");

// 发送粒子效果
player.spawnParticle(Particle.FLAME, player.getLocation(), 100, 0.5, 0.5, 0.5, 0.01);

// 播放音效
player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
```

## 13. 经济系统（Vault）

```java
// 依赖 Vault + 经济插件（如 EssentialsX、CMI）
// 在 plugin.yml 中：
// depend: [Vault]

public class EconomyManager {
    private Economy economy;

    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
            .getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public void payPlayer(Player from, Player to, double amount) {
        EconomyResponse r1 = economy.withdrawPlayer(from, amount);
        if (r1.transactionSuccess()) {
            EconomyResponse r2 = economy.depositPlayer(to, amount);
            if (r2.transactionSuccess()) {
                from.sendMessage("§a你支付了 $" + amount + " 给 " + to.getName());
                to.sendMessage("§a你收到了来自 " + from.getName() + " 的 $" + amount);
            }
        }
    }
}
```

## 14. PlaceholderAPI 集成

```java
// 在 plugin.yml 中：
// softdepend: [PlaceholderAPI]

public class MyPlaceholderExpansion extends PlaceholderExpansion {
    private final MyPlugin plugin;

    public MyPlaceholderExpansion(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "myplugin"; }
    @Override public String getAuthor() { return "YourName"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";

        return switch (params) {
            case "balance" -> String.valueOf(getBalance(player));
            case "playtime" -> String.valueOf(getPlayTime(player));
            default -> null;
        };
    }
}

// 在 onEnable 中注册
if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
    new MyPlaceholderExpansion(this).register();
}
```

## 15. 构建与部署

```bash
# 构建插件
./gradlew build          # 产物在 build/libs/MyPlugin-1.0.0.jar

# 部署到服务器
cp build/libs/MyPlugin-1.0.0.jar /path/to/server/plugins/

# 重载服务器（推荐重启而非 /reload）
/restart
```

## 16. 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 插件未加载 | plugin.yml 路径错误 | 确认在 `src/main/resources/plugin.yml` |
| NoClassDefFoundError | 依赖未打包 | 使用 Shadow 插件打包依赖 |
| 命令未注册 | 未在 `onEnable` 中 `setExecutor` | 确认 `getCommand("name").setExecutor()` |
| 事件未触发 | 未注册 Listener | 确认 `registerEvents()` 已调用 |
| 异步任务调用 Bukkit API | 跨线程访问 | 使用 `runTask()` 同步回主线程 |
| NullPointerException | 对象未初始化 | 检查 `getServer().getPlayer()` 返回值 |

## 参考资源

- PaperMC 文档：https://docs.papermc.io/
- Spigot 插件开发指南：https://www.spigotmc.org/wiki/spigot-plugin-development/
- Bukkit API JavaDoc：https://hub.spigotmc.org/javadocs/bukkit/
- Paper API JavaDoc：https://jd.papermc.io/paper/1.21/
- 插件模板：https://github.com/PaperMC/paperweight-test-plugin