package com.company.parser.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit Breaker для защиты от перегрузки
 */
@Slf4j
@Component
public class CircuitBreaker {

    private static final int FAILURE_THRESHOLD = 5;
    private static final long TIMEOUT_MS = 30_000; // 30 секунд
    private static final long HALF_OPEN_TIMEOUT_MS = 10_000; // 10 секунд

    private final Map<String, CircuitState> states = new ConcurrentHashMap<>();

    /**
     * Проверка возможности выполнения запроса
     */
    public boolean allowRequest(String domain) {
        CircuitState state = states.computeIfAbsent(domain, k -> new CircuitState());
        return state.allowRequest();
    }

    /**
     * Регистрация успешного запроса
     */
    public void recordSuccess(String domain) {
        CircuitState state = states.get(domain);
        if (state != null) {
            state.recordSuccess();
        }
    }

    /**
     * Регистрация неудачного запроса
     */
    public void recordFailure(String domain) {
        CircuitState state = states.computeIfAbsent(domain, k -> new CircuitState());
        state.recordFailure();
    }

    /**
     * Получение состояния для домена
     */
    public State getState(String domain) {
        CircuitState state = states.get(domain);
        return state != null ? state.getState() : State.CLOSED;
    }

    /**
     * Состояние circuit breaker
     */
    public enum State {
        CLOSED,     // Нормальная работа
        OPEN,       // Блокировка запросов
        HALF_OPEN   // Пробный режим
    }

    /**
     * Внутреннее состояние для домена
     */
    private static class CircuitState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private volatile State state = State.CLOSED;

        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            switch (state) {
                case CLOSED:
                    return true;

                case OPEN:
                    if (now - lastFailureTime.get() > TIMEOUT_MS) {
                        state = State.HALF_OPEN;
                        log.info("Circuit breaker entering HALF_OPEN state");
                        return true;
                    }
                    return false;

                case HALF_OPEN:
                    return true;

                default:
                    return false;
            }
        }

        synchronized void recordSuccess() {
            switch (state) {
                case HALF_OPEN:
                    successCount.incrementAndGet();
                    if (successCount.get() >= 3) { // 3 успешных запроса для восстановления
                        reset();
                        log.info("Circuit breaker recovered to CLOSED state");
                    }
                    break;

                case CLOSED:
                    failureCount.set(0);
                    break;
            }
        }

        synchronized void recordFailure() {
            lastFailureTime.set(System.currentTimeMillis());

            switch (state) {
                case CLOSED:
                    if (failureCount.incrementAndGet() >= FAILURE_THRESHOLD) {
                        state = State.OPEN;
                        log.warn("Circuit breaker opened after {} failures", FAILURE_THRESHOLD);
                    }
                    break;

                case HALF_OPEN:
                    state = State.OPEN;
                    failureCount.set(FAILURE_THRESHOLD);
                    log.warn("Circuit breaker reopened from HALF_OPEN state");
                    break;
            }
        }

        private void reset() {
            failureCount.set(0);
            successCount.set(0);
            state = State.CLOSED;
        }

        State getState() {
            return state;
        }
    }
}
