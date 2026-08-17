package co.eci.prime;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Trabajador que extrae números a comprobar y actualiza el contador de primos.
 *
 * Diseño de concurrencia:
 * - Antes de procesar cada número el hilo comprueba la condición de pausa
 *   bajo el monitor {@code pauseLock}. Si {@code paused[0]} es true, el hilo
 *   invoca {@code pauseLock.wait()} hasta que el hilo principal haga
 *   {@code notifyAll()}.
 * - Esto evita la espera activa (busy-waiting) y usa el mismo monitor que
 *   el hilo principal, cumpliendo el requisito del laboratorio.
 */
public class PrimeWorker implements Runnable {

    private final AtomicLong current;     // siguiente número a verificar
    private final AtomicLong primesFound; // contador compartido de primos
    private final Object pauseLock;       // monitor compartido para pausa/reanudar
    private final boolean[] paused;       // condición de pausa (guardada por pauseLock)

    public PrimeWorker(AtomicLong current, AtomicLong primesFound, Object pauseLock, boolean[] paused) {
        this.current = current;
        this.primesFound = primesFound;
        this.pauseLock = pauseLock;
        this.paused = paused;
    }

    @Override
    public void run() {
        while (true) {
            // Comprobar si debe pausar: usar while para proteger contra spurious wakeups
            synchronized (pauseLock) {
                while (paused[0]) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            // Obtener siguiente número y comprobar si es primo
            long n = current.getAndIncrement();
            if (isPrime(n)) {
                primesFound.incrementAndGet();
            }
        }
    }

    /**
     * Comprobación simple de primalidad (sin optimizaciones avanzadas).
     */
    private boolean isPrime(long n) {
        if (n < 2) return false;
        if (n % 2 == 0) return n == 2;
        for (long i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
