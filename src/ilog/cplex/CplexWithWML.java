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
package ilog.cplex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.wmlconnector.Connector;
import com.ibm.wmlconnector.Credentials;
import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;
import com.ibm.wmlconnector.impl.ConnectorImpl;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;
import ilog.concert.IloException;
import org.json.JSONArray;
import org.json.JSONObject;

public class CplexWithWML extends ExternalCplex {
    private static final Logger LOGGER = Logger.getLogger(CplexWithWML.class.getName());
    private static final long serialVersionUID = 1;

    protected Credentials wml_credentials;
    protected WMLConnectorImpl wml;
    protected WMLConnector.ModelType wml_type;
    protected WMLConnector.TShirtSize wml_size;
    protected int wml_nodes = 1;
    protected String wml_name;
    protected String deployment_id = null;
    public CplexWithWML(Credentials credentials) throws IloException {
        this(NamingStrategy.MAKE_NAMES, credentials, WMLConnector.ModelType.CPLEX_12_10, WMLConnector.TShirtSize.S, "CPLEXWithWML");
    }
    public CplexWithWML(Credentials credentials, WMLConnector.ModelType type,  WMLConnector.TShirtSize size) throws IloException {
        this(NamingStrategy.MAKE_NAMES, credentials, type, size, "CPLEXWithWML");
    }
    public CplexWithWML(Credentials credentials, WMLConnector.ModelType type,  WMLConnector.TShirtSize size, String name) throws IloException {
        this(NamingStrategy.MAKE_NAMES, credentials, type, size, name);
    }
    public CplexWithWML(NamingStrategy namingStrategy, Credentials credentials, WMLConnector.ModelType type,  WMLConnector.TShirtSize size, String name) throws IloException {
        super(namingStrategy);
        wml_credentials = credentials;
        wml = new WMLConnectorImpl(wml_credentials);
        this.wml_type = type;
        this.wml_size = size;
        wml_name = name;
    }

    @Override
    protected Solution externalSolve(Set<String> knownVariables, Set<String> knownConstraints) throws IloException {

        try {
            // Create temporary files for model input and solution output.
            final File model = File.createTempFile("cpx", ".sav.gz");
            final File solution = new File(model.getAbsolutePath() + ".sol");
            solution.deleteOnExit();
            exportModel(model.getAbsolutePath());
            LOGGER.fine("Exported .sav.gz file.");
            final File parameters = File.createTempFile("cpx", ".prm");
            writeParam(parameters.getAbsolutePath());
            LOGGER.fine("Exported .prm file to " + parameters.getAbsolutePath());
            boolean hasStartSolution = false;
            try {

                if (deployment_id == null)
                    deployment_id = wml.getDeploymentIdByName(wml_name);
                if (deployment_id == null) {
                    LOGGER.info("Creating model and deployment");
                    LOGGER.fine("Create Empty " + wml_type + " Model");
                    String model_id = wml.createNewModel(wml_name, wml_type, null);
                    LOGGER.fine("model_id = " + model_id);

                    deployment_id = wml.deployModel(wml_name, model_id, wml_size, wml_nodes);
                }
                LOGGER.fine("deployment_id = " + deployment_id);

                JSONArray input_data = new JSONArray();
                input_data.put(wml.createDataFromFile(wml_name+".sav.gz", model.getAbsolutePath()));
                input_data.put(wml.createDataFromFile(wml_name+".prm", parameters.getAbsolutePath()));
                if (isMIP() && result != null && result.hasSolution()) // TODO only for MIP ?
                    input_data.put(wml.createDataFromString(wml_name+".sol", result.getSolution()));
                WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, null);

                switch (job.getSolveStatus()) {

                    case "TBD":
                        return new Solution();
                    default:
                        // We have a feasible solution. Parse the solution file
                        FileWriter myWriter = new FileWriter(solution.getAbsolutePath());
                        myWriter.write(job.getSolution());
                        myWriter.close();
                        System.out.println(job.getLog().replaceAll("\n\n", "\n"));
                        return new Solution(solution, knownVariables, knownConstraints);
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
