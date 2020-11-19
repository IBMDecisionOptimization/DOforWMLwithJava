package com.ibm.ml.ilog.v4;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONObject;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.utils.HttpUtils;
import ilog.concert.IloException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class COSConnector extends HttpUtils implements com.ibm.ml.ilog.COSConnector {

    private static final Logger logger = LogManager.getLogger();

    private void checkCredentials() throws IloException {
        for (String k : Credentials.COSFields) {
            if (!wml_credentials.containsKey(k)) {
                throw new IloException("Missing config for COS: " + k);
            }
        }
    }

    public COSConnector(Credentials creds) throws IloException {
        super(creds);
        logger.info("Creation of a COS connector.");
        if (creds.isCPD) {
            logger.warn("Credentials are for CPD but deal with COS...");
        }
        checkCredentials();
    }

    @Override
    public JSONObject getDataReferences(String id) {
        String cos_endpoint = wml_credentials.get(Credentials.COS_ENDPOINT);
        String cos_access_key_id = wml_credentials.get(Credentials.COS_ACCESS_KEY_ID);
        String cos_bucket = wml_credentials.get(Credentials.COS_BUCKET);
        String cos_secret_access_key = wml_credentials.get(Credentials.COS_SECRET_ACCESS_KEY);
        String data = "{\n" +
                "\"id\": \"" + id + "\",\n" +
                "\"type\": \"s3\",\n" +
                "\"connection\": {\n" +
                "\"endpoint_url\": \"" + cos_endpoint + "\",\n" +
                "\"access_key_id\": \"" + cos_access_key_id + "\",\n" +
                "\"secret_access_key\": \"" + cos_secret_access_key + "\"\n" +
                "}, \n" +
                "\"location\": {\n" +
                "\"bucket\": \"" + cos_bucket + "\",\n" +
                "\"path\": \"" + id + "\"\n" +
                "}\n" +
                "}\n";
        return parseJson(data);
    }

    @Override
    public String putFile(String fileName, String filePath) throws IloException {
        String cos_bucket = wml_credentials.get(Credentials.COS_BUCKET);

        byte[] bytes = getFileContent(filePath);

        Map<String, String> params = getPlatformParams();
        params.put("content_format", "native");

        Map<String, String> headers = getPlatformHeaders();
        headers.put("Content-Type", "text/plain");

        return doPut(
                wml_credentials.get(Credentials.COS_ENDPOINT),
                "/" + cos_bucket + "/" + fileName,
                params, headers, bytes);
    }

    @Override
    public String getFile(String fileName) throws IloException {
        String cos_bucket = wml_credentials.get(Credentials.COS_BUCKET);

        Map<String, String> params = getPlatformParams();
        Map<String, String> headers = getPlatformHeaders();

        String res = doGet(
                wml_credentials.get(Credentials.COS_ENDPOINT),
                "/" + cos_bucket + "/" + fileName,
                params,
                headers);

        return res;
    }
}
