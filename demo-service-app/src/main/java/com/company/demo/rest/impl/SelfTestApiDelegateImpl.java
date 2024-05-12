package com.company.demo.rest.impl;

import com.x.openapi.template.generated.api.SelfTestApiDelegate;
import com.x.openapi.template.generated.model.ServiceFeaturesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;

@Service
public class SelfTestApiDelegateImpl implements SelfTestApiDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelfTestApiDelegateImpl.class);

  protected static final String MODE_MONITOR = "monitor";
  protected static final String MODE_INFO = "info";
  protected static final String MODE_PERFORMANCE = "perf";

//@formatter:off
  private static final String HTML_BODY_START = "<html>"
      + "<head>"
      + "<title>Selftest</title>"
      + "</head>"
      + "<body>"
      + "<span style='font:12px Arial,Helvetica,sans-serif;'>";
//@formatter:on

  private static final String OK = "<span style=\"font-weight:bold;color:green;\">OK</span>";
  private static final String KO = "<span style=\"font-weight:bold;color:saddlebrown;\">KO</span>";
  private static final String HTML_BODY_END = "</span></body></html>";

  private static final String TYPE_JMX_MEMORY_POOL = "MemoryPool";
  private static final String[] MEMORY_POOL_ATTRIBUTES = { "init", "committed", "used", "max" };

  private static final String TYPE_JMX_OS = "OperatingSystem";
  private static final String[] OS_ATTRIBUTES = { "SystemCpuLoad", "ProcessCpuLoad", "CommittedVirtualMemorySize", "ProcessCpuTime", "FreePhysicalMemorySize", "TotalPhysicalMemorySize" };

  private final Object lockObj = new Object();

  @Override
  public ResponseEntity<String> selfTest(String mode) {
    long ms = System.currentTimeMillis();

    StringBuilder buff = new StringBuilder();
    buff.append("#SELFTEST#;");

    if (mode == null || MODE_MONITOR.equalsIgnoreCase(mode)) {
      buff.append("MONITOR;");
      return processMonitorMode();

    } else if (MODE_INFO.equalsIgnoreCase(mode)) {
      buff.append("INFO;");
      return processInfoMode();

    } else if (MODE_PERFORMANCE.equalsIgnoreCase(mode)) {
      buff.append("PERFORMANCE;");
      return processPerformanceMode();

    } else {
      buff.append("MODE NOT RECOGNIZED: " + mode);
    }

    buff.append(System.currentTimeMillis() - ms + " ms");
    LOGGER.info(buff.toString());

    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<String> processPerformanceMode() {
    StringBuilder output = new StringBuilder();
    HttpStatus resultStatus = HttpStatus.OK;

    try {
      StringBuilder buff4Rt = new StringBuilder();
      StringBuilder buff4Mem = new StringBuilder();
      StringBuilder buff4OS = new StringBuilder();

      printString(HTML_BODY_START, output);
      printString("<H2>Performance Mode</H2>", output);

      buff4Rt.append("<div><table>");
      buff4Rt.append("<tr><th colspan='2' align='left'>Runtime</th></tr>");

      RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
      buff4Rt.append("<tr><td>VM</td><td>[" + runtimeMx.getVmName() + " - " + runtimeMx.getVmVendor() + " - " + runtimeMx.getVmVersion() + "]</td></tr>");
      buff4Rt.append("<tr><td>Spec</td><td>[" + runtimeMx.getSpecName() + " - " + runtimeMx.getSpecVendor() + " - " + runtimeMx.getSpecVersion() + "]</td></tr>");

      Runtime runtime = Runtime.getRuntime();
      buff4Rt.append("<tr><td>Available Processors</td><td>[" + runtime.availableProcessors() + "]</td></tr>");
      buff4Rt.append("<tr><td>Total Memory</td><td>[" + ((runtime.totalMemory()) / (1024 * 1024)) + " MB]</td></tr>");
      buff4Rt.append("<tr><td>Max Memory</td><td>[" + ((runtime.maxMemory()) / (1024 * 1024)) + " MB]</td></tr>");
      buff4Rt.append("<tr><td>Available Memory</td><td>[" + ((runtime.freeMemory()) / (1024 * 1024)) + " MB]</td></tr>");
      buff4Rt.append("</table></div></br>");

      ArrayList<MBeanServer> list = MBeanServerFactory.findMBeanServer(null);

      MBeanServer mbeanServer = null;

      if (!list.isEmpty()) {
        mbeanServer = list.get(0);
      } else {
        mbeanServer = ManagementFactory.getPlatformMBeanServer();
      }

      if (mbeanServer != null) {
        Set<ObjectName> namesSet = mbeanServer.queryNames(null, null);

        for (ObjectName objName : namesSet) {
          String canonName = objName.getCanonicalName();

          if (canonName.contains("type=" + TYPE_JMX_MEMORY_POOL)) {
            Hashtable<String, String> propsList = objName.getKeyPropertyList();

            for (String ky : propsList.keySet()) {
              if ("name".equalsIgnoreCase(ky)) {
                try {
                  CompositeDataSupport attr = (CompositeDataSupport) mbeanServer.getAttribute(objName, "Usage");

                  buff4Mem.append("<div><table>");
                  buff4Mem.append("<tr><th colspan='2' align='left'>" + TYPE_JMX_MEMORY_POOL + " " + propsList.get(ky) + "</th></tr>");

                  List<String> attribs = Arrays.asList(MEMORY_POOL_ATTRIBUTES);

                  for (String attrName : attribs) {
                    Long someData = ((Long) attr.get(attrName)) / (1024 * 1024); // in mb
                    buff4Mem.append("<tr><td>Usage " + attrName + "</td><td>[" + someData + " MB]</td></tr>");
                  }

                  buff4Mem.append("<tr><td colspan='2'></td></tr>");

                  attr = (CompositeDataSupport) mbeanServer.getAttribute(objName, "PeakUsage");

                  for (String attrName : attribs) {
                    Long someData = ((Long) attr.get(attrName)) / (1024 * 1024); // in mb
                    buff4Mem.append("<tr><td>PeakUsage " + attrName + "</td><td>[" + someData + " MB]</td></tr>");
                  }

                  buff4Mem.append("</table></div></br>");

                } catch (Exception e) {
                  LOGGER.warn("Exception", e);
                }
              }
            }
          } else if (canonName.contains("type=" + TYPE_JMX_OS)) {
            buff4OS.append("<div><table>");
            buff4OS.append("<tr><th colspan='2' align='left'>" + TYPE_JMX_OS + "</th></tr>");

            try {
              for (String attrName : OS_ATTRIBUTES) {
                Object attr = mbeanServer.getAttribute(objName, attrName);
                String attrString = attr.toString();

                if ((attr instanceof Long) && (attrName.endsWith("MemorySize"))) {
                  attrString = String.valueOf(((Long) attr / (1024 * 1024))) + " MB"; // in mb
                }

                buff4OS.append("<tr><td>" + attrName + "</td><td>[" + attrString + "]</td></tr>");
              }
            } catch (Exception e) {
              LOGGER.warn("Exception", e);
            }

            buff4OS.append("</table></div></br>");
          }
        }
      }

      printString(buff4OS.toString(), output);
      printString(buff4Rt.toString(), output);
      printString(buff4Mem.toString(), output);

    } catch (Exception ex) {
      LOGGER.warn("An Exception occured:", ex);
      printString("An Exception occured:" + ex, output);
      resultStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    printString(HTML_BODY_END, output);
    String result = output.toString();
    return new ResponseEntity<>(result, resultStatus);
  }

  private ResponseEntity<String> processInfoMode() {
    StringBuilder output = new StringBuilder();
    HttpStatus resultStatus = HttpStatus.OK;

    try {
      printString(HTML_BODY_START, output);
      printString("<H2>Info Mode</H2>", output);;

        List<ServiceFeaturesDto> serviceFeaturesDtos = new ArrayList<>(); // get the service features

        if (serviceFeaturesDtos.isEmpty()) {
          output.append("<br/>service_features fetched: " + KO);
        } else {
          output.append("<br/>service_features fetched: " + OK);
        }

    } catch (Exception ex) {
      LOGGER.warn("An Exception occured:", ex);
      printString("An Exception occured:" + ex, output);
      resultStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    printString(HTML_BODY_END, output);
    String result = output.toString();
    return new ResponseEntity<>(result, resultStatus);
  }

  private ResponseEntity<String> processMonitorMode() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  protected void printString(String data, StringBuilder output) {
    printString(data, null, output);
  }

  protected void printString(String data, Exception e, StringBuilder output) {
    if (output != null) {
      synchronized (lockObj) {
        output.append(data);

        if (null != e) {
          output.append(e.getMessage());
        }
      }
    }
  }
}
