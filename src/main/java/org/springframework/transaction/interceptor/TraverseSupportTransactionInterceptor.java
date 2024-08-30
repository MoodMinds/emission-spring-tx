package org.springframework.transaction.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.moodminds.elemental.Association;
import org.moodminds.emission.Emittable;
import org.moodminds.traverse.Traversable;
import org.moodminds.traverse.TraverseMethod;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.moodminds.traverse.TraverseSupport;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Optional.ofNullable;
import static org.moodminds.emission.Emittable.emittable;
import static org.springframework.aop.support.AopUtils.getTargetClass;
import static org.springframework.util.ClassUtils.getQualifiedMethodName;

/**
 * The {@link Emittable} and {@link Traversable} transactional interceptor.
 */
public class TraverseSupportTransactionInterceptor extends TransactionInterceptor {

    private static final long serialVersionUID = -3979034825290177342L;

    private final MethodInterceptor transactionInterceptor;

    /**
     * Construct the interceptor with the specified dependencies.
     *
     * @param transactionManager the specified transaction manager instance
     * @param transactionAttributeSource the specified transaction attribute source instance
     * @param beanFactory the specified bean factory instance
     */
    public TraverseSupportTransactionInterceptor(TransactionManager transactionManager,
                                                 TransactionAttributeSource transactionAttributeSource,
                                                 BeanFactory beanFactory, MethodInterceptor transactionInterceptor) {
        super(transactionManager, transactionAttributeSource);
        setBeanFactory(beanFactory); this.transactionInterceptor = transactionInterceptor;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Class<?> returnType = method.getReturnType();
        boolean isEmittable = Emittable.class.equals(returnType);
        boolean isTraversable = Traversable.class.equals(returnType);
        if (isTraversable || isEmittable) {
            Class<?> targetType = invocation.getThis() != null ? getTargetClass(invocation.getThis()) : null;
            TransactionAttribute transactionAttribute = ofNullable(getTransactionAttributeSource())
                    .map(attributes -> attributes.getTransactionAttribute(method, targetType)).orElse(null);
            TransactionManager transactionManager = determineTransactionManager(transactionAttribute);
            if (transactionManager instanceof PlatformTransactionManager) {
                TraverseSupport<?, ?> traversable = (TraverseSupport<?, ?>) invocation.proceed();
                if (traversable != null) {
                    traversable = new TransactionalTraversable<>(method, targetType, traversable,
                            (PlatformTransactionManager) transactionManager, transactionAttribute);
                    return isEmittable ? emittable(traversable) : traversable;
                } else return null;
            }
        }
        return transactionInterceptor.invoke(invocation);
    }

    private String methodIdentification(Method method, Class<?> targetClass, TransactionAttribute transactionAttribute) {
        String identification = methodIdentification(method, targetClass);
        if (identification == null) {
            if (transactionAttribute instanceof DefaultTransactionAttribute)
                identification = ((DefaultTransactionAttribute) transactionAttribute).getDescriptor();
            if (identification == null)
                identification = getQualifiedMethodName(method, targetClass);
        }
        return identification;
    }

    protected void commitTransactionAfterCompletion(TransactionInfo transactionInfo) {
        commitTransactionAfterReturning(transactionInfo);
    }

    private void rollbackTransactionAfterFulfillment(TransactionInfo transactionInfo) {
        if (transactionInfo.getTransactionStatus() != null) {
            if (logger.isTraceEnabled())
                logger.trace("Rolling back the transaction [" + transactionInfo.getJoinpointIdentification() + "] as the traversal demand was fulfilled before the completion.");
            transactionInfo.getTransactionManager().rollback(transactionInfo.getTransactionStatus());
        }
    }

    protected void rollbackTransactionAfterThrowing(TransactionInfo transactionInfo, Throwable exception) {
        completeTransactionAfterThrowing(transactionInfo, exception);
    }

    /**
     * The transactional {@link Traversable} implementation.
     *
     * @param <V> the type of the emitting values
     * @param <E> the type of possible exception that might be thrown
     */
    private class TransactionalTraversable<V, E extends Exception> implements Traversable<V, E> {

        final Method method;
        final Class<?> target;
        final TraverseSupport<V, E> traversable;
        final PlatformTransactionManager transactionManager;
        final TransactionAttribute transactionAttribute;

        final AtomicBoolean informedSequential = new AtomicBoolean(false);

        /**
         * Construct the interceptor object with the specified dependencies.
         *
         * @param method the transactional method reflection object
         * @param target the type of the transactional service object
         * @param traversable the specified {@link TraverseSupport} to wrap in transaction
         * @param transactionManager the specified transaction manager object
         * @param transactionAttribute the specified transaction attribute object
         */
        TransactionalTraversable(Method method, Class<?> target, TraverseSupport<V, E> traversable,
                                 PlatformTransactionManager transactionManager, TransactionAttribute transactionAttribute) {
            this.method = method;
            this.target = target;
            this.traversable = traversable;
            this.transactionManager = transactionManager;
            this.transactionAttribute = transactionAttribute;
        }

        @Override
        public <H1 extends Exception, H2 extends Exception> boolean traverse(TraverseMethod method, Traverse<V, E, ? extends H1, ? extends H2> traverse, Association<?, ?, ?> ctx) throws E, H1, H2 {

            TransactionInfo transactionInfo = createTransactionIfNecessary(transactionManager, transactionAttribute,
                    methodIdentification(this.method, target, transactionAttribute));

            informSequential(method, transactionInfo);

            boolean complete;

            try {
                if (!(complete = traversable.sequence(traverse, ctx)))
                    rollbackTransactionAfterFulfillment(transactionInfo);
            } catch (Throwable ex) {
                rollbackTransactionAfterThrowing(transactionInfo, ex);
                throw ex;
            } finally {
                cleanupTransactionInfo(transactionInfo);
            }

            if (complete)
                commitTransactionAfterCompletion(transactionInfo);

            return complete;
        }

        private void informSequential(TraverseMethod method, TransactionInfo transactionInfo) {
            if (logger.isInfoEnabled() && !method.isSequence() && informedSequential.compareAndSet(false, true))
                logger.info("\n!!! Important !!!\n"
                        + "Enforcing sequential traversal for the current thread within a transaction context ["
                        + transactionInfo.getJoinpointIdentification() + "(" + traversable.getClass() + ")" + "].");
        }
    }
}
