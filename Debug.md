
Here is a typical Log file when delegating a solve to Decision Optimization in Watson Machine Learning and its analysis. 

The log file provides a list of calls to WML endpoints in cURL format with sensitive information removed.

Any delegated solve will contain the same kind of log bricks. 
 
```
2020-11-12 16:53:49 INFO  WmlCplex:40 - Starting CPLEX with do_12.10.M.1
2020-11-12 16:53:49 INFO  Connector:104 - WMLConnector using V4 final APIs with runtime: do_12.10, size: M, nodes: 1
2020-11-12 16:53:49 INFO  Connector:106 - Using TLSv1.2 SSL Context with WML.
2020-11-12 16:53:49 INFO  Connector:107 - Using 10 minutes as token refresh rate
2020-11-12 16:53:49 INFO  Connector:108 - Using 500 msec as status refresh rate
2020-11-12 16:53:49 INFO  Connector:118 - No export path defined.
2020-11-12 16:53:49 INFO  WmlCplex:45 - Default time limit is 60 minutes.
```
When a solve is delegated, the sum up of its characteristics is provided.
Here, we run a CPLEX model and delegate it to a 12.10 version, with a medium configuration size.
The authentication token is refreshed every 10 minutes.
When the job is submitted, the connector checks for a new status every 500 milliseconds.
No export path is defined, which means we are not in debug mode.
The WML default time limit for the job is 1 hour.

```
2020-11-12 16:53:49 INFO  ExternalCplex:492 - Naming stategy took 0 seconds.
2020-11-12 16:53:49 INFO  HttpUtils:183 - Lookup Bearer Token from IAM (ASYNCH)
2020-11-12 16:53:49 INFO  HttpUtils:325 - Curl info: curl --request POST "https://iam.cloud.ibm.com/identity/token?apikey=#####&grant_type=urn:ibm:params:oauth:grant-type:apikey" --header "Accept: application/json" --header "Content-Type: application/x-www-form-urlencoded"
2020-11-12 16:53:53 INFO  HttpUtils:339 - status 200
2020-11-12 16:53:53 INFO  HttpUtils:168 - Bearer Token OK
```
An IAM token is generated.

```
2020-11-12 16:53:53 INFO  WmlCplex:69 - Time limit has been set by user to 1800.0
```
Our CPLEX model provides a time limit of 1800 seconds which will override the WML one.

```
2020-11-12 16:53:53 INFO  WmlCplex:78 - Starting export
2020-11-12 16:53:53 INFO  WmlCplex:82 - Exported .sav.gz file in 0 seconds
2020-11-12 16:53:53 INFO  WmlCplex:108 - Exported .sav.gz file to C:\Users\VINCEN~1\AppData\Local\Temp\cpx5877312276137620236.sav.gz
2020-11-12 16:53:53 INFO  WmlCplex:109 - Exported .prm file to C:\Users\VINCEN~1\AppData\Local\Temp\cpx1077154734455555275.prm
2020-11-12 16:53:53 INFO  WmlCplex:112 - Exported .flt file to C:\Users\VINCEN~1\AppData\Local\Temp\cpx1374694310697811406.flt
2020-11-12 16:53:53 INFO  WmlCplex:122 - No annotation to export.
```
Artifacts are created for WML. These consist of various export files, depending on the engine. 

```
2020-11-12 16:53:53 INFO  HttpUtils:325 - Curl info: curl --request GET "https://us-south.ml.cloud.ibm.com/ml/v4/deployments?version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json"
Default filter names f1, f2 ... to be created.
2020-11-12 16:53:54 INFO  HttpUtils:339 - status 200
2020-11-12 16:53:54 INFO  Connector:891 - Reusing deployment_id aeec41e5-7be3-4dd4-927d-fcd44cd82a47
2020-11-12 16:53:54 INFO  Connector:892 - deployment_id = aeec41e5-7be3-4dd4-927d-fcd44cd82a47
```
The connector checks if a deployment named CPLEXWithWML.do_12.10.M.1 exists to reuse it.
In this case, it exists.
 
