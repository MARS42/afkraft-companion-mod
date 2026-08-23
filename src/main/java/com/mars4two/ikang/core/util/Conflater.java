package com.mars4two.ikang.core.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Conflater con flush periódico.
 *
 * <p>Semántica:
 * <ul>
 *   <li><b>Último valor por entidad:</b> si una misma clave cambia N veces antes
 *       del flush, solo se libera su estado final (last-writer-wins).</li>
 *   <li><b>Cambio vs. último emitido:</b> un valor solo se acumula si su token de
 *       cambio difiere del token de lo último que se emitió para esa clave. Un
 *       valor que vuelve a su estado ya emitido NO se re-emite.</li>
 *   <li><b>Flush a intervalo fijo:</b> cada {@code periodMillis} se drena y emite
 *       lo acumulado en un solo batch.</li>
 * </ul>
 *
 * <p><b>Contrato del emitter:</b> la emisión ocurre FUERA de cualquier lock por
 * clave. El emitter debería encapsular su propio manejo de errores / reintentos
 * y no lanzar excepción. Si lanza, el batch que recibió se considera entregado
 * (at-most-once) y la excepción se enruta a {@code errorHandler} para
 * observabilidad. Si necesitas at-least-once, haz que tu emitter reintente
 * internamente en lugar de propagar.
 *
 * <p><b>Memoria:</b> como "cambio" se mide contra el último emitido, se recuerda
 * el token emitido de cada clave de forma indefinida. Si tu espacio de claves es
 * ilimitado (p. ej. incluye IDs efímeros), respalda el estado con un cache con
 * TTL/eviction (Caffeine) en lugar de este mapa.
 *
 * <p>Thread-safe: {@link #submit(Object)} puede llamarse desde múltiples hilos.
 */
public final class Conflater<T> implements AutoCloseable {

    private static Logger log = LoggerFactory.getLogger(Conflater.class);

    /** Consumidor del batch drenado en cada flush. */
    @FunctionalInterface
    public interface BatchConsumer<T> {
        void accept(List<T> batch) throws Exception;
    }

    /** Estado por clave. Inmutable; las transiciones se hacen con compute(). */
    private static final class Entry<T> {
        final Object emittedToken; // token del último valor emitido (null = nunca emitido)
        final T pending;           // valor esperando emisión (null cuando está limpio)
        final Object pendingToken; // token del pending
        final boolean dirty;       // hay algo por emitir

        Entry(Object emittedToken, T pending, Object pendingToken, boolean dirty) {
            this.emittedToken = emittedToken;
            this.pending = pending;
            this.pendingToken = pendingToken;
            this.dirty = dirty;
        }
    }

    private final ConcurrentHashMap<String, Entry<T>> state = new ConcurrentHashMap<>();
    // Índice de claves candidatas a estar sucias. Es solo una pista: la verdad es
    // Entry.dirty, re-verificada bajo compute() durante el flush.
    private final Set<String> dirtyKeys = ConcurrentHashMap.newKeySet();

    private final Function<? super T, String> keyFn;
    private final Function<? super T, ?> tokenFn;
    private final BatchConsumer<T> emitter;
    private final Consumer<Throwable> errorHandler;
    private final long periodMillis;

    private final ScheduledExecutorService scheduler;
    private volatile ScheduledFuture<?> task;

    /**
     * @param periodMillis  intervalo de flush en milisegundos
     * @param keyFn         extrae la clave de referencia (qué entidad es)
     * @param tokenFn       calcula el token de cambio (para comparar contra lo emitido).
     *                      Debe ser estable: mismo contenido lógico -> mismo token.
     * @param emitter       recibe cada batch drenado (fuera de locks)
     * @param errorHandler  recibe errores del flush/emitter (puede ser null)
     */
    public Conflater(long periodMillis,
                     Function<? super T, String> keyFn,
                     Function<? super T, ?> tokenFn,
                     BatchConsumer<T> emitter,
                     Consumer<Throwable> errorHandler) {
        if (periodMillis <= 0) throw new IllegalArgumentException("periodMillis debe ser > 0");
        this.periodMillis = periodMillis;
        this.keyFn = Objects.requireNonNull(keyFn, "keyFn");
        this.tokenFn = Objects.requireNonNull(tokenFn, "tokenFn");
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.errorHandler = (errorHandler != null) ? errorHandler : t -> { };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread th = new Thread(r, "AFKraft conflater");
            th.setDaemon(true);
            return th;
        });
    }

    /** Ingesta de un valor producido por la acción. Barato; se puede llamar en caliente. */
    public void submit(T value) {
        final String key = keyFn.apply(value);
        final Object token = tokenFn.apply(value);

        Entry<T> result = state.compute(key, (k, e) -> {
            Object emitted = (e == null) ? null : e.emittedToken;
            if (Objects.equals(token, emitted)) {
                // Igual a lo ya emitido: descarta cualquier pendiente, queda limpio.
                return new Entry<>(emitted, null, null, false);
            }
            // Difiere de lo emitido -> se vuelve/sobrescribe el pendiente (last-writer-wins).
            return new Entry<>(emitted, value, token, true);
        });

        if (result.dirty) {
            dirtyKeys.add(key);
        }
    }

    /** Arranca el flush periódico. Idempotente-no: llamar una sola vez. */
    public void start() {
        if (task != null) {
            log.error("Conflater already started");
            return;
        }
        task = scheduler.scheduleAtFixedRate(
                this::safeFlush, periodMillis, periodMillis, TimeUnit.MILLISECONDS);
        log.info("Conflater started");
    }

    private void safeFlush() {
        try {
            flush();
        } catch (Exception t) {
            // Nunca dejar morir la tarea programada (scheduleAtFixedRate cancela
            // ejecuciones futuras si la tarea lanza).
            log.error("Error flushing conflater", t);
            errorHandler.accept(t);
        }
    }

    /**
     * Drena las entradas sucias y las emite como un solo batch.
     * La emisión ocurre fuera de los locks por clave. Público para permitir
     * flush manual o en tests.
     */
    public void flush() throws Exception {
        // Snapshot + limpieza del índice de sucias.
        List<String> candidates = new ArrayList<>();
        for (Iterator<String> it = dirtyKeys.iterator(); it.hasNext(); ) {
            candidates.add(it.next());
            it.remove();
        }
        if (candidates.isEmpty()) return;

        List<T> batch = new ArrayList<>(candidates.size());
        for (String key : candidates) {
            state.compute(key, (k, e) -> {
                if (e == null || !e.dirty) {
                    return e; // falso positivo del índice; nada que emitir
                }
                batch.add(e.pending);
                // El pendiente pasa a ser "lo emitido"; la entrada queda limpia.
                return new Entry<>(e.pendingToken, null, null, false);
            });
        }

        if (!batch.isEmpty()) {
            emitter.accept(batch);
        }
    }

    /** Detiene el scheduler y hace un flush final para no perder lo acumulado. */
    @Override
    public void close() {
        if (task != null) task.cancel(false);
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(periodMillis + 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Error waiting for scheduler termination", ie);
        }
        try {
            flush(); // drenado final (asume que los productores ya se detuvieron)
        } catch (Exception t) {
            log.error("Error flushing conflater", t);
            errorHandler.accept(t);
        }
    }
}
