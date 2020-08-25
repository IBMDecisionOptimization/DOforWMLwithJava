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

    Credentials wml_credentials;
    String wml_name;
    public CplexWithWML(Credentials credentials) throws IloException { this(NamingStrategy.MAKE_NAMES, credentials, "CPLEXWithWML"); }
    public CplexWithWML(Credentials credentials, String name) throws IloException { this(NamingStrategy.MAKE_NAMES, credentials, name); }
    public CplexWithWML(NamingStrategy namingStrategy, Credentials credentials, String name) throws IloException {
        super(namingStrategy);
        wml_credentials = credentials;
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
            LOGGER.fine("Exported sav file.");
            try {

                WMLConnector.ModelType type = WMLConnector.ModelType.CPLEX_12_9;
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
                input_data.put(wml.createDataFromFile(wml_name+".sav.gz", model.getAbsolutePath()));
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
