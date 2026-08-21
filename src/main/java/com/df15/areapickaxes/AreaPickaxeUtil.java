package com.df15.areapickaxes;

import org.bukkit.ChatColor;
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
 */
public class AreaPickaxeUtil
{
    public static final NamespacedKey AREA_WIDTH_KEY = new NamespacedKey("areapickaxes", "area-width");
    public static final NamespacedKey AREA_HEIGHT_KEY = new NamespacedKey("areapickaxes", "area-height");

    /**
     * Sesion 2026-08-15, segunda pasada (la primera version -- tiers por
     * area total con nombres tipo "Pico Titánico"/"Pico Apocalíptico" --
     * no convencio al usuario: "Vamos a hacer que el contexto sea de
     * cárcel. Tipo con escapadas, túneles, butrones y cosas así").
     * Rediseñado con tematica de fuga carcelaria, y ahora emparejado
     * EXACTO por tamaño (no por rango de area): el usuario dio 4
     * tamaños concretos con su color exacto, no una formula. width y
     * height se comparan sin importar el orden (2x1 y 1x2 son el mismo
     * tier) porque a efectos de minado da igual la orientacion.
     *
     * Formato pedido explicitamente: nombre siempre en NEGRITA (con el
     * color del tier), lore siempre en BLANCO CURSIVA (sin mezclar
     * grises ni otros colores como en la version anterior).
     *
     * Tamaños sin tier explicito (p.ej. 1x1, 4x4... el comando admite
     * hasta 9x9): caen en un tier por defecto ("Pico del Recluso", gris)
     * para que el plugin no se rompa si se usan, pero no estan pensados
     * para usarse normalmente -- si se quieren tematizar tambien, avisar.
     */
    private static final class Tier
    {
        final int a;
        final int b;
        final String displayName;
        final ChatColor color;
        final String[] flavorLore;

        // matchAny=true para el tier por defecto (cualquier tamaño no
        // listado explicitamente).
        final boolean matchAny;

        Tier(int a, int b, String displayName, ChatColor color, String... flavorLore)
        {
            this.a = a;
            this.b = b;
            this.displayName = displayName;
            this.color = color;
            this.flavorLore = flavorLore;
            this.matchAny = false;
        }

        Tier(String displayName, ChatColor color, String... flavorLore)
        {
            this.a = -1;
            this.b = -1;
            this.displayName = displayName;
            this.color = color;
            this.flavorLore = flavorLore;
            this.matchAny = true;
        }
    }

    private static final Tier[] TIERS = new Tier[]
    {
        new Tier(2, 1, "Pico del Túnel",         ChatColor.BLUE,
            "Cavas un poco cada noche, sin hacer ruido."),
        new Tier(2, 2, "Pico del Butrón",        ChatColor.YELLOW,
            "Un agujero en la pared y ya estás fuera de tu celda."),
        new Tier(2, 3, "Pico de la Fuga",        ChatColor.RED,
            "Ya no hay vuelta atrás, la salida está cerca."),
        new Tier(3, 3, "Pico de la Gran Evasión", ChatColor.DARK_PURPLE,
            "La fuga que se cuenta durante generaciones."),
        // Por defecto, para cualquier tamaño no listado arriba.
        new Tier("Pico del Recluso", ChatColor.GRAY,
            "Todavía no ha visto la luz del día.")
    };

    private static Tier resolveTier(int width, int height)
    {
        int lo = Math.min(width, height);
        int hi = Math.max(width, height);

        for(Tier tier : TIERS)
        {
            if(!tier.matchAny && Math.min(tier.a, tier.b) == lo && Math.max(tier.a, tier.b) == hi)
            {
                return tier;
            }
        }

        // El ultimo elemento de TIERS es siempre el tier por defecto (matchAny).
        return TIERS[TIERS.length - 1];
    }

    /**
     * Marca un ItemStack como pico de area con el ancho/alto dados.
     * Anade tambien nombre y lore (segun el tier que le corresponda por
     * tamaño exacto, ver resolveTier()) para que se distinga a simple
     * vista de un pico normal. Modifica el ItemStack en el sitio (via
     * setItemMeta).
     */
    public static void tagAsAreaPickaxe(ItemStack pickaxe, int width, int height)
    {
        ItemMeta meta = pickaxe.getItemMeta();

        meta.getPersistentDataContainer().set(AREA_WIDTH_KEY, PersistentDataType.INTEGER, width);
        meta.getPersistentDataContainer().set(AREA_HEIGHT_KEY, PersistentDataType.INTEGER, height);

        Tier tier = resolveTier(width, height);

        // Nombre: color del tier + negrita, sin excepciones.
        meta.setDisplayName(tier.color + "" + ChatColor.BOLD + tier.displayName);

        // Lore: blanco cursiva, sin excepciones (incluida la linea
        // funcional del tamaño real, para que el admin lo siga viendo).
        List<String> lore = new ArrayList<>();

        for(String flavorLine : tier.flavorLore)
        {
            lore.add(ChatColor.WHITE + "" + ChatColor.ITALIC + flavorLine);
        }

        lore.add("");
        lore.add(ChatColor.WHITE + "" + ChatColor.ITALIC + "Mina un área de " + width + "x" + height + " bloques de una vez.");

        meta.setLore(lore);

        // Brillo de encantamiento puramente visual (API moderna de Paper
        // 1.20.5+), mismo patron ya usado en RareItemHunter para sus
        // items especiales -- no anade ningun encantamiento real.
        meta.setEnchantmentGlintOverride(true);

        pickaxe.setItemMeta(meta);
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
