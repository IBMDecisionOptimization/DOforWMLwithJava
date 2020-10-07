package com.ibm.wmlconnector.impl;

import com.ibm.wmlconnector.Credentials;
import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import java.util.*;



public class WMLConnectorImpl extends ConnectorImpl implements WMLConnector {

    //public static String RESULTS_FORMAT = "XML";
    public static String RESULTS_FORMAT = "JSON";
    public static final Logger LOGGER = Logger.getLogger(WMLConnectorImpl.class.getName());



    protected String wml_url;
    protected String wml_instance_id;
    protected String api_url = null;

    public WMLConnectorImpl(Credentials credentials) {
        super(credentials, false);
        this.wml_url = credentials.WML_URL;
        this.api_url = credentials.API_URL;
        if (credentials.USE_V4_FINAL) {
            LOGGER.info("WMLConnector using V4 final APIs");
        } else {
            LOGGER.info("WMLConnector using V4 BETA APIs");
        }

        this.wml_instance_id = credentials.WML_INSTANCE_ID;
        lookupBearerToken();
    }


    class WMLJobImpl implements WMLJob {
        String deployment_id;
        String job_id;
        JSONObject status = null;
        WMLJobImpl(String deployment_id, String job_id) {
            this.deployment_id = deployment_id;
            this.job_id = job_id;
        }

        @Override
        public String getId() {
            return job_id;
        }

        @Override
        public void updateStatus() {
            try {

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "bearer " + bearerToken);
                headers.put("ML-Instance-ID", wml_instance_id);
                headers.put("cache-control", "no-cache");

                String url = "";
                if (credentials.USE_V4_FINAL)
                    url = wml_url + "/ml/v4/deployment_jobs/" + job_id + "?version="+credentials.WML_VERSION+"&space_id="+credentials.WML_SPACE_ID;
                else
                    url = wml_url + "/v4/jobs/" + job_id;

                String res = doGet(url, headers);

                status = new JSONObject(res);

            } catch (JSONException e) {
                LOGGER.severe("Error updateStatus: " + e);
            }

        }

        @Override
        public JSONObject getStatus() {
            return status;
        }

        @Override
        public String getState() {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("status").getString("state");
        }

        @Override
        public boolean hasSolveState() {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").has("solve_state");
        }

        @Override
        public String getSolveStateDetail(String key) {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").getJSONObject("details").getString(key);
        }

        @Override
        public boolean hasSolveStatus() {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").has("solve_status");
        }

        @Override
        public String getSolveStatus() {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").getString("solve_status");
        }

        @Override
        public boolean hasLatestEngineActivity() {
            return status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").has("latest_engine_activity");
        }

        @Override
        public String getLatestEngineActivity() {
            JSONArray lines = status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").getJSONArray("latest_engine_activity");
            String log = "";
            for (Iterator<Object> it = lines.iterator(); it.hasNext(); )
                log += (String)it.next() + "\n";
            return log;
        }

