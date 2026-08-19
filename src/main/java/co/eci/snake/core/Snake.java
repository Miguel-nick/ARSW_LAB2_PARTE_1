package co.eci.snake.core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Snake {

  private final Deque<Position> body = new ArrayDeque<>();

  private volatile Direction direction;

  private int maxLength = 5;

  /*
   * Estado de la serpiente.
   * volatile permite que la UI y el hilo de la serpiente
   * observen correctamente el estado.
   */
  private volatile boolean alive = true;

  /*
   * Momento en el que murió la serpiente.
   * Se utiliza para determinar cuál murió primero.
   */
  private volatile long deathOrder = -1;

  private Snake(Position start, Direction dir) {
    body.addFirst(start);
    this.direction = dir;
  }

  public static Snake of(int x, int y, Direction dir) {
    return new Snake(
            new Position(x, y),
            dir
    );
  }

  public Direction direction() {
    return direction;
  }

  public boolean isAlive() {
    return alive;
  }

  public long deathOrder() {
    return deathOrder;
  }

  public void turn(Direction dir) {

    if (!alive) {
      return;
    }

    if (
            (direction == Direction.UP
                    && dir == Direction.DOWN)
                    ||
                    (direction == Direction.DOWN
                            && dir == Direction.UP)
                    ||
                    (direction == Direction.LEFT
                            && dir == Direction.RIGHT)
                    ||
                    (direction == Direction.RIGHT
                            && dir == Direction.LEFT)
    ) {
      return;
    }

    this.direction = dir;
  }

  public synchronized Position head() {
    return body.peekFirst();
  }

  public synchronized Deque<Position> snapshot() {
    return new ArrayDeque<>(body);
  }

  public synchronized int length() {
    return body.size();
  }

  public synchronized void advance(
          Position newHead,
          boolean grow
  ) {

    if (!alive) {
      return;
    }

    body.addFirst(newHead);

    if (grow) {
      maxLength++;
    }

    while (body.size() > maxLength) {
      body.removeLast();
    }
  }

  /**
   * Marca la serpiente como muerta.
   *
   * El contador recibido representa el orden global
   * de muerte y debe ser generado por una estructura
   * compartida.
   */
  public void die(long order) {
    alive = false;
    deathOrder = order;
  }
}