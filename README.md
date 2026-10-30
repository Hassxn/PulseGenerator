# Pulse Generator

Plugin gratuito desarrollado por **Hassxn** para crear mundos de HCF en **Spigot 1.8.8**, con bytecode compatible con Java 8. Proyecto Maven sin dependencias adicionales que instalar en el servidor.

## Instalar y crear un mundo

1. Usa un servidor Spigot 1.8.8 con Java 8. Haz una copia de seguridad antes de instalar plugins.
2. Copia `PulseGenerator-1.0.0.jar` a `plugins/` y reinicia el servidor.
3. Ajusta los archivos de `plugins/PulseGenerator/` antes de crear el mundo.
4. Ejecuta desde consola o como administrador:

```text
/pulsegenerator create hcf 3000 --seed 12345
/pulsegenerator status hcf
```

El ejemplo crea un mundo de **3000 bloques de lado**, centrado en 0,0: bloques X/Z de -1500 a 1499. No significa un radio de 3000. Se admiten lados entre 512 y 30000. Cada creación inicia automáticamente la pregeneración. El nombre no puede coincidir con una carpeta o mundo existente; el plugin no incluye comandos de borrado ni regeneración destructiva.

Espera a que `status` indique completado antes de abrir la temporada. Se generan, decoran y guardan los chunks; no se mantienen todos en RAM. El borde limita la zona jugable, pero el generador puede producir terreno fuera de él si otro plugin solicita chunks allí.

## Qué genera

- Llanuras y colinas suaves, sin océanos, grandes lagos, montañas ni barrancos abiertos. El relieve tiene amplitud y escala configurables.
- Spawn plano de **160 × 160** por defecto, mínimo 150, con transición suave. Toda su columna subterránea queda sin cuevas ni minerales.
- Cuatro roads orientadas a los puntos cardinales, a la misma altura que el spawn y libres de árboles. Ancho y material ajustables.
- Hasta cuatro zonas planas para KoTH/eventos, configurables por coordenadas. Las zonas que no caben con su transición dentro del borde se omiten; un mapa de 512 no usa las coordenadas predeterminadas de ±700.
- Robles compactos, abedules estrechos y robles secos, distribuidos con separación. Usan bloques vanilla y hojas con decay natural. La probabilidad de manzana configurada añade una posibilidad extra al decay vanilla del roble.
- Pequeños estanques de agua con arena y caña de azúcar; animales en grupos de dos. Son recursos distribuidos por semilla, no una garantía de animales por cada región.
- **Minas naturales:** cuevas subterráneas serpenteantes, bajas y ligeramente ampliadas. No hay cubos prison, reinicios de minas, cofres de recompensa ni spawners. Se accede excavando; no se fuerzan entradas en la superficie.
- Vetas de carbón, hierro, oro, redstone, lapislázuli, diamante y esmeralda. Perfil HCF más abundante y refuerzo de intentos cerca de cuevas. Solo sustituyen piedra. Los drops y la experiencia de minería conservan el comportamiento vanilla, incluidos Silk Touch y Fortune.

Los minerales personalizados son distribución, frecuencia, alturas y tamaño de vetas de los materiales de Minecraft 1.8. No se añaden nuevos tipos de bloque ni se necesita un paquete de recursos.

## Archivos de configuración

| Archivo | Contenido |
| --- | --- |
| `config.yml` | Terreno, spawn, roads, eventos, recursos y rendimiento de pregeneración |
| `trees.yml` | Separación, densidad, mezcla de árboles y manzanas extra |
| `ores.yml` | Perfil `vanilla`, `hcf` o `custom`, multiplicadores y vetas por mineral |
| `mines.yml` | Cuevas naturales, frecuencia, altura, radio, longitud y refuerzo mineral |
| `worlds/<mundo>.yml` | Semilla, tamaño y copia del perfil usado al crear el mundo |
| `jobs/<mundo>.yml` | Progreso persistido de pregeneración |
| `reports/<mundo>.yml` | Informe final: semilla, tamaño, chunks, perfil y coordenadas de eventos |

`/pulsegenerator reload` aplica parámetros operativos de pregeneración y prepara la configuración para mundos nuevos. **Los mundos existentes conservan su perfil completo**, también árboles, protección y minerales, para evitar cambios de generación en sus bordes. No edites su snapshot durante una temporada. Usa un mundo nuevo para cambiar su terreno.

