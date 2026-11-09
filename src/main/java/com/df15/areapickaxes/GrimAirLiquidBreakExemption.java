package com.df15.areapickaxes;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nota (2026-09-03): al minar con un pico de area (ver
 * AreaPickaxeListener), los bloques "extra" se rompen con
 * Player.breakBlock(Block), que SI dispara un BlockBreakEvent real para
 * cada uno -- pero eso NO envia ningun paquete de digging al cliente
 * para esos bloques. Si el jugador sigue picando/moviendose hacia esa
 * zona justo cuando el server ya la ha vaciado, su cliente puede enviar
 * un paquete START_DIGGING para una posicion que, en el propio estado
 * que GrimAC lleva de ese bloque, ya es aire -- eso es exactamente lo
 * que el check "AirLiquidBreak" de GrimAC esta diseñado para detectar
 * ("Breaking a block that cannot be broken"), y en jugadores usando
 * picos de area es un falso positivo estructural de la funcionalidad,
 * no un intento de hack.
 *
 * No existe (a fecha de esta sesion) un permiso de bypass por-check en
 * GrimAC, asi que en vez de eximir al jugador de TODOS los checks (
 * demasiado permisivo/inseguro), escuchamos el evento publico de GrimAC
 * "ac.grim.grimac.api.events.FlagEvent" (cancelable) y cancelamos SOLO
 * los flags de AirLiquidBreak que le lleguen a un jugador dentro de una
 * ventana corta (GRACE_MS) despues de que nuestro propio plugin le haya
 * roto bloques de area -- para cualquier otro jugador, o para cualquier
 * otro check, GrimAC sigue actuando con total normalidad.
 *
 * Todo esto se hace por REFLEXION (Class.forName + registerEvent de bajo
 * nivel con un EventExecutor) para que AreaPickaxes NO dependa de GrimAC
 * en tiempo de compilacion -- si GrimAC no esta instalado, o cambia esta
 * API concreta en una actualizacion futura, este enganche simplemente no
 * se activa (o falla en silencio en el propio evento) y el resto del
 * plugin sigue funcionando exactamente igual.
 *
 * IMPORTANTE: el metodo publico "FlagEvent" que usamos aqui esta
 * marcado @Deprecated(forRemoval = true) desde GrimAC 1.2.1.0 (sigue
 * presente y funcional en la 2.3.74 instalada ahora) -- si en una
 * futura actualizacion de GrimAC dejara de existir, este enganche
 * simplemente no se registrara (falla en silencio, ver register()) y
 * habria que revisar la API nueva de GrimAC para reemplazarlo.
 */
public class GrimAirLiquidBreakExemption
{
    private static final String FLAG_EVENT_CLASS = "ac.grim.grimac.api.events.FlagEvent";
    private static final String AIR_LIQUID_BREAK_STABLE_KEY = "grim.breaking.air_liquid_break";

    // Cuanto tiempo, tras un break de area nuestro, seguimos dispuestos
    // a perdonar un AirLiquidBreak de ese jugador.
    private static final long GRACE_MS = 1500L;

    private static final Map<UUID, Long> recentAreaBreaks = new ConcurrentHashMap<>();

    private GrimAirLiquidBreakExemption() {}

    /**
     * Llamar justo antes (o durante) de romper los bloques "extra" de un
     * pico de area para un jugador -- ver AreaPickaxeListener.onBlockBreak.
     */
    public static void markRecentAreaBreak( Player player )
    {
        recentAreaBreaks.put( player.getUniqueId(), System.currentTimeMillis() );
    }

    @SuppressWarnings( "unchecked" )
    public static void register( Plugin plugin )
    {
        try
        {
            Class<? extends Event> flagEventClass =
                    (Class<? extends Event>) Class.forName( FLAG_EVENT_CLASS );

            EventExecutor executor = (listener, event) -> {
                try
                {
                    handleFlagEvent( event );
                }
                catch ( Throwable t )
                {
                    // Nunca dejar que un fallo aqui rompa GrimAC ni tumbe
                    // el resto del procesamiento de eventos del server.
                    plugin.getLogger().warning(
                            "AreaPickaxes: error comprobando un FlagEvent de GrimAC: " + t.getMessage() );
                }
            };

            Bukkit.getPluginManager().registerEvent(
                    flagEventClass, new Listener() {}, EventPriority.NORMAL,
                    executor, plugin, false );

            plugin.getLogger().info(
                    "AreaPickaxes: enganchado a GrimAC (evita falsos positivos de AirLiquidBreak con picos de area)." );
        }
        catch ( ClassNotFoundException e )
        {
            // GrimAC no esta instalado -- nada que hacer.
        }
        catch ( Throwable t )
        {
            plugin.getLogger().warning(
                    "AreaPickaxes: no se pudo enganchar a GrimAC (puede que haya cambiado su API): " + t.getMessage() );
        }
    }

    private static void handleFlagEvent( Event event ) throws Exception
    {
        Method getCheck = event.getClass().getMethod( "getCheck" );
        Object check = getCheck.invoke( event );
        if ( check == null )
        {
            return;
        }

        Method getStableKey = check.getClass().getMethod( "getStableKey" );
        Object stableKey = getStableKey.invoke( check );

        if ( !AIR_LIQUID_BREAK_STABLE_KEY.equals( stableKey ) )
        {
            // No es el check que nos interesa -- dejar que GrimAC actue normal.
            return;
        }

        Method getUser = event.getClass().getMethod( "getUser" );
        Object grimUser = getUser.invoke( event );
        if ( grimUser == null )
        {
            return;
        }

        Method getUniqueId = grimUser.getClass().getMethod( "getUniqueId" );
        UUID uuid = (UUID) getUniqueId.invoke( grimUser );

        Long last = recentAreaBreaks.get( uuid );
        if ( last == null || System.currentTimeMillis() - last > GRACE_MS )
        {
            // Este jugador no ha usado un pico de area hace poco -- puede
            // ser un intento real, dejar que GrimAC lo procese normal.
            return;
        }

        Method setCancelled = event.getClass().getMethod( "setCancelled", boolean.class );
        setCancelled.invoke( event, true );
    }
}
