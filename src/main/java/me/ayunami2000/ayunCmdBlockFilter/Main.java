package me.ayunami2000.ayunCmdBlockFilter;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends JavaPlugin implements Listener {
    private final Set<String> blockedCommands = new HashSet<>();
    private boolean loadedBlockedCommands = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        List<String> rawBlockedCmds = getConfig().getStringList("blockedCommands");

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            CommandMap commandMap = getCommandMap();
            if (commandMap == null) return;
            loadedBlockedCommands = false;
            blockedCommands.clear();
            for (String realRawBlockedCmd : rawBlockedCmds) {
                String rawBlockedCmd = realRawBlockedCmd.toLowerCase();
                String[] pieces = rawBlockedCmd.split(" ",2);
                String theRest = pieces.length == 1 ? "" : (" " + pieces[1]);
                Command cmd = commandMap.getCommand(pieces[0]);
                if (cmd == null){
                    getLogger().warning("The command \"" + rawBlockedCmd + "\" was not found! Blocking anyways...");
                    blockedCommands.add(rawBlockedCmd);
                }else {
                    blockedCommands.add(cmd.getName() + theRest);
                    for (String alias : cmd.getAliases()) {
                        blockedCommands.add(alias.toLowerCase() + theRest);
                    }
                }
            }
            loadedBlockedCommands = true;
        });
    }

    @Override
    public void onDisable() {

    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event){
        CommandSender commandSender = event.getSender();
        if (commandSender instanceof BlockCommandSender blockCommandSender){
            Material type = blockCommandSender.getBlock().getType();
            if (!(type == Material.COMMAND_BLOCK || type == Material.CHAIN_COMMAND_BLOCK || type == Material.REPEATING_COMMAND_BLOCK)) return;
        }else if(!(commandSender instanceof CommandMinecart)) return;
        if (!loadedBlockedCommands) {
            event.setCancelled(true);
            return;
        }
        String cmdStr = event.getCommand().stripLeading();
        if (cmdStr.startsWith("/")) cmdStr = cmdStr.substring(1).stripLeading();
        if (cmdStr.matches("^[^ :]+:.+$")) cmdStr = cmdStr.substring(cmdStr.indexOf(':')+1);
        boolean matched = false;
        for (String blockedCommand : blockedCommands) {
            matched = cmdStr.equalsIgnoreCase(blockedCommand) || cmdStr.toLowerCase().startsWith(blockedCommand + " ");
            if (matched) break;
        }
        if (matched) event.setCancelled(true);
    }

    private CommandMap getCommandMap() {
        try {
            SimplePluginManager spm = (SimplePluginManager) getServer().getPluginManager();
            Field cmf = SimplePluginManager.class.getDeclaredField("commandMap");
            cmf.setAccessible(true);
            return (SimpleCommandMap)cmf.get(spm);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
