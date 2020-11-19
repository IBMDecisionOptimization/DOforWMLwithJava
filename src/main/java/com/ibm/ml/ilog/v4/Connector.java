package com.ibm.ml.ilog.v4;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ml.ilog.utils.HttpUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Job;
import ilog.concert.IloException;

public class Connector extends HttpUtils implements com.ibm.ml.ilog.Connector {
    private static final Logger logger = LogManager.getLogger();
    private final com.ibm.ml.ilog.Connector.Runtime wml_runtime;
    private final com.ibm.ml.ilog.Connector.TShirtSize wml_size;
    private final int wml_nodes;
    private final String resultFormat;

    private final String DECISION_OPTIMIZATION = "decision_optimization";
    private final String SOLVE_STATE = "solve_state";
    private final String FAILURE = "failure";

    private final String SOLVE_STATUS = "solve_status";
    private final String STATE = "state";
    private final String STATUS = "status";
    private final String ENTITY = "entity";
    private final String DETAILS = "details";
    private final String LATEST_ENGINE_ACTIVITY = "latest_engine_activity";
    private final String OUTPUT_DATA = "output_data";
    private final String CONTENT = "content";
    private final String ID = "id";
    private final String METADATA = "metadata";


    private final String RESOURCES = "resources";
    private final String NAME = "name";
    private final String LOGS = "log.txt";

    private final String MLV4 = "/ml/v4";
    private final String MLV4_DEPLOYMENT_JOBS = MLV4 + "/deployment_jobs";
    private final String MLV4_MODELS = MLV4 + "/models";
    private final String MLV4_DEPLOYMENTS = MLV4 + "/deployments";
    private final String MLV4_INSTANCES = MLV4 + "/instances";

    private final String V2 = "/v2";
    private final String V2_SOFTWARESPECS = V2 + "/software_specifications";
    private final String V2_SPACES = V2 + "/spaces";
    private final String V2_CATALOG = V2 + "/catalogs";


    private final String COMPLETED = "completed";
    private final String FAILED = "failed";
    private final String[] status = {COMPLETED, "failed", "canceled", "deleted"};
    private final Set<String> completedStatus = new java.util.HashSet<>(Arrays.asList(status));

    private final int statusRefreshRate = config.getInt("wmlconnector.v4.status_rate");
    private final boolean showEngineProgress = config.getBoolean("wmlconnector.v4.engine_progress");

    private final String engineLogLevel = config.getString("wmlconnector.v4.engine_log_level");

    private String exportPath = null;


    public Connector(Credentials creds) throws IloException {
        this(creds, com.ibm.ml.ilog.Connector.Runtime.DO_12_10, com.ibm.ml.ilog.Connector.TShirtSize.M, 1, null);
    }

    public Connector(Credentials creds, com.ibm.ml.ilog.Connector.Runtime runtime, com.ibm.ml.ilog.Connector.TShirtSize size, int nodes) throws IloException {
        this(creds, runtime, size, nodes, null);
    }


    public Connector(Credentials creds, com.ibm.ml.ilog.Connector.Runtime runtime, com.ibm.ml.ilog.Connector.TShirtSize size, int nodes, String format) throws IloException {
        super(creds);
        if (nodes == 0) {
            logger.error("Cannot set a 0 node number in WML");
            throw new IloException("Cannot set a 0 node number in WML");
        }
        wml_runtime = runtime;
        wml_size = size;
        wml_nodes = nodes;
        logger.info("WMLConnector using V4 final APIs with runtime: " + wml_runtime + ", size: " + wml_size + ", nodes: " + wml_nodes);
        resultFormat = format;
        logger.info("Using " + sslContextName + " SSL Context with WML.");
        logger.info("Using " + (tokenRefreshRate / 1000 / 60) + " minutes as token refresh rate");
        logger.info("Using " + statusRefreshRate + " msec as status refresh rate");
        if (config.hasPath("wmlconnector.v4.export_path")) {
            String path = config.getString("wmlconnector.v4.export_path");
            File f = new File(path);
            if (f.exists())
                exportPath = path;
            else
                logger.error("Path " + path + "does not exist. Ignoring debug export action.");
        } else
            logger.info("No export path defined.");
    }


