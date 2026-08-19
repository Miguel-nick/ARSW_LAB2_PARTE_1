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

---


# SOLUCION REPORTE DE LABORATORIO

**Reporte — Parte I (Ejercicio wait/notify)**

- **Objetivo breve:** hacer que varios hilos busquen números primos en paralelo, y que cada cierto tiempo (t ms) el programa pause todos los hilos, muestre cuántos primos se han encontrado y espere ENTER para reanudar.

- **Qué implementé:** añadí un pequeño programa en `src/main/java/co/eci/prime/` con dos clases:
  - `PrimeFinder`: arranca los hilos trabajadores, cada `t` ms solicita la pausa, muestra el conteo y espera ENTER para reanudar.
  - `PrimeWorker`: cada hilo toma números para comprobar y aumenta el contador cuando encuentra un primo.

- **Cómo se pausa y se reanuda (explicación simple):**
  - Todos los hilos usan el mismo objeto como "cerradura" (monitor). Cuando toca pausar, el hilo principal pone una señal de pausa y los trabajadores se quedan esperando sin consumir CPU (se bloquean). Al pulsar ENTER el hilo principal quita la señal y despierta a todos.
  - Técnicamente esto se hace con `synchronized` sobre ese objeto y las llamadas `wait()` (trabajadores) y `notifyAll()` (hilo principal). El resultado: no hay "esperas activas" que malgasten CPU.

- **Por qué esto evita problemas comunes:**
  - Se usa un único monitor para coordinar, por lo que no se corrige información entre distintos candados.
  - Los trabajadores usan `while (paused) wait()` para protegerse de reactivaciones espurias y evitar condiciones de carrera al entrar/ salir de la pausa.

- **Pruebas realizadas:**
  - El proyecto compila con `mvn -DskipTests compile`.
  - Ejecuté el programa y verifiqué que cada intervalo pausa, muestra el número de primos encontrados y espera la tecla ENTER para reanudar. También comprobé que no hay busy-waiting y que la reanudación despierta a todos los hilos.

- **Cómo ejecutar (ejemplo):**
```bash
mvn -DskipTests compile
java -Dthreads=4 -Dt=2000 -cp target/classes co.eci.prime.PrimeFinder
```

- **Archivos añadidos/modificados importantes:**
  - `src/main/java/co/eci/prime/PrimeFinder.java` (programa principal, con comentarios en español)
  - `src/main/java/co/eci/prime/PrimeWorker.java` (trabajador, con comentarios en español)
  - `README.md` (este reporte agregado al final)

- **Notas finales:** el diseño busca ser mínimo y seguro: sólo se protege la región necesaria para la pausa/reanudación (no hay bloqueos largos), y se evita busy-wait. La parte práctica del ejercicio (pausar cada t ms, mostrar conteo y esperar ENTER) está implementada y probada.
- Creamos un programa que lanza hilos que buscan primos, lo pausa periódicamente mostrando cuántos primos hay y espera ENTER para seguir; la pausa usa el mecanismo estándar de monitores en Java evitando esperas activas.


---


# Reporte - Parte II
1. Modelo de ejecución
- Objetivo breve: analizar y corregir los problemas de concurrencia del juego SnakeRace, haciendo que cada serpiente pueda ejecutarse de manera independiente y que el estado compartido del tablero sea seguro para múltiples hilos. Cómo funciona: cada serpiente tiene un trabajador independiente representado por SnakeRunner.

- Los trabajadores se ejecutan utilizando Virtual Threads de Java 21:
```bash
var exec = Executors.newVirtualThreadPerTaskExecutor();
```
- Cada serpiente se ejecuta mediante:
```bash
exec.submit(new SnakeRunner(s, board, clock));
```
2. Condiciones de carrera encontradas durante el análisis se identificaron varios recursos compartidos entre los diferentes SnakeRunner.Estado del Board

- El tablero contiene varias colecciones:
- De esta manera, cada serpiente tiene su propio flujo de ejecución y puede avanzar de forma autónoma.
```bash
Set<Position> mice;
Set<Position> obstacles;
Set<Position> turbo;
Map<Position, Position> teleports;
```
- Estas estructuras utilizan HashSet y HashMap, que no son thread-safe. Como varias serpientes pueden acceder al tablero al mismo tiempo, existía el riesgo de:

- modificaciones concurrentes;
- pérdida de actualizaciones;
- lecturas inconsistentes;
- problemas al agregar o eliminar elementos simultáneamente.

Solución

- Se protegieron las operaciones del tablero mediante synchronized.

