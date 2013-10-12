/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.conf;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.hadoop.hive.common.LogUtils;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hive.common.util.HiveTestUtils;

/**
 * TestHiveLogging
 *
 * Test cases for HiveLogging, which is initialized in HiveConf.
 * Loads configuration files located in common/src/test/resources.
 */
public class TestHiveLogging extends TestCase {
  private Runtime runTime;
  private Process process;

  public TestHiveLogging() {
    super();
    runTime = Runtime.getRuntime();
    process = null;
  }

  private void configLog(String hiveLog4jTest, String hiveExecLog4jTest) 
  throws Exception {
    String expectedLog4jTestPath = HiveTestUtils.getFileFromClasspath(hiveLog4jTest);
    String expectedLog4jExecPath = HiveTestUtils.getFileFromClasspath(hiveExecLog4jTest);
    System.setProperty(ConfVars.HIVE_LOG4J_FILE.varname, expectedLog4jTestPath);
    System.setProperty(ConfVars.HIVE_EXEC_LOG4J_FILE.varname, expectedLog4jExecPath);

    LogUtils.initHiveLog4j();

    HiveConf conf = new HiveConf();
    assertEquals(expectedLog4jTestPath, conf.getVar(ConfVars.HIVE_LOG4J_FILE));
    assertEquals(expectedLog4jExecPath, conf.getVar(ConfVars.HIVE_EXEC_LOG4J_FILE));
  }

  private void runCmd(String cmd) throws Exception {
    process = runTime.exec(cmd);
    process.waitFor();
  }

  private void getCmdOutput(String logFile) throws Exception {
    boolean logCreated = false;
    BufferedReader buf = new BufferedReader(
      new InputStreamReader(process.getInputStream()));
    String line = "";
    while((line = buf.readLine()) != null) {
      if (line.equals(logFile))
        logCreated = true;
    }
    assertEquals(true, logCreated);
  }

  private void RunTest(String cleanCmd, String findCmd, String logFile,
    String hiveLog4jProperty, String hiveExecLog4jProperty) throws Exception {
    // clean test space
    runCmd(cleanCmd);

    // config log4j with customized files
    // check whether HiveConf initialize log4j correctly
    configLog(hiveLog4jProperty, hiveExecLog4jProperty);

    // check whether log file is created on test running
    runCmd(findCmd);
    getCmdOutput(logFile);

    // clean test space
    runCmd(cleanCmd);
  }

  public void testHiveLogging() throws Exception {
    // customized log4j config log file to be: /tmp/TestHiveLogging/hiveLog4jTest.log
    String customLogPath = "/tmp/" + System.getProperty("user.name") + "-TestHiveLogging/";
    String customLogName = "hiveLog4jTest.log";
    String customLogFile = customLogPath + customLogName;
    String customCleanCmd = "rm -rf " + customLogFile;
    String customFindCmd = "find " + customLogPath + " -name " + customLogName;
    RunTest(customCleanCmd, customFindCmd, customLogFile,
      "hive-log4j-test.properties", "hive-exec-log4j-test.properties");
  }
}
