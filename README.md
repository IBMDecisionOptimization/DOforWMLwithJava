# DOforWMLwithJava

This repository includes code to support execution of IBM Decision Optimization (DO) on Watson Machine Learning (WML).
DO is a set of modeling and solving libraries, along with development and deployment tools for prescriptive analytics using Mathematical and Constraint Programming techniques.
WML offers Python and REST API to develop and execute DO models.
The code included in this repository provides two ways to use WML to solve DO models.
1. in the `com.ibm.*` package are included classes to create **models** and **deployments** in WML, and then create and poll **jobs**. 
With these classes, you can have a detailed control on the execution and integration of DO in WML.
2. in the `ilog.cplex.*` and `ilog.cp.*`packages ares included classes to easily transform an existing Java code to solve on WML.
The executing code should include both modeling and execution of the DO model, and using a single line change, execution will occur on WML.

## Pre-requisites
In this section are described some common requirements to both packages.

### Java
Of course, these packages are Java code, and this will apply with your Java developments.

### CPLEX installation
You will need some CPLEX installation in order to compile and run this code.
You don't need an official commercial version of CPLEX to run this as the optimization will happen on WML.
The compilation and modeling can be done with the Community Edition of CPLEX that is freely available. 

### WML instance and credentials
In order to run on WML, you will need an instance with some credentials.
You can refer to the [DO for WML documentation](https://dataplatform.cloud.ibm.com/docs/content/DO/DODS_Introduction/deployintro.html?audience=wdp&context=cpdaas) or to [this post](https://medium.com/@AlainChabrier/use-do-on-different-wml-locations-31e353955088).

### Project settings
This repository is built around a IntelliJ project but you should be able toadapt to any Java environement you prefer.
As always the important things to check are the link to the Concert and CPLEX Java packages, and the right setting of the path to the CPLEX native library. 

## The `ilog.cplex` and `ilog.cp` packages

Using these packages, you will be able to very easily modify an existing DO Java code, with some modeling and execution parts, so that the execution will occur on WML instead of being run locally.
These classes are reusing the `com.ibm` package but are hiding the complexity and the detailed control.
From outside of the engine class, everything will behave the same (see supported API and limitations) as if solve would happen locally.

### How to start?

You can start looking at one of the example, such as `Diet.java` for CPLEX or `Color.java` for CP.
To replicate the same behaviour on your application, you should include all classes from the different packages into your code. 

You will see in the examples, that from an original code where the engine is created using:

```
IloCplex cplex = new IloCplex();
```

In order to solve on WML, you only need to use the new class and provide some WML credentials (see the section below about  creating you own credentials class).

```
IloCplex cplex = new CplexWithWML( new MyProdBetaV4Credentials());
```

The new CPLEX class will behave exactly like the original one (see supported APi and limitations), you can call solve, get the variables values, etc

### Supported API and limitations

#### CPLEX

##### Supported API
```
  public boolean solve() throws IloException      
  public double getObjValue() throws IloException 
  public double getValue(IloNumVar v) throws IloException 
  public double[] getValues(IloNumVar[] v) throws IloException
  public double getDual(IloRange r) throws IloException
  public double[] getDuals(IloRange[] r) throws IloException
  public Status getStatus() throws IloException
  public CplexStatus getCplexStatus() throws IloException
```
##### Unsupported API

Currently callbacks and goals are not supported.
 
#### CP

**TO BE DONE**


## The `com.ibm` package

**TO BE DONE**

## WML credentials

All your credentials to use WML are grouped in a `com.ibm.wmlconnector.Credentials` subclass.
You can start from the `MyCredentials` class to start.
Using the current public beta v4 API, you only need to set the following values.

```
        WML_URL = "https://us-south.ml.cloud.ibm.com";
        WML_APIKEY  = "xxxxxxxxxxxxxxxxxxxxxxxxxxx";
        WML_INSTANCE_ID = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
```


## Other reading
Read some introduction on this repository [in this post](https://medium.com/@AlainChabrier/using-do-for-wml-from-java-27f726b34d13).
