package com.wangtao.tools.lock;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 加锁结果, api设计借鉴于{@link Optional}
 * @author wangtao
 * Created at 2026-10-07
 */
public class LockResult<T> {

    private static final LockResult<?> NOT_ACQUIRED =
        new LockResult<>(false, null);

    private final boolean lockAcquired;
    private final T value;

    private LockResult(boolean lockAcquired, T value) {
        this.lockAcquired = lockAcquired;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> LockResult<T> lockNotAcquired() {
        return (LockResult<T>) NOT_ACQUIRED;
    }

    public static <T> LockResult<T> acquired(T value) {
        return new LockResult<>(true, value);
    }

    public boolean isLockAcquired() {
        return lockAcquired;
    }

    public T get() {
        if (!lockAcquired) {
            throw new LockNotAcquiredException();
        }
        return value;
    }

    public T orElse(T other) {
        return lockAcquired ? value : other;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return lockAcquired ? value : supplier.get();
    }

    public void ifLockAcquired(Consumer<? super T> consumer) {
        if (lockAcquired) {
            consumer.accept(value);
        }
    }

    public <R> LockResult<R> map(Function<? super T, ? extends R> mapper) {
        if (!lockAcquired) {
            return lockNotAcquired();
        }

        return acquired(mapper.apply(value));
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (lockAcquired) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }
}
