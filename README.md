# AreaPickaxes

Plugin para Paper 1.21.x que da picos de minería en área: en vez de minar un solo bloque, minan un rectángulo (2x2, 2x3, 3x3, etc.) alrededor del bloque golpeado.

## Comandos

| Comando | Descripción | Permiso |
|---|---|---|
| `/apick give <jugador> <AREAxAREA> [material]` | Da un pico de minería por área (solo admin) | `areapickaxes.admin.give` |

## Instalación

Descarga el `.jar` desde [Releases](../../releases) (o compílalo tú mismo, ver abajo) y colócalo en la carpeta `plugins/` de tu servidor Paper 1.21.x.

## Compilar desde el código fuente

Requiere JDK 21 y Maven.

```
mvn package
```

El `.jar` resultante queda en `target/areapickaxes-<version>.jar`.

## Autor

Lorealex
