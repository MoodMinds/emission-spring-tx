package org.springframework.transaction.interceptor;

import org.moodminds.emission.Emittable;
import org.moodminds.traverse.Traversable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionManager;

/**
 * The {@link Emittable} and {@link Traversable} transaction interceptor registration configuration bean.
 */
@Configuration
public class TraverseSupportTransactionAdvisory implements InitializingBean {

    private final TransactionInterceptor transactionInterceptor;
    private final BeanFactory beanFactory;
    private final TransactionManager transactionManager;
    private final TransactionAttributeSource transactionAttributeSource;
    private final BeanFactoryTransactionAttributeSourceAdvisor transactionAttributeSourceAdvisor;

    /**
     * Construct the configuration bean object with the specified dependencies.
     *
     * @param transactionInterceptor the specified transaction interceptor instance
     * @param beanFactory the specified bean factory instance
     * @param transactionManager the specified transaction manager instance
     * @param transactionAttributeSource the specified transaction attribute source instance
     * @param transactionAttributeSourceAdvisor the specified transaction attribute source advisor
     */
    public TraverseSupportTransactionAdvisory(TransactionInterceptor transactionInterceptor, BeanFactory beanFactory,
                                              TransactionManager transactionManager, TransactionAttributeSource transactionAttributeSource,
                                              BeanFactoryTransactionAttributeSourceAdvisor transactionAttributeSourceAdvisor) {
        this.transactionInterceptor = transactionInterceptor;
        this.beanFactory = beanFactory;
        this.transactionManager = transactionManager;
        this.transactionAttributeSource = transactionAttributeSource;
        this.transactionAttributeSourceAdvisor = transactionAttributeSourceAdvisor;
    }

    /**
     * Register the transaction interceptor at the end of the bean initialization.
     */
    @Override
    public void afterPropertiesSet() {
        transactionAttributeSourceAdvisor.setAdvice(new TraverseSupportTransactionInterceptor(
                transactionManager, transactionAttributeSource, beanFactory, transactionInterceptor));
    }
}
