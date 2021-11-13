/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.experimental.irma;

import com.google.common.hash.Hashing;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.JsonSerialization;

import javax.json.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class IrmaQRFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    @Override
    public void action(AuthenticationFlowContext context) {
        //After 200 OK, Irma.js client will invoke form POST action
        try {
            String token = context.getAuthenticationSession().getAuthNote("token");
            URI uri = URI.create(context.getAuthenticatorConfig().getConfig().get("url"));
            String entity = getIrmaSessionInfo(uri, token);

            JsonReader reader = Json.createReader(new StringReader(entity));
            JsonObject jsonObject = reader.readObject();
            JsonArray disclosing = jsonObject.getJsonArray("disclosed");
            JsonString attribute = disclosing.getJsonArray(0).getJsonObject(0).getJsonString("rawvalue");

            String hash = Hashing.sha256().hashString(attribute.getString(), StandardCharsets.UTF_8).toString();

            UserModel user = context.getSession().users().searchForUserByUserAttribute("bsn", hash, context.getRealm())
                    .stream().findFirst().orElseGet(() -> {
                        UserModel u = context.getSession().users()
                                .addUser(context.getRealm(), UUID.randomUUID().toString());
                        u.setEnabled(true);
                        u.setAttribute("bsn", Arrays.asList(hash));
                        return u;
                    });

            context.getAuthenticationSession().setAuthenticatedUser(user);
            context.setUser(user);
            context.success();
            return;
        } catch (Exception ex) {
            System.out.println(ex.getClass().getCanonicalName());
            System.out.println("ex:" + ex.getMessage());
            context.challenge(context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        //create irma session irmaClient.createSession
        //put irma session.token in context context.getAuthenticationSession().setAuthNote("token", session.token);
        //return session.token to imra.js
        try {
            if (context.getHttpRequest().getUri().getQueryParameters().getFirst("start") != null) {
                //Create disclose request and start session
                URI uri = URI.create(context.getAuthenticatorConfig().getConfig().get("url"));
                String attribute = context.getAuthenticatorConfig().getConfig().get("attribute");

                String session = getIrmaDiscloseSession(uri, attribute);
//                System.out.println("Session-token:" + session);
                Root root = JsonSerialization.readValue(session, Root.class);

                context.getAuthenticationSession().setAuthNote("token", root.token);
                context.challenge(Response.ok(session).build());
                return;
            }
        } catch (IOException e) {
            context.challenge(context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }

        //On successfull reveal of attributes, reply to irma.js client (session.result()) with 200 OK
        if (context.getAuthenticationSession().getAuthNote("token") != null) { //token is set, so start must have happened
            context.challenge(Response.ok("{}").build());
            return;
        }

        //Initial creation of the form with Irma QR-code
        String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).build().toString();
        context.form().setAttribute("irmaurl", link);
        Response response = context.form().createForm("login-irma-only.ftl");
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        context.challenge(response);
    }

    private String getIrmaDiscloseSession(URI uri, String attribute) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(new StringEntity(
                "{\"@context\":\"https://irma.app/ld/request/disclosure/v2\"," +
                    "\"disclose\":[" +
                        "[" +
                            "[\"" + attribute +"\"]" + //e.g. pbdf.sidn-pbdf.email.email, pbdf.bzkpilot.personalData.bsn
                        "]" +
                    "]}"));

        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = httpClient.execute(httpPost);
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }

    private String getIrmaSessionInfo(URI uri, String token) throws IOException {
        URI sessionResultUri = UriBuilder.fromUri(uri)
                .path(token)
                .path("result")
                .build();

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(sessionResultUri);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        return EntityUtils.toString(response.getEntity());
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
