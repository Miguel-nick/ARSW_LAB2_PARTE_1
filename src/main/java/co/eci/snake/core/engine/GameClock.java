package co.eci.snake.core.engine;

import co.eci.snake.core.GameState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class GameClock implements AutoCloseable {

  private final ScheduledExecutorService scheduler =
          Executors.newSingleThreadScheduledExecutor();

  private final long periodMillis;
  private final Runnable tick;

  private final AtomicReference<GameState> state =
          new AtomicReference<>(GameState.STOPPED);

  /*
   * Lock utilizado para coordinar la pausa
   * y reanudación de los SnakeRunner.
   */
  private final ReentrantLock pauseLock =
          new ReentrantLock();

  /*
   * Condición que utilizan los SnakeRunner
   * mientras el juego está pausado.
   */
  private final Condition canRun =
          pauseLock.newCondition();

  /*
   * Condición utilizada por el hilo que solicita
   * la pausa para esperar a que los runners
   * lleguen a un punto seguro.
   */
  private final Condition allPaused =
          pauseLock.newCondition();

  /*
   * Cantidad de runners actualmente activos.
   */
  private int registeredRunners = 0;

  /*
   * Cantidad de runners que ya llegaron
   * al punto de pausa.
   */
  private int pausedRunners = 0;

  public GameClock(
          long periodMillis,
          Runnable tick
  ) {

    if (periodMillis <= 0) {
      throw new IllegalArgumentException(
              "periodMillis must be > 0"
      );
    }

    this.periodMillis = periodMillis;

    this.tick =
            java.util.Objects.requireNonNull(
                    tick,
                    "tick"
            );
  }

  public void start() {

    if (
            state.compareAndSet(
                    GameState.STOPPED,
                    GameState.RUNNING
            )
    ) {

      scheduler.scheduleAtFixedRate(
              () -> {

                if (
                        state.get()
                                == GameState.RUNNING
                ) {
                  tick.run();
                }

              },
              0,
              periodMillis,
              TimeUnit.MILLISECONDS
      );
    }
  }

  /**
   * Registra un SnakeRunner activo.
   */
  public void registerRunner() {

    pauseLock.lock();

    try {

      registeredRunners++;

    } finally {

      pauseLock.unlock();
    }
  }

  /**
   * Elimina un SnakeRunner cuando termina.
   *
   * Esto es importante porque una serpiente puede morir
   * y su hilo dejar de existir.
   */
  public void unregisterRunner() {

    pauseLock.lock();

    try {

      if (registeredRunners > 0) {
        registeredRunners--;
      }

      /*
       * Si estamos esperando una pausa y un runner
       * terminó, puede que ya se haya alcanzado
       * el número necesario de runners pausados.
       */
      if (
              pausedRunners >= registeredRunners
      ) {
        allPaused.signalAll();
      }

    } finally {

      pauseLock.unlock();
    }
  }

  /**
   * Espera mientras el juego esté pausado.
   *
   * No utiliza busy-waiting.
   */
  public void awaitIfPaused()
          throws InterruptedException {

    pauseLock.lock();

    try {

      while (
              state.get()
                      == GameState.PAUSED
      ) {

        pausedRunners++;

        /*
         * Si todos los runners activos llegaron
         * al punto de pausa, avisamos al hilo
         * que solicitó la pausa.
         */
        if (
                pausedRunners
                        >= registeredRunners
        ) {

          allPaused.signalAll();
        }

        try {

          canRun.await();

        } finally {

          pausedRunners--;
        }
      }

    } finally {

      pauseLock.unlock();
    }
  }

  /**
   * Solicita la pausa y espera hasta que todos
   * los runners activos lleguen a un punto seguro.
   */
  public void pauseAndWait()
          throws InterruptedException {

    pauseLock.lock();

    try {

      if (
              state.get()
                      != GameState.RUNNING
      ) {
        return;
      }

      state.set(GameState.PAUSED);

      /*
       * Esperamos mediante una Condition.
       * No usamos Thread.sleep ni ciclos activos.
       */
      while (
              pausedRunners
                      < registeredRunners
      ) {

        /*
         * Si ya no quedan runners activos,
         * no hay nada que esperar.
         */
        if (registeredRunners == 0) {
          break;
        }

        allPaused.await();
      }

    } finally {

      pauseLock.unlock();
    }
  }

  /**
   * Reanuda todos los runners pausados.
   */
  public void resume() {

    pauseLock.lock();

    try {

      state.set(GameState.RUNNING);

      canRun.signalAll();

    } finally {

      pauseLock.unlock();
    }
  }

  public void stop() {

    pauseLock.lock();

    try {

      state.set(GameState.STOPPED);

      canRun.signalAll();
      allPaused.signalAll();

    } finally {

      pauseLock.unlock();
    }
  }

  public GameState state() {
    return state.get();
  }

  @Override
  public void close() {

    stop();

    scheduler.shutdownNow();
  }
}