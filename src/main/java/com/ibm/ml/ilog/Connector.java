package com.ibm.ml.ilog;


import ilog.concert.IloException;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

import java.io.IOException;
import java.util.HashMap;


/* A WML interface */
public interface Connector extends TokenHandler {
    static String getCplexPrefix() {
        return "CPLEXWithWML.";
    }

    static String getCPOPrefix() {
        return "CPOWithWML.";
    }

    static String getOPLPrefix() {
        return "OPLWithWML.";
    }

    static String getPythonPrefix() {
        return "PythonWithWML.";
    }

    /* Creates a connector */
    static Connector getConnector(Credentials creds, Connector.Runtime runtime, Connector.TShirtSize size, int nodes, String format) throws IloException {
        return new com.ibm.ml.ilog.v4.Connector(creds, runtime, size, nodes, format);
    }

    /* Creates a connector */
    static Connector getConnector(Credentials creds, Connector.Runtime runtime, Connector.TShirtSize size, int nodes) throws IloException {
        return new com.ibm.ml.ilog.v4.Connector(creds, runtime, size, nodes);
    }

    /* Creates a connector */
    static Connector getConnector(Credentials creds) throws IloException {
        return new com.ibm.ml.ilog.v4.Connector(creds);
    }

    enum Runtime {
        DO_12_9("do_12.9"),
        DO_12_10("do_12.10"),
        DO_20_1("do_20.1"),
        DO_22_1("do_22.1");

        private final String shortName;

        Runtime(String shortName) {
            this.shortName = shortName;
        }

        public String getShortName() {
            return shortName;
        }

        @Override
        public String toString() {
            return shortName;
        }
    }

    enum ModelType {
        CPLEX_12_9("do-cplex_12.9"),
        CPO_12_9("do-cpo_12.9"),
        OPL_12_9("do-opl_12.9"),
        DOCPLEX_12_9("do-docplex_12.9"),
        CPLEX_12_10("do-cplex_12.10"),
        CPO_12_10("do-cpo_12.10"),
        OPL_12_10("do-opl_12.10"),
        DOCPLEX_12_10("do-docplex_12.10"),
        CPLEX_20_1("do-cplex_20.1"),
        CPO_20_1("do-cpo_20.1"),
        OPL_20_1("do-opl_20.1"),
        DOCPLEX_20_1("do-docplex_20.1"),
        CPLEX_22_1("do-cplex_22.1"),
        CPO_22_1("do-cpo_22.1"),
        OPL_22_1("do-opl_22.1"),
        DOCPLEX_22_1("do-docplex_22.1");

        private final String shortName;

        ModelType(String name) {
            this.shortName = name;
        }

        @Override
        public String toString() {
            return shortName;
        }

        public String getShortName() {
            return shortName;
        }
    }

    static ModelType getCPLEXModelType(Runtime r) throws IloException {
        switch (r) {
            case DO_12_9:
                return ModelType.CPLEX_12_9;
            case DO_12_10:
                return ModelType.CPLEX_12_10;
            case DO_20_1:
                return ModelType.CPLEX_20_1;
            case DO_22_1:
                return ModelType.CPLEX_22_1;
        }
        throw new IloException("Runtime " + r + " is not supported currently");
    }

    static ModelType getCPOModelType(Runtime r) throws IloException {
        switch (r) {
            case DO_12_9:
                return ModelType.CPO_12_9;
            case DO_12_10:
                return ModelType.CPO_12_10;
            case DO_20_1:
                return ModelType.CPO_20_1;
            case DO_22_1:
                return ModelType.CPO_22_1;
        }
        throw new IloException("Runtime " + r + " is not supported currently");
    }

    enum TShirtSize {
        S("S"),
        M("M"),
        L("L"),
        XL("XL");

        private final String name;

        TShirtSize(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    String getOrMakeDeployment(String name, boolean isCplex) throws IloException;

    String createNewModel(String modelName, Runtime runtime, ModelType type, String modelAssetFilePath, HashMap<String,String> custom) throws IloException;

    String deployModel(String deployName, String model_id, TShirtSize size, int nodes) throws IloException;

    JSONObject createDataFromString(String id, String text);

    JSONObject createDataFromFile(String id, String fileName);

    JSONObject createDataFromBytes(String id, byte[] bytes);

    Job createJob(String deployment_id,
                  JSONArray input_data,
                  JSONArray input_data_references,
                  JSONArray output_data,
                  JSONArray output_data_references, HashMap<String,String> custom) throws IloException;

    Job createAndRunJob(String deployment_id,
                        JSONArray input_data,
                        JSONArray input_data_references,
                        JSONArray output_data,
                        JSONArray output_data_references, HashMap<String,String> custom) throws IloException;
    Job createAndRunJob(String deployment_id,
                        JSONArray input_data_references,
                        JSONArray output_data_references, HashMap<String,String> custom) throws IloException;
    JSONObject getDeployments() throws IloException;

    JSONObject getModels() throws IloException;

    JSONObject getDeploymentJobs() throws IloException;

    String getDeploymentIdByName(String deployment_name) throws IloException;

    void deleteModel(String id) throws IloException;

    void deleteDeployment(String id) throws IloException;

    void deleteJob(String id) throws IloException;

    int deleteModels() throws IloException;

    int deleteDeployments() throws IloException;

    int deleteJobs() throws IloException;

    void cleanSpace() throws IloException;

    JSONObject getInstances() throws IloException;

    JSONObject getStorage() throws IloException;

    String getCatalogId() throws IloException;

    JSONObject getSoftwareSpecifications() throws IloException;

    String createDeploymentSpace(String name, String cos_crn, String compute_name) throws IloException;

    JSONObject getDeploymentSpaces() throws IloException;

    String getDeploymentSpaceIdByName(String spaceName) throws IloException;

    JSONObject getAssetFiles(String space_id);

    JSONObject getStorageBySpaceId(String space_id) throws IloException;

    String getCatalogIdBySpaceId(String space_id) throws IloException;
}
