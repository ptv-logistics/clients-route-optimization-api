package com.ptvgroup.developer.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConditionAwaiter<T> {

    public static class Builder<T> {

        private static final String DEFAULT_DESCRIPTION = "some condition";
        private static final long DFEAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
        private static final long DFEAULT_RETRY = TimeUnit.SECONDS.toMillis(1);

        private T initialValue;
        private Function<T, T> valueUpdater;
        private Predicate<T> condition;
        private String description = DEFAULT_DESCRIPTION;
        private String expectedMessage;
        private long timeoutInMilliseconds = DFEAULT_TIMEOUT;
        private long retryIntervallInMilliseconds = DFEAULT_RETRY;

        public Builder<T> withTimeout(final long duration, final TimeUnit timeUnit) {
            this.timeoutInMilliseconds = timeUnit.toMillis(duration);
            return this;
        }
        
        public Builder<T> withRetryIntervall(final long duration, final TimeUnit timeUnit) {
            this.retryIntervallInMilliseconds = timeUnit.toMillis(duration);
            return this;
        }

        public Builder<T> withInitialValue(final T initialValue) {
            this.initialValue = initialValue;
            return this;
        }

        public Builder<T> repeatAction(final Supplier<T> action) {
            this.valueUpdater = value -> action.get();
            return this;
        }

        public Builder<T> repeatModification(final Consumer<T> modification) {
            this.valueUpdater = value -> {
                modification.accept(value);
                return value;
            };
            return this;
        }

        public Builder<T> untilCondition(final String name, final Predicate<T> condition) {
            this.expectedMessage = String.format("Expected condition '%s'.", name);
            this.condition = condition;
            return this;
        }

        public Builder<T> untilValueEquals(final T expected) {
            this.expectedMessage = String.format("Expected value to equal %s.", expected);
            this.condition = expected::equals;
            return this;
        }

        public Builder<T> untilValueReaches(final int expected) {
            this.expectedMessage = String.format("Expected value to reach %d.", expected);
            this.condition = value -> (Integer) value >= expected;
            return this;
        }

        public Builder<T> untilValueHasSize(final int expectedSize) {
            this.expectedMessage = String.format("Expected value has size %d.", expectedSize);
            this.condition = value -> ((Collection<?>) value).size() == expectedSize;
            return this;
        }

        public Builder<T> whileValueEquals(final T unexpected) {
            this.expectedMessage = String.format("Expected value to not equal %s.", unexpected);
            this.condition = value -> !Objects.equals(value, unexpected);
            return this;
        }

        public ConditionAwaiter<T> build() {
            return new ConditionAwaiter<>(this);
        }

        public T await(final String description) throws TimeoutException, InterruptedException {
            this.description = description;
            return build().await();
        }

        public T await() throws TimeoutException, InterruptedException {
            return build().await();
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(ConditionAwaiter.class);

    private final Function<T, T> valueUpdater;
    private final Predicate<T> condition;
    private final String description;
    private final String expectedMessage;
    private final long timeoutInMilliseconds;
    private final long retryIntervallInMilliseconds;
    private final T initialValue;


    public ConditionAwaiter(final Builder<T> builder) {
        this.valueUpdater = Objects.requireNonNull(builder.valueUpdater);
        this.condition = Objects.requireNonNull(builder.condition);
        this.description = Objects.requireNonNull(builder.description);
        this.expectedMessage = Objects.requireNonNull(builder.expectedMessage);
        this.timeoutInMilliseconds = builder.timeoutInMilliseconds;
        this.retryIntervallInMilliseconds = builder.retryIntervallInMilliseconds;
        this.initialValue = builder.initialValue;
    }

    public T await() throws TimeoutException, InterruptedException {
        LOG.debug("Awaiting {}: {}", description, expectedMessage);
        final long deadline = System.currentTimeMillis() + timeoutInMilliseconds;
        T value = initialValue;
        do {
            value = valueUpdater.apply(value);
            if (condition.test(value)) {
                LOG.debug("Success: Value is now {}.", value);
                return value;
            }
            if (System.currentTimeMillis() + retryIntervallInMilliseconds <= deadline) {
                Thread.sleep(retryIntervallInMilliseconds);
            } else {
                break;
            }
        } while (System.currentTimeMillis() <= deadline);
        throw new TimeoutException(
                String.format("Timeout while awaiting %s. %s Current value is %s.",
                        description, expectedMessage, value));
    }

    public static <T> Builder<List<T>> builder(@SuppressWarnings("rawtypes") final Class<List> listClass, final Class<T> classInstance) {
        return new Builder<>();
    }

    public static <T> Builder<T> builder(final Class<T> classInstance) {
        return new Builder<>();
    }

    public static <T extends Number> Builder<T> forNumericValue() {
        return new Builder<>();
    }

    public static Builder<Integer> forIntegerValue() {
        return new Builder<>();
    }

    public static <T> Builder<T> forInitialValue(final T initialValue) {
        return new Builder<T>().withInitialValue(initialValue);
    }

}