Por ejemplo:
```bash
public synchronized MoveResult step(Snake snake)
```
- También se protegieron las operaciones de lectura:
```bash
public synchronized Set<Position> mice()
public synchronized Set<Position> obstacles()
public synchronized Set<Position> turbo()
public synchronized Map<Position, Position> teleports()
```
- Además, las colecciones no se entregan directamente. Se devuelve una copia:
```bash
return new HashSet<>(mice);
```
- Esto evita que la interfaz gráfica pueda modificar accidentalmente las colecciones internas del tablero.
- 
3. Problema con el cuerpo de Snake

El cuerpo de cada serpiente utiliza:
```bash
Deque<Position>
```
con una implementación:
```bash
ArrayDeque
```
ArrayDeque no es una colección thread-safe.

- Esto podía generar problemas porque el SnakeRunner modifica el cuerpo mientras la interfaz gráfica intenta leerlo para dibujarlo.

Solución

Se implementó un método snapshot():
```bash
public Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
}
```
La interfaz gráfica no accede directamente al body.

En su lugar obtiene una copia:
```bash
var body = s.snapshot();
```
De esta manera, la UI trabaja sobre una fotografía del estado de la serpiente y no sobre la estructura que está modificando el hilo de ejecución.

4. Dirección de las serpientes

La dirección de cada serpiente se declaró como:
```bash
private volatile Direction direction;
```
Se utiliza volatile porque la dirección puede ser modificada desde los controles de la interfaz mientras el SnakeRunner la está leyendo.

Esto garantiza la visibilidad de los cambios entre los diferentes hilos.

Por ejemplo, cuando el usuario presiona una flecha:
```bash
player.turn(Direction.LEFT);
```
el hilo que ejecuta la serpiente puede observar correctamente el nuevo valor.

5. Eliminación de esperas activas

Otro objetivo importante fue evitar el uso de busy-waiting.

En lugar de utilizar ciclos que revisen constantemente si el juego está pausado, se utilizó Condition junto con ReentrantLock en GameClock.

La espera se realiza mediante:
```bash
canRun.await();
```
y la reanudación mediante:
```bash
canRun.signalAll();
```
Esto permite que los hilos permanezcan bloqueados mientras esperan, sin consumir CPU innecesariamente.

6. Coordinación de la pausa

Un problema importante es que cambiar el estado a PAUSED no significa que todos los hilos se hayan detenido exactamente en ese momento.

Cada SnakeRunner puede encontrarse ejecutando una operación cuando se solicita la pausa.

Por esta razón se implementó el mecanismo:
```bash
pauseAndWait()
```
Este método solicita la pausa y espera hasta que los trabajadores activos hayan alcanzado un punto seguro.
Se utilizan dos condiciones:
```bash
Condition canRun;
Condition allPaused;
```

canRun controla cuándo los trabajadores pueden continuar.
allPaused permite coordinar al hilo que solicita la pausa con los trabajadores que todavía están terminando su operación actual.

De esta manera, las estadísticas mostradas por la interfaz corresponden a un estado consistente del juego.

7. Problema encontrado al terminar una serpiente

Durante las pruebas se encontró un problema relacionado con el registro de los trabajadores.

Cuando una serpiente terminaba, su SnakeRunner podía finalizar, pero el reloj todavía podía considerarlo como un trabajador activo.

Esto podía provocar que el mecanismo de pausa esperara indefinidamente por un trabajador que ya había terminado.

Solución

Se agregaron los métodos:
```bash
registerRunner()
```
y:
```bash
unregisterRunner()
```
Cada trabajador se registra al comenzar:
```bash
clock.registerRunner();
```
y se elimina cuando termina:
```bash
finally {
    clock.unregisterRunner();
}
```
El uso de finally permite garantizar que el trabajador sea retirado del registro incluso si termina debido a una interrupción.

Con este cambio se evitó el bloqueo durante la pausa cuando alguna serpiente ya había terminado.

8. Regiones críticas

Se buscó que las regiones críticas fueran únicamente las necesarias.

La operación principal del tablero es:
```bash
public synchronized MoveResult step(Snake snake)
```
Esta operación se mantiene protegida porque realiza varias modificaciones que deben considerarse como una sola operación consistente:

- Obtener la posición siguiente.
- Comprobar obstáculos.
- Comprobar teletransportadores.
- Comprobar si se encontró un ratón.
- Comprobar si se encontró turbo.
- Actualizar la serpiente.
- Crear nuevos elementos del tablero.

