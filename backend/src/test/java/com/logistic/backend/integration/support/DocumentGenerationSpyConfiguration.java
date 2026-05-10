package com.logistic.backend.integration.support;

import com.logistic.backend.document.DocumentGenerationService;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wraps the singleton {@link DocumentGenerationService} with a Mockito spy for integration tests that
 * verify async document prefetch.
 */
@Configuration(proxyBeanMethods = false)
public class DocumentGenerationSpyConfiguration {

    @Bean
    BeanPostProcessor spyDocumentGenerationService() {
        return new SpyDocumentGenerationBeanPostProcessor();
    }

    private static final class SpyDocumentGenerationBeanPostProcessor implements BeanPostProcessor, Ordered {

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (!(bean instanceof DocumentGenerationService d)) {
                return bean;
            }
            if (Mockito.mockingDetails(bean).isMock()) {
                return bean;
            }
            return Mockito.spy(d);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
