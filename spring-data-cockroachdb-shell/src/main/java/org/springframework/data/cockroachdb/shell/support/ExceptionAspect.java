package org.springframework.data.cockroachdb.shell.support;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.cockroachdb.shell.event.CommandExceptionEvent;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExceptionAspect {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Around(value = "@annotation(org.springframework.shell.standard.ShellMethod)")
    public Object handleMethodCall(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            applicationEventPublisher.publishEvent(new CommandExceptionEvent(this, t));
            throw t;
        }
    }
}
