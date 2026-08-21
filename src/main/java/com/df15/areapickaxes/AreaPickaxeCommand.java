package com.df15.areapickaxes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sesion 2026-08-13 (a peticion del usuario): "mediante un comando (al
 * cual obviamente luego los usuarios no tendran permiso) podamos crear
 * picos que minen en areas de 2x2, 2x3, 3x3, etc." -- comando de solo
 * admin (permiso areapickaxes.admin.give, default op, ver plugin.yml),
 * NUNCA pensado para que un jugador normal lo tenga.
 *
 * Sintaxis: /apick give <jugador> <AREAxAREA> [material]
 * Ejemplos:  /apick give Notch 2x2
 *            /apick give Notch 3x3 netherite_pickaxe
 */
public class AreaPickaxeCommand implements CommandExecutor
{
    private static final Pattern AREA_PATTERN = Pattern.compile("^(\\d+)[xX](\\d+)$");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(args.length < 1 || !args[0].equalsIgnoreCase("give"))
        {
            sender.sendMessage(ChatColor.DARK_GREEN + "------ /apick give <jugador> <AREAxAREA> [material] ------");
            sender.sendMessage("Da un pico que mina un area rectangular de bloques de una vez.");
            sender.sendMessage(ChatColor.DARK_GREEN + "Ejemplo: " + ChatColor.WHITE + "/apick give Notch 3x3");
            sender.sendMessage(ChatColor.GRAY + "Material por defecto: diamond_pickaxe");

            return true;
        }

        if(args.length < 3)
        {
            sender.sendMessage(ChatColor.RED + "Uso: /apick give <jugador> <AREAxAREA> [material]");

            return true;
        }

        Server server = sender.getServer();
        Player target = server.getPlayer(args[1]);

        if(target == null)
        {
            sender.sendMessage(ChatColor.RED + "Ese jugador no esta conectado!");

            return true;
        }

        Matcher matcher = AREA_PATTERN.matcher(args[2]);

        if(!matcher.matches())
        {
            sender.sendMessage(ChatColor.RED + "Formato de area invalido! Usa algo como 2x2, 2x3, 3x3...");

            return true;
        }

        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));

        if(width < 1 || height < 1 || width > 9 || height > 9)
        {
            sender.sendMessage(ChatColor.RED + "El area tiene que estar entre 1x1 y 9x9!");

            return true;
        }

        Material material = Material.DIAMOND_PICKAXE;

        if(args.length >= 4)
        {
            Material parsed = Material.matchMaterial(args[3]);

            if(parsed == null || !parsed.name().endsWith("_PICKAXE"))
            {
                sender.sendMessage(ChatColor.RED + "Ese material no es un pico valido!");

                return true;
            }

            material = parsed;
        }

        ItemStack pickaxe = new ItemStack(material);

        AreaPickaxeUtil.tagAsAreaPickaxe(pickaxe, width, height);

        target.getInventory().addItem(pickaxe);

        target.sendMessage(ChatColor.GREEN + "Has recibido un pico de area " + width + "x" + height + "!");

        if(!sender.equals(target))
        {
            sender.sendMessage(ChatColor.GREEN + "Le has dado a " + target.getName() + " un pico de area " + width + "x" + height + "!");
        }

        return true;
    }
}