    class JobImpl implements Job {
        String deployment_id;
        String job_id;
        JSONObject status = null;

        JobImpl(String deployment_id, String job_id) {
            this.deployment_id = deployment_id;
            this.job_id = job_id;
        }

        @Override
        public String getId() {
            return job_id;
        }

        @Override
        public void updateStatus() throws IloException {
            Map<String, String> headers = getWMLHeaders();
            headers.put(ACCEPT, APPLICATION_JSON);

            Map<String, String> params = getWMLParams();
            params.put("include", "output_data,status,solve_state");
            String res = doGet(
                    wml_credentials.get(Credentials.WML_HOST),
                    MLV4_DEPLOYMENT_JOBS + "/" + job_id,
                    params, headers);

            status = parseJson(res);
        }

        @Override
        public JSONObject getStatus() {
            return status;
        }

        private JSONObject getDO() {
            return (JSONObject) ((JSONObject) status.get(ENTITY)).get(DECISION_OPTIMIZATION);
        }

        private JSONObject getSolveState() {
            return (JSONObject) getDO().get(SOLVE_STATE);
        }

        @Override
        public String getFailure() {
            if (hasFailure()) {
                return ((JSONObject) getDO().get(STATUS)).get(FAILURE).toString();
            } else return "Missing failure in WML answer.";
        }

        @Override
        public JSONObject getJobStatus() {
            return (JSONObject) getDO().get(STATUS);
        }

        @Override
        public String getState() {
            return (String) ((JSONObject) getDO().get(STATUS)).get(STATE);
        }

        @Override
        public boolean hasSolveState() {
            return getDO().containsKey(SOLVE_STATE);
        }

        @Override
        public boolean hasFailure() {
            return ((JSONObject) getDO().get(STATUS)).containsKey(FAILURE);
        }

        @Override
        public boolean hasSolveStatus() {
            return getSolveState().containsKey(SOLVE_STATUS);
        }

        @Override
        public String getSolveStatus() {
            return (String) getSolveState().get(SOLVE_STATUS);
        }

        @Override
        public boolean hasLatestEngineActivity() {
            return getSolveState().containsKey(LATEST_ENGINE_ACTIVITY);
        }

        @Override
        public String getLatestEngineActivity() {
            JSONArray lines = (JSONArray) getSolveState().get(LATEST_ENGINE_ACTIVITY);
            StringBuilder log = new StringBuilder();
            for (Iterator it = lines.iterator(); it.hasNext(); )
                log.append(it.next()).append("\n");
            return log.toString();
        }

        @Override
        public HashMap<String, Object> getKPIs() {
            JSONObject details = (JSONObject) getSolveState().get(DETAILS);
            Iterator keys = details.keySet().iterator();

            HashMap<String, Object> kpis = new LinkedHashMap<>();
            while (keys.hasNext()) {
                String key = (String)keys.next();
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
                return (JSONArray) getDO().get(OUTPUT_DATA);
            } catch (Exception e) {
                logger.error("Error extractOutputData: " + e);
            }
            return null;
        }

        @Override
        public String getLog() {
            JSONArray output_data = extractOutputData();
            for (Iterator it = output_data.iterator(); it.hasNext(); ) {
                JSONObject o = (JSONObject) it.next();
                if ((o.get(ID)).equals(LOGS)) {
                    byte[] encoded = ((String) o.get(CONTENT)).getBytes(StandardCharsets.UTF_8);
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    String log = new String(decoded, StandardCharsets.UTF_8);
                    return log;

                }
            }
            return "Empty logs.";
        }

