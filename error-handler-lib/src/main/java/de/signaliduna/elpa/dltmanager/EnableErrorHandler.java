package de.signaliduna.elpa.dltmanager;


import de.signaliduna.elpa.dltmanager.config.CloudEventMessageConverterConfiguration;
import de.signaliduna.elpa.dltmanager.config.ErrorHandlerConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Enables the error-handler library to configure itself and add the required beans to the application context.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({ErrorHandlerConfig.class, CloudEventMessageConverterConfiguration.class})

public @interface EnableErrorHandler {
}