Las cantidades de mineral son intentos de vetas: pueden ser menores cerca de cuevas, otras vetas o límites de chunk. El perfil `vanilla` es una referencia de cantidades; no reproduce el generador vanilla de Minecraft.

## Comandos y permisos

```text
/pulsegenerator help
/pulsegenerator create <mundo> <lado> [--seed <numero>]
/pulsegenerator pregen <mundo>
/pulsegenerator pause <mundo>
/pulsegenerator resume <mundo>
/pulsegenerator status <mundo>
/pulsegenerator tp <mundo>
/pulsegenerator reload
```

Permiso: `pulsegenerator.admin`, por defecto para operadores. `pregen` inicia una tarea si no existe; una tarea pausada se continúa con `resume`. Una tarea completada no se vuelve a ejecutar.

## Pregeneración y reinicios

El trabajo usa el hilo principal de Bukkit y un límite de chunks y tiempo por tick. Si baja la estimación de TPS, espera a que se recupere. El presupuesto es flexible: una llamada individual de generación no puede interrumpirse. Por eso debe realizarse antes de abrir el servidor a jugadores.

El control de TPS usa el contador del servidor (`getTPS()` o `recentTps` en Spigot 1.8.8). Si no está disponible, calcula una estimación por ventanas de cinco segundos. Así, una cadencia irregular del scheduler no sustituye los TPS nativos ni bloquea el trabajo con una estimación falsa. `status` distingue `esperando recuperacion de TPS`, `pausado`, `activo` y `tarea sin actividad`, e indica si los TPS son del servidor o estimados. `minimum-tps: 0.0` desactiva la espera; para volver a activar la protección después de actualizar, ajusta el valor a `17.0` y ejecuta `/pulsegenerator reload`. Los archivos de configuración existentes se conservan.

Se cargan vecinos para completar la población en 1.8 y se incluye un halo fuera del borde. Se descargan y guardan chunks lejos de jugadores respetando los controles de uso del servidor. Los puntos de control guardan el mundo antes de persistir el cursor, con reemplazo atómico cuando el sistema de archivos lo permite. Un cierre inesperado puede repetir trabajo reciente, sin regenerar chunks guardados. No sustituye una copia de seguridad contra fallos de disco.

La pregeneración verifica población y completa explícitamente la pasada de iluminación diferida de Spigot 1.8.8 mediante reflexión sobre `v1_8_R3`. No marca un chunk completado si esa pasada falla. Esta parte requiere esa revisión del servidor; otros forks deben conservar sus métodos internos.

Tras un reinicio se cargan los mundos registrados y se retoman las tareas activas si `auto-resume: true`. Las tareas pausadas explícitamente siguen pausadas. El tamaño del halo se conserva por tarea.

## Integración con HCF

Pulse prepara el terreno y ofrece protección básica del spawn frente a bloques, cubos, explosiones, fuego, pistones y cambios de entidades. Los administradores pueden construir. No modifica cancelaciones de otros plugins.

El core HCF debe gestionar **claims, safezone, PvP, equipos, KoTH, temporizadores y portales**. Las zonas planas no registran eventos por sí mismas. Roads y pads no se protegen como claims automáticamente. El plugin genera el Overworld; no modifica Nether o End.

Para usar el mundo como principal, créalo primero, detén el servidor y configura `level-name=hcf` en `server.properties`. Puedes indicar su generador en `bukkit.yml`:

```yaml
worlds:
  hcf:
    generator: PulseGenerator
```

Conserva la carpeta del mundo y `plugins/PulseGenerator/worlds/hcf.yml`. No uses gestores externos para cargarlo previamente con otro generador. La compatibilidad específica con forks y cores HCF requiere probarlos juntos.

## Compilar

Con Maven 3.9 y JDK 17 o superior:

```text
mvn clean package
```

El resultado es `target/PulseGenerator-1.0.0.jar`, compilado para Java 8. Las dependencias de Spigot tienen scope `provided` y no se incluyen en el JAR. El módulo histórico `bungeecord-chat` se excluye porque el plugin no utiliza esa API.

Las pruebas automáticas verifican spawn sólido y despejado, repetibilidad, suavidad, roads, abundancia de mineral, presencia de cuevas, borde y pads de eventos.
