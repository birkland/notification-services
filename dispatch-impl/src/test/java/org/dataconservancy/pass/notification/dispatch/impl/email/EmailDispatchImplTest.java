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
package org.dataconservancy.pass.notification.dispatch.impl.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.User;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.Notification.Param;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.Recipient;
import org.simplejavamail.mailer.Mailer;

import javax.mail.Message.RecipientType;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmailDispatchImplTest {

    private Notification notification;

    private NotificationConfig config;

    private TemplatePrototype templateProto;

    private TemplateResolver templateResolver;

    private Template template;

    private Handlebars handlebars;

    private ObjectMapper mapper = new ObjectMapper();

    private Map<Name, String> templateRefs;

    private Mailer mailer;

    private PassClient passClient;

    private String mailtoUri = "mailto:John%20Doe%3Cjohndoe%40example.org%3E";

    private User user;

    private String userUri = "https://pass.jhu.edu/fcrepo/users/123456";

    private String userEmail = "user@bar.com";

    private String from = "pass-noreply@jhu.edu";

    private String cc = "pass-prod-cc@jhu.edu,pass-admin@jhu.edu";

    private String metadata = "" +
            "[\n" +
            "  {\n" +
            "    \"a sample metadata\": \"blob\",\n" +
            "    \"for\": \"a Submission\"\n" +
            "  }\n" +
            "]";

    private EmailDispatchImpl underTest;


    @Before
    public void setUp() throws Exception {
        notification = mock(Notification.class);
        config = mock(NotificationConfig.class);
        templateProto = mock(TemplatePrototype.class);
        templateResolver = mock(TemplateResolver.class);
        template = mock(Template.class);
        handlebars = mock(Handlebars.class);
        passClient = mock(PassClient.class);
        user = mock(User.class);
        mailer = mock(Mailer.class);

        when(config.getTemplates()).thenReturn(Collections.singletonList(templateProto));
        when(templateProto.getRefs()).thenReturn(new HashMap<Name, String> () {
            {
                put(Name.SUBJECT, "A Subject");
                put(Name.BODY, "A Body");
                put(Name.FOOTER, "A Footer");
            }
        });
        when(user.getId()).thenReturn(URI.create(userUri));
        when(user.getEmail()).thenReturn(userEmail);
        when(passClient.readResource(URI.create(userUri), User.class)).thenReturn(user);

        underTest = new EmailDispatchImpl(config, passClient, templateResolver, mapper, handlebars, mailer);
    }

    @Test
    public void simpleSuccess() throws IOException {
        when(notification.getType()).thenReturn(Notification.Type.SUBMISSION_APPROVAL_INVITE);
        when(notification.getParameters()).thenReturn(
                new HashMap<Param, String>() {
                    {
                        put(Param.FROM, from);
                        put(Param.TO, userUri);
                        put(Param.CC, cc);
                        put(Param.RESOURCE_METADATA, metadata);
                    }
                });
        when(notification.getRecipients()).thenReturn(Collections.singleton(userUri));
        when(notification.getSender()).thenReturn(from);
        when(notification.getCc()).thenReturn(Collections.singleton(cc));

        when(templateProto.getNotificationType()).thenReturn(Notification.Type.SUBMISSION_APPROVAL_INVITE);
        when(templateResolver.resolve(any(), any())).thenAnswer(inv ->
                IOUtils.toInputStream(inv.getArgument(1), "UTF-8"));

        when(handlebars.compileInline(any())).thenReturn(template);
        when(template.apply(any())).thenAnswer(inv -> inv.getArgument(0));

        underTest.dispatch(notification);

        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);

        verify(mailer).sendMail(emailCaptor.capture());

        Email email = emailCaptor.getValue();

        // Verify the email was composed properly
        assertEquals(from, email.getFromRecipient().getAddress());

        assertTrue(email.getRecipients()
                .stream()
                .filter(recipient -> recipient.getType() == RecipientType.TO)
                .anyMatch(recipient -> recipient.getAddress().equals(userEmail)));

        email.getRecipients()
            .stream()
            .filter(recipient -> recipient.getType() == RecipientType.CC)
            .map(Recipient::getAddress)
            .forEach(address ->
                    assertTrue("Missing expected CC address '" + address + "' from CC recipients",
                            cc.contains(address)));

        assertEquals("A Subject", email.getSubject());
        assertEquals(String.join("\n\n", "A Body", "A Footer"), email.getPlainText());
    }
}