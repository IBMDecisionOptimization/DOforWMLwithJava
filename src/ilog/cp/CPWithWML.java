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

import com.ibm.wmlconnector.Connector;
import com.ibm.wmlconnector.Credentials;
import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;
import com.ibm.wmlconnector.impl.ConnectorImpl;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.concert.IloSolution;
import ilog.cplex.ExternalCplex;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

public class CPWithWML extends ExternalCP {
    private static final Logger LOGGER = Logger.getLogger(CPWithWML.class.getName());
    private static final long serialVersionUID = 1;

    protected Credentials wml_credentials;
    protected String wml_name;
    public CPWithWML(Credentials credentials) throws IloException {
        this(credentials, "CPWithWML");
    }
    public CPWithWML(Credentials credentials, String name) throws IloException {
        super();
        wml_credentials = credentials;
        wml_name = name;
    }

    @Override
    protected Solution externalSolve(Set<String> vars, Set<String> intervalVars) throws IloException {

        WMLConnectorImpl.RESULTS_FORMAT = "JSON";

        try {
            // Create temporary files for model input and solution output.
            final File model = File.createTempFile("cpo", ".cpo");
            final File solution = new File(model.getAbsolutePath() + ".sol");
            solution.deleteOnExit();
            exportModel(model.getAbsolutePath());
            LOGGER.fine("Exported cpo file.");
            try {

                WMLConnector.ModelType type = WMLConnector.ModelType.CPO_12_9;
                WMLConnector.TShirtSize size = WMLConnector.TShirtSize.S;
                int nodes = 1;
                WMLConnectorImpl wml = new WMLConnectorImpl(wml_credentials);

                String deployment_id = wml.getDeploymentIdByName(wml_name);
                if (deployment_id == null) {
                    LOGGER.fine("Create Empty " + type + " Model");
                    String model_id = wml.createNewModel(wml_name, type, null);
                    LOGGER.fine("model_id = " + model_id);

                    deployment_id = wml.deployModel(wml_name, model_id, size, nodes);
                }
                LOGGER.fine("deployment_id = " + deployment_id);

                JSONArray input_data = new JSONArray();
                input_data.put(wml.createDataFromFile(wml_name+".cpo", model.getAbsolutePath()));
                WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, null);

                switch (job.getSolveStatus()) {

                    case "TBD":
                        return null;
                    default:
                        // We have a feasible solution. Parse the solution file
                        return new Solution(new JSONObject(job.getSolution()), vars, intervalVars);
                }



            }
            finally {
                model.delete();
                solution.delete();
            }
        }

        catch (IOException e) { throw new IloException(e.getMessage()); }

    }



}
