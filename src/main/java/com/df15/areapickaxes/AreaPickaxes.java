package com.df15.areapickaxes;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin nuevo (sesion 2026-08-13), a peticion explicita del usuario --
 * ver AreaPickaxeCommand.java y AreaPickaxeListener.java para la logica
 * real.
 */
public class AreaPickaxes extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(new AreaPickaxeListener(), this);

        getCommand("apick").setExecutor(new AreaPickaxeCommand());

        getLogger().info("AreaPickaxes habilitado.");
    }
}
