package com.ibm.wmlconnector;

public class Credentials {
    public boolean USE_V4_FINAL = false;

    public String IAM_URL = "https://iam.cloud.ibm.com/identity/token";

    public String API_URL = null;               // required only for V4 final
    public String USER_APIKEY = null;           // required only for V4 final

    public String WML_NAME = null;
    public String WML_CRN = null;               // required only for V4 final
    public String WML_INSTANCE_ID = null;
    public String WML_APIKEY  = null;           // NOT required for V4 final
    public String WML_URL = null;

    public String WML_SPACE_NAME = null;        // required only for V4 final
    public String WML_SPACE_ID = null;          // required only for V4 final
    public String WML_VERSION = "2020-08-07";   // required only for V4 final


    public String COS_NAME = null;              // required only for V4 final
    public String COS_CRN = null;               // required only for V4 final
    public String COS_APIKEY  =  null;
    public String COS_ACCESS_KEY_ID = null;
    public String COS_SECRET_ACCESS_KEY = null;

    public String COS_ENDPOINT = null;
    public String COS_BUCKET = null;


    public String cplex_deployment_id = null;   // CPLEX empty model reusable deployment (for testing)
    public String cpo_deployment_id = null;     // CPO empty model reusable deployment (for testing)

    public Credentials() {}
}
