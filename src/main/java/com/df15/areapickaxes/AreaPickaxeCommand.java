package com.df15.areapickaxes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
 *
 * Sesion 2026-08-21 (a peticion del usuario): autocompletado con tabulador
 * para los 4 argumentos -- ver onTabComplete() mas abajo.
 */
public class AreaPickaxeCommand implements CommandExecutor, TabCompleter
{
    private static final Pattern AREA_PATTERN = Pattern.compile("^(\\d+)[xX](\\d+)$");

    private static final List<String> SUBCOMMANDS = Collections.singletonList("give");

    // Solo una lista de sugerencias, no una lista cerrada -- el comando
    // sigue aceptando cualquier "AxB" entre 1x1 y 9x9 aunque no aparezca
    // aqui (ver AREA_PATTERN + la validacion de rango mas abajo). Estos
    // son simplemente los tamaños mas comunes para no obligar a
    // escribirlos enteros.
    private static final List<String> SUGGESTED_AREAS = Arrays.asList(
        "1x1", "2x2", "2x3", "3x2", "3x3", "4x4", "5x5", "6x6", "7x7", "8x8", "9x9"
    );

    private static final List<String> PICKAXE_MATERIALS = Arrays.asList(
        "wooden_pickaxe", "stone_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe", "netherite_pickaxe"
    );

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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        if(args.length == 1)
        {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("give"))
        {
            List<String> names = new ArrayList<>();

            for(Player p : Bukkit.getOnlinePlayers())
            {
                names.add(p.getName());
            }

            return filterStartsWith(names, args[1]);
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("give"))
        {
            return filterStartsWith(SUGGESTED_AREAS, args[2]);
        }

        if(args.length == 4 && args[0].equalsIgnoreCase("give"))
        {
            return filterStartsWith(PICKAXE_MATERIALS, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filterStartsWith(Collection<String> options, String typed)
    {
        List<String> result = new ArrayList<>();
        String lower = typed.toLowerCase(Locale.ROOT);

        for(String option : options)
        {
            if(option.toLowerCase(Locale.ROOT).startsWith(lower))
            {
                result.add(option);
            }
        }

        return result;
    }
}
