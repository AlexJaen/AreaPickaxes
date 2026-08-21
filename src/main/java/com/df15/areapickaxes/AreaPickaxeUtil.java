package com.df15.areapickaxes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesion 2026-08-13: helpers para leer/escribir el "ancho x alto" de un
 * pico de area en su PersistentDataContainer. Dos claves separadas
 * (width/height) en vez de una sola "2x3" para no tener que parsear un
 * String cada vez que se rompe un bloque -- se lee directo como enteros.
 *
 * Sesion 2026-08-21 (a peticion del usuario, de cara a publicar el
 * plugin para que lo use gente de otros servidores): se ha quitado por
 * completo el sistema de "tiers" con nombres/colores/lore tematicos de
 * carcel (Pico del Tunel, Pico del Butron...) que solo tenia sentido en
 * este servidor concreto. Ahora el nombre y la descripcion son 100%
 * genericos y se generan solo a partir del tamaño y el material real
 * del pico, sin ninguna tabla de nombres que mantener.
 */
public class AreaPickaxeUtil
{
    public static final NamespacedKey AREA_WIDTH_KEY = new NamespacedKey("areapickaxes", "area-width");
    public static final NamespacedKey AREA_HEIGHT_KEY = new NamespacedKey("areapickaxes", "area-height");

    /**
     * Marca un ItemStack como pico de area con el ancho/alto dados.
     * Nombre generico "<ancho>x<alto> <Material> Pickaxe" (ej. "3x2 Iron
     * Pickaxe") y una descripcion que solo explica el funcionamiento,
     * sin ningun nombre/tema especifico de servidor. Modifica el
     * ItemStack en el sitio (via setItemMeta).
     */
    public static void tagAsAreaPickaxe(ItemStack pickaxe, int width, int height)
    {
        ItemMeta meta = pickaxe.getItemMeta();

        meta.getPersistentDataContainer().set(AREA_WIDTH_KEY, PersistentDataType.INTEGER, width);
        meta.getPersistentDataContainer().set(AREA_HEIGHT_KEY, PersistentDataType.INTEGER, height);

        String materialName = formatMaterialName(pickaxe.getType());

        meta.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + width + "x" + height + " " + materialName + " Pickaxe");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Mina un area de " + width + "x" + height + " bloques");
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "de una sola vez, alrededor del bloque que rompas.");
        lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Por lo demas, funciona igual que un pico normal.");

        meta.setLore(lore);

        // Brillo de encantamiento puramente visual (API moderna de Paper
        // 1.20.5+), mismo patron ya usado en RareItemHunter para sus
        // items especiales -- no anade ningun encantamiento real.
        meta.setEnchantmentGlintOverride(true);

        pickaxe.setItemMeta(meta);
    }

    /**
     * "IRON_PICKAXE" -> "Iron", "NETHERITE_PICKAXE" -> "Netherite". Si
     * algun dia se usa con un material que no termine en "_PICKAXE" (no
     * deberia pasar, AreaPickaxeCommand ya lo valida antes de llegar
     * aqui) simplemente no recorta el sufijo y devuelve el nombre
     * completo formateado, para no romperse.
     */
    private static String formatMaterialName(Material material)
    {
        String name = material.name();

        if(name.endsWith("_PICKAXE"))
        {
            name = name.substring(0, name.length() - "_PICKAXE".length());
        }

        StringBuilder result = new StringBuilder();

        for(String word : name.split("_"))
        {
            if(word.isEmpty())
            {
                continue;
            }

            if(result.length() > 0)
            {
                result.append(' ');
            }

            result.append(Character.toUpperCase(word.charAt(0)));
            result.append(word.substring(1).toLowerCase());
        }

        return result.toString();
    }

    /**
     * @return {ancho, alto} si el item es un pico de area, o null si no
     *         lo es (item vacio, sin meta, o sin las claves de PDC).
     */
    public static int[] getAreaDimensions(ItemStack item)
    {
        if(item == null || item.getType().isAir())
        {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if(meta == null)
        {
            return null;
        }

        Integer width = meta.getPersistentDataContainer().get(AREA_WIDTH_KEY, PersistentDataType.INTEGER);
        Integer height = meta.getPersistentDataContainer().get(AREA_HEIGHT_KEY, PersistentDataType.INTEGER);

        if(width == null || height == null)
        {
            return null;
        }

        return new int[]{width, height};
    }
}
