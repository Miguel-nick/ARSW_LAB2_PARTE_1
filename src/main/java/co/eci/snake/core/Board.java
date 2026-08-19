package co.eci.snake.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class Board {

  private final int width;
  private final int height;

  private final Set<Position> mice = new HashSet<>();
  private final Set<Position> obstacles = new HashSet<>();
  private final Set<Position> turbo = new HashSet<>();
  private final Map<Position, Position> teleports = new HashMap<>();

  /*
   * Contador global utilizado para establecer el orden
   * en el que mueren las serpientes.
   */
  private final AtomicLong deathCounter = new AtomicLong(0);

  public enum MoveResult {
    MOVED,
    ATE_MOUSE,
    HIT_OBSTACLE,
    ATE_TURBO,
    TELEPORTED,
    DIED
  }

  public Board(int width, int height) {

    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
              "Board dimensions must be positive"
      );
    }

    this.width = width;
    this.height = height;

    for (int i = 0; i < 6; i++) {
      mice.add(randomEmpty());
    }

    for (int i = 0; i < 4; i++) {
      obstacles.add(randomEmpty());
    }

    for (int i = 0; i < 3; i++) {
      turbo.add(randomEmpty());
    }

    createTeleportPairs(2);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  /*
   * Se devuelven copias para que la UI nunca pueda
   * modificar directamente el estado interno.
   */
  public synchronized Set<Position> mice() {
    return new HashSet<>(mice);
  }

  public synchronized Set<Position> obstacles() {
    return new HashSet<>(obstacles);
  }

  public synchronized Set<Position> turbo() {
    return new HashSet<>(turbo);
  }

  public synchronized Map<Position, Position> teleports() {
    return new HashMap<>(teleports);
  }

  /**
   * Ejecuta un movimiento completo de una serpiente.
   *
   * Todas las operaciones que modifican el estado del
   * tablero ocurren dentro de esta región crítica.
   */
  public synchronized MoveResult step(Snake snake) {

    Objects.requireNonNull(
            snake,
            "snake"
    );

    /*
     * Una serpiente que ya murió no debe continuar
     * realizando movimientos.
     */
    if (!snake.isAlive()) {
      return MoveResult.DIED;
    }

    var head = snake.head();
    var dir = snake.direction();

    Position next =
            new Position(
                    head.x() + dir.dx,
                    head.y() + dir.dy
            ).wrap(width, height);

    /*
     * Los obstáculos producen rebote y NO muerte.
     */
    if (obstacles.contains(next)) {
      return MoveResult.HIT_OBSTACLE;
    }

    boolean teleported = false;

    /*
     * Teletransportación.
     */
    if (teleports.containsKey(next)) {

      next = teleports.get(next);
      teleported = true;
    }

    /*
     * Comprobar si la nueva posición coincide
     * con el cuerpo actual de la serpiente.
     *
     * Se obtiene una copia segura del cuerpo.
     */
    var body = snake.snapshot();

    if (body.contains(next)) {

      long deathOrder =
              deathCounter.incrementAndGet();

      snake.die(deathOrder);

      return MoveResult.DIED;
    }

    /*
     * Consumir recursos del tablero.
     *
     * Como estamos dentro de step() sincronizado,
     * dos serpientes no pueden consumir simultáneamente
     * el mismo recurso.
     */
    boolean ateMouse = mice.remove(next);
    boolean ateTurbo = turbo.remove(next);

    snake.advance(
            next,
            ateMouse
    );

    /*
     * Comer un mouse genera un nuevo mouse y
     * un nuevo obstáculo.
     */
    if (ateMouse) {

      mice.add(randomEmpty());
      obstacles.add(randomEmpty());

      if (
              ThreadLocalRandom.current()
                      .nextDouble() < 0.2
      ) {
        turbo.add(randomEmpty());
      }
    }

    if (ateTurbo) {
      return MoveResult.ATE_TURBO;
    }

    if (ateMouse) {
      return MoveResult.ATE_MOUSE;
    }

    if (teleported) {
      return MoveResult.TELEPORTED;
    }

    return MoveResult.MOVED;
  }

  private void createTeleportPairs(int pairs) {

    for (int i = 0; i < pairs; i++) {

      Position a = randomEmpty();
      Position b = randomEmpty();

      teleports.put(a, b);
      teleports.put(b, a);
    }
  }

  private Position randomEmpty() {

    var rnd = ThreadLocalRandom.current();

    int maxAttempts = width * height * 2;

    /*
     * Primero se intenta encontrar una posición
     * de forma aleatoria.
     */
    for (int i = 0; i < maxAttempts; i++) {

      Position p =
              new Position(
                      rnd.nextInt(width),
                      rnd.nextInt(height)
              );

      if (
              !mice.contains(p)
                      && !obstacles.contains(p)
                      && !turbo.contains(p)
                      && !teleports.containsKey(p)
      ) {
        return p;
      }
    }

    /*
     * Si el tablero está muy lleno, se busca
     * sistemáticamente una posición libre.
     */
    for (int y = 0; y < height; y++) {

      for (int x = 0; x < width; x++) {

        Position p =
                new Position(x, y);

        if (
                !mice.contains(p)
                        && !obstacles.contains(p)
                        && !turbo.contains(p)
                        && !teleports.containsKey(p)
        ) {
          return p;
        }
      }
    }

    throw new IllegalStateException(
            "No hay posiciones libres en el tablero"
    );
  }
}