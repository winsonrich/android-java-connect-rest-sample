/*
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license.
 * See LICENSE in the project root for license information.
 */
package com.microsoft.office365.connectmicrosoftgraph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

public class ConnectUnitTests {
    private static String accessToken;
    private static String clientId = System.getenv("test_client_id_v1");
    private static String username = System.getenv("test_username");
    private static String password = System.getenv("test_password");

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String GRANT_TYPE = "password";
    private static final String TOKEN_ENDPOINT = Constants.AUTHORITY_URL + "/oauth2/token";
    private static final String REQUEST_METHOD = "POST";

    private final String SUBJECT = "Email sent from test in android connect sample";
    private final String BODY = "<html><body>The body of the test email</body></html>";

    @BeforeClass
    public static void getAccessTokenUsingPasswordGrant() throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException, JSONException {
        URL url = new URL(TOKEN_ENDPOINT);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        String urlParameters = String.format(
                "grant_type=%1$s&resource=%2$s&client_id=%3$s&username=%4$s&password=%5$s",
                GRANT_TYPE,
                URLEncoder.encode(Constants.MICROSOFT_GRAPH_API_ENDPOINT_RESOURCE_ID, "UTF-8"),
                clientId,
                username,
                password
        );

        connection.setRequestMethod(REQUEST_METHOD);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.getBytes("UTF-8").length));

        connection.setDoOutput(true);
        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.writeBytes(urlParameters);
        dataOutputStream.flush();
        dataOutputStream.close();

        connection.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        JsonParser jsonParser = new JsonParser();
        JsonObject grantResponse = (JsonObject)jsonParser.parse(response.toString());
        accessToken = grantResponse.get("access_token").getAsString();
    }

    @Test
    public void sendMail_messageSent() throws IOException {
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                request = request.newBuilder()
                        .addHeader("Authorization", "Bearer " + accessToken)
                        // This header has been added to identify this sample in the Microsoft Graph service.
                        // If you're using this code for your project please remove the following line.
                        .addHeader("SampleID", "android-java-connect-rest-sample")
                        .build();

                Response response = chain.proceed(request);
                return response;
            }
        };

        MSGraphAPIController controller = new MSGraphAPIController(interceptor);
        Call<Void> result = controller.sendMail(username, SUBJECT, BODY);
        retrofit2.Response response = result.execute();
        Assert.assertTrue("HTTP Response was not successful", response.isSuccessful());
    }
}