# Snake Race — ARSW Lab #2 (Java 21, Virtual Threads)

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de programación concurrente: condiciones de carrera, sincronización y colecciones seguras.

---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.

Respuesta:

Exíte una clase llamada SnakeRunner que implementa runnable, eso quiere decir que la función de la clase
es decir qué tarea debe hacer el  hilo. Esta clase tiene 5 atributos: `snake`, `board`, `baseSleepMs`, `turboSleepMs` y `turboTicks`.

A continuación se mostrará lo que hace el hilo al iniciar:
1. Verifíca que el hilo no esté interrumpido.
2. Si no lo está entonces puede que la serpiente cambie de dirección en el tablero.
3. Si la serpiente choca contra un obstáculo, esta cambia de dirección de forma aleatoria.
4. Si la serpiente come un turbo entonces el tiempo en el que está dormido el hilo será menor por lo que la serpiente se mueve más rápido, pero con el tiempo vuelve a su velocidad normal.

Cabe resaltar que cada serpiente corre su método run() en paralelo con los demás.

- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**




## REPORTE DE LABORATORIO
### 1) Análisis de concurrencia
**Cómo los hilos dan autonimía a cada serpiente**

Exíte una clase llamada SnakeRunner que implementa runnable, eso quiere decir que la función de la clase
es decir qué tarea debe hacer el  hilo. Esta clase tiene 5 atributos: `snake`, `board`, `baseSleepMs`, `turboSleepMs` y `turboTicks`.

A continuación se mostrará lo que hace el hilo al iniciar:
1. Verifíca que el hilo no esté interrumpido.
2. Si no lo está entonces puede que la serpiente cambie de dirección en el tablero.
3. Si la serpiente choca contra un obstáculo, esta cambia de dirección de forma aleatoria.
4. Si la serpiente come un turbo entonces el tiempo en el que está dormido el hilo será menor por lo que la serpiente se mueve más rápido, pero con el tiempo vuelve a su velocidad normal.

Cabe resaltar que cada serpiente corre su método run() en paralelo con los demás.

**Condiciones carrera**

Hay tres candidatos posibles: `res`, `turboTicks` y `board.step(snake)`

- res: es una variable local dentro del hilo asi que cada hilo tiene un res diferente por lo que no sería una condición
carrera.
- turboTicks: en cada interación turboTicks está disminuyendo, así que esta si sería una variable mutable. El problema es que
la varaible no se comparte en cada hilo, es una variable "privada" de cada hilo.
- board.step(snake): puede ser una condición de carrera porque todas las serpientes, cada una manejada con un hilo, interactuan con un mismo tablero.

- La clase **snake**: La clase es accedida concurrentemente por múltiples threads sin sincronización.

**Colecciones o estructuras no seguras para hilos**

- HashSet: La clase Board usa esta colección para almacenar  `obstáculos`,  `ratones` y `turbos`. 

¿Cuál es el inconveniente?

El problema es que HashSet no es una colección segura para hilos. Cuando varias serpientes interactua al mismo tiempo con estas colecciones, pueden ocurrir condiciones de carrera. En el caso de los ratones, si una serpiente se come un ratón, el ratón debe ser 
eliminado del Hash y además se debe poner un nuevo ratón aleatoriamente en el tablero y se debe agregar a la colección. Si otra serpiente modifíca la colección al mismo tiempo, puede que halla un resultado incosistente.

- HashMap: la clase Board utiliza esta estructura de datos para guardar `teleports`.

HashMap tampoco es thread-safe. Si no se protegiera adecuadamente, accesos concurrentes podrían causar inconsistencias en los pares de teletransporte.


**Sincronización innecesaria**
![Captura de pantalla 2026-02-05 113609.png](src/img/Captura%20de%20pantalla%202026-02-05%20113609.png)

En la imagen podemos ver que estos métodos utilizan la palabra clave `synchronized`. Aunque estos métodos pueden ser llamados desde distintos hilos, no interactúan directamente con la lógica de 
movimiento de las serpientes ni modifican el estado del tablero, por lo que su sincronización resulta innecesaria. 


