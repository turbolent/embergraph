/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import java.util.StringTokenizer;

/*
 * Parses the Linux environment--Uses the proc filesystem to determine all the CPU information.
 *
 * @author <a href="mailto:dev@avalon.apache.org">Avalon Development Team</a>
 * @version $Id$
 */
public final class Linux implements CPUParser {
  private final int m_processors;
  private final String m_cpuInfo;

  public Linux() {
    int procs = 1;
    String info = "";

    try {
      BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
      procs = 0;

      Properties props = new Properties();
      String line = null;

      while ((line = reader.readLine()) != null) {
        String[] args = split(line, ":\t");

        if (args.length > 1) {
          props.setProperty(args[0].trim(), args[1].trim());
          if (args[0].trim().equals("processor")) {
            procs++;
          }
        }
      }

      String buf = props.getProperty("model name")
          + " Family "
          + props.getProperty("cpu family")
          + " Model "
          + props.getProperty("model")
          + " Stepping "
          + props.getProperty("stepping")
          + ", "
          + props.getProperty("vendor_id");
      info = buf;
    } catch (Exception e) {
      procs = 1;
      e.printStackTrace();
    }

    m_processors = procs;
    m_cpuInfo = info;
  }

  /** Return the number of processors available on the machine */
  public int numProcessors() {
    return m_processors;
  }

  /*
   * Return the cpu info for the processors (assuming symetric multiprocessing which means that all
   * CPUs are identical). The format is:
   *
   * <p>${arch} family ${family} Model ${model} Stepping ${stepping}, ${identifier}
   */
  public String cpuInfo() {
    return m_cpuInfo;
  }

  /*
   * Splits the string on every token into an array of strings.
   *
   * @param string the string
   * @param onToken the token
   * @return the resultant array
   */
  private static String[] split(final String string, final String onToken) {
    final StringTokenizer tokenizer = new StringTokenizer(string, onToken);
    final String[] result = new String[tokenizer.countTokens()];

    for (int i = 0; i < result.length; i++) {
      result[i] = tokenizer.nextToken();
    }

    return result;
  }
}
