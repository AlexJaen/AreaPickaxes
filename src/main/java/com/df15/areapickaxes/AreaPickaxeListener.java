package com.df15.areapickaxes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nota (2026-08-13): "picos que minen en areas
 * de 2x2, 2x3, 3x3, etc." -- cuando un jugador rompe un bloque con un
 * pico marcado (ver AreaPickaxeUtil), se rompen TAMBIEN los bloques
 * vecinos en un rectangulo de ancho x alto alrededor del bloque
 * original, en el plano PERPENDICULAR a hacia donde esta mirando el
 * jugador (si miras hacia el norte/sur, el rectangulo se extiende en
 * X/Y; si miras hacia arriba/abajo, en X/Z; etc.) -- el mismo criterio
 * que usan los picos de excavacion/quarry de otros servidores de
 * prision, para poder vaciar una cara de mina de una pasada.
 *
 * Los bloques extra se rompen con Player.breakBlock(Block) (API de
 * Paper) en vez de Block.breakNaturally(...) a proposito: breakBlock()
 * simula un bloque roto REALMENTE por ese jugador, disparando
 * BlockBreakEvent de verdad para cada uno -- asi WorldGuard (proteccion
 * de regiones), Prison (economia de bloques minados/rankup) y
 * cualquier otro plugin que escuche BlockBreakEvent siguen funcionando
 * exactamente igual que si el jugador hubiera roto cada bloque a mano
 * uno a uno. Esto SI significa que nuestro propio listener se volveria
 * a llamar a si mismo para cada bloque extra -- de ahi el guard
 * "synthesizedBreaks" de abajo, para no re-expandir el area
 * recursivamente sobre los bloques que ya generamos nosotros.
 *
 * Nota (2026-10-01): bug confirmado, los contenedores rotos con el pico de area perdian su contenido. El BlockBreakEvent real SI se
 * dispara (WorldGuard/Prison/CoreProtect lo ven bien), pero
 * Player.breakBlock() NO reproduce el mecanismo interno de Minecraft que
 * vacia el inventario de un contenedor (cofre/barril/horno/etc) al mundo
 * antes de destruirlo -- ese vaciado ocurre en el camino normal de
 * "jugador excava un bloque" a traves del paquete del cliente
 * (PlayerInteractManager de NMS), NO en el metodo generico que usa esta
 * API de conveniencia de Paper para simular la rotura. El resultado neto
 * es identico a "borrar" el bloque para cualquier bloque con inventario:
 * el contenedor desaparece y sus items simplemente no llegan a
 * dropearse, aunque el propio bloque (el item "cofre", "horno", etc.) si
 * caiga con normalidad.
 *
 * Fix: antes de romper cada bloque extra, comprobar si es un
 * org.bukkit.block.Container (cofre, barril, horno/alto horno/ahumador,
 * dispensador, soltador, tolva, soporte de pociones, shulker box -- la
 * interfaz cubre todos los bloques con inventario de una vez, sin listar
 * materiales a mano) y, si lo es, capturar una copia de su inventario
 * ANTES de romperlo. Si breakBlock() tiene exito, se vacia el inventario
 * (por si el vaciado real no ocurrio, para evitar duplicados si acaso SI
 * ocurriera en alguna version futura de Paper) y se sueltan los items
 * capturados al mundo manualmente con dropItemNaturally(), igual que
 * haria un cofre roto a mano. Si breakBlock() devuelve false (region
 * protegida), no se toca el inventario para nada -- se captura la copia
 * pero nunca se llega a vaciar ni soltar, el cofre se queda exactamente
 * como estaba.
 *
 * Cofres dobles: Chest#getInventory() en la mitad capturada devuelve la
 * DoubleChestInventory COMPLETA (54 slots, ambas mitades) si el cofre
 * esta emparejado, y esa vista es real (no una copia), asi que
 * vaciarla tras un break con exito vacia literalmente las DOS mitades de
 * una vez. Cuando el bucle llegue a la segunda mitad (si tambien esta
 * dentro del area), su inventario ya estara vacio -- no hay doble drop.
 */
