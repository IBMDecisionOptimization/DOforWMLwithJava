package com.ibm.wmlconnector.impl;

import com.ibm.wmlconnector.COSConnector;
import com.ibm.wmlconnector.Credentials;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.logging.Logger;

public class COSConnectorImpl extends ConnectorImpl implements COSConnector {

    private static final Logger LOGGER = Logger.getLogger(COSConnectorImpl.class.getName());


    protected String cos_url;
    protected String cos_bucket;

    protected String cos_access_key_id;
    protected String cos_secret_access_key;

    public COSConnectorImpl(Credentials credentials) {
        super(credentials, true);
        this.cos_url = credentials.COS_ENDPOINT;
        this.cos_bucket = credentials.COS_BUCKET;
        this.cos_access_key_id = credentials.COS_ACCESS_KEY_ID;
        this.cos_secret_access_key = credentials.COS_SECRET_ACCESS_KEY;
        lookupBearerToken();
    }


    @Override
    public void putFile(String fileName, String filePath) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("Content-Type", "text/plain");

        doPut(cos_url + "/" + cos_bucket + "/" + fileName, headers, getFileContent(filePath));
    }

    @Override
    public void putBinaryFile(String fileName, String filePath) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);

        byte[] bytes = getBinaryFileContent(filePath);

        doPut(cos_url + "/" + cos_bucket + "/" + fileName, headers, bytes);

    }

    @Override
    public String getFile(String fileName) {

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("Content-Type", "text/plain");


        String res = doGet(cos_url + "/" + cos_bucket + "/" + fileName, headers);

        return res;
    }

    @Override
    public JSONObject getDataReferences(String id) {
        String data = "{\n" +
                        "\"id\": \"" + id + "\",\n" +
                        "\"type\": \"s3\",\n" +
                        "\"connection\": {\n" +
                            "\"endpoint_url\": \"" + cos_url + "\",\n" +
                            "\"access_key_id\": \"" + cos_access_key_id + "\",\n" +
                            "\"secret_access_key\": \"" + cos_secret_access_key + "\"\n" +
                        "}, \n" +
                        "\"location\": {\n" +
                            "\"bucket\": \"" + cos_bucket + "\",\n" +
                            "\"path\": \"" + id + "\"\n" +
                        "}\n" +
                        "}\n";
        JSONObject jsonData  = new JSONObject(data);
        return jsonData;
    }


}