Por ejemplo, dos serpientes no deberían poder consumir simultáneamente el mismo ratón.

Por esta razón, estas operaciones se realizan dentro de una región crítica.

Las operaciones independientes, como el cálculo de dirección o el tiempo de espera, no mantienen innecesariamente el bloqueo del tablero.

9. UI y consistencia visual

La interfaz gráfica utiliza Swing.

Las actualizaciones relacionadas con el repintado se realizan mediante:
```bash
SwingUtilities.invokeLater(...)
```
El GameClock solicita el repintado mediante:
```bash
SwingUtilities.invokeLater(gamePanel::repaint);
```
Para dibujar las serpientes se utiliza:
```bash
snapshot()
```
Esto evita que la interfaz recorra directamente el ArrayDeque que puede estar siendo modificado por el SnakeRunner.

De esta manera se reduce el riesgo de mostrar un estado inconsistente durante la actualización de la pantalla.

10. Iniciar, Pausar y Reanudar

La interfaz utiliza los estados:

STOPPED
RUNNING
PAUSED

El flujo de ejecución es:

INICIO -> PAUSA -> REANUDAR

Al pausar el juego se espera a que los trabajadores alcancen un punto seguro antes de mostrar las estadísticas.

Las estadísticas solicitadas son:

serpiente viva más larga;
serpiente que murió primero.

Esto permite que la información mostrada corresponda al estado estable del juego y no a una actualización que todavía esté siendo modificada por otro hilo.

11. Pruebas realizadas
Compilación

Se ejecutó:
```bash
mvn clean verify
```
Resultado:

BUILD SUCCESS
Ejecución normal

Se ejecutó:
```bash
mvn -q -DskipTests exec:java -Dsnakes=4
```
Se verificó:

- movimiento autónomo de las serpientes;
- controles del jugador;
- obstáculos;
- ratones;
- teletransportadores;
- turbo;
- wrap-around;
- pausa;
- reanudación;
- actualización de la interfaz.

Resultado: correcto.

Prueba con alta concurrencia

También se ejecutó:
```bash
mvn -q -DskipTests exec:java -Dsnakes=20
```
Se realizaron pruebas con 20 o más serpientes para aumentar la carga concurrente.

Durante las pruebas no se observaron:

- ConcurrentModificationException;
- deadlocks;
- bloqueos indefinidos;
- fallos del juego;
- errores visibles de concurrencia.

Resultado: correcto.

12. Resumen de cambios realizados:

| Componente    | Cambio                        | Motivo                                    |
| ------------- | ----------------------------- | ----------------------------------------- |
| `Board`       | Métodos sincronizados         | Proteger el estado compartido             |
| `Board`       | Copias de colecciones         | Evitar exponer estructuras internas       |
| `Snake`       | `snapshot()`                  | Permitir lecturas seguras desde la UI     |
| `Snake`       | `volatile direction`          | Garantizar visibilidad entre hilos        |
| `SnakeRunner` | Virtual Threads               | Dar autonomía a cada serpiente            |
| `GameClock`   | `ReentrantLock` + `Condition` | Coordinar pausa y reanudación             |
| `GameClock`   | Registro de runners           | Saber qué trabajadores siguen activos     |
| `SnakeRunner` | `unregisterRunner()`          | Manejar correctamente la finalización     |
| `SnakeApp`    | Pausar/Reanudar               | Controlar la ejecución del juego          |
| `SnakeApp`    | Estadísticas                  | Mostrar información consistente al pausar |
| `SnakeApp`    | `SwingUtilities.invokeLater`  | Actualizar Swing de forma segura          |

13. Conclusiones

Se logró ejecutar cada serpiente de manera independiente utilizando Virtual Threads de Java 21.
Se identificaron estructuras que podían presentar problemas al ser utilizadas desde múltiples hilos, principalmente las colecciones HashSet, HashMap y ArrayDeque.
El uso de synchronized permitió proteger las operaciones críticas del Board sin necesidad de convertir todas las estructuras en colecciones concurrentes.
El uso de snapshot() permitió separar la lectura realizada por la UI de las modificaciones realizadas por los SnakeRunner.
Se utilizaron mecanismos de suspensión como wait() y await() en lugar de busy-waiting, evitando consumo innecesario de CPU.
La coordinación de GameClock permite realizar una pausa controlada y esperar a que los trabajadores alcancen un estado seguro antes de mostrar las estadísticas.
Finalmente, se probó el juego con 20 o más serpientes, verificando que continuara funcionando correctamente sin excepciones de concurrencia ni bloqueos indefinidos.
