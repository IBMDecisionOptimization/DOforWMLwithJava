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

import com.ibm.wmlconnector.WMLConnector;
import com.ibm.wmlconnector.WMLJob;
import com.ibm.wmlconnector.impl.WMLConnectorImpl;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import org.json.JSONArray;
import org.json.JSONObject;

public class IloCplexWithWML extends ExternalCplex {
    private static final Logger LOGGER = Logger.getLogger(IloCplexWML.class.getName());
    private static final long serialVersionUID = 1;

    String wml_url;
    String wml_apikey;
    String wml_instance_id;
    public IloCplexWithWML(String url, String apikey, String instance_id) throws IloException { this(NamingStrategy.MAKE_NAMES, url, apikey, instance_id); }
    public IloCplexWithWML(NamingStrategy namingStrategy, String url, String apikey, String instance_id) throws IloException {
        super(namingStrategy);
        wml_url = url;
        wml_apikey = apikey;
        wml_instance_id = instance_id;
    }

    byte[] getFileContentAsBytes(String inputFilename)  {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(inputFilename));
        } catch (IOException e) {
            LOGGER.severe("Error getting file" + e.getStackTrace());
        }
        return bytes;
    }

    JSONObject createDataFromFile(String fileName) {

        byte[] bytes = getFileContentAsBytes(fileName);
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

    @Override
    protected Solution externalSolve(Set<String> knownVariables) throws IloException {

        try {
            // Create temporary files for model input and solution output.
            final File model = File.createTempFile("cpx", ".sav.gz");
            final File solution = new File(model.getAbsolutePath() + ".sol");
            solution.deleteOnExit();
            exportModel(model.getAbsolutePath());
            LOGGER.info("Exported sav file.");
            try {

                WMLConnector.ModelType type = WMLConnector.ModelType.CPLEX_12_9;
                WMLConnector.TShirtSize size = WMLConnector.TShirtSize.S;
                int nodes = 1;
                WMLConnectorImpl wml = new WMLConnectorImpl(wml_url, wml_instance_id, wml_apikey);
                LOGGER.info("Create Empty " + type + " Model");

                String model_id = wml.createNewModel("empty-"+type+"-model",type, null);
                LOGGER.info("model_id = "+ model_id);

                String deployment_id = wml.deployModel("empty-"+type+"-deployment-"+size+"-"+nodes, wml.getModelHref(model_id, false), size, nodes);
                LOGGER.info("deployment_id = "+ deployment_id);


                JSONArray input_data = new JSONArray();
                input_data.put(createDataFromFile(model.getAbsolutePath()));
                WMLJob job = wml.createAndRunJob(deployment_id, input_data, null, null, null);

                FileWriter myWriter = new FileWriter(solution.getAbsolutePath());
                myWriter.write(job.getSolution());
                myWriter.close();

                return new Solution(solution, knownVariables);

                /*
                try (final Executor executor = new Executor(JobExecutorFactory.createDefault())) {
                    JobClient client = JobClientFactory.createDefault(url, key);
                    final JobResponse response = client.newRequest()
                            .input(model)
                            .parameter(JobParameters.RESULTS_FORMAT, JobParameters.RESULTS_FORMAT_XML)
                            .deleteOnCompletion(true)
                            .output(solution)
                            .execute(executor)
                            .get();

                    switch (response.getJob().getSolveStatus()) {
                        case INFEASIBLE_SOLUTION:
                        case INFEASIBLE_OR_UNBOUNDED_SOLUTION:
                        case UNKNOWN:
                            return new Solution();
                        default:
                            // We have a feasible solution. Parse the solution file
                            return new Solution(solution, knownVariables);
                    }
                }
                */

            }
            finally {
                model.delete();
                solution.delete();
            }
        }

        catch (IOException e) { throw new IloException(e.getMessage()); }

    }



}