```
2020-11-12 16:53:54 INFO  HttpUtils:415 - Size of the input file C:\Users\VINCEN~1\AppData\Local\Temp\cpx1077154734455555275.prm is 8.96453857421875E-5 MB
2020-11-12 16:53:54 INFO  HttpUtils:415 - Size of the input file C:\Users\VINCEN~1\AppData\Local\Temp\cpx1374694310697811406.flt is 6.389617919921875E-5 MB
2020-11-12 16:53:54 INFO  HttpUtils:405 - Size of the encoded model file C:\Users\VINCEN~1\AppData\Local\Temp\cpx5877312276137620236.sav.gz is 0.00562286376953125 MB
2020-11-12 16:53:54 INFO  HttpUtils:407 - Encoding the file C:\Users\VINCEN~1\AppData\Local\Temp\cpx5877312276137620236.sav.gz took 0 seconds.
2020-11-12 16:53:54 INFO  WmlCplex:160 - Building the payload took 0 seconds
2020-11-12 16:53:54 INFO  Connector:561 - Create engine job
2020-11-12 16:53:54 INFO  HttpUtils:325 - Curl info: curl --request POST "https://us-south.ml.cloud.ibm.com/ml/v4/deployment_jobs?version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json" --header "Content-Type: application/json"
2020-11-12 16:53:57 INFO  HttpUtils:339 - status 202
2020-11-12 16:53:57 INFO  Connector:588 - WML job_id = 7b030833-b59b-40a2-85f6-63f5d043a271
2020-11-12 16:53:57 INFO  Connector:589 - Creating the job in WML took 3 seconds.
```
The connector creates a job on the previous deployment, and provides the inputs.
It then starts querying for a status which indicates when the job has started, is running and ended. 

```
2020-11-12 16:53:58 INFO  HttpUtils:325 - Curl info: curl --request GET "https://us-south.ml.cloud.ibm.com/ml/v4/deployment_jobs/7b030833-b59b-40a2-85f6-63f5d043a271?include=output_data,status,solve_state&version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json"
2020-11-12 16:53:59 INFO  HttpUtils:339 - status 200
2020-11-12 16:53:59 INFO  Connector:618 - Latest Engine Activity : 
2020-11-12 16:53:59 INFO  Connector:633 - Job State: running
2020-11-12 16:53:59 INFO  HttpUtils:325 - Curl info: curl --request GET "https://us-south.ml.cloud.ibm.com/ml/v4/deployment_jobs/7b030833-b59b-40a2-85f6-63f5d043a271?include=output_data,status,solve_state&version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json"
2020-11-12 16:54:00 INFO  HttpUtils:339 - status 200
2020-11-12 16:54:00 INFO  Connector:618 - Latest Engine Activity : 
2020-11-12 16:54:00 INFO  Connector:633 - Job State: running
2020-11-12 16:54:00 INFO  HttpUtils:325 - Curl info: curl --request GET "https://us-south.ml.cloud.ibm.com/ml/v4/deployment_jobs/7b030833-b59b-40a2-85f6-63f5d043a271?include=output_data,status,solve_state&version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json"
2020-11-12 16:54:02 INFO  HttpUtils:339 - status 200
2020-11-12 16:54:02 INFO  Connector:616 - WML Solve Status : optimal_solution
```
The solve ended successfully.
Engine logs of what occured between 2 status checks are displayed.
For long runs, you can see the engine progress by observing the gap for example.
 
