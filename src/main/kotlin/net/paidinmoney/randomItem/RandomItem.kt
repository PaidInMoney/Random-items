package net.paidinmoney.randomItem

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class RandomItem : JavaPlugin() {

    private var task: BukkitTask? = null

    override fun onEnable() {
        logger.info("Random Items plugin enabled.")

        // Save default config if not present
        saveDefaultConfig()

        // Registering the command and the tab completer
        val command = getCommand("randomitems")
        if (command == null) {
            logger.severe("Command 'randomitems' not found in plugin.yml!")
            return
        }

        command.setExecutor(RandomItemCommand())
        command.tabCompleter = RandomItemCommandTabCompleter()
    }

    override fun onDisable() {
        logger.info("Random Items plugin disabled.")
        task?.cancel()
    }

    private fun startRandomItems(player: Player) {
        logger.info("Starting random item task for player: ${player.name}")

        // Read chances from config
        val chances = config.getConfigurationSection("chances")
        if (chances == null) {
            logger.severe("No chances section found in config.yml! Please configure the plugin.")
            return
        }

        val blocksChance = chances.getInt("blocks", 40)
        val itemsChance = chances.getInt("items", 30)
        val spawnEggsChance = chances.getInt("spawn_eggs", 10)
        val armorChance = chances.getInt("armor", 10)
        val weaponsChance = chances.getInt("weapons", 10)

        if (blocksChance + itemsChance + spawnEggsChance + armorChance + weaponsChance != 100) {
            logger.severe("Chances in config.yml do not add up to 100%! Please fix the configuration.")
            return
        }

        task = object : BukkitRunnable() {
            override fun run() {
                val random = (1..100).random()
                val category = when {
                    random <= blocksChance -> "blocks"
                    random <= blocksChance + itemsChance -> "items"
                    random <= blocksChance + itemsChance + spawnEggsChance -> "spawn_eggs"
                    random <= blocksChance + itemsChance + spawnEggsChance + armorChance -> "armor"
                    else -> "weapons"
                }

                val material = getRandomMaterial(category)
                if (material != null) {
                    val randomItem = ItemStack(material)
                    player.inventory.addItem(randomItem)
                    logger.info("Gave ${randomItem.type} from category '$category' to ${player.name}")
                } else {
                    logger.warning("No materials available for category '$category'.")
                }
            }
        }.runTaskTimer(this, 0L, 100L) // Runs every 5 seconds (100 ticks)
    }

    private fun stopRandomItems() {
        if (task != null) {
            logger.info("Stopping random item task.")
            task?.cancel()
            task = null
        } else {
            logger.warning("Attempted to stop random item task, but no task was running.")
        }
    }

    private fun getRandomMaterial(category: String): Material? {
        val materials = when (category) {
            "blocks" -> Material.entries.filter { it.isBlock }
            "items" -> Material.entries.filter { it.isItem && !it.isBlock && !it.name.contains("SPAWN_EGG") }
            "spawn_eggs" -> Material.entries.filter { it.name.contains("SPAWN_EGG") }
            "armor" -> Material.entries.filter {
                it.name.endsWith("_HELMET") || it.name.endsWith("_CHESTPLATE") ||
                        it.name.endsWith("_LEGGINGS") || it.name.endsWith("_BOOTS")
            }
            "weapons" -> Material.entries.filter {
                it.name.endsWith("_SWORD") || it.name.endsWith("_AXE")
            }
            else -> emptyList()
        }
        return materials.randomOrNull()
    }

    inner class RandomItemCommand : org.bukkit.command.CommandExecutor {
        override fun onCommand(
            sender: CommandSender,
            command: Command,
            label: String,
            args: Array<String>
        ): Boolean {
            if (!sender.hasPermission("randomitems.use")) {
                sender.sendMessage("You do not have permission to use this command.")
                logger.warning("${sender.name} attempted to use the 'randomitems' command without permission.")
                return true
            }

            if (args.isEmpty()) {
                sender.sendMessage("Usage: /randomitems <start|stop>")
                return false
            }

            when (args[0].lowercase()) {
                "start" -> {
                    if (sender is Player) {
                        if (task != null) {
                            sender.sendMessage("Random items are already being given!")
                        } else {
                            startRandomItems(sender)
                            sender.sendMessage("Started receiving random items!")
                        }
                    } else {
                        sender.sendMessage("This command can only be run by a player!")
                    }
                }
                "stop" -> {
                    if (task != null) {
                        stopRandomItems()
                        sender.sendMessage("Stopped receiving random items!")
                    } else {
                        sender.sendMessage("Random items are not running.")
                    }
                }
                else -> {
                    sender.sendMessage("Unknown subcommand. Usage: /randomitems <start|stop>")
                }
            }
            return true
        }
    }

    inner class RandomItemCommandTabCompleter : TabCompleter {
        override fun onTabComplete(
            sender: CommandSender,
            command: Command,
            label: String,
            args: Array<String>
        ): List<String> {
            return when {
                args.size == 1 -> listOf("start", "stop").filter { it.startsWith(args[0], ignoreCase = true) }
                else -> emptyList()
            }
        }
    }
}
