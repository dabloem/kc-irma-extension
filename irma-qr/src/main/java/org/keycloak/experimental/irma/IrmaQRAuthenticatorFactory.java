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

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IrmaQRAuthenticatorFactory implements AuthenticatorFactory {

    public static final String ID = "magic-form";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.CONDITIONAL,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IrmaQRFormAuthenticator();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getReferenceCategory() {
        return "irma";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "IRMA";
    }

    @Override
    public String getHelpText() {
        return "IRMA Authentication";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configs = new ArrayList<>();
        configs.add(new ProviderConfigProperty("url" ,"Irma url", "url to IRMA server", ProviderConfigProperty.STRING_TYPE, "http://irma:8088/session"));
        configs.add(new ProviderConfigProperty("attribute" ,"attribute", "attribute", ProviderConfigProperty.STRING_TYPE, "pbdf.sidn-pbdf.email.email"));
        return configs;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
