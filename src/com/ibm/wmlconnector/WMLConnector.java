package com.ibm.wmlconnector;

import org.json.JSONArray;
import org.json.JSONObject;

public interface WMLConnector extends Connector {

    public enum Runtime {
        DO_12_9 ("/v4/runtimes/do_12.9", "do_12.9"),
        DO_12_10 ("/v4/runtimes/do_12.10", "do_12.10");

        private String name = "";
        private String shortName = "";

        Runtime(String name, String shortName){
            this.name = name;
            this.shortName = shortName;
        }

        public String getShortName() {
            return shortName;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    public enum ModelType {
        CPLEX_12_9 ("do-cplex_12.9"),
        CPO_12_9 ("do-cpo_12.9"),
        OPL_12_9 ("do-opl_12.9"),
        DOCPLEX_12_9 ("do-docplex_12.9"),
        CPLEX_12_10 ("do-cplex_12.10"),
        CPO_12_10 ("do-cpo_12.10"),
        OPL_12_10 ("do-opl_12.10"),
        DOCPLEX_12_10 ("do-docplex_12.10");

        private String name = "";

        ModelType(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    public enum TShirtSize {
        S ("S"),
        M ("M"),
        XL ("XL");

        private String name = "";

        TShirtSize(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    public void lookupBearerToken();
    public String getBearerToken();

    public JSONObject getInstances();

    public JSONObject getSoftwareSpecifications();
    public String createDeploymentSpace(String name);
    public JSONObject getDeploymentSpaces();
    public String getDeploymentSpaceIdByName(String spaceName);
    public JSONObject getAssetFiles(String space_id);

    public String createNewModel(String modelName, ModelType type, String modelAssetFilePath, Runtime runtime);
    public String createNewModel(String modelName, ModelType type, String modelAssetFilePath);
    public String getModelHref(String modelId, boolean displayModel);
    public String deployModel(String deployName, String model_id, TShirtSize size, int nodes);

    public JSONObject createDataFromString(String id, String text);
    public JSONArray createDataFromJSONPayload(String fileName);
    public JSONObject createDataFromCSV(String id, String fileName);
    public JSONObject createDataFromFile(String id, String fileName);

    public WMLJob createJob(String deployment_id,
                            JSONArray input_data,
                            JSONArray input_data_references,
                            JSONArray output_data,
                            JSONArray output_data_references);
    public WMLJob createAndRunJob(String deployment_id,
                                  JSONArray input_data,
                                  JSONArray input_data_references,
                                  JSONArray output_data,
                                  JSONArray output_data_references);
    public void deleteDeployment(String deployment_id);

    public JSONObject getDeployments();
    public String getDeploymentIdByName(String deployment_name);
}
