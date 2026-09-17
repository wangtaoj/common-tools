package com.wangtao.tools.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/**
 * 事务同步工具类
 * @author wangtao
 * Created at 2026-09-17
 */
public final class TransactionSynchronizationUtils {

    private TransactionSynchronizationUtils() {

    }

    /**
     * 事务提交之后执行
     * @param action 执行动作
     */
    public static void executeAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        }
    }

    /**
     * 事务回滚之后执行
     * @param action 执行动作
     */
    public static void executeAfterRollback(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
                        action.run();
                    }
                }
            });
        }
    }

    /**
     * 事务完成后执行, 回滚或者提交都有可能
     * 根据status来判断
     * @param action 执行动作
     */
    public static void executeAfterCompletion(Consumer<Integer> action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    action.accept(status);
                }
            });
        }
    }
}
