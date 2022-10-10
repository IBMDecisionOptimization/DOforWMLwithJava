package com.ibm.ml.ilog.v4;

import com.ibm.json.java.JSONObject;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.utils.HttpUtils;
import ilog.concert.IloException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.Map;

public class COSConnector extends HttpUtils implements com.ibm.ml.ilog.COSConnector {

    private String _connectionId = null;
    private String _bucketPath = null;
    private static final Logger logger = LogManager.getLogger();
    @Override
    public void setBucketPath(String path) throws IloException {
        if (_bucketPath != null) throw new IloException("Path in bucket already exist");
        _bucketPath = path;
    }

    @Override
    public void end() {
        if (_connectionId != null){
            Map<String, String> params = getWMLParams();

            Map<String, String> headers = getPlatformHeaders();

            long t1 = new Date().getTime();
            try {
                doDelete(
                        wml_credentials.get(Credentials.PLATFORM_HOST),
                        V2_CONNECTIONS+"/"+_connectionId,
                        params, headers);
            } catch (IloException e) {
                logger.error(e.getMessage());
            }
            long t2 = new Date().getTime();
            logger.info("Connection Id = " + _connectionId);
            logger.info("Deleting the connection took " + (t2 - t1) / 1000 + " seconds.");
            _connectionId = null;
        }
        super.end();
    }

    public void deleteConnection() {
        if (_connectionId != null) {
            Map<String, String> params = getWMLParams();

            Map<String, String> headers = getPlatformHeaders();

            long t1 = new Date().getTime();
            String res = null;
            try {
                res = doDelete(
                        wml_credentials.get(Credentials.PLATFORM_HOST),
                        V2_CONNECTIONS + "/" + _connectionId,
                        params, headers);
            } catch (IloException e) {
                logger.error(e.getMessage());
            }
            long t2 = new Date().getTime();
            logger.info("Connection Id = " + _connectionId);
            logger.info("Deleting the connection took " + (t2 - t1) / 1000 + " seconds.");
            _connectionId = null;
        }
    }

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
    public JSONObject getDataReferences(String id) throws IloException {

        String cos_bucket = wml_credentials.get(Credentials.COS_BUCKET);

        String filename = "\"file_name\": \"";
        if (_bucketPath != null)
            filename = filename + _bucketPath + "/";
        filename = filename + id  + "\"\n";
        String data = "{\n" +
                //"\"id\": \"" + id + "\",\n" +
                "\"type\": \"connection_asset\",\n" +
                "\"id\": \"" + id  + "\",\n" +
                "\"connection\": {\n" +
                "\"id\": \"" + getConnectionId()  + "\"\n" +
                "},\n" +
                "\"location\": {\n" +
                "\"bucket\": \"" + cos_bucket  + "\",\n" +
                filename +
                "},\n" +
                "}\n";
        return parseJson(data);
    }

    @Override
    public String putFile(String fileName, String filePath) throws IloException {
        byte[] bytes = getFileContent(filePath);

        Map<String, String> params = getPlatformParams();
        params.put("content_format", "native");

        Map<String, String> headers = getPlatformHeaders();
        headers.put("Content-Type", "text/plain");

        long t1 = new Date().getTime();
        String ret = doPut(
                wml_credentials.get(Credentials.COS_ENDPOINT),
                getTargetUrl(fileName),
                params, headers, bytes);
        long t2 = new Date().getTime();
        logger.info("Uploading in COS took " + (t2 - t1) / 1000 + " seconds.");
        return ret;
    }

    private String getTargetUrl(String fileName){
        String cos_bucket = wml_credentials.get(Credentials.COS_BUCKET);
        String targetUrl = "/" + cos_bucket + "/";
        if (_bucketPath != null)
            targetUrl = targetUrl + _bucketPath + "/";
        targetUrl = targetUrl + fileName;
        return targetUrl;
    }

    private String getS3Id() throws IloException{
        Map<String, String> params = getWMLParams();
        Map<String, String> headers = getPlatformHeaders();

        String res = doGet(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_COS,
                params, headers);
        JSONObject json = parseJson(res);
        return (String) ((JSONObject) json.get(METADATA)).get(ASSET_ID);
    }


    @Override
    public String getConnectionId() throws IloException {
        if (_connectionId != null)
            return _connectionId;
        long t1 = new Date().getTime();
        Map<String, String> params = getWMLParams();

        Map<String, String> headers = getPlatformHeaders();
        headers.put("Content-Type", "application/json");

        String cos_access_key_id = wml_credentials.get(Credentials.COS_ACCESS_KEY_ID);
        String cos_secret_access_key = wml_credentials.get(Credentials.COS_SECRET_ACCESS_KEY);
        String cos_origin = wml_credentials.get(Credentials.COS_ORIGIN_COUNTRY);
        String url = wml_credentials.get(Credentials.COS_ENDPOINT);
        String cos_connId = getS3Id();
        String payload = "{\n" +
                "\"name\": " +  "\"s3_shared_cxn\""    + ",\n" +
                "\"datasource_type\": \"" +  cos_connId    + "\",\n" +
                "\"origin_country\": \"" + cos_origin + "\",\n" +
                "\"properties\": {\n" +
                "\"access_key\": \"" + cos_access_key_id + "\",\n" +
                "\"secret_key\": \"" + cos_secret_access_key + "\",\n" +
                "\"url\": \"" + url + "\"\n" +
                "} \n" +
                "}\n";
        JSONObject data = parseJson(payload);
    String res = doPost(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_CONNECTIONS,
                params, headers, data.toString());
        JSONObject json = parseJson(res);
        long t2 = new Date().getTime();

        _connectionId = (String) ((JSONObject) json.get(METADATA)).get(ASSET_ID);

        logger.info("Connection Id = " + _connectionId);
        logger.info("Creating the connection took " + (t2 - t1) / 1000 + " seconds.");
        return _connectionId;
    }

    @Override
    public String getFile(String fileName) throws IloException {
        Map<String, String> params = getPlatformParams();
        Map<String, String> headers = getPlatformHeaders();

        String res = doGet(
                wml_credentials.get(Credentials.COS_ENDPOINT),
                getTargetUrl(fileName),
                params,
                headers);

        return res;
    }
}