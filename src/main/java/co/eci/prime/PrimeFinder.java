package co.eci.prime;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Programa de ejemplo para la Parte I del laboratorio:
 * - Inicia N hilos trabajadores que buscan números primos de forma concurrente.
 * - Cada {@code t} milisegundos el hilo principal solicita que todos los
 *   trabajadores se pausen, muestra cuántos primos se han encontrado y
 *   espera que el usuario pulse ENTER para reanudar.
 *
 * Sincronización:
 * - Se utiliza un único monitor {@code pauseLock} para coordinar pausa/reanudar.
 * - La condición de pausa está representada por el array booleano {@code paused[0]}
 *   (protegido por el mismo monitor). Los trabajadores esperan con
 *   {@code pauseLock.wait()} y el hilo principal despierta a todos con
 *   {@code pauseLock.notifyAll()}.
 * - No se usa busy-waiting.
 */
public class PrimeFinder {

    /**
     * Punto de entrada.
     * Parámetros del sistema:
     * -threads=N  número de hilos trabajadores (por defecto 4)
     * -t=ms       intervalo de pausa en milisegundos (por defecto 2000)
     */
    public static void main(String[] args) throws Exception {
        int threads = Integer.parseInt(System.getProperty("threads", "4"));
        long t = Long.parseLong(System.getProperty("t", "2000")); // milliseconds

        // Monitor compartido para pausa/reanudación
        final Object pauseLock = new Object();
        // Condición de pausa: se usa un array para permitir mutabilidad desde lambdas/hilos
        final boolean[] paused = new boolean[] { false }; // guardado por pauseLock

        AtomicLong current = new AtomicLong(2);       // siguiente número a comprobar
        AtomicLong primesFound = new AtomicLong(0);   // contador de primos encontrados

        // Crear y arrancar los hilos trabajadores
        for (int i = 0; i < threads; i++) {
            Thread w = new Thread(new PrimeWorker(current, primesFound, pauseLock, paused));
            w.setName("PrimeWorker-" + i);
            w.setDaemon(true);
            w.start();
        }

        System.out.println("PrimeFinder iniciado. Hilos=" + threads + ", pausa cada t=" + t + "ms.");

        // Bucle principal: cada t ms solicita la pausa, muestra estado y espera ENTER
        while (true) {
            Thread.sleep(t);

            // Solicitar pausa: actualizar la condición bajo el monitor
            synchronized (pauseLock) {
                paused[0] = true;
            }

            // Pequeña espera para dar tiempo a que los trabajadores entren en la sección
            Thread.sleep(20);

            long found = primesFound.get();
            System.out.println("\n--- PAUSA --- Primos encontrados hasta ahora: " + found);
            System.out.println("Pulse ENTER para reanudar...");

            // Esperar ENTER del usuario (lectura de System.in)
            try {
                while (System.in.available() == 0) {
                    int b = System.in.read();
                    if (b == '\n' || b == '\r') break;
                }
            } catch (IOException e) {
                // En caso de error con available(), leer bloqueante como fallback
                System.in.read();
            }

            // Reanudar: cambiar la condición y notificar a todos los trabajadores
            synchronized (pauseLock) {
                paused[0] = false;
                pauseLock.notifyAll();
            }

            System.out.println("Reanudado.\n");
        }
    }
}
