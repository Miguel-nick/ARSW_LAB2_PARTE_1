package co.eci.snake.concurrency;

import co.eci.snake.core.Board;
import co.eci.snake.core.Direction;
import co.eci.snake.core.Snake;
import co.eci.snake.core.engine.GameClock;

import java.util.concurrent.ThreadLocalRandom;

public final class SnakeRunner implements Runnable {

  private final Snake snake;
  private final Board board;
  private final GameClock clock;

  private final int baseSleepMs = 80;
  private final int turboSleepMs = 40;

  private int turboTicks = 0;

  public SnakeRunner(
          Snake snake,
          Board board,
          GameClock clock
  ) {

    this.snake = snake;
    this.board = board;
    this.clock = clock;
  }

  @Override
  public void run() {

    clock.registerRunner();

    try {

      while (
              !Thread.currentThread()
                      .isInterrupted()
      ) {

        /*
         * Si el juego está pausado,
         * el hilo espera sin consumir CPU.
         */
        clock.awaitIfPaused();

        maybeTurn();

        var res =
                board.step(snake);

        if (
                res
                        == Board.MoveResult.HIT_OBSTACLE
        ) {

          randomTurn();

        } else if (
                res
                        == Board.MoveResult.ATE_TURBO
        ) {

          turboTicks = 100;

        } else if (
                res
                        == Board.MoveResult.DIED
        ) {

          /*
           * La serpiente murió.
           * El runner termina.
           */
          break;
        }

        int sleep =
                (turboTicks > 0)
                        ? turboSleepMs
                        : baseSleepMs;

        if (turboTicks > 0) {
          turboTicks--;
        }

        Thread.sleep(sleep);
      }

    } catch (InterruptedException e) {

      Thread.currentThread().interrupt();

    } finally {

      /*
       * IMPORTANTE:
       * avisamos al GameClock que este runner
       * ya no está activo.
       */
      clock.unregisterRunner();
    }
  }

  private void maybeTurn() {

    double p =
            (turboTicks > 0)
                    ? 0.05
                    : 0.10;

    if (
            ThreadLocalRandom.current()
                    .nextDouble()
                    < p
    ) {

      randomTurn();
    }
  }

  private void randomTurn() {

    var dirs =
            Direction.values();

    snake.turn(
            dirs[
                    ThreadLocalRandom.current()
                            .nextInt(dirs.length)
                    ]
    );
  }
}