```
2020-11-12 16:54:02 INFO  Connector:618 - Latest Engine Activity : [2020-11-12T15:53:58Z, INFO] 
[2020-11-12T15:53:58Z, INFO] Repeating presolve.
[2020-11-12T15:53:58Z, INFO] Tried aggregator 1 time.
[2020-11-12T15:53:58Z, INFO] Reduced MIP has 495 rows, 55 columns, and 1485 nonzeros.
[2020-11-12T15:53:58Z, INFO] Reduced MIP has 55 binaries, 0 generals, 0 SOSs, and 0 indicators.
[2020-11-12T15:53:58Z, INFO] Presolve time = 0.00 sec. (0.65 ticks)
[2020-11-12T15:53:58Z, INFO] Tried aggregator 1 time.
[2020-11-12T15:53:58Z, INFO] Reduced MIP has 495 rows, 55 columns, and 1485 nonzeros.
[2020-11-12T15:53:58Z, INFO] Reduced MIP has 55 binaries, 0 generals, 0 SOSs, and 0 indicators.
[2020-11-12T15:53:58Z, INFO] Presolve time = 0.00 sec. (1.02 ticks)
[2020-11-12T15:53:58Z, INFO] Represolve time = 0.01 sec. (4.25 ticks)
[2020-11-12T15:53:58Z, INFO]    2308     0       32.8616    55       19.0000      Cuts: 54    27409   34.21%
[2020-11-12T15:53:58Z, INFO]    2308     0       32.7088    55       19.0000       Cuts: 9    27458   34.21%
[2020-11-12T15:53:58Z, INFO]    2308     0       32.6373    55       19.0000      Cuts: 10    27499   34.21%
[2020-11-12T15:53:58Z, INFO]    2308     0       32.5913    55       19.0000       Cuts: 8    27534   34.21%
[2020-11-12T15:53:58Z, INFO]    2308     2       32.5913    55       19.0000       25.5000    27534   34.21%
[2020-11-12T15:53:59Z, INFO]    3273   669       22.5000    35       19.0000       25.5000    40350   34.21%
[2020-11-12T15:53:59Z, INFO]    5277  1928       21.7500    32       19.0000       24.7500    58179   30.26%
[2020-11-12T15:53:59Z, INFO]    7841  3209       21.5000    30       19.0000       24.0000    82106   26.32%
[2020-11-12T15:53:59Z, INFO]   10521  4285       21.0000    30       19.0000       23.4286   104068   23.31%
[2020-11-12T15:53:59Z, INFO] * 12777  5367      integral     0       20.0000       23.0000   132772   15.00%
[2020-11-12T15:53:59Z, INFO]   13172  5290        cutoff             20.0000       23.0000   130750   15.00%
[2020-11-12T15:53:59Z, INFO]   15714  3229       22.0000    35       20.0000       22.2500   156981   11.25%
[2020-11-12T15:54:00Z, INFO]   18324  1748        cutoff             20.0000       21.5714   187454    7.86%
[2020-11-12T15:54:00Z, INFO] 
[2020-11-12T15:54:00Z, INFO] Zero-half cuts applied:  37
[2020-11-12T15:54:00Z, INFO] Lift and project cuts applied:  10
[2020-11-12T15:54:00Z, INFO] Gomory fractional cuts applied:  1
[2020-11-12T15:54:00Z, INFO] 
[2020-11-12T15:54:00Z, INFO] Root node processing (before b&c):
[2020-11-12T15:54:00Z, INFO]   Real time             =    0.42 sec. (200.78 ticks)
[2020-11-12T15:54:00Z, INFO] Parallel b&c, 3 threads:
[2020-11-12T15:54:00Z, INFO]   Real time             =    2.08 sec. (2458.98 ticks)
[2020-11-12T15:54:00Z, INFO]   Sync time (average)   =    0.13 sec.
[2020-11-12T15:54:00Z, INFO]   Wait time (average)   =    0.00 sec.
[2020-11-12T15:54:00Z, INFO]                           ------------
[2020-11-12T15:54:00Z, INFO] Total (root+branch&cut) =    2.50 sec. (2659.76 ticks)
[2020-11-12T15:54:00Z, INFO] Incumbent solution:
[2020-11-12T15:54:00Z, INFO] MILP objective                                 2.0000000000e+01
[2020-11-12T15:54:00Z, INFO] MILP solution norm |x| (Total, Max)            2.00000e+01  1.00000e+00
[2020-11-12T15:54:00Z, INFO] MILP solution error (Ax=b) (Total, Max)        0.00000e+00  0.00000e+00
[2020-11-12T15:54:00Z, INFO] MILP x bound error (Total, Max)                0.00000e+00  0.00000e+00
[2020-11-12T15:54:00Z, INFO] MILP x integrality error (Total, Max)          0.00000e+00  0.00000e+00
[2020-11-12T15:54:00Z, INFO] MILP slack bound error (Total, Max)            0.00000e+00  0.00000e+00
[2020-11-12T15:54:00Z, INFO] integer optimal solution (101)
```

```
2020-11-12 16:54:02 INFO  Connector:633 - Job State: completed
2020-11-12 16:54:02 INFO  Connector:651 - Job final state is completed
2020-11-12 16:54:02 INFO  WmlCplex:164 - SolveStatus = optimal_solution
2020-11-12 16:54:02 INFO  HttpUtils:325 - Curl info: curl --request DELETE "https://us-south.ml.cloud.ibm.com/ml/v4/deployment_jobs/7b030833-b59b-40a2-85f6-63f5d043a271?version=2020-08-07&space_id=ce81a7cf-8250-49ec-8a29-fe4c1d44c26d&hard_delete=true" --header "Authorization: Authorization ####" --header "cache-control: no-cache" --header "Accept: application/json"
2020-11-12 16:54:03 INFO  HttpUtils:339 - status 204
```
The WML job is no longer needed and is deleted.
The engine can be queried to retrieve the values.
Deployment remains to speed up next delegated solve.
