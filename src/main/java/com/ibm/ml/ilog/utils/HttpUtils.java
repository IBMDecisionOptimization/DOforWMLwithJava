package com.ibm.ml.ilog.utils;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSON;

import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.TokenHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;


public class HttpUtils implements TokenHandler {
    private static final Logger logger = LogManager.getLogger();

    public Config config = ConfigFactory.parseResources("resources.conf").resolve();
    protected final int tokenRefreshRate = config.getInt("wmlconnector.v4.refresh_rate") * 60 * 1000;

    protected String sslContextName = config.getString("wmlconnector.v4.ssl_context");
    protected final Credentials wml_credentials;


    protected final String DECISION_OPTIMIZATION = "decision_optimization";
    protected final String SOLVE_STATE = "solve_state";
    protected final String FAILURE = "failure";

    protected final String SOLVE_STATUS = "solve_status";
    protected final String STATE = "state";
    protected final String STATUS = "status";
    protected final String ENTITY = "entity";
    protected final String DETAILS = "details";
    protected final String LATEST_ENGINE_ACTIVITY = "latest_engine_activity";
    protected final String OUTPUT_DATA = "output_data";
    protected final String CONTENT = "content";
    protected final String ID = "id";
    protected final String ASSET_ID = "asset_id";
    protected final String METADATA = "metadata";


    protected final String RESOURCES = "resources";
    protected final String NAME = "name";
    protected final String LOGS = "log.txt";

    protected final String MLV4 = "/ml/v4";
    protected final String MLV4_DEPLOYMENT_JOBS = MLV4 + "/deployment_jobs";
    protected final String MLV4_MODELS = MLV4 + "/models";
    protected final String MLV4_DEPLOYMENTS = MLV4 + "/deployments";
    protected final String MLV4_INSTANCES = MLV4 + "/instances";

    protected final String V2 = "/v2";
    protected final String V2_SOFTWARESPECS = V2 + "/software_specifications";
    protected final String V2_SPACES = V2 + "/spaces";
    protected final String V2_CATALOG = V2 + "/catalogs";

    protected final String V2_CONNECTIONS = V2 + "/connections";


    protected final String COMPLETED = "completed";
    protected final String FAILED = "failed";
    protected final String[] status = {COMPLETED, FAILED, "canceled", "deleted"};

    protected final String AUTHORIZATION = "Authorization";
    protected final String APIKEY = "apikey";
    protected final String BEARER = "bearer";
    protected final String APPLICATION_JSON = "application/json";
    protected final String CONTENT_TYPE = "Content-Type";
    protected final String ACCEPT = "Accept";
    protected final String ACCESS_TOKEN = "access_token";
    protected final String ACCESSTOKEN = "accessToken";
    protected final String POST = "POST";
    protected final String PUT = "PUT";
    protected final String GET = "GET";
    protected final String DELETE = "DELETE";
    protected final String ML_Instance_ID = "ML-Instance-ID";
    protected final String CACHE_CONTROL = "cache-control";
    protected final String NO_CACHE = "no-cache";
    protected final String VERSION = "version";
    public final String SPACE_ID = "space_id";


    private CloseableHttpClient httpClient = null;//createHttpsClient();

    protected String bearerToken;

    protected Timer timer = null;

    public static JSONObject parseJson(String input){
        try {
            return (JSONObject) JSON.parse(input);
        } catch (IOException e) {
            logger.warn("Error in json should not happen!!! "+e.getMessage());
            return new JSONObject();
        }
    }
    protected String getAuth() {
        if (bearerToken == null)
            logger.warn("Token is empty...");
        return BEARER + " " + bearerToken;
    }

    public HttpUtils(Credentials creds) throws IloException {
        this.wml_credentials = creds;
    }


