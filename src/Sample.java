import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import com.ibm.wmlconnector.Credentials;
import com.ibm.wmlconnector.COSConnector;
import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;
import com.ibm.wmlconnector.impl.COSConnectorImpl;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;
import org.json.JSONArray;
import org.json.JSONObject;

public class Sample {
    private static final Logger LOGGER = Logger.getLogger(Sample.class.getName());

    //private static final Credentials CREDENTIALS = new MyDevFinalV4Credentials();
    private static final Credentials CREDENTIALS = new MyProdBetaV4Credentials();

    public static String getFileContent(String inputFilename)  {
        String res = "";
        try {
            List<String> lines = Files.readAllLines(Paths.get(inputFilename));
            for (Iterator<String> it = lines.iterator(); it.hasNext();)
                res += it.next() + "\n";
        } catch (IOException e) {
            LOGGER.severe("Error getting file" + e.getStackTrace());
        }

        return res;
    }

    public static byte[] getFileContentAsBytes(String inputFilename)  {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(inputFilename));
        } catch (IOException e) {
            LOGGER.severe("Error getting file" + e.getStackTrace());
        }
        return bytes;
    }

    private static JSONArray createDataFromJSONPayload(String fileName) {
        String file = getFileContent("src/resources/"+fileName);

        JSONObject payload = new JSONObject(file);
        return payload.getJSONObject("decision_optimization").getJSONArray("input_data");
    }

    private static JSONObject createDataFromCSV(String fileName) {

        JSONObject data = new JSONObject();
        data.put("id", fileName);

        JSONArray fields = new JSONArray();
        JSONArray all_values = new JSONArray();
        String file = getFileContent("src/resources/"+fileName);
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


    public static JSONObject createDataFromFile(String fileName) {

        byte[] bytes = getFileContentAsBytes("src/resources/"+fileName);
        byte[] encoded = Base64.getEncoder().encode(bytes);

        JSONObject data = new JSONObject();
        data.put("id", fileName);

        JSONArray fields = new JSONArray();
        fields.put("___TEXT___");
        data.put("fields", fields);

        JSONArray values = new JSONArray();
        values.put(new JSONArray().put(new String(encoded)));
        data.put("values", values);

        return data;
    }

    public String createAndDeployEmptyModel(WMLConnector wml, WMLConnector.ModelType type, WMLConnector.TShirtSize size, int nodes) {

        LOGGER.info("Create Empty " + type + " Model");

        String model_id = wml.createNewModel("empty-"+type+"-model",type, null);
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("empty-"+type+"-deployment-"+size+"-"+nodes, model_id, size, nodes);
        LOGGER.info("deployment_id = "+ deployment_id);

        return deployment_id;
    }

    public String createAndDeployDietPythonModel(WMLConnector wml) {

        LOGGER.info("Create Python Model");

        String model_id = wml.createNewModel("Diet", WMLConnector.ModelType.DOCPLEX_12_10,"src/resources/diet.zip", WMLConnector.Runtime.DO_12_10);
        //String model_id = wml.createNewModel("Diet","do-docplex_12.9","src/resources/diet.zip", "/v4/runtimes/do_12.9");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("diet-test-wml-2", model_id, WMLConnector.TShirtSize.M,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        return deployment_id;
    }

    public void deleteDeployment(WMLConnector wml, String deployment_id) {
        LOGGER.info("Delete deployment");

        wml.deleteDeployment(deployment_id);
    }

    public void fullDietPythonFlow(boolean useOutputDataReferences, int nJobs) {

        LOGGER.info("Full flow with Diet");

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployDietPythonModel(wml);
        JSONArray input_data = new JSONArray();
        input_data.put(createDataFromCSV("diet_food.csv"));
        input_data.put(createDataFromCSV("diet_food_nutrients.csv"));
        input_data.put(createDataFromCSV("diet_nutrients.csv"));
        JSONArray output_data_references = null;
        COSConnector cos = null;
        if (useOutputDataReferences) {
            cos = new COSConnectorImpl(CREDENTIALS);
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
        }
        long startTime = System.nanoTime();
        for (int i=0; i<nJobs; i++) {
            WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, output_data_references);
            if (useOutputDataReferences) {
                getLogFromCOS(cos); // Don't log
            } else {
                getLogFromJob(job); // Don't log
            }
            long endTime   = System.nanoTime();
            long totalTime = endTime - startTime;
            LOGGER.info("Total time: " + (totalTime/1000000000.));
            startTime = System.nanoTime();
        }
        deleteDeployment(wml, deployment_id);
    }

    public void fullLPFLow(String filename) {
        LOGGER.info("Create and authenticate WML Connector");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPLEX_12_9, WMLConnector.TShirtSize.S, 1);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile(filename, "src/resources/"+filename);
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences(filename));
        JSONArray output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("solution.json"));
        output_data_references.put(cos.getDataReferences("log.txt"));

        wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        LOGGER.info("Log:" + getLogFromCOS(cos));
        LOGGER.info("Solution:" + getSolutionFromCOS(cos));
        deleteDeployment(wml, deployment_id);
    }

    public void deleteLPJob(String filename) {
        LOGGER.info("Create and authenticate WML Connector");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPLEX_12_9, WMLConnector.TShirtSize.S, 1);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile(filename, "src/resources/"+filename);
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences(filename));
        JSONArray output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("solution.json"));
        output_data_references.put(cos.getDataReferences("log.txt"));

        WMLJob job = wml.createJob(deployment_id, null, input_data_references, null, output_data_references);
        job.delete();
        deleteDeployment(wml, deployment_id);
    }

    public void parallelFullLPInlineFlow(String filename, int nodes, int nJobs) {

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPLEX_12_9, WMLConnector.TShirtSize.S, nodes);

        long startTime = System.nanoTime();
        List<WMLJob> jobs = new ArrayList<WMLJob>();
        JSONArray input_data = new JSONArray();
        input_data.put(createDataFromFile(filename));
        for (int i=0; i<nJobs; i++) {
            WMLJob job = wml. createJob(deployment_id, input_data, null, null, null);
            jobs.add(job);
        }
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        LOGGER.info("Total create job time: " + (totalTime/1000000000.));
        startTime = System.nanoTime();

        while (!jobs.isEmpty()) {
            LOGGER.info("Number of jobs " + jobs.size());
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            int n = jobs.size();
            for (int j=n-1; j>=0; j--) {
                WMLJob job = jobs.get(j);
                job.updateStatus();
                String state = job.getState();
                LOGGER.info("Job " + job.getId() + ": " + state);
                if (state.equals("completed") || state.equals("failed")) {
                    jobs.remove(job);
                }
            }
        }
        endTime   = System.nanoTime();
        totalTime = endTime - startTime;
        LOGGER.info("Total time: " + (totalTime/1000000000.));
        LOGGER.info("Per instance: " + (totalTime/1000000000.)/nJobs);
        startTime = System.nanoTime();

        //deleteDeployment(wml, deployment_id);
    }

    public void fullLPInlineFLow(String filename, int nJobs) {

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPLEX_12_9, WMLConnector.TShirtSize.S, 1);

        long startTime = System.nanoTime();
        for (int i=0; i<nJobs; i++) {
            JSONArray input_data = new JSONArray();
            input_data.put(createDataFromFile(filename));
            WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, null);
            getLogFromJob(job); // don't log
            getSolutionFromJob(job); // don'tlog
            long endTime   = System.nanoTime();
            long totalTime = endTime - startTime;
            LOGGER.info("Total time: " + (totalTime/1000000000.));
            startTime = System.nanoTime();
        }
        deleteDeployment(wml, deployment_id);
    }


    public String getLogFromJob(WMLJob job) {
        return job.getLog();
    }

    public String getSolutionFromJob(WMLJob job) {
        return job.getSolution();
    }


    public String getLogFromCOS(COSConnector cos) {
        return getFileFromCOS(cos,"log.txt");
    }

    public String getSolutionFromCOS(COSConnector cos) {
        return getFileFromCOS(cos, "solution.json");
    }

    public String getFileFromCOS(COSConnector cos, String fileName) {
        String content = cos.getFile(fileName);
        content = content.replaceAll("\\r", "\n");
        return content;
    }

    public void fullInfeasibleLPFLow() {
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPLEX_12_9, WMLConnector.TShirtSize.S, 1);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile("infeasible.lp", "src/resources/infeasible.lp");
        cos.putFile("infeasible.feasibility", "src/resources/infeasible.feasibility");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("infeasible.lp"));
        input_data_references.put(cos.getDataReferences("infeasible.feasibility"));
        JSONArray output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("log.txt"));
        output_data_references.put(cos.getDataReferences("conflict.json"));
        wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        LOGGER.info("Log:" + getLogFromCOS(cos));
        LOGGER.info("Conflict:" + getFileFromCOS(cos,"conflict.json"));

        deleteDeployment(wml, deployment_id);
    }


    public void runCPO(String modelName) {
        String deployment_id = CREDENTIALS.cpo_deployment_id;
        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile(modelName + ".cpo", "src/resources/" + modelName + ".cpo");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences(modelName + ".cpo"));
        JSONArray output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("log.txt"));
        output_data_references.put(cos.getDataReferences("solution.json"));
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        LOGGER.info("Log:" + getLogFromCOS(cos));
        LOGGER.info("Solution:" + getSolutionFromCOS(cos));
    }

    public void fullCPOFlow(String modelName) {

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        CREDENTIALS.cpo_deployment_id = createAndDeployEmptyModel(wml, WMLConnector.ModelType.CPO_12_9, WMLConnector.TShirtSize.XL, 1);

        String deployment_id = CREDENTIALS.cpo_deployment_id;
        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile(modelName + ".cpo", "src/resources/" + modelName + ".cpo");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences(modelName + ".cpo"));
        JSONArray output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("log.txt"));
        output_data_references.put(cos.getDataReferences("solution.json"));
        wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        LOGGER.info("Log:" + getLogFromCOS(cos));
        LOGGER.info("Solution:" + getSolutionFromCOS(cos));

        deleteDeployment(wml, deployment_id);
    }

    public String createAndDeployWarehouseOPLModel(WMLConnector wml) {

        LOGGER.info("Create Warehouse OPL Model");

        String model_id = wml.createNewModel("Warehouse", WMLConnector.ModelType.OPL_12_9,"src/resources/warehouse.zip");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("warehouse-opl-test-wml-2", model_id, WMLConnector.TShirtSize.S,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        return deployment_id;
    }

    public void fullWarehouseOPLFlow(boolean useOutputDataReferences) {

        LOGGER.info("Full Warehouse with OPL");

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployWarehouseOPLModel(wml);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile("warehouse.dat", "src/resources/warehouse.dat");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("warehouse.dat"));
        JSONArray output_data_references = null;
        if (useOutputDataReferences) {
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
        }

        WMLJob job = wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        if (useOutputDataReferences) {
            LOGGER.info("Log:" + getLogFromCOS(cos));
        } else {
            LOGGER.info("Log:" + getLogFromJob(job));
        }

        deleteDeployment(wml, deployment_id);
    }


    public String createAndDeployDietOPLModel(WMLConnector wml) {

        LOGGER.info("Create Diet OPL Model");

        String model_id = wml.createNewModel("Diet OPL", WMLConnector.ModelType.OPL_12_9,"src/resources/dietopl.zip");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("diet-opl-test-wml-2", model_id, WMLConnector.TShirtSize.S,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        return deployment_id;
    }

    public String createAndDeployDietMainOPLModel(WMLConnector wml) {

        LOGGER.info("Create Diet Main OPL Model");

        String model_id = wml.createNewModel("Diet Main OPL", WMLConnector.ModelType.OPL_12_9,"src/resources/dietoplmain.zip");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("diet-main-opl-test-wml-2", model_id, WMLConnector.TShirtSize.S,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        return deployment_id;
    }


    public void fullDietOPLWithDatFlow(boolean useOutputDataReferences) {

        LOGGER.info("Full Diet with OPL");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);

        String deployment_id = createAndDeployDietOPLModel(wml);
        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile("diet.dat", "src/resources/diet.dat");
        //cos.putFile("dietxls.dat", "src/resources/dietxls.dat");
        //cos.putBinaryFile("diet.xlsx", "src/resources/diet.xlsx");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("diet.dat"));
        //input_data_references.put(cos.getDataReferences("dietxls.dat"));
        //input_data_references.put(cos.getDataReferences("diet.xlsx"));
        JSONArray output_data_references = null;
        if (useOutputDataReferences) {
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
            output_data_references.put(cos.getDataReferences("solution.json"));
        } else {

        }

        WMLJob job = wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        if (useOutputDataReferences) {
            LOGGER.info("Log:" + getLogFromCOS(cos));
            LOGGER.info("Solution:" + getSolutionFromCOS(cos));
        } else {
            LOGGER.info("Log:" + getLogFromJob(job));
        }
        deleteDeployment(wml, deployment_id);
    }

    public void fullDietMainOPLWithDatFlow(boolean useOutputDataReferences) {

        LOGGER.info("Full Diet with Main OPL");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);

        String deployment_id = createAndDeployDietMainOPLModel(wml);
        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile("diet.dat", "src/resources/diet.dat");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("diet.dat"));
        JSONArray output_data_references = null;
        if (useOutputDataReferences) {
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
            output_data_references.put(cos.getDataReferences("solution.json"));
        } else {

        }

        WMLJob job = wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        if (useOutputDataReferences) {
            LOGGER.info("Log:" + getLogFromCOS(cos));
            LOGGER.info("Solution:" + getSolutionFromCOS(cos));
        } else {
            LOGGER.info("Log:" + getLogFromJob(job));
        }

        deleteDeployment(wml, deployment_id);
    }

    public void fullDietOPLWithCSVFlow(boolean useOutputDataReferences) {

        LOGGER.info("Full Diet with OPL");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployDietOPLModel(wml);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        JSONArray input_data = new JSONArray();
        input_data.put(createDataFromCSV("diet_food.csv"));
        input_data.put(createDataFromCSV("diet_food_nutrients.csv"));
        input_data.put(createDataFromCSV("diet_nutrients.csv"));
        JSONArray output_data_references = null;
        if (useOutputDataReferences) {
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
            output_data_references.put(cos.getDataReferences("solution.json"));
        } else {

        }

        WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, output_data_references);
        if (useOutputDataReferences) {
            LOGGER.info("Log:" + getLogFromCOS(cos));
            LOGGER.info("Solution:" + getSolutionFromCOS(cos));
        } else {
            LOGGER.info("Log:" + getLogFromJob(job));
        }

        deleteDeployment(wml, deployment_id);
    }

    public void fullOPLWithJSONFlow(boolean useOutputDataReferences) {

        LOGGER.info("Full JSON Test with OPL");

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);

        String model_id = wml.createNewModel("JSON Test OPL", WMLConnector.ModelType.OPL_12_9,"src/resources/jsontest.zip");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("json-test-opl-test-wml-2", model_id, WMLConnector.TShirtSize.S,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        cos.putFile("Nurses.json", "src/resources/Nurses.json");
        cos.putFile("spokes.json", "src/resources/spokes.json");
        JSONArray input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("Nurses.json"));
        input_data_references.put(cos.getDataReferences("spokes.json"));
        JSONArray output_data_references = null;
        if (useOutputDataReferences) {
            output_data_references = new JSONArray();
            output_data_references.put(cos.getDataReferences("log.txt"));
            output_data_references.put(cos.getDataReferences("solution.json"));
        } else {

        }
        WMLJob job = wml.createAndRunJob(deployment_id, null, input_data_references, null, output_data_references);
        if (useOutputDataReferences) {
            LOGGER.info("Log:" + getLogFromCOS(cos));
            LOGGER.info("Solution:" + getSolutionFromCOS(cos));
        } else {
            LOGGER.info("Log:" + getLogFromJob(job));
        }
        deleteDeployment(wml, deployment_id);
    }

    public void fullInfeasibleDietOPLFlow() {

        LOGGER.info("Full Infeasible Diet with OPL");
        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);
        String deployment_id = createAndDeployDietOPLModel(wml);

        COSConnector cos = new COSConnectorImpl(CREDENTIALS);
        JSONArray input_data = null;
        JSONArray input_data_references = null;
        cos.putFile("infeasible_diet.dat", "src/resources/infeasible_diet.dat");
        cos.putFile("infeasible_diet.ops", "src/resources/infeasible_diet.ops");
        input_data_references = new JSONArray();
        input_data_references.put(cos.getDataReferences("infeasible_diet.dat"));
        input_data_references.put(cos.getDataReferences("infeasible_diet.ops"));

        JSONArray output_data_references = null;
        output_data_references = new JSONArray();
        output_data_references.put(cos.getDataReferences("log.txt"));
        output_data_references.put(cos.getDataReferences("solution.json"));

        WMLJob job = wml.createAndRunJob(deployment_id, input_data, input_data_references, null, output_data_references);
        LOGGER.info("Status:" + job.getStatus());
        LOGGER.info("Log:" + getLogFromCOS(cos));
        LOGGER.info("Solution:" + getSolutionFromCOS(cos));

        deleteDeployment(wml, deployment_id);
    }


    public void fullOPLWithPayload() {
        LOGGER.info("Full JSON Test with OPL");

        WMLConnectorImpl wml = new WMLConnectorImpl(CREDENTIALS);

        String model_id = wml.createNewModel("JSON Test OPL", WMLConnector.ModelType.OPL_12_9,"src/resources/test_payload.zip");
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("json-test-opl-test-wml-2", model_id, WMLConnector.TShirtSize.S,1);
        LOGGER.info("deployment_id = "+ deployment_id);

        JSONArray input_data = createDataFromJSONPayload("test_payload_input_job.json");

        WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null,null);
        LOGGER.info("Log:" + getLogFromJob(job));
        LOGGER.info("Solution:" + getSolutionFromJob(job));

        deleteDeployment(wml, deployment_id);
    }

    public void testPerfs(int N) {
        LOGGER.info("Test perfs with " + N + " repeatitions.");

        LOGGER.info("Test get token.");
        WMLConnector wml = null;
        long startTime = System.nanoTime();
        long midTime = startTime;
        for (int i=0; i<N; i++) {
            wml = new WMLConnectorImpl(CREDENTIALS);
            LOGGER.info("Execution time: " + ((System.nanoTime()-midTime)/1000000000.));
            midTime = System.nanoTime();
        }
        long endTime   = System.nanoTime();
        long totalTime = endTime - startTime;
        LOGGER.info("Total time: " + (totalTime/1000000000.));
        LOGGER.info("Average per execution time: " + (totalTime/(N*1000000000.)));


        LOGGER.info("Test get deployments.");
        startTime = System.nanoTime();
        midTime = startTime;
        for (int i=0; i<N; i++) {
            wml.getDeploymentIdByName("dummy");
            LOGGER.info("Execution time: " + ((System.nanoTime()-midTime)/1000000000.));
            midTime = System.nanoTime();
        }
        endTime   = System.nanoTime();
        totalTime = endTime - startTime;
        LOGGER.info("Total time: " + (totalTime/1000000000.));
        LOGGER.info("Average per execution time: " + (totalTime/(N*1000000000.)));


        LOGGER.info("Test create job.");
        String deployment_id = createAndDeployDietPythonModel(wml);
        JSONArray input_data = new JSONArray();
        input_data.put(createDataFromCSV("diet_food.csv"));
        input_data.put(createDataFromCSV("diet_food_nutrients.csv"));
        input_data.put(createDataFromCSV("diet_nutrients.csv"));
        JSONArray output_data_references = null;
        startTime = System.nanoTime();
        midTime = startTime;
        for (int i=0; i<N; i++) {
            WMLJob job = wml.createJob(deployment_id, input_data, null, null, output_data_references);
            LOGGER.info("Execution time: " + ((System.nanoTime()-midTime)/1000000000.));
            midTime = System.nanoTime();
        }
        endTime   = System.nanoTime();
        totalTime = endTime - startTime;
        LOGGER.info("Total time: " + (totalTime/1000000000.));
        LOGGER.info("Average per execution time: " + (totalTime/(N*1000000000.)));


        deleteDeployment(wml, deployment_id);
    }

    void testV4final() {
        LOGGER.info("Test v4 final.");
        WMLConnector wml = new WMLConnectorImpl(CREDENTIALS);


        //LOGGER.info("Instances: " + wml.getInstances());
        LOGGER.info("Spaces: " + wml.getDeploymentSpaces());

        //LOGGER.info("Software Specifications: " + wml.getSoftwareSpecifications());

        //wml.createDeploymentSpace("test_space");
        //LOGGER.info("Spaces: " + wml.getDeploymentSpaces());


        LOGGER.info("Deployments: " + wml.getDeployments());
    }
    public static void main(String[] args) {
        Sample main = new Sample();

        //main.testV4final();

        //main.testPerfs(1);

        // Python
        main.fullDietPythonFlow(false, 1);

        // OPL
        //main.fullWarehouseOPLFlow(true);
        //main.fullDietOPLWithDatFlow(false);
        //main.fullDietOPLWithCSVFlow(false);

        //main.fullDietMainOPLWithDatFlow(false);
        //main.fullOPLWithJSONFlow(true);

//        main.fullOPLWithPayload();

        //KO main.fullInfeasibleDietOPLFlow();

        // CPLEX
        //main.
        // ("bigone.mps");

        //main.fullLPInlineFLow("bigone.mps", 1 );
        //main.fullLPInlineFLow("diet.lp", 1 );
        //main.parallelFullLPInlineFlow("diet.lp", 5, 100 );
        //main.fullLPInlineFLow("acc-tight4.lp", 20 );
        //main.parallelFullLPInlineFlow("acc-tight4.lp", 5, 100 );

//        main.fullInfeasibleLPFLow();


        // CPO
        //main.fullCPOFlow("mycpo");
        //main.runCPO("colors");
        //main.runCPO("plant_location");

        // Other
        //main.deleteLPJob("diet.lp");
    }
}
