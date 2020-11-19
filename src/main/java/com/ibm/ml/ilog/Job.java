package com.ibm.ml.ilog;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import ilog.concert.IloException;

import java.util.HashMap;

/* A simple interface to WML Jobs. */
public interface Job {
    void updateStatus() throws IloException;

    String getId();

    JSONObject getStatus();

    String getFailure();

    String getState();

    boolean hasSolveState();

    boolean hasFailure();

    boolean hasSolveStatus();

    String getSolveStatus();

    JSONObject getJobStatus();

    boolean hasLatestEngineActivity();

    String getLatestEngineActivity();

    HashMap<String, Object> getKPIs();

    JSONArray extractOutputData();

    String getLog();

    String getSolution();
}

