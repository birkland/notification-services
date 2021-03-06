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
package org.dataconservancy.pass.notification.app;

import org.dataconservancy.pass.notification.app.config.SpringBootNotificationConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot entry point for launching Notification Services.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@SpringBootApplication
@ComponentScan("org.dataconservancy.pass")
@Import(SpringBootNotificationConfig.class)
@EnableAspectJAutoProxy
public class NotificationApp {

    /**
     * Spring Boot entry point.
     *
     * @param args command line args to be processed
     */
    public static void main(String[] args) {
        System.exit(1);
    }

}