        @Override
        public String getSolution() {
            JSONArray output_data = extractOutputData();
            StringBuilder solution = null;
            for (Iterator it = output_data.iterator(); it.hasNext(); ) {
                JSONObject o = (JSONObject) it.next();
                String id = (String) o.get(ID);
                if (id.equals("solution.json") || id.equals("solution.xml")) {
                    byte[] encoded = ((String) o.get(CONTENT)).getBytes(StandardCharsets.UTF_8);
                    byte[] decoded = Base64.getDecoder().decode(encoded);
                    solution = new StringBuilder(new String(decoded, StandardCharsets.UTF_8));
                    break;
                } else if (id.endsWith("csv")) {
                    if (solution == null) {
                        solution = new StringBuilder();
                    }
                    solution.append(id).append("\n");
                    JSONArray fields = (JSONArray) o.get("fields");
                    boolean first = true;
                    for (int f = 0; f < fields.size(); f++) {
                        if (!first)
                            solution.append(",");
                        solution.append(fields.get(f));
                        first = false;
                    }
                    solution.append("\n");
                    JSONArray values = (JSONArray) o.get("values");
                    for (int r = 0; r < values.size(); r++) {
                        JSONArray row = (JSONArray) values.get(r);
                        first = true;
                        for (int f = 0; f < row.size(); f++) {
                            if (!first)
                                solution.append(",");
                            solution.append(row.get(f));
                            first = false;
                        }
                        solution.append("\n");
                    }
                }
            }
            return (solution == null ? null : solution.toString());
        }
    }

    @Override
    public JSONObject createDataFromString(String id, String text) {
        byte[] bytes = text.getBytes();
        byte[] encoded = Base64.getEncoder().encode(bytes);

        JSONObject data = new JSONObject();
        data.put(ID, id);

        data.put(CONTENT, new String(encoded));

        return data;
    }

    @Override
    public JSONObject createDataFromFile(String id, String fileName) {
        byte[] bytes = getFileContent(fileName);
        return createDataFromBytes(id, bytes);
    }

    public JSONObject createDataFromBytes(String id, byte[] bytes) {
        byte[] encoded = Base64.getEncoder().encode(bytes);

        JSONObject data = new JSONObject();
        data.put(ID, id);

        data.put(CONTENT, new String(encoded));

        return data;
    }

