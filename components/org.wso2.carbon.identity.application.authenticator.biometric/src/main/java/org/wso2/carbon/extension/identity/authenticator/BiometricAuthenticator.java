package org.wso2.carbon.extension.identity.authenticator;

/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.extension.identity.authenticator.dao.impl.BiometricDAOImpl;
import org.wso2.carbon.extension.identity.authenticator.notification.handler.impl.PushNotificationSenderImpl;
import org.wso2.carbon.identity.application.authentication.framework.AbstractApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Biometric Authenticator.
 */

public class BiometricAuthenticator extends AbstractApplicationAuthenticator
        implements FederatedApplicationAuthenticator {

    private static final Log log = LogFactory.getLog(BiometricAuthenticator.class);
    public static String sessionDataKey;
    public static String randomChallenge;
    public static String signedChallenge;
    public static String serviceProviderName;
    public static String hostname;

    /**
     * Get the friendly name of the Authenticator.
     */
    @Override
    public String getFriendlyName() {
        return BiometricAuthenticatorConstants.AUTHENTICATOR_FRIENDLY_NAME;
    }

    @Override
    public boolean canHandle(HttpServletRequest request) {
        signedChallenge = request.getParameter(BiometricAuthenticatorConstants.SIGNED_CHALLENGE);
        log.info("signed challenge from the form submitted: " + signedChallenge);
        if (signedChallenge == null) {
            log.info("False branch " );
            return false;
        } else {
            log.info("True branch " );
            return true;
        }

    }


    @Override
    public String getContextIdentifier(javax.servlet.http.HttpServletRequest request) {
        sessionDataKey = request.getParameter(BiometricAuthenticatorConstants.SESSION_DATA_KEY);
        log.info("autogenerated Session Data Key is : " + sessionDataKey);

        return sessionDataKey;
    }



    /**
     * Get the name of the Authenticator.
     */
    @Override
    public String getName() {
        return BiometricAuthenticatorConstants.AUTHENTICATOR_NAME;
    }
//
//    private String getTokenEndpoint(Map<String, String> authenticatorProperties) {
//        return authenticatorProperties.get(BiometricAuthenticatorConstants.SERVER_KEY);
//    }

    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context) {

        AuthenticatedUser user = context.getSequenceConfig().getStepMap().get(1).getAuthenticatedUser();

        String usernameDB = user.getUserName();


        Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
        String serverKey = authenticatorProperties.get(BiometricAuthenticatorConstants.SERVER_KEY);


        List<String> putIds;
        hostname = IdentityUtil.getHostName();
        serviceProviderName = context.getServiceProviderName();
        String message = usernameDB + " is trying to log into " + serviceProviderName + " from " + hostname;



        UUID challenge = UUID.randomUUID();
        randomChallenge = challenge.toString();
        log.info("Random Challenge is  = " + randomChallenge);
        log.info("this is my randomly generated challenge2: " + randomChallenge);

        BiometricDAOImpl biometricDAO = BiometricDAOImpl.getInstance();

        String deviceID = biometricDAO.getDeviceID(usernameDB);



        putIds = new ArrayList();
        putIds.add(deviceID);

        log.info("new sessiondatakey is here: " + context.getContextIdentifier());
        log.info("find device ID : " + deviceID);

        PushNotificationSenderImpl pushNotificationSender = PushNotificationSenderImpl.getInstance();
        pushNotificationSender.sendPushNotification(deviceID, serverKey, message, randomChallenge, sessionDataKey);



        try {
            String waitPage = "https://localhost:9443/authenticationendpoint/wait.jsp?sdk=" + sessionDataKey;

            response.sendRedirect(waitPage);
        } catch (IOException e) {
            log.info("Error");

        }
    }

    /**
     * Process the response of the SMSOTP end-point.
     *
     * @param httpServletRequest  the HttpServletRequest
     * @param httpServletResponse the HttpServletResponse
     * @param authenticationContext  the AuthenticationContext
     */

    @Override
    protected void processAuthenticationResponse(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, AuthenticationContext authenticationContext) {

        log.info("signed challenge from the form--->now inside processAuth method: " + signedChallenge);
        log.info(randomChallenge + "     AAANNNNDDDD   " + signedChallenge);
        if (randomChallenge.equals(signedChallenge)) {
            AuthenticatedUser user = authenticationContext.getSequenceConfig().
                    getStepMap().get(1).getAuthenticatedUser();
            authenticationContext.setSubject(user);

        } else {
            log.info("2 challenges are not the same!");
        }

    }

    /**
     * Get Configuration Properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<>();


        Property serverKey = new Property();
        serverKey.setName(BiometricAuthenticatorConstants.SERVER_KEY);
        serverKey.setDisplayName("Firebase Server Key");
        serverKey.setDescription("Enter the firebase server key of the android app");
        serverKey.setDisplayOrder(1);
        configProperties.add(serverKey);


        return configProperties;
    }
}


