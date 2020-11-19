/* --------------------------------------------------------------------------
   Source file provided under Apache License, Version 2.0, January 2004,
   http://www.apache.org/licenses/
   (c) Copyright IBM Corp. 2016

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
   --------------------------------------------------------------------------
 */
package ilog.cp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONObject;
import com.ibm.ml.ilog.Connector;
import com.ibm.ml.ilog.utils.HttpUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ibm.ml.ilog.Credentials;
import com.ibm.ml.ilog.Job;
import ilog.concert.IloException;

public class WmlCP extends ExternalCP {
	private static final Logger logger = LogManager.getLogger();

	/**
	 * Specifies the name of the CPO command to be executed by the CPO Worker
	 * (solve, refine conflict or propagate).
	 */
	public static final String CPO_COMMAND = "oaas.cpo.command";

	/**
	 * String value for {@link #CPO_COMMAND} parameter to specify SOLVE as command
	 * to be executed by CPO worker. This is the default command.
	 */
	public static final String CPO_COMMAND_SOLVE = "Solve";

	/**
	 * String value for {@link #CPO_COMMAND} parameter to specify REFINE_CONFLICT as
	 * command to be executed by CPO worker.
	 */
	public static final String CPO_COMMAND_REFINE_CONFLICT = "RefineConflict";

	/**
	 * String value for {@link #CPO_COMMAND} parameter to specify PROPAGATE as
	 * command to be executed by CPO worker.
	 */
	public static final String CPO_COMMAND_PROPAGATE = "Propagate";

	private final String wml_name;
	private final com.ibm.ml.ilog.v4.Connector wmlConnector;
    private final Integer timeLimit;

	private Job job;
	private String solution;

	public WmlCP(Credentials credentials, Connector.Runtime runtime, Connector.TShirtSize size, int numNodes) throws IloException {
        super();
        logger.info("Starting CPO with "+runtime+"."+size+"."+numNodes);
        wmlConnector = (com.ibm.ml.ilog.v4.Connector) Connector.getConnector(credentials, runtime, size, numNodes, "JSON");
        wml_name = Connector.getCPOPrefix()+runtime+"."+size+"."+numNodes;
        timeLimit = wmlConnector.config.getInt("wmlconnector.v4.time_limit");
    }

    @Override
    public void end(){
        if (job != null){
			logger.info("Cleaning remaining job "+job.getId());
			try {
				wmlConnector.deleteJob(job.getId());
			} catch (IloException e) {
				logger.warn("Ignoring error: "+e.getMessage());
			}
		}
        wmlConnector.end();
        super.end();
    }

    protected String externalProcess(String cpoCommand) throws IloException {
    	this.resetStatus();

    	wmlConnector.initToken();
		solution = null;
		String solveStatus = null;

        if (getParameter(IloCP.DoubleParam.TimeLimit) == getParameterDefault(IloCP.DoubleParam.TimeLimit)){
            logger.info("Setting the time limit to default WML: " + timeLimit*60 + " seconds");
            setParameter(IloCP.DoubleParam.TimeLimit, timeLimit*60);
        }
        else{
            logger.info("Time limit has been set by user to " + getParameter(IloCP.DoubleParam.TimeLimit));
        }
		try {
			// Create temporary files for model input and solution output.
			final File model = File.createTempFile("cpo", ".cpo");
			exportModel(model.getAbsolutePath());

			logger.info("Exported cpo file.");
			try {
				String deployment_id =wmlConnector.getOrMakeDeployment(wml_name, false);
				JSONArray input_data = new JSONArray();

				Map<String, String> overriden_solve_parameters = new HashMap<>();
				if (cpoCommand != null) {
					overriden_solve_parameters.put(CPO_COMMAND, cpoCommand);
				}
				byte[] payload = wmlConnector.buildPayload(deployment_id, wml_name+".cpo", model.getAbsolutePath(), input_data, overriden_solve_parameters);

				job = wmlConnector.createAndRunEngineJob(deployment_id, payload);
				if (job.hasSolveState()) {
	        		solveStatus = job.getSolveStatus();
	        		logger.info("SolveStatus = " + solveStatus);
				} else {
					throw new IloException(job.getJobStatus().toString());
				}

				solution = job.getSolution();
        		return solveStatus;
				
			} finally {
				model.delete();
				if (job != null) {
					wmlConnector.deleteJob(job.getId());
					job = null;
				}
				wmlConnector.close();
			}
		} catch (Exception e) {
			throw new IloException(e.getMessage());
		}
    }
    
    @Override
	protected String externalSolve() throws IloException {
		return externalProcess(CPO_COMMAND_SOLVE);
	}


    @Override
    protected String externalRefineConflict() throws IloException {
		return externalProcess(CPO_COMMAND_REFINE_CONFLICT);
    }
    

    @Override
	protected Solution getSolution() {
		if (solution != null)
			return new Solution(HttpUtils.parseJson(solution));
		else
			return new Solution();
	}
}
