package com.affiliatelink.offer.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect @Component
public class OfferLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(OfferLoggingAspect.class);

    @Around("execution(* com.affiliatelink.offer.service.OfferService.*(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodeName = joinPoint.getSignature().getName();

        log.debug("starting method: {}", methodeName);

        long start = System.currentTimeMillis();

        try{
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            log.debug("Method {} completed at {} ms", methodeName, duration);
            return result;
        }
        catch(Throwable e){
            long duration = System.currentTimeMillis() - start;
            log.debug("Method {} failed after {} ms",methodeName, duration);
            throw e;
        }
    }

}
