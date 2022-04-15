package com.ibm.ml.ilog;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ilog.concert.IloException;

import java.util.HashMap;
import java.util.LinkedHashMap;

/*
Method to handle the credentials to plya with WML, the platform...
You simply use the put method to add a credentials field.
Fields should come from the Credentials class.
You should create this class from Credentials.getCredentials and not by yourself.
 */
public class Credentials {
    public boolean isCPD;

    public Credentials(boolean cpdOrPublic) {
        isCPD = cpdOrPublic;
    }

    private final HashMap<String, String> credentials = new LinkedHashMap<>();

    public Credentials() {
    }

    /*
    Adds or replace the credentials value.
     */
    public void put(String key, String value) {
        if (credentials.containsKey(key))
            credentials.replace(key, value);
        else
            credentials.put(key, value);
    }

    public String get(String key) {
        return credentials.get(key);
    }

    public boolean containsKey(String key) {
        return credentials.containsKey(key);
    }

    public static final String IAM_URL = "service.iam.url";
    public static final String IAM_HOST = "service.iam.host";
    public static final String WML_HOST = "service.wml.host";
    public static final String WML_API_KEY = "service.wml.api_key";
    public static final String WML_SPACE_ID = "service.wml.space_id";
    public static final String WML_VERSION = "service.wml.version";
    public static final String CPD_USERNAME = "service.cpd.username";
    public static final String CPD_PASSWORD = "service.cpd.password";
    public static final String CPD_URL = "service.cpd.url";
    public static final String PLATFORM_HOST = "service.platform.host";

    public static final String COS_ENDPOINT = "service.cos.endpoint";
    public static final String COS_BUCKET = "service.cos.bucket";
    public static final String COS_ACCESS_KEY_ID = "service.cos.access_key_id";
    public static final String COS_SECRET_ACCESS_KEY = "service.cos.secret_access_key";
    public static final String COS_ORIGIN_COUNTRY = "service.cos.origin_country";

    public static final String[] COSFields = new String[]{
            COS_ACCESS_KEY_ID,
            COS_BUCKET,
            COS_ENDPOINT,
            COS_SECRET_ACCESS_KEY,
            COS_ORIGIN_COUNTRY
    };

    public static final String[] CPDFields = new String[]{
            CPD_USERNAME,
            CPD_PASSWORD,
            WML_VERSION,
            WML_SPACE_ID,
            WML_HOST,
            CPD_URL
    };
    public static final String[] publicFields = new String[]{
            IAM_URL,
            IAM_HOST,
            WML_VERSION,
            WML_SPACE_ID,
            WML_HOST,
            WML_API_KEY,
            PLATFORM_HOST
    };

    private static boolean isCPDConfig(Config config) {
        for (String key : CPDFields) {
            if (!config.hasPath(key))
                return false;
        }
        return true;
    }

    private static boolean isPublicConfig(Config config) {
        for (String key : publicFields) {
            if (!config.hasPath(key))
                return false;
        }
        return true;
    }

    private static boolean hasCOSConfig(Config config) {
        for (String key : COSFields) {
            if (!config.hasPath(key))
                return false;
        }
        return true;
    }

    /*
    Builds a Credentials object from a config file name
    */
    public static Credentials getCredentials(String configFileName) throws IloException {
        Config config = ConfigFactory.parseResources(configFileName).resolve();
        return getCredentials(config);
    }

    /*
    Builds a credentials object from a config.
     */
    public static Credentials getCredentials(Config config) throws IloException {
        Credentials creds = null;
        if (isCPDConfig(config)) {
            creds = getCPDCredentials(config);
        }
        if (isPublicConfig(config))
            creds = getPublicCredentials(config);

        if (creds == null) throw new IloException("unknown config type.");
        if (hasCOSConfig(config)) {
            for (String key : COSFields) {
                creds.put(key, config.getString(key));
            }
        }
        return creds;
    }

    private static Credentials getCredentials(Config config, boolean isCPD, String[] keys) throws IloException {
        Credentials ret = new Credentials(isCPD);
        for (String val : keys) {
            if (config.hasPath(val))
                ret.put(val, config.getString(val));
            else
                throw new IloException("Missing key " + val + " in config " + config.toString());
        }
        return ret;
    }

    private static Credentials getCPDCredentials(Config config) throws IloException {
        Credentials ret = getCredentials(config, true, CPDFields);
        ret.put(PLATFORM_HOST, config.getString(WML_HOST));
        return ret;
    }

    private static Credentials getPublicCredentials(Config config) throws IloException {
        return getCredentials(config, false, publicFields);
    }
}