    private void copyFile(File src, File dest) {
        try (InputStream inputStream = new FileInputStream(src); OutputStream outputStream = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPath(String export, String date, String name) {
        return export + "/" + date + "/" + name;
    }

    private void dump2Disk(JSONArray input_data, String date) {
        try {
            for (int i = 0; i < input_data.size(); i++) {
                JSONObject obj = (JSONObject) input_data.get(i);
                if (obj.containsKey(ID) && obj.containsKey(CONTENT)) {
                    String id = (String) obj.get(ID);
                    String path = getPath(exportPath, date, id);
                    logger.info("Exporting " + id + " in " + path);
                    byte[] content = Base64.getDecoder().decode(
                            ((String) obj.get(CONTENT)).getBytes(StandardCharsets.UTF_8)
                    );
                    OutputStream os = new FileOutputStream(path);
                    os.write(content);
                    os.close();
                } else
                    logger.warn("Missing " + ID + " and/or " + CONTENT);
            }
        } catch (IOException e) {
            logger.warn("Ignoring IO exception when dumping info for debug.");
        }
    }

    private String getTimeStamp() {
        return (new Date()).toString().replace(" ", "-").replace(":", "-");
    }

    public byte[] buildPayload(String deployment_id, String sav, String savFileName, JSONArray input_data, Map<String, String> overriden_solve_parameters) {
        String date = getTimeStamp();
        if (exportPath != null) {
            logger.info("Exporting the WML DO input data in " + date);
            File f = new File(exportPath + "/" + date);
            f.mkdirs();
            dump2Disk(input_data, date);
            String path = getPath(exportPath, date, sav);
            logger.info("Exporting " + savFileName + " in " + path);
            copyFile(new File(savFileName), new File(path));
        }

        JSONObject payload = new JSONObject();
        payload.put(NAME, "Job_for_" + deployment_id);
        payload.put(SPACE_ID, wml_credentials.get(Credentials.WML_SPACE_ID));

        JSONObject deployment = new JSONObject();
        deployment.put(ID, deployment_id);
        payload.put("deployment", deployment);

        JSONObject json_do = new JSONObject();
        JSONObject solve_parameters = new JSONObject();
        if (showEngineProgress)
            solve_parameters.put("oaas.logTailEnabled", "true");
        else
            solve_parameters.put("oaas.logTailEnabled", "false");
        solve_parameters.put("oaas.includeInputData", "false");
        solve_parameters.put("oaas.resultsFormat", resultFormat);
        solve_parameters.put("oaas.engineLogLevel", engineLogLevel);
        // Override default solve_parameters
        if (overriden_solve_parameters != null) {
            for (Entry<String, String> param : overriden_solve_parameters.entrySet()) {
                solve_parameters.put(param.getKey(), param.getValue());
            }
        }

        json_do.put("solve_parameters", solve_parameters);
        payload.put(DECISION_OPTIMIZATION, json_do);

        JSONArray output_data = new JSONArray();
        JSONObject outcsv = new JSONObject();
        outcsv.put(ID, ".*\\.csv");
        output_data.add(outcsv);
        JSONObject outtxt = new JSONObject();
        outtxt.put(ID, ".*\\.txt");
        output_data.add(outtxt);
        JSONObject outjson = new JSONObject();
        outjson.put(ID, ".*\\.json");
        output_data.add(outjson);
        JSONObject outxml = new JSONObject();
        outxml.put(ID, ".*\\.xml");
        output_data.add(outxml);
        json_do.put(OUTPUT_DATA, output_data);

        json_do.put("input_data", "XXX");

        String toto = payload.toString();
        int where = toto.indexOf("\"XXX\"");

        String before = toto.substring(0, where);
        String after = toto.substring(where + 5);


        String json = input_data.toString();
        json = json.substring(0, json.length() - 1);

        byte[] encoded = getFileContentAsEncoded64(savFileName);

        byte[] jsid = (before + json + (json.length() > 1 ? "," : "") + "{\"" + ID + "\": \"" + sav + "\"").getBytes();
        byte[] jscontent = (",\"" + CONTENT + "\": \"").getBytes();
        byte[] jsend = ("\"}]" + after).getBytes();

        byte[] ret = new byte[jsid.length + jscontent.length + jsend.length + encoded.length];
        System.arraycopy(jsid, 0, ret, 0, jsid.length);
        System.arraycopy(jscontent, 0, ret, jsid.length, jscontent.length);
        System.arraycopy(encoded, 0, ret, jscontent.length + jsid.length, encoded.length);
        System.arraycopy(jsend, 0, ret, encoded.length + jscontent.length + jsid.length, jsend.length);

        if (ret.length > 100000000) {
            logger.error("!!!! Beware: you are certainly above the WML size limits: " + ret.length + " bytes for the model !!!");
        }
        if (exportPath != null) {
            String path = getPath(exportPath, date, "/wml_payload.wml");
            logger.info("Exporting the WML payload to " + path);
            try {
                OutputStream os = new FileOutputStream(path);
                os.write(ret);
                os.close();
            }
            catch (IOException e){
                logger.warn("Ingornig error:" +e.getMessage());
            }
        }
        return ret;
    }

    @Override
    public Job createJob(String deployment_id,
                         JSONArray input_data,
                         JSONArray input_data_references,
                         JSONArray output_data,
                         JSONArray output_data_references) throws IloException {
        logger.info("Create job");
        JSONObject payload = new JSONObject();

        payload.put(NAME, "Job_for_" + deployment_id);
        payload.put(SPACE_ID, wml_credentials.get(Credentials.WML_SPACE_ID));

        JSONObject deployment = new JSONObject();
        deployment.put(ID, deployment_id);
        payload.put("deployment", deployment);

        JSONObject json_do = new JSONObject();
        JSONObject solve_parameters = new JSONObject();
        solve_parameters.put("oaas.logAttachmentName", LOGS);
        if (showEngineProgress)
            solve_parameters.put("oaas.logTailEnabled", "true");
        else
            solve_parameters.put("oaas.logTailEnabled", "false");
        solve_parameters.put("oaas.includeInputData", "false");
        solve_parameters.put("oaas.resultsFormat", resultFormat);
        solve_parameters.put("oaas.engineLogLevel", engineLogLevel);
        json_do.put("solve_parameters", solve_parameters);

        if (input_data != null)
            json_do.put("input_data", input_data);

        if (input_data_references != null)
            json_do.put("input_data_references", input_data_references);

        if (output_data != null)
            json_do.put(OUTPUT_DATA, output_data);

        if ((output_data == null) && (output_data_references == null)) {
            output_data = new JSONArray();
            JSONObject outcsv = new JSONObject();
            outcsv.put(ID, ".*\\.csv");
            output_data.add(outcsv);
            JSONObject outtxt = new JSONObject();
            outtxt.put(ID, ".*\\.txt");
            output_data.add(outtxt);
            JSONObject outjson = new JSONObject();
            outjson.put(ID, ".*\\.json");
            output_data.add(outjson);
            JSONObject outxml = new JSONObject();
            outxml.put(ID, ".*\\.xml");
            output_data.add(outxml);
            json_do.put(OUTPUT_DATA, output_data);
        }

        if (output_data_references != null)
            json_do.put("output_data_references", output_data_references);


        payload.put(DECISION_OPTIMIZATION, json_do);

        Map<String, String> headers = getWMLHeaders();
        headers.put(ACCEPT, APPLICATION_JSON);
        headers.put(CONTENT_TYPE, APPLICATION_JSON);

        long t1 = new Date().getTime();
        String res = doPost(
                wml_credentials.get(Credentials.WML_HOST),
                MLV4_DEPLOYMENT_JOBS,
                getWMLParams(), headers, payload.toString());
        long t2 = new Date().getTime();

        JSONObject json = parseJson(res);

        String job_id = (String) ((JSONObject) json.get(METADATA)).get(ID);

        logger.info("WML job_id = " + job_id);
        logger.info("Creating the job in WML took " + (t2 - t1) / 1000 + " seconds.");

        return new JobImpl(deployment_id, job_id);
    }

    public Job createEngineJob(String deployment_id,
                               byte[] payload) throws IloException {
        logger.info("Create engine job");

        Map<String, String> headers = getWMLHeaders();
        headers.put(ACCEPT, APPLICATION_JSON);
        headers.put(CONTENT_TYPE, APPLICATION_JSON);

        long t1 = new Date().getTime();
        String res = doPost(
                wml_credentials.get(Credentials.WML_HOST),
                MLV4_DEPLOYMENT_JOBS,
                getWMLParams(), headers, payload);
        //HACK
        int entityIndex = res.indexOf("\"" + ENTITY + "\"");
        int metadataIndex = res.indexOf("\"" + METADATA + "\"");
        if (metadataIndex > entityIndex) {
            res = "{" + res.substring(metadataIndex);
        } else {
            res = res.substring(0, entityIndex).replace("},", "}}");
        }
        long t2 = new Date().getTime();

        JSONObject json = parseJson(res);

        String job_id = (String) ((JSONObject) json.get(METADATA)).get(ID);

        logger.info("WML job_id = " + job_id);
        logger.info("Creating the job in WML took " + (t2 - t1) / 1000 + " seconds.");

        return new JobImpl(deployment_id, job_id);
    }

    private String waitCompletion(Job job) throws IloException {
        String state = null;
        do {
            try {
                Thread.sleep(statusRefreshRate);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            job.updateStatus();

            try {
                state = job.getState();
                if (job.hasSolveState()) {
                    if (job.hasSolveStatus())
                        logger.info("WML Solve Status : " + job.getSolveStatus());
                    if (showEngineProgress && job.hasLatestEngineActivity())
                        logger.info("Latest Engine Activity : " + job.getLatestEngineActivity());

                    HashMap<String, Object> kpis = job.getKPIs();

                    Iterator<String> keys = kpis.keySet().iterator();

                    while (keys.hasNext()) {
                        String kpi = keys.next();
                        logger.info("KPI: " + kpi + " = " + kpis.get(kpi));
                    }
                }
            } catch (Exception e) {
                logger.error("Error extractState: " + e);
            }

            logger.info("Job State: " + state);
            if (state == null || state.equals(FAILED)) {
                logger.error("WML Failure: " + job.getFailure());
            }
        } while (!completedStatus.contains(state));
        if (state != null && state.equals(COMPLETED)) {
            job.extractOutputData();
            //logger.info("output_data = " + output_data);
        }
        return state;
    }

    public Job createAndRunEngineJob(String deployment_id,
                                     byte[] input_data) throws IloException {
        Job job = createEngineJob(deployment_id, input_data);

        String state = waitCompletion(job);

        logger.info("Job final state is " + state);
        if (exportPath != null) {
            String date = getTimeStamp();
            logger.info("Dumping the WML output in " + date);
            File f = new File(exportPath + "/" + date);
            f.mkdirs();

            String path = getPath(exportPath, date, "/wml_answer.wml");
            logger.info("Exporting the WML payload to " + path);
            try {
                OutputStream os = new FileOutputStream(path);
                os.write(job.getStatus().toString().getBytes());
                os.close();
                dump2Disk(job.extractOutputData(), date);
            }
            catch (IOException e){
                logger.warn("Ignoring error: "+e.getMessage());
            }
        }
        return job;
    }


    @Override
    public Job createAndRunJob(String deployment_id,
                               JSONArray input_data,
                               JSONArray input_data_references,
                               JSONArray output_data,
                               JSONArray output_data_references) throws IloException {
        Job job = createJob(deployment_id, input_data, input_data_references, output_data, output_data_references);

        String state = waitCompletion(job);

        if (state.equals(COMPLETED)) {
            output_data = job.extractOutputData();
        } else {
            logger.error("Job is " + state);
            logger.error("Job status:" + job.getStatus());
        }
        return job;
    }


    @Override
    public String createNewModel(String modelName, Runtime runtime, ModelType type, String modelAssetFilePath) throws IloException {
        String modelId = null;
        {
            Map<String, String> headers = getWMLHeaders();
            headers.put(CONTENT_TYPE, APPLICATION_JSON);

            JSONObject payload = new JSONObject();
            payload.put(NAME, modelName);
            payload.put("description", modelName);
            payload.put("type", type.getShortName());
            JSONObject soft = new JSONObject();
            soft.put(NAME, runtime.getShortName());
            payload.put("software_spec", soft);
            payload.put(SPACE_ID, wml_credentials.get(Credentials.WML_SPACE_ID));

            String res = doPost(
                    wml_credentials.get(Credentials.WML_HOST),
                    MLV4_MODELS,
                    getPlatformParams(), headers, payload.toString());

            JSONObject json = parseJson(res);
            modelId = (String) ((JSONObject) json.get(METADATA)).get(ID);
        }

        {
            Map<String, String> headers = getWMLHeaders();

            if (modelAssetFilePath != null) {
                byte[] bytes = getFileContent(modelAssetFilePath);

                Map<String, String> params = getWMLParams();
                params.put("content_format", "native");

                String res = doPut(
                        wml_credentials.get(Credentials.WML_HOST),
                        MLV4_MODELS + "/" + modelId + "/content",
                        params, headers, bytes);
                if (res == null)
                    logger.error("Problem putting the model");
            }
        }

        return modelId;
    }


    @Override
    public String deployModel(String deployName, String model_id, TShirtSize size, int nodes) throws IloException {
        Map<String, String> headers = getWMLHeaders();
        headers.put(CONTENT_TYPE, APPLICATION_JSON);

        JSONObject payload = new JSONObject();
        payload.put(NAME, deployName);
        payload.put(SPACE_ID, wml_credentials.get(Credentials.WML_SPACE_ID));
        JSONObject asset = new JSONObject();
        asset.put(ID, model_id);
        payload.put("asset", asset);
        JSONObject hardware = new JSONObject();
        hardware.put(NAME, size.toString());
        payload.put("hardware_spec", hardware);
        payload.put("num_nodes", nodes);
        payload.put("batch", new JSONObject());

        String res = doPost(
                wml_credentials.get(Credentials.WML_HOST),
                MLV4_DEPLOYMENTS,
                getPlatformParams(),
                headers, payload.toString());

            JSONObject json = parseJson(res);
            return (String) ((JSONObject) json.get(METADATA)).get(ID);
    }

    private JSONObject wmlGet(String endpoint) throws IloException {
        Map<String, String> headers = getWMLHeaders();
        headers.put(ACCEPT, APPLICATION_JSON);

        String res = doGet(
                wml_credentials.get(Credentials.WML_HOST),
                endpoint,
                getWMLParams()
                , headers);

        return parseJson(res);
    }

    @Override
    public JSONObject getDeployments() throws IloException {
        return wmlGet(MLV4_DEPLOYMENTS);
    }

    @Override
    public JSONObject getModels() throws IloException {
        return wmlGet(MLV4_MODELS);
    }

    @Override
    public JSONObject getDeploymentJobs() throws IloException {
        return wmlGet(MLV4_DEPLOYMENT_JOBS);
    }

    @Override
    public String getDeploymentIdByName(String deployment_name) throws IloException {
        JSONObject json = getDeployments();
        JSONArray resources = (JSONArray) json.get(RESOURCES);
        int len = resources.size();
        for (int i = 0; i < len; i++) {
            JSONObject metadata = (JSONObject) ((JSONObject) resources.get(i)).get(METADATA);
            if ((metadata.get(NAME)).equals(deployment_name)) {
                return (String) metadata.get(ID);
            }
        }
        logger.error("Deployment " + deployment_name + " does not exist in WML");
        return null;
    }

    private void delete(String url, Map<String, String> extraParams) throws IloException {
        Map<String, String> headers = getWMLHeaders();
        headers.put(ACCEPT, APPLICATION_JSON);

        Map<String, String> params = getWMLParams();
        for (String key : extraParams.keySet())
            params.put(key, extraParams.get(key));

        doDelete(
                wml_credentials.get(Credentials.WML_HOST),
                url,
                params, headers);
    }

    @Override
    public void deleteModel(String id) throws IloException {
        delete(MLV4_MODELS + "/" + id, new HashMap<>());
    }

    @Override
    public void deleteDeployment(String id) throws IloException {
        delete(MLV4_DEPLOYMENTS + "/" + id, new HashMap<>());
    }

    @Override
    public void deleteJob(String id) throws IloException {
        String hardDelete = "true";
        if (!config.getBoolean("wmlconnector.v4.hard_delete")) hardDelete = "false";

        Map<String, String> hardDel = new HashMap<String, String>();
        hardDel.put("hard_delete", hardDelete);
        delete(MLV4_DEPLOYMENT_JOBS + "/" + id, hardDel);
    }

    @Override
    public int deleteModels() throws IloException {
        JSONObject json = getModels();
        JSONArray resources = (JSONArray) json.get(RESOURCES);
        int len = resources.size();
        for (int i = 0; i < len; i++) {
            JSONObject metadata = (JSONObject) ((JSONObject) resources.get(i)).get(METADATA);
            deleteModel((String) metadata.get(ID));
        }
        logger.info("delete " + len + " Models");
        return len;
    }

    @Override
    public int deleteDeployments() throws IloException {
        JSONObject json = getDeployments();
        JSONArray resources = (JSONArray) json.get(RESOURCES);
        int len = resources.size();
        for (int i = 0; i < len; i++) {
            JSONObject metadata = (JSONObject) ((JSONObject) resources.get(i)).get(METADATA);
            deleteDeployment((String) metadata.get(ID));
        }
        logger.info("delete " + len + " Deployments");
        return len;
    }

    @Override
    public int deleteJobs() throws IloException {
        JSONObject json = getDeploymentJobs();
        JSONArray resources = (JSONArray) json.get(RESOURCES);
        int len = resources.size();
        for (int i = 0; i < len; i++) {
            JSONObject metadata = (JSONObject) ((JSONObject) resources.get(i)).get(METADATA);
            deleteJob((String) metadata.get(ID));
        }
        logger.info("delete " + len + " Jobs");
        return len;
    }

    @Override
    public void cleanSpace() throws IloException {
        logger.info("cleanSpace called");
        int j = deleteJobs();
        int d = deleteDeployments();
        int m = deleteModels();
        logger.info("Deleted " + j + " jobs, " + d + " deployments, " + m + " models.");
    }


    @Override
    public String getOrMakeDeployment(String name, boolean isCplex) throws IloException {
        String deployment_id = this.getDeploymentIdByName(name);
        if (deployment_id == null) {
            logger.info("Creating model and deployment");
            logger.info("Create Empty " + wml_runtime + " Model");
            ModelType type = null;
            if (isCplex)
                type = com.ibm.ml.ilog.Connector.getCPLEXModelType(wml_runtime);
            else
                type = com.ibm.ml.ilog.Connector.getCPOModelType(wml_runtime);

            String model_id = this.createNewModel(name, wml_runtime, type, null);
            logger.info("model_id = " + model_id);

            deployment_id = this.deployModel(name, model_id, wml_size, wml_nodes);
        } else
            logger.info("Reusing deployment_id " + deployment_id);
        logger.info("deployment_id = " + deployment_id);
        return deployment_id;
    }


    @Override
    public JSONObject getSoftwareSpecifications() throws IloException {
        String res = doGet(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_SOFTWARESPECS,
                getPlatformParams(),
                getPlatformHeaders());
        return parseJson(res);
    }

    @Override
    public String createDeploymentSpace(String name, String cos_crn, String compute_name) throws IloException {
        Map<String, String> headers = getPlatformHeaders();
        headers.put(CONTENT_TYPE, APPLICATION_JSON);

        JSONObject payload = new JSONObject();
        payload.put(NAME, name);
        payload.put("description", name);
        JSONObject storage = new JSONObject();
        storage.put("resource_crn", cos_crn);
        payload.put("storage", storage);
        JSONObject crn = new JSONObject();
        crn.put(NAME, compute_name);
        crn.put("crn", cos_crn);
        payload.put("compute", new JSONArray().add(crn));

        String res = doPost(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_SPACES,
                getPlatformParams(),
                headers, payload.toString());
        JSONObject json = parseJson(res);
        return (String) ((JSONObject) json.get(RESOURCES)).get("ig");
    }

    @Override
    public JSONObject getDeploymentSpaces() throws IloException {
        String res = doGet(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_SPACES,
                getPlatformParams(),
                getPlatformHeaders());
        return parseJson(res);
    }

    @Override
    public String getDeploymentSpaceIdByName(String spaceName) throws IloException {
        JSONObject json = getDeploymentSpaces();
        JSONArray resources = (JSONArray) json.get(RESOURCES);
        int len = resources.size();
        for (int i = 0; i < len; i++) {
            JSONObject entity = (JSONObject) ((JSONObject) resources.get(i)).get(ENTITY);
            JSONObject metadata = (JSONObject) ((JSONObject) resources.get(i)).get(METADATA);
            if (entity.get(NAME).equals(spaceName))
                return (String) metadata.get("id");
        }
        return null;
    }

    @Override
    public JSONObject getAssetFiles(String space_id) {
        if (wml_credentials.isCPD) {
            logger.error("getAssetFiles is not yet implemented for CPD");
        } else
            logger.error("getAssetFiles is not yet implemented for public cloud");
        return null;
    }

    @Override
    public JSONObject getInstances() throws IloException {
        if (wml_credentials.isCPD) {
            logger.error("Cannot get WML instances in a CPD env");
            return null;
        }

        String res = doGet(
                wml_credentials.get(Credentials.WML_HOST),
                MLV4_INSTANCES,
                getWMLParams(), getPlatformHeaders());

        return parseJson(res);
    }

    @Override
    public String getCatalogIdBySpaceId(String space_id) throws IloException {
        JSONObject res = parseJson(doGet(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_CATALOG,
                getWMLParams(), getPlatformHeaders()));

        if (!res.containsKey("catalogs"))
            return null;
        JSONArray catalogs = (JSONArray) res.get("catalogs");
        for (int i = 0; i < catalogs.size(); i++) {
            JSONObject catalog = (JSONObject) catalogs.get(i);
            if (((JSONObject) catalog.get(ENTITY)).containsKey(SPACE_ID) &&
                    ((JSONObject) catalog.get(ENTITY)).get(SPACE_ID).equals(space_id))
                return (String) ((JSONObject) catalog.get(METADATA)).get("guid");
        }
        return null;
    }

    @Override
    public JSONObject getStorageBySpaceId(String space_id) throws IloException {
        String res = doGet(
                wml_credentials.get(Credentials.PLATFORM_HOST),
                V2_SPACES,
                getWMLParams(), getPlatformHeaders()
        );
        JSONArray spaces = (JSONArray) parseJson(res).get(RESOURCES);
        for (int i = 0; i < spaces.size(); i++) {
            JSONObject space = (JSONObject) spaces.get(i);
            if (((JSONObject) space.get(METADATA)).get("id").equals(space_id))
                return (JSONObject) ((JSONObject) space.get(ENTITY)).get("storage");
        }
        return null;
    }

    @Override
    public JSONObject getStorage() throws IloException {
        return getStorageBySpaceId(wml_credentials.get(Credentials.WML_SPACE_ID));
    }

    @Override
    public String getCatalogId() throws IloException {
        return getCatalogIdBySpaceId(wml_credentials.get(Credentials.WML_SPACE_ID));
    }

}
