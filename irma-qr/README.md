# IRMA QR authenticator for Keycloak

Allows users to authenticate through a QR code instead of using a password.

![irma](login.png)
## Usage

1. Deploy to Keycloak:

    mvn clean install wildfly:deploy

2. Configure SMTP server for realm

3. Configure realm authentication flow

   * Create copy of Browser flow
   * Delete "Username Password Form" and "OTP Form" executors
   * Click on Actions next to "Copy Of Browser Forms" and click "Add execution"
   * Add "Irma"
   * Set requirement "Required" on "Irma" executor
   * Click on bindings and switch "Browser flow" to "Copy of browser flow" 
