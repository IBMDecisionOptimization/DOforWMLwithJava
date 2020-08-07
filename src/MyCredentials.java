import com.ibm.wmlconnector.Credentials;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;

public class MyCredentials extends Credentials {


    public MyCredentials() {
        super();

        WMLConnectorImpl.USE_V4_FINAL = false;

        // PROD
        IAM_URL = "https://iam.cloud.ibm.com/identity/token";

        WML_URL = "https://us-south.ml.cloud.ibm.com";
        WML_APIKEY  = "xxxxxxxxxxxxxxxxxxxxxxxxxxx";
        WML_INSTANCE_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";


        // COS PUBLIC
        COS_APIKEY  = "xxxxxxxxxxxxxxxxxxxxxxxxx";
        COS_ACCESS_KEY_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxx";
        COS_SECRET_ACCESS_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        COS_ENDPOINT = "https://s3.eu-gb.cloud-object-storage.appdomain.cloud";
        COS_BUCKET = "test-lp";

    }
}