    public Map<String, String> getPlatformParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(VERSION, wml_credentials.get(Credentials.WML_VERSION));
        return params;
    }

    public Map<String, String> getWMLParams() {
        Map<String, String> params = getPlatformParams();
        params.put(SPACE_ID, wml_credentials.get(Credentials.WML_SPACE_ID));
        return params;
    }

    public Map<String, String> getPlatformHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(AUTHORIZATION, getAuth());
        return headers;
    }

    public Map<String, String> getWMLHeaders() {
        Map<String, String> headers = getPlatformHeaders();
        headers.put(CACHE_CONTROL, NO_CACHE);
        return headers;
    }

    private class TokenTimer extends TimerTask {
        public TokenTimer() {
        }

        @Override
        public void run() {
            try {
                lookupToken();
            } catch (IloException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    @Override
    public void initToken() throws IloException {
        if (httpClient == null)
            httpClient = createHttpsClient();
        if (bearerToken != null)
            return;
        lookupToken();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TokenTimer(), tokenRefreshRate, tokenRefreshRate);
    }

    private void lookupToken() throws IloException {
        if (wml_credentials.isCPD) lookupIcpToken();
        else lookupBearerToken();
    }


    private void lookupIcpToken() throws IloException {
        logger.info("Lookup Bearer Token from ICP (ASYNCH)");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, APPLICATION_JSON);

        String userName = wml_credentials.get(Credentials.CPD_USERNAME);
        String password = wml_credentials.get(Credentials.CPD_PASSWORD);
        if (userName == null || password == null)
            throw new IloException("Missing credentials for CPD");

        String authString = userName + ":" + password;
        String encodedAuth = new String(Base64.getEncoder().encode(authString.getBytes(StandardCharsets.UTF_8)));

        headers.put(AUTHORIZATION, "Basic " + encodedAuth);

        Map<String, String> params = new LinkedHashMap<>();

        String res = doGet(
                wml_credentials.get(Credentials.WML_HOST),
                wml_credentials.get(Credentials.CPD_URL),
                params,
                headers);

        extractToken(res);
    }

    private void extractToken(String reqAnswer) throws IloException {
            JSONObject json = parseJson(reqAnswer);
            if (json.containsKey(ACCESS_TOKEN))
                bearerToken = (String) json.get(ACCESS_TOKEN);
            else if (json.containsKey(ACCESSTOKEN))
                bearerToken = (String) json.get(ACCESSTOKEN);
            else
                throw new IloException("Missing token in authentication call");
            logger.info("Bearer Token OK");
            //logger.info("Bearer Token OK : " + this.bearerToken);
    }

    private void lookupBearerToken() throws IloException {
        // Cloud
        logger.info("Lookup Bearer Token from IAM (ASYNCH)");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, APPLICATION_JSON);
        headers.put(CONTENT_TYPE, "application/x-www-form-urlencoded");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("grant_type", "urn:ibm:params:oauth:grant-type:apikey");
        params.put(APIKEY, wml_credentials.get(Credentials.WML_API_KEY));

        String res = doPost(
                wml_credentials.get(Credentials.IAM_HOST),
                wml_credentials.get(Credentials.IAM_URL),
                params,
                headers);

        extractToken(res);
    }

    private static class MyX509TrustManager implements X509TrustManager {
        public MyX509TrustManager() {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] var1, String var2) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] var1, String var2) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private String buildTargetUrl(String host, String url, Map<String, String> params) {
        StringBuilder targetUrl = new StringBuilder(host);
        if (!targetUrl.toString().startsWith("https://"))
            targetUrl.insert(0, "https://");

        targetUrl.append(url);
        if (params != null) {
            if (params.size() != 0) targetUrl.append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                targetUrl.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            if (params.size() != 0) targetUrl = new StringBuilder(targetUrl.toString().replace("?&", "?"));
        }
        return targetUrl.toString();
    }

    private HttpRequestBase getRequest(String targetUrl, String method, byte[] body) throws IloException {
        switch (method) {
            case GET:
                return new HttpGet(targetUrl);
            case PUT:
                HttpPut put = new HttpPut(targetUrl);
                if (body != null) {
                    put.setEntity(
                            new ByteArrayEntity(body)
                    );
                }
                return put;
            case POST:
                HttpPost post = new HttpPost(targetUrl);
                if (body != null) {
                    post.setEntity(
                            new ByteArrayEntity(body)
                    );
                }
                return post;
            case DELETE:
                return new HttpDelete(targetUrl);
        }
        throw new IloException("Unkown method type: "+method + " for "+targetUrl);
    }


    private CloseableHttpClient createHttpsClient() throws IloException {
        try {
            SSLContext sslContext = SSLContext.getInstance(sslContextName);


            TrustManager[] managers = new TrustManager[]{new MyX509TrustManager()};
            sslContext.init(null, managers, new SecureRandom());
            return HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier())
                    .setSslcontext(sslContext)
                    .build();
        } catch (KeyManagementException e) {
            throw new IloException(e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new IloException(e.getMessage());
        }
    }

    public void close() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        try {
            if (httpClient != null) httpClient.close();
        } catch (IOException e) {
            logger.warn("Ignoring issue when closing the http client: " + e.getMessage());
        }
        httpClient = null;
    }

    @Override
    public void end() {
        close();
    }


    protected String doCall(String host, String url, Map<String, String> params, Map<String, String> headers, byte[] body, String method) throws IloException {
        String targetUrl = buildTargetUrl(host, url, params);
        Map<String, String> curlParams = new HashMap<>();
        for (String k : params.keySet()) {
            if (k.equals(APIKEY))
                curlParams.put(k, "#####");
            else
                curlParams.put(k, params.get(k));
        }

        String curlUrl = buildTargetUrl(host, url, curlParams);
        HttpRequestBase getReq = getRequest(targetUrl, method, body);

        StringBuilder curl = new StringBuilder();
        curl.append("curl --request ").append(method).append(" \"").append(curlUrl).append("\"");

        Iterator<String> it = headers.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = headers.get(key);
            getReq.addHeader(key, value);
            if (!key.equals(AUTHORIZATION))
                curl.append(" --header \"").append(key).append(": ").append(value).append("\"");
            else {
                curl.append(" --header \"").append(key).append(": ").append(key).append(" ####\"");
                if (value == null)
                    logger.warn("Calling " + url + " with empty token!");
            }
        }
        logger.info("Curl info: " + curl);

        try {
            return executeQuery(getReq);
        }
        catch (IloException | IOException e){
            throw new IloException(e.getMessage());
        }
    }

    private String executeQuery(HttpRequestBase getReq) throws IloException, IOException{
        HttpResponse response = httpClient.execute(getReq);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (statusCode >= 400) {
            logger.error("status " + statusCode);
            String msg = null;
            if (entity != null) {
                String rawResponse = EntityUtils.toString(response.getEntity());
                msg = "Error(" + statusCode + ") calling " + getReq.getURI() + " : " + rawResponse;
                logger.error(msg);
            } else
                msg = "Error(" + statusCode + ") calling " + getReq.getURI();
            throw new IloException(msg);
        } else {
            logger.info("status " + statusCode);
            if (entity != null) {
                return EntityUtils.toString(entity);
            } else
                return null;
        }
    }


    public String doPost(String host, String targetUrl, Map<String, String> params, Map<String, String> headers) throws IloException {
        return doPost(host, targetUrl, params, headers, (byte[]) null);
    }

    public String doPost(String host, String targetUrl, Map<String, String> params, Map<String, String> headers, String body) throws IloException {
        byte[] b = null;
        if (body != null) b = body.getBytes();
        return doPost(host, targetUrl, params, headers, b);
    }

    public String doPost(String host, String targetUrl, Map<String, String> params, Map<String, String> headers, byte[] body) throws IloException {
            return doCall(host, targetUrl, params, headers, body, POST);
    }

    public String doGet(String host, String targetUrl, Map<String, String> params, Map<String, String> headers) throws IloException {
            return doCall(host, targetUrl, params, headers, null, GET);
    }

    public String doDelete(String host, String targetUrl, Map<String, String> params, Map<String, String> headers) throws IloException {
            return doCall(host, targetUrl, params, headers, null, DELETE);
    }

    public String doPut(String host, String targetUrl, Map<String, String> params, Map<String, String> headers, byte[] body) throws IloException {
            return doCall(host, targetUrl, params, headers, body, PUT);
    }

    public byte[] getFileContentAsEncoded64(String inputFile) {
        long t1 = new Date().getTime();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (OutputStream out = Base64.getEncoder().wrap(stream)) {
            Files.copy(Paths.get(inputFile), out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        long t2 = new Date().getTime();
        byte[] ret = stream.toByteArray();
        logger.info("Size of the encoded model file " + inputFile + " is " + ((ret.length + 0.0) / 1024.0 / 1024.0) + " MB");

        logger.info("Encoding the file " + inputFile + " took " + (t2 - t1) / 1000 + " seconds.");
        return ret;
    }

    public static byte[] getFileContent(String inputFilename) {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(inputFilename));
            logger.info("Size of the input file " + inputFilename + " is " + ((bytes.length + 0.0) / 1024.0 / 1024.0) + " MB");
        } catch (IOException e) {
            logger.error("Error getting file " + Arrays.toString(e.getStackTrace()));
        }
        return bytes;
    }
}
