<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))?no_esc}
    <#elseif section = "form">
        <#if realm.password>
            <script src="${url.resourcesPath!}/js/irma.js"></script>
            <div id="irma">
            </div>

            <div style="display: none">
                <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post"></form>
            </div>

            <script>
                var url = '${irmaurl}'.replaceAll('amp;', '');

                function showQR(){
                    const irmaWeb = irma.newWeb({
                        debugging: true,    // Enable to get helpful output in the browser console
                        element:   '#irma', // Which DOM element to render to

                        // Back-end options
                        session: {
                            start: {
                                url: o => url + '&start',
                                method: 'GET'
                            },
                            result: {
                                url: (o, {sessionPtr, sessionToken}) => url,
                                method: 'GET'
                            }
                        }
                    });

                    irmaWeb.start()
                        .then(result => {
                            document.getElementById("kc-form-login").submit();
                        })
                        .catch(error => {
                            console.error("Couldn't do what you asked 😢", error);
                        });
                }

                showQR();
            </script>
        </#if>
    </#if>
</@layout.registrationLayout>
