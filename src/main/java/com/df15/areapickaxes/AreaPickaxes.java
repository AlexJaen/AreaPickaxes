package com.df15.areapickaxes;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin AreaPickaxes --
 * ver AreaPickaxeCommand.java y AreaPickaxeListener.java para la logica
 * real.
 */
public class AreaPickaxes extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(new AreaPickaxeListener(), this);

        AreaPickaxeCommand apickCommand = new AreaPickaxeCommand();

        getCommand("apick").setExecutor(apickCommand);
        getCommand("apick").setTabCompleter(apickCommand);

        // Nota (2026-09-03): evita falsos positivos del check
        // "AirLiquidBreak" de GrimAC causados por los bloques "extra"
        // que rompen los picos de area (ver GrimAirLiquidBreakExemption
        // para la explicacion completa). No hace nada si GrimAC no esta
        // instalado.
        GrimAirLiquidBreakExemption.register(this);

        getLogger().info("AreaPickaxes enabled.");
    }
}
