# SQLSolver: Proving Query Equivalence Using Linear Integer Arithmetic

SQLSolver is an automated prover that verifies the equivalence of SQL queries, which is presented in
the paper "[Proving Query Equivalence Using Linear Integer Arithmetic](https://dl.acm.org/doi/abs/10.1145/3626768)" (SIGMOD 2024).

## Table of Contents

- [Environment setup](#environment-setup)
  - [Requirements](#requirements)
  - [Install Python](#install-python)
  - [Install Java and Gradle](#install-java-and-gradle)
- [Quick Start](#quick-start)
  - [Compile](#compile)
  - [Building the JAR file](#building-the-jar-file)
  - [Using the JAR file](#using-the-jar-file)
    - [Example](#example) 
- [API](#api)
  - [DEMO](#demo)
- [Benchmark](#benchmark)
- [File Structure](#file-structure)
- [Citation](#citation)
- [Contact](#contact)


## Environment setup

### Requirements

- Ubuntu (22.04.1 LTS is tested)
- z3 4.8.9 (SMT solver)
- antlr 4.8 (Generate tokenizer and parser for SQL AST)
- Python 3
- Java 17
- Gradle 7.3.3

z3 and antlr library have been put in `lib/` off-the-shelf.

#### Install Python

Python is typically installed by default.
Type `python3 --version` to check whether Python 3 is installed.
In cases where it is not installed, use this following instruction:

```shell
sudo apt install python3
```

#### Install Java and Gradle

If you do not have Java or Gradle installed, you may refer to these instructions:

```shell
# Installing Java 17
sudo add-apt-repository ppa:linuxuprising/java
sudo apt update
sudo apt-get install -y oracle-java17-installer oracle-java17-set-default

# Installing Gradle 7.3.3
wget https://services.gradle.org/distributions/gradle-7.3.3-bin.zip -P /tmp
sudo unzip -d /opt/gradle /tmp/gradle-7.3.3-bin.zip
sudo touch /etc/profile.d/gradle.sh
sudo chmod a+wx /etc/profile.d/gradle.sh
sudo echo -e "export GRADLE_HOME=/opt/gradle/gradle-7.3.3 \nexport PATH=\${GRADLE_HOME}/bin:\${PATH}" >> /etc/profile.d/gradle.sh
source /etc/profile
```

## Quick Start

### Compile 

We use Gradle as the project build tool.
After all the above dependencies are installed, you can compile the SQLSolver project
with the following command.

```shell
gradle compileJava
```

### Building the JAR file

After Compilation, you can build a JAR file using the following command.

```shell
gradle fatjar
```

The JAR file will be generated in the `build/libs/` directory relative
to the current directory.

### Using the JAR file

To use the JAR file of SQLSolver, you need to put libz3.so and libz3java.so in a custom directory: `<path/to/lib>`
and then use the command:
```shell
export LD_LIBRARY_PATH=<path/to/lib>
```

The code below shows how to use SQLSolver's JAR File to verify SQLs.
- `-sql1=<path/to/query1>`: the first file whose path is `<path/to/query1>` to store the SQLs.
- `-sql2=<path/to/query2>`: the second file whose path is `<path/to/query2>` to store the SQLs.
- `-schema=<path/to/schema>`: the schema file whose path is `<path/to/schema>` to store the schema.
- `-print`: output the result to standard output stream.
- `-output=<path/to/output>`: output the result to a file whose path is `<path/to/output>`.

Each SQL file has multiple SQL statements and should store a SQL statement in one line,
and the corresponding lines in both files will be considered as pairs of SQL statements to be verified for equivalence.

```shell
java -jar sqlsolver.jar -sql1=<path/to/query1> -sql2=<path/to/query2> -schema=<path/to/schema> [-print] [-output=<path/to/output>]
```

Since SQLSolver uses the parser of [Calcite](https://calcite.apache.org/) to parse SQL queries, **please ensure that queries in SQL files satisfy the syntax requirements of Calcite parser**.

#### Example

The example here shows how to use SQLSolver's JAR File to verify three simple SQLs.


First SQL file with path `./example_sql1.sql`:
```sql
SELECT i, j FROM a
SELECT x, y FROM b
```

Second SQL file with path `./example_sql2.sql`:
```sql
SELECT T.COL1, T.COL2 FROM (SELECT i AS COL1, j AS COL2 FROM a) AS T
SELECT T.COL1, T.COL2 FROM (SELECT x AS COL1, y AS COL2 FROM b) AS T
```

Schema file with path `./example_schema.sql`:
```sql
CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );
CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );
```

Then, use the following command to verify the three SQL pairs:
```shell
java -jar sqlsolver.jar -sql1=./example_sql1.sql -sql2=./example_sql2.sql -schema=./example_schema.sql -output=./result
```

You will get a file named `result` in the current directory with the following content:
```text
EQ
EQ
```

## API
The Java users can directly download SQLSolver's source code and access the package `sqlsolver.api`.
The entry of SQLSolver is in `sqlsolver.api.entry.Verification`.


There are two main interfaces:

```java
  /**
   * Verify two sql equivalence.
   */
  VerificationResult verify(String sql0, String sql1, String schema) 
  /**
   * Verify pairwise sql equivalence in the sqlList.
   * The two sql to verify are in the same index of both lists.
   * The two sqlList (sqlList0 and sqlList1) should have same size.
   */
  List<VerificationResult> verify(List<String> sqlList0, List<String> sqlList1, String schema)

```

The VerificationResult is an enum class for verification result which has four cases:

- **EQ**: two SQLs are equivalent
- **NEQ**: two SQLs are not equivalent
- **UNKNOWN**: SQLSolver doesn't know the equivalency between the two SQLs

Note that verifying the equivalence of two SQL queries is an undecidable problem.
Thus, the verification algorithm of SQLSolver does not guarantee to identify all equivalent queries.
SQLSolver may output NEQ or UNKNOWN for some equivalent query pairs.

### Demo

You can import SQLSolver by JAR File or directly by downloading source code in your JAVA's project.

By calling the interface of SQLSolver, you can get the SQLs verification result.
Before you use SQLSolver, you should put `libz3.so` and `libz3java.so` in a custom directory: `<path/to/lib>`
and then use the command:
```shell
export LD_LIBRARY_PATH=<path/to/lib>
```

Here is a little demo that shows how to use SQLSolver's interface:

```java
import sqlsolver.api.entry.Verification;
import sqlsolver.superopt.logic.VerificationResult;

public class Main {
  public static void main(String[] args) {
    String sql1 = "SELECT i, j FROM a";
    String sql2 = "SELECT T.COL1, T.COL2 FROM (SELECT i AS COL1, j AS COL2 FROM a) AS T";
    String schema = "CREATE TABLE a ( i INT PRIMARY KEY, j INT, k INT );\n" +
            "CREATE TABLE b ( x INT PRIMARY KEY, y INT, z INT );";

    VerificationResult result = Verification.verify(sql1, sql2, schema);
    System.out.println(result);
  }
}
```

This program will output a single `EQ` that indicates the verification result of two SQLs `sql1` and `sql2` 
under the schema `schema`.

## Benchmark

In our paper, SQLSolver is evaluated on four benchmarks, including test cases derived from Calcite, Spark SQL, TPC-C, and TPC-H.
The files of SQL queries and schemas are listed in the following table.

Each file of SQL queries consists of multiple pairs of SQL queries.
Each query in the odd-numbered line is equivalent to the query in the next line.
For example, the first query is equivalent to the second query in each file.

| Benchmark | File of Schema                                                                   | File of SQL Queries                                                    |
|:-----------:|:--------------------------------------------------------------------------:|:----------------------------------------------------------------:|
| Calcite   | [Calcite Schema](/sqlsolver_data/schemas/calcite_test.base.schema.sql)   | [Calcite Test Set](sqlsolver_data/calcite/calcite_tests)           |
| Spark SQL | [Spark SQL Schema](/sqlsolver_data/schemas/calcite_test.base.schema.sql) | [Spark SQL Test Set](sqlsolver_data/db_rule_instances/spark_tests) |
| TPC-C     | [TPC-C Schema](/sqlsolver_data/schemas/tpcc.base.schema.sql)             | [TPC-C Test Set](sqlsolver_data/prepared/rules.tpcc.spark.txt)     |
| TPC-H     | [TPC-H Schema](/sqlsolver_data/schemas/tpch.base.schema.sql)             | [TPC-H Test Set](sqlsolver_data/prepared/rules.tpch.spark.txt)     |

## File Structure

This repository includes the source code and benchmarks.

```
|-- api               # SQLSolver's entry.
|-- lib               # Required external library.
|-- common            # Common utilites.
|-- sql               # Data structures of SQL AST and query plan.
|-- stmt              # Manipulation program of queries.
|-- superopt          # Core algorithm of SQLSolver
|-- sqlsolver_data    # Data input/output directory, such as benchmarks
```

## Citation
If you use SQLSolver in your projects or research, please kindly cite our [paper](https://dl.acm.org/doi/abs/10.1145/3626768):
> @article{sqlsolver,  
> author = {Ding, Haoran and Wang, Zhaoguo and Yang, Yicun and Zhang, Dexin and Xu, Zhenglin and Chen, Haibo and Piskac, Ruzica and Li, Jinyang},  
> title = {Proving Query Equivalence Using Linear Integer Arithmetic},  
> year = {2023},  
> issue_date = {December 2023},  
> publisher = {Association for Computing Machinery},  
> address = {New York, NY, USA},  
> volume = {1},  
> number = {4},  
> journal = {Proc. ACM Manag. Data},  
> month = {Dec},  
> articleno = {227},  
> numpages = {26},  
> }

## Contact

If you have any questions, please submit an issue or contact our <a href="mailto:nhaorand@sjtu.edu.cn">email</a>.


