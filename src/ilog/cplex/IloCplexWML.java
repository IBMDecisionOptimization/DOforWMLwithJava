package ilog.cplex;

import com.ibm.Credentials;
import com.ibm.Sample;
import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.logging.Logger;

public class IloCplexWML extends IloCplex {
    private static final Logger LOGGER = Logger.getLogger(IloCplexWML.class.getName());

    IloCplex _inner;
    String _wml_url;
    String _wml_apikey;
    String _wml_instance_id;
    WMLJob _job;
    public IloCplexWML(IloCplex inner, String url, String apikey, String instance_id) throws IloException {
        super();
        _inner = inner;
        _wml_url = url;
        _wml_apikey = apikey;
        _wml_instance_id = instance_id;
    }

    @Override
    public boolean solve() throws IloException {
        WMLConnector.ModelType type = WMLConnector.ModelType.CPLEX_12_9;
        WMLConnector.TShirtSize size = WMLConnector.TShirtSize.S;
        int nodes = 1;
        WMLConnectorImpl wml = new WMLConnectorImpl(Credentials.WML_URL, Credentials.WML_INSTANCE_ID, Credentials.WML_APIKEY);
        LOGGER.info("Create Empty " + type + " Model");

        String model_id = wml.createNewModel("empty-"+type+"-model",type, null);
        LOGGER.info("model_id = "+ model_id);

        String deployment_id = wml.deployModel("empty-"+type+"-deployment-"+size+"-"+nodes, wml.getModelHref(model_id, false), size, nodes);
        LOGGER.info("deployment_id = "+ deployment_id);

        _inner.exportModel("src/resources/mymodel.sav");
        LOGGER.info("Exported sav file.");

        JSONArray input_data = new JSONArray();
        input_data.put(Sample.createDataFromFile("mymodel.sav"));
        _job = wml.createAndRunJob(deployment_id, input_data, null, null, null);

        //deleteDeployment(wml, deployment_id);

        return true;
    }

    @Override
    public IloCplex.Status getStatus() throws IloException {
        if (!_job.hasSolveStatus())
            return Status.Unknown;
        else {
            String status = _job.getSolveStatus();
            switch (status) {
                case "optimal_solution": return Status.Optimal;
                default: return Status.Unknown;
            }
        }
    }

    @Override
    public double getValue(IloNumVar var1) throws IloCplex.UnknownObjectException, IloException {
        String solution = _job.getSolution();
        JSONObject sol = (new JSONObject(solution)).getJSONObject("CPLEXSolution");
        JSONArray variables = sol.getJSONArray("variables");
        int idx = ((CpxNumVar)var1).getVarIndexValue();
        //int idx = 0;
        return variables.getJSONObject(idx).getDouble("value");
    }

    @Override
    public double getObjValue() throws IloException {
        return Double.parseDouble(_job.getSolveStateDetail("PROGRESS_CURRENT_OBJECTIVE"));
    }

}