public class AreaPickaxeListener implements Listener
{
    private final Set<Location> synthesizedBreaks = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e)
    {
        Location loc = e.getBlock().getLocation();

        if(synthesizedBreaks.remove(loc))
        {
            // Este break lo hemos disparado nosotros mismos expandiendo
            // un area -- no volver a expandir sobre el (evita recursion
            // infinita / un area que crece sola).
            return;
        }

        Player player = e.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        int[] dims = AreaPickaxeUtil.getAreaDimensions(tool);

        if(dims == null)
        {
            return;
        }

        int width = dims[0];
        int height = dims[1];

        // Nota (2026-09-03): marca a este jugador como "acaba de usar un
        // pico de area" -- si GrimAC le marca un falso positivo de
        // AirLiquidBreak en los proximos instantes (causado por los
        // breaks sinteticos de abajo), GrimAirLiquidBreakExemption lo
        // cancela. Ver esa clase para la explicacion completa.
        GrimAirLiquidBreakExemption.markRecentAreaBreak(player);

        for(Block extra : computeAreaBlocks(e.getBlock(), player.getEyeLocation(), width, height))
        {
            if(extra.getType().isAir() || extra.getType() == Material.BEDROCK)
            {
                continue;
            }

            Location extraLoc = extra.getLocation();

            // Nota (2026-11-04): DUPE DE SHULKER BOX.
            // Una shulker box conserva su contenido DENTRO del propio item
            // (componente container) cuando se rompe, asi que el drop
            // vanilla de breakBlock() ya incluye todo. Pero el codigo de
            // abajo captura el inventario y lo suelta ADEMAS a mano con
            // dropItemNaturally() -> el jugador recibe la shulker con N
            // items + N items sueltos; mete los sueltos dentro, la coloca,
            // la rompe de nuevo con el pico de area (junto a otro bloque
            // cualquiera, p.ej. un caldero, que actua de bloque "primario")
            // y el contenido se DUPLICA en cada vuelta (1,2,4,8,...). Visto
            // en CoreProtect desde 2026-10-22 (parche de 2026-10-01).
            // Fix: una shulker box NUNCA se rompe como bloque extra del
            // area; se queda en su sitio (se puede romper a mano, que si
            // funciona bien).
            if(extra.getType().name().endsWith("SHULKER_BOX"))
            {
                continue;
            }

            // Si es un contenedor (cofre/barril/horno/dispensador/tolva/
            // shulker box/etc), capturamos una copia de su contenido ANTES
            // de romperlo -- Player.breakBlock() no vacia contenedores por
            // si solo, ver el comentario grande de la clase.
            List<ItemStack> containerContents = null;

            BlockState extraState = extra.getState();

            if(extraState instanceof Container)
            {
                Inventory inv = ((Container) extraState).getInventory();

                containerContents = new ArrayList<>();

                for(ItemStack item : inv.getContents())
                {
                    if(item != null && !item.getType().isAir())
                    {
                        containerContents.add(item.clone());
                    }
                }
            }

            synthesizedBreaks.add(extraLoc);

            boolean broke = player.breakBlock(extra);

            if(!broke)
            {
                // No se pudo romper (region protegida, bloque irrompible,
                // etc.) -- limpiar el guard para no dejarlo acumulado por
                // si el jugador rompe ese mismo bloque a mano mas tarde.
                // El inventario capturado arriba NUNCA se toca ni se
                // suelta en este caso -- el contenedor se queda intacto.
                synthesizedBreaks.remove(extraLoc);
            }
            else if(containerContents != null && !containerContents.isEmpty())
            {
                // Rotura con exito de un contenedor con items dentro --
                // vaciar por si acaso (cofres dobles comparten esta misma
                // llamada a inventario, ver comentario de clase) y soltar
                // los items capturados al mundo, en el centro del bloque.
                Inventory inv = ((Container) extraState).getInventory();
                inv.clear();

                Location dropLoc = extraLoc.clone().add(0.5D, 0.5D, 0.5D);

                for(ItemStack item : containerContents)
                {
                    extra.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
        }
    }

    /**
     * Nota (2026-08-14): esta version YA NO usa Player.getFacing().
     * Ese metodo colapsa la mirada del jugador a UN SOLO eje (el mas
     * cercano al vector de vision, de los 6 posibles: N/S/E/W/UP/DOWN) --
     * en la practica, minar hacia delante mirando ligeramente hacia abajo
     * (muy habitual al picar un tunel horizontal caminando) podia colapsar
     * a DOWN en vez de a la direccion horizontal real, y el area salia en
     * el plano equivocado (suelo en vez de pared). Ahora se calcula el
     * plano a mano a partir del pitch/yaw reales del jugador, con un
     * umbral explicito para "estoy mirando hacia arriba/abajo de verdad"
     * (pitch >= 55 grados en cualquier signo) -- fuera de ese umbral,
     * siempre se usa la pared vertical correspondiente al yaw, cubriendo
     * con fiabilidad tanto la mineria horizontal (tunel) como la vertical
     * (pozo/escalera) en sus dos ejes cada una.
     */
    private List<Block> computeAreaBlocks(Block origin, Location eyeLoc, int width, int height)
    {
        List<Block> result = new ArrayList<>();

        int startW = -(width / 2);
        int startH = -(height / 2);

        for(int w = 0; w < width; w++)
        {
            for(int h = 0; h < height; h++)
            {
                int offsetW = startW + w;
                int offsetH = startH + h;

                if(offsetW == 0 && offsetH == 0)
                {
                    // Ese es el bloque original, ya lo rompe el propio evento.
                    continue;
                }

                Block extra = offsetBlock(origin, eyeLoc, offsetW, offsetH);

                result.add(extra);
            }
        }

        return result;
    }

    /**
     * Traduce un desplazamiento (offsetW, offsetH) dentro del plano de
     * mineria a coordenadas de mundo reales, calculando el plano
     * directamente del pitch/yaw del jugador (ver comentario de
     * computeAreaBlocks):
     *   - |pitch| >= 55 (mirando arriba/abajo)         -> plano X (ancho) / Z (alto)
     *   - resto (mirando mas o menos en horizontal):
     *       - yaw ~ Norte/Sur (mirando por el eje Z)   -> plano X (ancho) / Y (alto)
     *       - yaw ~ Este/Oeste (mirando por el eje X)  -> plano Z (ancho) / Y (alto)
     * El bucket de yaw usa la misma convencion que BlockFace de Bukkit
     * (yaw 0/360 = Sur, 90 = Oeste, 180 = Norte, 270 = Este), dividido en
     * 4 sectores de 90 grados centrados en cada direccion cardinal.
     */
    private Block offsetBlock(Block origin, Location eyeLoc, int offsetW, int offsetH)
    {
        int dx = 0, dy = 0, dz = 0;

        float pitch = eyeLoc.getPitch();

        if(Math.abs(pitch) >= 55f)
        {
            // Mirando hacia arriba o hacia abajo -- plano horizontal X/Z
            // (pozo, escalera, minar el suelo/techo).
            dx = offsetW;
            dz = offsetH;
        }
        else
        {
            // Mirando mas o menos en horizontal -- normaliza el yaw a
            // 0-360 y lo bucketiza en 4 sectores de 90 grados.
            float yaw = eyeLoc.getYaw() % 360f;

            if(yaw < 0f)
            {
                yaw += 360f;
            }

            boolean facingNorthSouth = (yaw >= 135f && yaw < 225f) || yaw >= 315f || yaw < 45f;

            if(facingNorthSouth)
            {
                // Mirando hacia el Norte o el Sur -- la pared que se mina
                // esta en el plano X (ancho) / Y (alto).
                dx = offsetW;
                dy = offsetH;
            }
            else
            {
                // Mirando hacia el Este o el Oeste -- la pared que se mina
                // esta en el plano Z (ancho) / Y (alto).
                dz = offsetW;
                dy = offsetH;
            }
        }

        return origin.getRelative(dx, dy, dz);
    }
}