        @Override
        public HashMap<String, Object> getKPIs() {
            JSONObject details = status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONObject("solve_state").getJSONObject("details");
            Iterator<String> keys = details.keys();

            HashMap<String, Object> kpis = new LinkedHashMap<String, Object>();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.startsWith("KPI.")) {
                    String kpi = key.substring(4);
                    kpis.put(kpi, details.get(key));
                }
            }

            return kpis;
        }

        @Override
        public JSONArray extractOutputData() {
            try {
                JSONArray output_data = status.getJSONObject("entity").getJSONObject("decision_optimization").getJSONArray("output_data");
                return output_data;

            } catch (JSONException e) {
                LOGGER.severe("Error extractSolution: " + e);
            }
            return null;
        }

        @Override
        public void delete() {
            try {

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Accept", "application/json");
                headers.put("Authorization", "bearer " + bearerToken);
                headers.put("ML-Instance-ID", wml_instance_id);
                headers.put("cache-control", "no-cache");

                doDelete(wml_url + "/v4/jobs/" + job_id + "?hard_delete=true", headers);

            } catch (JSONException e) {
                LOGGER.severe("Error updateStatus: " + e);
            }
        }

        @Override
        public String getLog() {
            JSONArray output_data = extractOutputData();
            for (Iterator<Object> it = output_data.iterator(); it.hasNext(); ) {
                JSONObject o = (JSONObject)it.next();
                if (o.getString("id").equals("log.txt")) {
                    byte[] encoded = new byte[0];
                    try {
                        if (credentials.USE_V4_FINAL)
                            encoded = o.getString("content").getBytes("UTF-8");
                        else
                            encoded = o.getJSONArray("values").getJSONArray(0).getString(0).getBytes("UTF-8");
                        byte[] decoded = Base64.getDecoder().decode(encoded);
                        String log = new String(decoded, "UTF-8");
                        return log;

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        public String getSolution() {
            JSONArray output_data = extractOutputData();
            String solution = "";
            for (Iterator<Object> it = output_data.iterator(); it.hasNext(); ) {
                JSONObject o = (JSONObject)it.next();
                if (o.getString("id").equals("solution.json")) {
                    byte[] encoded = new byte[0];
                    try {
                        if (credentials.USE_V4_FINAL)
                            encoded = o.getString("content").getBytes("UTF-8");
                        else
                            encoded = o.getJSONArray("values").getJSONArray(0).getString(0).getBytes("UTF-8");
                        byte[] decoded = Base64.getDecoder().decode(encoded);
                        solution = new String(decoded, "UTF-8");
                        break;

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (o.getString("id").equals("solution.xml")) {
                    byte[] encoded = new byte[0];
                    try {
                        if (credentials.USE_V4_FINAL)
                            encoded = o.getString("content").getBytes("UTF-8");
                        else
                            encoded = o.getJSONArray("values").getJSONArray(0).getString(0).getBytes("UTF-8");
                        byte[] decoded = Base64.getDecoder().decode(encoded);
                        solution = new String(decoded, "UTF-8");
                        break;

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (o.getString("id").endsWith("csv")) {
                    solution += o.getString("id") + "\n";
                    JSONArray fields = o.getJSONArray("fields");
                    boolean first = true;
                    for (int f =0; f<fields.length(); f++) {
                        if (!first)
                            solution += ",";
                        solution += fields.getString(f);
                        first = false;
                    }
                    solution += "\n";
                    JSONArray values = o.getJSONArray("values");
                    for (int r = 0; r<values.length(); r++) {
                        JSONArray row = values.getJSONArray(r);
                        first = true;
                        for (int f =0; f<row.length(); f++) {
                            if (!first)
                                solution += ",";
                            solution += row.get(f);
                            first = false;
                        }
                        solution += "\n";
                    }
                }
            }
            return solution;
        }
    }


    @Override
    public JSONArray createDataFromJSONPayload(String fileName) {
        String file = getFileContent(fileName);

        JSONObject payload = new JSONObject(file);
        return payload.getJSONObject("decision_optimization").getJSONArray("input_data");
    }

    @Override
    public JSONObject createDataFromCSV(String id, String fileName) {

        JSONObject data = new JSONObject();
        data.put("id", id);

        JSONArray fields = new JSONArray();
        JSONArray all_values = new JSONArray();
        String file = getFileContent(fileName);
        String[] lines = file.split("\n");
        int nlines = lines.length;
        String[] fields_array = lines[0].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        int nfields = fields_array.length;
        for (int i=0; i<nfields; i++) {
            String field = fields_array[i];
            if (field.charAt(0) == '"')
                field = field.substring(1);
            if  (field.charAt(field.length()-1) == '"')
                field = field.substring(0, field.length()-1);
            fields.put(field);
        }
        data.put("fields", fields);

        for (int i = 1; i<nlines; i++) {
            JSONArray values = new JSONArray();
            String[] values_array = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (int j=0; j<nfields; j++) {
                String value = values_array[j];
                if (value.charAt(0) == '"')
                    value = value.substring(1);
                if (value.charAt(value.length() - 1) == '"')
                    value = value.substring(0, value.length() - 1);

                try {
                    int ivalue = Integer.parseInt(value);
                    values.put(ivalue);
                } catch (NumberFormatException e) {
                    try {
                        double dvalue = Double.parseDouble(value);
                        values.put(dvalue);
                    } catch (NumberFormatException e2) {
                        values.put(value);
                    }
                }
            }
            all_values.put(values);
        }
        data.put("values", all_values);
        return data;
    }

    @Override
    public JSONObject createDataFromString(String id, String text) {
        byte[] bytes = text.getBytes();
        byte[] encoded = Base64.getEncoder().encode(bytes);

        JSONObject data = new JSONObject();
        data.put("id", id);

        if (credentials.USE_V4_FINAL)
            data.put("content", new String(encoded));
        else {
            JSONArray fields = new JSONArray();
            fields.put("___TEXT___");
            data.put("fields", fields);

            JSONArray values = new JSONArray();
            values.put(new JSONArray().put(new String(encoded)));
            data.put("values", values);
        }

        return data;
    }

    @Override
    public JSONObject createDataFromFile(String id, String fileName) {

        byte[] bytes = getFileContentAsBytes(fileName);
        byte[] encoded = Base64.getEncoder().encode(bytes);

        JSONObject data = new JSONObject();
        data.put("id", id);

        if (credentials.USE_V4_FINAL)
            data.put("content", new String(encoded));
        else {
            JSONArray fields = new JSONArray();
            fields.put("___TEXT___");
            data.put("fields", fields);

            JSONArray values = new JSONArray();
            values.put(new JSONArray().put(new String(encoded)));
            data.put("values", values);
        }

        return data;
    }


    @Override
    public WMLJob createJob(String deployment_id,
                            JSONArray input_data,
                            JSONArray input_data_references,
                            JSONArray output_data,
                            JSONArray output_data_references) {
        LOGGER.fine("Create job");

        try {
            JSONObject payload = new JSONObject();

            if (credentials.USE_V4_FINAL) {
                payload.put("name", "Job_for_"+deployment_id);
                payload.put("space_id", credentials.WML_SPACE_ID);
            }

            JSONObject deployment = new JSONObject();
            if (credentials.USE_V4_FINAL)
                deployment.put("id",deployment_id);
            else
                deployment.put("href","/v4/deployments/"+deployment_id);
            payload.put("deployment", deployment);

            JSONObject decision_optimization = new JSONObject();
            JSONObject solve_parameters = new JSONObject();
            solve_parameters.put("oaas.logAttachmentName", "log.txt");
            solve_parameters.put("oaas.logTailEnabled", "true");
            solve_parameters.put("oaas.includeInputData", "false");
            solve_parameters.put("oaas.resultsFormat", RESULTS_FORMAT);
            decision_optimization.put("solve_parameters", solve_parameters);

            if (input_data != null)
                decision_optimization.put("input_data", input_data);

            if (input_data_references != null)
                decision_optimization.put("input_data_references", input_data_references);


            if (output_data != null)
                decision_optimization.put("output_data", output_data);

            if ((output_data == null) && (output_data_references == null)) {
                output_data = new JSONArray();
                JSONObject outcsv = new JSONObject();
                outcsv.put("id", ".*\\.csv");
                output_data.put(outcsv);
                JSONObject outtxt = new JSONObject();
                outtxt.put("id", ".*\\.txt");
                output_data.put(outtxt);
                JSONObject outjson = new JSONObject();
                outjson.put("id", ".*\\.json");
                output_data.put(outjson);
                JSONObject outxml = new JSONObject();
                outxml.put("id", ".*\\.xml");
                output_data.put(outxml);
                decision_optimization.put("output_data", output_data);
            }

            if (output_data_references != null)
                decision_optimization.put("output_data_references", output_data_references);


            payload.put("decision_optimization", decision_optimization);

            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Accept", "application/json");
            headers.put("Authorization", "bearer " + bearerToken);
            headers.put("ML-Instance-ID", wml_instance_id);
            headers.put("cache-control", "no-cache");
            headers.put("Content-Type", "application/json");

            String url = null;
            if (credentials.USE_V4_FINAL)
                url = wml_url + "/ml/v4/deployment_jobs?version="+credentials.WML_VERSION+"&space_id="+credentials.WML_SPACE_ID;
            else
                url = wml_url + "/v4/jobs";
            String res = doPost(url, headers, payload.toString());

            JSONObject json = new JSONObject(res);

            String job_id = null;
            if (credentials.USE_V4_FINAL)
                job_id = (String)((JSONObject)json.get("metadata")).get("id");
            else
                job_id = (String)((JSONObject)json.get("metadata")).get("guid");

            LOGGER.fine("job_id = "+ job_id);

            return new WMLJobImpl(deployment_id, job_id);


        } catch (JSONException e) {
            LOGGER.severe("Error CreateJob: " + e);
        }

        return null;
    }

    @Override
    public WMLJob createAndRunJob(String deployment_id,
                                  JSONArray input_data,
                                  JSONArray input_data_references,
                                  JSONArray output_data,
                                  JSONArray output_data_references) {

        WMLJob job  = createJob(deployment_id, input_data, input_data_references, output_data, output_data_references);

        LOGGER.fine("job_id = " + job.getId());

        String state = null;
        do {
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            job.updateStatus();

            try {
                state = job.getState();
                if (job.hasSolveState()) {

                    if (job.hasSolveStatus())
                        LOGGER.fine("Solve Status : " + job.getSolveStatus());
                    if (job.hasLatestEngineActivity())
                        LOGGER.finer("Latest Engine Activity : " + job.getLatestEngineActivity());

                    HashMap<String, Object> kpis = job.getKPIs();

                    Iterator<String> keys = kpis.keySet().iterator();

                    while (keys.hasNext()) {
                        String kpi = keys.next();
                        LOGGER.finer("KPI: " + kpi + " = " + kpis.get(kpi));
                    }
                }
            } catch (JSONException e) {
                LOGGER.severe("Error extractState: " + e);
            }

            LOGGER.fine("Job State: " + state);
        } while (!state.equals("completed") && !state.equals("failed"));

        if (state.equals("failed")) {
            LOGGER.severe("Job failed.");
            LOGGER.severe("Job status:" + job.getStatus());
        } else {
            output_data = job.extractOutputData();
            LOGGER.fine("output_data = " + output_data);
        }

        return job;
    }

    @Override
    public String createNewModel(String modelName, ModelType type, String modelAssetFilePath) {
        return this.createNewModel(modelName, type, modelAssetFilePath, WMLConnector.Runtime.DO_12_10);
    }

    @Override
    public String createNewModel(String modelName, ModelType type, String modelAssetFilePath, Runtime runtime) {

        String iamToken = getBearerToken();
        String modelId = null;
        {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "bearer " + bearerToken);
            headers.put("ML-Instance-ID", wml_instance_id);
            headers.put("cache-control", "no-cache");
            headers.put("Content-Type", "application/json");

            JSONObject payload = null;
            String url = null;
            if (credentials.USE_V4_FINAL) {
                payload = new JSONObject().put("name", modelName)
                        .put("description", modelName)
                        .put("type", type.toString())
                        .put("software_spec", new JSONObject().put("name", runtime.getShortName()))
                        .put("space_id", credentials.WML_SPACE_ID);
                url = wml_url + "/ml/v4/models?version=" + credentials.WML_VERSION;
            } else {
                payload = new JSONObject("{\"name\":\"" + modelName + "\", \"description\":\"" + modelName + "\", \"type\":\"" + type + "\",\"runtime\": {\"href\":\"" + runtime + "\"}}");
                url = wml_url + "/v4/models";
            }

            String res = doPost(url, headers, payload.toString());

            JSONObject json = new JSONObject(res);
            if (credentials.USE_V4_FINAL)
                modelId = json.getJSONObject("metadata").getString("id");
            else
                modelId = json.getJSONObject("metadata").getString("guid");

        }

        {
            HashMap<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "bearer " + bearerToken);
            headers.put("ML-Instance-ID", wml_instance_id);
            headers.put("cache-control", "no-cache");


            if (modelAssetFilePath != null) {
                byte[] bytes = getBinaryFileContent(modelAssetFilePath);

                String url = null;
                if (credentials.USE_V4_FINAL) {
                    url = wml_url + "/ml/v4/models/" + modelId + "/content?version="+credentials.WML_VERSION + "&content_format=native&space_id=" + credentials.WML_SPACE_ID;
                } else {
                    url = wml_url + "/v4/models/" + modelId + "/content";
                }

                doPut(url, headers, bytes);
            }
        }

        return modelId;
    }

    @Override
    public String getModelHref(String modelId, boolean displayModel)  {

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("ML-Instance-ID", wml_instance_id);
        headers.put("cache-control", "no-cache");
        headers.put("Content-Type", "application/json");

        String res = doGet(wml_url + "/v4/models/"+ modelId, headers);

        JSONObject json = new JSONObject(res);
        String modelHref = json.getJSONObject("metadata").getString("href");

        return modelHref;
    }

    @Override
    public String deployModel(String deployName, String model_id, TShirtSize size, int nodes) {

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("ML-Instance-ID", wml_instance_id);
        headers.put("cache-control", "no-cache");
        headers.put("Content-Type", "application/json");

        JSONObject payload = null;
        String url = null;
        if (credentials.USE_V4_FINAL) {
             payload = new JSONObject()
                    .put("name", deployName)
                    .put("space_id", credentials.WML_SPACE_ID)
                    .put("asset", new JSONObject().put("id", model_id))
                    .put("hardware_spec", new JSONObject().put("name", size.toString()).put("num_nodes", nodes))
                    .put("batch", new JSONObject());
             url = wml_url + "/ml/v4/deployments?version="+credentials.WML_VERSION;
        } else {
            String modelHref = getModelHref(model_id, false);
            payload = new JSONObject("{\"name\":\"" + deployName + "\", \"asset\": { \"href\": \"" + modelHref + "\"  }, \"batch\": {}, \"compute\" : { \"name\" : \"" + size + "\", \"nodes\" : " + nodes + " }}");
            url = wml_url + "/v4/deployments";
        }

        String res = doPost(url, headers, payload.toString());

        JSONObject json = new JSONObject(res);
        String deployment_id = null;
        if (credentials.USE_V4_FINAL)
            deployment_id = json.getJSONObject("metadata").getString("id");
        else
            deployment_id = json.getJSONObject("metadata").getString("guid");

        /*
        res = doGet(wml_url + "/v4_private/"+deployment_id+"/payload_logging_configuration", headers);
        JSONObject jsonres = new JSONObject(res);
        LOGGER.severe(jsonres.toString());
        */

        return deployment_id;
    }

    public void deleteDeployment(String deployment_id) {

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("ML-Instance-ID", wml_instance_id);
        headers.put("cache-control", "no-cache");
        headers.put("Content-Type", "application/json");

        String url = null;
        if (credentials.USE_V4_FINAL)
            url = wml_url + "/ml/v4/deployments/" + deployment_id + "?version="+credentials.WML_VERSION+"&space_id="+credentials.WML_SPACE_ID;
        else
            url = wml_url + "/v4/deployments/" + deployment_id;

        String res = doDelete(url, headers);

    }

    @Override
    public JSONObject getInstances() {
        HashMap<String, String> headers = new HashMap<String, String>();
        //headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        //headers.put("cache-control", "no-cache");

        String url = wml_url + "/ml/v4/instances?version=" +credentials. WML_VERSION;
        LOGGER.info("Url: " + url);

        String res = doGet(url, headers);

        return new JSONObject(res);
    }

    @Override
    public JSONObject getSoftwareSpecifications() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        headers.put("cache-control", "no-cache");

        String res = doGet(api_url + "/v2/software_specifications", headers);

        return new JSONObject(res);
    }

    @Override
    public String createDeploymentSpace(String name) {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("ML-Instance-ID", wml_instance_id);
        headers.put("cache-control", "no-cache");
        headers.put("Content-Type", "application/json");


        /*
        curl --location --request POST 'https://api.dataplatform.dev.cloud.ibm.com/v2/spaces' \
            --header 'Authorization: Bearer XXX' \
            --header 'ML-Instance-ID: XXXX' \
            --header 'Content-Type: application/json' \
            --data-raw '{
                "name": "name",
                "description": "string",
                "storage": {
                    "resource_crn": "crn_from_cos"
                },
                "compute": [{
                    "name": "name_of_machine_learning_instance",
                            "crn": "crn_from_machine_learning_instance"
                }]
            }'
         */

        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("description", name);
        payload.put("storage", new JSONObject().put("resource_crn", credentials.COS_CRN));
        payload.put("compute", new JSONArray().put(new JSONObject().put("name", credentials.WML_NAME).put("crn", credentials.WML_CRN)));

        String res = doPost(api_url + "/v2/spaces", headers, payload.toString());
        JSONObject json = new JSONObject(res);
        return json.getJSONObject("resources").getString("ig");
    }

    public JSONObject getDeploymentSpaces() {
        HashMap<String, String> headers = new HashMap<String, String>();
        //headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        //headers.put("ML-Instance-ID", instance_id);
        //headers.put("cache-control", "no-cache");


        String res = doGet(api_url + "/v2/spaces", headers);

        return new JSONObject(res);
    }

    @Override
    public JSONObject getDeployments() {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "bearer " + bearerToken);
        headers.put("ML-Instance-ID", wml_instance_id);
        headers.put("cache-control", "no-cache");

        String url = "";
        if (credentials.USE_V4_FINAL)
            url = wml_url + "/ml/v4/deployments?space_id="+credentials.WML_SPACE_ID+"&version=" +credentials. WML_VERSION;
        else
            url = wml_url + "/v4/deployments";

        String res = doGet(url, headers);

        return new JSONObject(res);
    }

    @Override
    public String getDeploymentIdByName(String deployment_name) {

        JSONObject json = getDeployments();
        JSONArray resources = json.getJSONArray("resources");
        int len = resources.length();
        for (int i=0; i<len; i++) {
            JSONObject metadata = resources.getJSONObject(i).getJSONObject("metadata");
            if (metadata.getString("name").equals(deployment_name)) {
                if (credentials.USE_V4_FINAL)
                    return metadata.getString("id");
                else
                    return metadata.getString("guid");
            }
        }
        return null;
    }

    @Override
    public String getDeploymentSpaceIdByName(String spaceName) {
        JSONObject json = getDeploymentSpaces();
        JSONArray resources = json.getJSONArray("resources");
        int len = resources.length();
        for (int i=0; i<len; i++) {
            JSONObject entity = resources.getJSONObject(i).getJSONObject("entity");
            JSONObject metadata = resources.getJSONObject(i).getJSONObject("metadata");
            if (entity.getString("name").equals(spaceName))
                return metadata.getString("id");
        }
        return null;
    }

    public String getCatalogIdBySpaceId(String space_id) {
        HashMap<String, String> headers = new HashMap<String, String>();
        //headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        //headers.put("ML-Instance-ID", instance_id);
        //headers.put("cache-control", "no-cache");


        String res = doGet(api_url + "/v2/catalogs/?space_id="+space_id, headers);
        JSONArray catalogs = (new JSONObject(res)).getJSONArray("catalogs");
        for (int i=0; i <catalogs.length(); i++) {
            JSONObject catalog = catalogs.getJSONObject(i);
            if (catalog.getJSONObject("entity").has("space_id") &&
                    catalog.getJSONObject("entity").getString("space_id").equals(space_id))
                return catalog.getJSONObject("metadata").getString("guid");
        }
        return null;
    }

    public JSONObject getStorageBySpaceId(String space_id) {
        HashMap<String, String> headers = new HashMap<String, String>();
        //headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        //headers.put("ML-Instance-ID", instance_id);
        //headers.put("cache-control", "no-cache");


        String res = doGet(api_url + "/v2/spaces/?space_id="+space_id, headers);
        JSONArray spaces = (new JSONObject(res)).getJSONArray("resources");
        for (int i=0; i <spaces.length(); i++) {
            JSONObject space = spaces.getJSONObject(i);
            if (space.getJSONObject("metadata").getString("id").equals(space_id))
                return space.getJSONObject("entity").getJSONObject("storage");
        }
        return null;
    }

    @Override
    public JSONObject getAssetFiles(String space_id) {
        HashMap<String, String> headers = new HashMap<String, String>();
        //headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer " + bearerToken);
        //headers.put("ML-Instance-ID", instance_id);
        //headers.put("cache-control", "no-cache");


        //String catalog_id = getCatalogIdBySpaceId(space_id);
        JSONObject storage = getStorageBySpaceId(space_id);

        String cos_url = storage.getJSONObject("properties").getString("endpoint_url");
        String cos_bucket = storage.getJSONObject("properties").getString("bucket_name");

        String res = doGet(cos_url + "/" + cos_bucket, headers);

        JSONObject files = new JSONObject();
        try {
            // pass the file name.. all relative entity
            // references will be resolved against this
            // as base URI.
            XMLStreamReader xmlr = XMLInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(res.getBytes()));

            String element;
            String text;
            String[] attrs;
            boolean isKey = false;
            while(xmlr.hasNext()) {
                switch (xmlr.next()) {
                    case XMLStreamConstants.START_DOCUMENT: /* nothing */
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        element = xmlr.getLocalName();
                        if (element.equals("Key"))
                            isKey = true;
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        element = xmlr.getLocalName();
                        if (element.equals("Key"))
                            isKey = false;
                        break;
                    case XMLStreamConstants.END_DOCUMENT: /* nothing */
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        if (isKey) {
                            int start = xmlr.getTextStart();
                            int length = xmlr.getTextLength();
                            String key = new String(xmlr.getTextCharacters(),
                                    start,
                                    length);

                            //String asset_id= key.split("/")[1];
                            //String name = key.split("/")[2];

                            if (key.endsWith(".csv")) {
                                String res2 = doGet(cos_url + "/" + cos_bucket + "/" + key, headers);
                                files.put(key, res2);
                            }
                        }
                        break;
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        //String res = doGet(api_url + "/v2/spaces/?space_id="+space_id, headers);

        //String res = doGet(api_url + "/v2/asset_imports/?space_id="+space_id, headers);
        //String res = doGet(api_url + "/v2/data_assets/?space_id="+space_id, headers);
        //String res = doGet( api_url + "/v2/catalogs/"+catalog_id, headers);
        //String res = doGet(api_url + "/v2/asset_files?catalog_id=3992bcee-362e-4806-964d-3527ad5218f2", headers);

        return files;
    }
}
