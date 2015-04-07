/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.conquestia.mobs.Commands.Admin;

import com.conquestia.mobs.Commands.CqmCommand;
import com.conquestia.mobs.Commands.CqmCommandHandler;
import com.conquestia.mobs.ConquestiaMobs;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Ferrago
 */
public class CmdAddZone implements CqmCommand {

    @Override
    public void execute(CqmCommandHandler handler, Plugin plugin, CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command!");
            return;
        }
        
        Player player = (Player)sender;
        World world = player.getWorld();
        
        if (!ConquestiaMobs.getEnabledWorlds().contains(world)) {
            player.sendMessage(ChatColor.RED + " This world is not enabled for mob levels!");
            return;
        }
        
        
        
    }

    @Override
    public String getPermissionNode() {
        return "conquestiamobs.admin";
    }

    @Override
    public String getArgsString() {
        return "None";
    }

    @Override
    public String getDescription() {
        return "Adds your current location to the list of spawn points";
    }

    @Override
    public String getHelp() {
        return ChatColor.GOLD + "[" + ChatColor.BLUE + "Conquestia" + ChatColor.YELLOW + "Mobs" + ChatColor.GOLD + "]" + ChatColor.LIGHT_PURPLE + " addzone " + ChatColor.WHITE + " - Adds your current location to the list of spawn points";
    }
    
}
