# Validación de Pulse Generator 1.0.0

Fecha: 30 de octubre de 2026.

## Compilación y pruebas

- Maven: `package` finalizado con BUILD SUCCESS.
- Bytecode generado para Java 8 (`--release 8`), compilado con JDK 25.
- Cinco pruebas JUnit aprobadas, sin fallos ni errores:
  - Spawn de 160 × 160 completamente plano, sólido bajo tierra y despejado.
  - Semilla determinista independientemente del orden de generación.
  - Suavidad del relieve y roads despejadas.
  - Cuevas presentes y mayor cantidad de mineral en el perfil HCF que en la referencia vanilla.
  - Límites del mapa de 3000 y pads de eventos planos.

## Ejecución real

Servidor de prueba aislado, enlazado a localhost:

- Spigot 1.8.8, `git-Spigot-21fe707-e1ebe52`, revisión `v1_8_R3`.
- Java 8, Amazon Corretto 1.8.0_512.
- Mundo `hcf_test`, lado 512, semilla 12345.
- Creación del mundo y activación del plugin correctas.
- Pausa guardada en 227/1296 chunks; tras apagar y reiniciar, estado y cursor conservados.
- Reanudación y finalización al 100 %.
- Mundo cargado también como mundo principal con `level-name` y generador en `bukkit.yml`.
- Creación sobre un nombre existente rechazada; tamaño menor a 512 rechazado.
- Informe final guardado con semilla, tamaño, perfil y 25 sistemas de cuevas con origen dentro del borde.

Se verificaron directamente los NBT de los archivos Anvil después del cierre del servidor. La pasada final, con iluminación explícita, produjo:

```text
Saved chunks: 1296/1296; TerrainPopulated: 1296; LightPopulated: 1296
Playable chunks saved: 1024/1024; lighting complete: 1024
```

El margen exterior explica la diferencia entre los 1024 chunks jugables y los 1296 objetivos de pregeneración. Los vecinos adicionales que solicita el motor pueden crear más chunks fuera de ese margen.

## Alcance

La prueba de servidor usó un mapa de 512 × 512; no se pregeneró un mapa completo de 3000 × 3000 durante esta validación. No se realizó una sesión visual de juego ni una prueba combinada con un core HCF o fork personalizado. Las protecciones y el rendimiento con jugadores requieren validación en el servidor de destino. La generación y sus archivos están preparados para tamaños configurables dentro del rango documentado.
