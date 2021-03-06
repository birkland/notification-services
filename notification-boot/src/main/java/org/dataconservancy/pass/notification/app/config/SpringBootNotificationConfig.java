/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.pass.notification.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientDefault;
import org.dataconservancy.pass.notification.dispatch.impl.email.CompositeResolver;
import org.dataconservancy.pass.notification.dispatch.impl.email.EmailDispatchImpl;
import org.dataconservancy.pass.notification.dispatch.impl.email.HandlebarsParameterizer;
import org.dataconservancy.pass.notification.dispatch.impl.email.InlineTemplateResolver;
import org.dataconservancy.pass.notification.dispatch.impl.email.SpringUriTemplateResolver;
import org.dataconservancy.pass.notification.dispatch.impl.email.TemplateParameterizer;
import org.dataconservancy.pass.notification.dispatch.impl.email.TemplateResolver;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.mock;

/**
 * Primary Spring Boot configuration class for Notification Services
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Configuration
public class SpringBootNotificationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootNotificationConfig.class);

    @Value("${pass.fedora.user}")
    private String fedoraUser;

    @Value("${pass.fedora.password}")
    private String fedoraPass;

    @Value("${pass.fedora.baseurl}")
    private String fedoraBaseUrl;

    @Value("${pass.elasticsearch.url}")
    private String esUrl;

    @Value("${pass.elasticsearch.limit}")
    private int esLimit;

    @Value("${pass.notification.http.agent}")
    private String passHttpAgent;

    @Value("${pass.notification.configuration}")
    private Resource notificationConfiguration;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public NotificationConfig notificationConfig(ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(notificationConfiguration.getInputStream(), NotificationConfig.class);
    }

    @Bean
    public PassClientDefault passClient() {

        // PassClientDefault can't be injected with configuration; requires system properties be set.
        // If a system property is already set, allow it to override what is resolved by the Spring environment.
        if (!System.getProperties().containsKey("pass.fedora.user")) {
            System.setProperty("pass.fedora.user", fedoraUser);
        }

        if (!System.getProperties().containsKey("pass.fedora.password")) {
            System.setProperty("pass.fedora.password", fedoraPass);
        }

        if (!System.getProperties().containsKey("pass.fedora.baseurl")) {
            System.setProperty("pass.fedora.baseurl", fedoraBaseUrl);
        }

        if (!System.getProperties().containsKey("pass.elasticsearch.url")) {
            System.setProperty("pass.elasticsearch.url", esUrl);
        }

        if (!System.getProperties().containsKey("pass.elasticsearch.limit")) {
            System.setProperty("pass.elasticsearch.limit", String.valueOf(esLimit));
        }

        if (!System.getProperties().containsKey("http.agent")) {
            System.setProperty("http.agent", passHttpAgent);
        }

        return new PassClientDefault();
    }

    @Bean
    public EmailDispatchImpl emailDispatchService(NotificationConfig config,
                                                  PassClient passClient,
                                                  TemplateResolver compositeResolver,
                                                  TemplateParameterizer templateParameterizer,
                                                  Mailer mailer) {

        return new EmailDispatchImpl(config, passClient, compositeResolver, templateParameterizer, mailer);

    }

    @Bean
    public InlineTemplateResolver inlineTemplateResolver() {
        return new InlineTemplateResolver();
    }

    @Bean
    public SpringUriTemplateResolver springUriTemplateResolver() {
        return new SpringUriTemplateResolver();
    }

    @Bean
    public CompositeResolver compositeResolver(InlineTemplateResolver inlineTemplateResolver,
                                               SpringUriTemplateResolver springUriTemplateResolver) {
        return new CompositeResolver(Arrays.asList(inlineTemplateResolver, springUriTemplateResolver));
    }

    @Bean
    public HandlebarsParameterizer handlebarsParameterizer(Handlebars handlebars, ObjectMapper objectMapper) {
        return new HandlebarsParameterizer(handlebars, objectMapper);
    }

    @Bean
    public Handlebars handlebars() {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        return handlebars;
    }

    @Bean
    public Mailer mailer() {
        return mock(Mailer.class);
    }


}
