package com.ibm.ml;

import com.ibm.json.java.JSONArray;

import com.ibm.json.java.JSONObject;
import com.ibm.json.java.JSON;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Job;
import ilog.concert.IloException;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class WMLSamples {

    private static Credentials CREDENTIALS = null;

    private static String createAndDeployDietPythonModel(Connector wml) throws IloException {

        System.out.println("Create Python Model");
        String path = WMLSamples.class.getClassLoader().getResource("diet.zip").getPath();//.substring(1);

        String model_id = wml.createNewModel("Diet", Connector.Runtime.DO_20_1, Connector.ModelType.DOCPLEX_12_10, path);
        System.out.println("model_id = "+ model_id);

        String deployment_id = wml.deployModel("diet-test-wml-2", model_id, Connector.TShirtSize.S,1);
        System.out.println("deployment_id = "+ deployment_id);

        return deployment_id;
    }

    public static void fullDietPythonFlow(int nJobs)  {
        Connector wml = null;
        try {
            System.out.println("Full flow with Diet");

            wml = Connector.getConnector(CREDENTIALS);
            wml.initToken();
            String deployment_id = createAndDeployDietPythonModel(wml);
            JSONArray input_data = new JSONArray();
            input_data.add(createDataFromCSV("diet_food.csv", "diet_food.csv"));
            input_data.add(createDataFromCSV("diet_food_nutrients.csv", "diet_food_nutrients.csv"));
            input_data.add(createDataFromCSV("diet_nutrients.csv", "diet_nutrients.csv"));

            long startTime = System.nanoTime();
            for (int i = 0; i < nJobs; i++) {
                Job job = wml.createAndRunJob(deployment_id, input_data, null, null, null);
                if (!job.hasFailure()) {

                        System.out.println("Log:" + getLogFromJob(job)); // Don't log
                        System.out.println("Solution:" + getSolutionFromJob(job));

                    long endTime = System.nanoTime();
                    long totalTime = endTime - startTime;
                    System.out.println("Total time: " + (totalTime / 1000000000.));
                    startTime = System.nanoTime();
                }
                else{
                    System.out.println("Error with job: "+ job.getFailure());
                }
            }
            wml.deleteDeployment(deployment_id);
        }
        catch(IloException e){
            System.out.println("Error occured: "+e.getMessage());
        }
        finally {
            if (wml != null) wml.end();
        }
        wml.end();
    }


    public static String createAndDeployWarehouseOPLModel(Connector wml) throws IloException {
        System.out.println("Create Warehouse OPL Model");
        String path = WMLSamples.class.getClassLoader().getResource("warehouse.zip").getPath();//.substring(1);

        String model_id = wml.createNewModel("Warehouse", Connector.Runtime.DO_12_9,  Connector.ModelType.OPL_12_9,path);
        System.out.println("model_id = "+ model_id);

        String deployment_id = wml.deployModel("warehouse-opl-test-wml-2", model_id, Connector.TShirtSize.S,1);
        System.out.println("deployment_id = "+ deployment_id);

        return deployment_id;
    }



    public static String getFileContent(String inputFilename) {
        String res = "";
        try {
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(WMLSamples.class.getClassLoader().getResourceAsStream(inputFilename), StandardCharsets.UTF_8)
            );
            String line;
            while ((line = in.readLine()) != null) {
                res += line + "\n";
            }
            in.close();
        } catch (IOException e) {
            System.out.println("Error getting text file" + e.getStackTrace());
        }

        return res;
    }
    private static JSONObject createDataFromCSV(String id, String fileName) {

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
            fields.add(field);
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
                    values.add(ivalue);
                } catch (NumberFormatException e) {
                    try {
                        double dvalue = Double.parseDouble(value);
                        values.add(dvalue);
                    } catch (NumberFormatException e2) {
                        values.add(value);
                    }
                }
            }
            all_values.add(values);
        }
        data.put("values", all_values);
        return data;
    }

    public static String getLogFromJob(Job job) {
        return job.getLog();
    }
    public static String getSolutionFromJob(Job job) {
        return job.getSolution();
    }


    public static void main(String[] args) {

        try {
            CREDENTIALS = Credentials.getCredentials("wml.public.conf");
            fullDietPythonFlow(2);
        }
        catch(Exception e){
            System.out.println("Something bad happened: "+e.getMessage());
        }
    }
}
