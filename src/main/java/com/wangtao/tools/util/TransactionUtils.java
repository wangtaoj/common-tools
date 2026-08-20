package com.wangtao.tools.util;

import com.wangtao.tools.exception.ToolRuntimeException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事务工具类
 * 提供编程式事务执行模板，支持传播行为配置，自动处理异常包装
 *
 * @author wangtao
 * Created at 2026-08-20
 */
public final class TransactionUtils {

    private TransactionUtils() {

    }

    /**
     * 无返回值的事务执行函数式接口
     */
    @FunctionalInterface
    public interface Executable {
        /**
         * 执行事务逻辑
         *
         * @throws Exception 业务异常
         */
        void execute() throws Exception;
    }

    /**
     * 有返回值的事务执行函数式接口
     *
     * @param <T> 返回结果类型
     */
    @FunctionalInterface
    public interface ExecutableWithResult<T> {
        /**
         * 执行事务逻辑并返回结果
         *
         * @return 执行结果
         * @throws Exception 业务异常
         */
        T execute() throws Exception;
    }

    /**
     * 执行无返回值事务（默认传播行为 REQUIRED）
     *
     * @param executable 业务逻辑
     */
    public static void execute(Executable executable) {
        execute(Propagation.REQUIRED, executable);
    }

    /**
     * 执行无返回值事务（自定义传播行为）
     *
     * @param propagation 事务传播行为
     * @param executable  业务逻辑
     */
    public static void execute(Propagation propagation, Executable executable) {
        executeWithResult(propagation, () -> {
            executable.execute();
            return null;
        });
    }

    /**
     * 执行有返回值事务（默认传播行为 REQUIRED）
     *
     * @param executable 业务逻辑
     * @param <T>        返回结果类型
     * @return 执行结果
     */
    public static <T> T executeWithResult(ExecutableWithResult<T> executable) {
        return executeWithResult(Propagation.REQUIRED, executable);
    }

    /**
     * 执行有返回值事务（自定义传播行为）
     *
     * @param propagation 事务传播行为
     * @param executable  业务逻辑
     * @param <T>         返回结果类型
     * @return 执行结果
     */
    public static <T> T executeWithResult(Propagation propagation, ExecutableWithResult<T> executable) {
        PlatformTransactionManager transactionManager = SpringContext.getBean(PlatformTransactionManager.class);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        // 设置传播行为
        transactionTemplate.setPropagationBehavior(propagation.value());
        // 执行事务
        return transactionTemplate.execute(status -> {
            try {
                return executable.execute();
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new ToolRuntimeException(e);
            }
        });
    }
}
