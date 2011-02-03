/*
 * Sonar JMeter Plugin
 * Copyright (C) 2010 eXcentia
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package es.excentia.sonar.jmeter;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Project;

import es.excentia.jmeter.report.client.JMeterReportClient;
import es.excentia.jmeter.report.client.JMeterReportConst;
import es.excentia.jmeter.report.client.data.GlobalSummary;
import es.excentia.sonar.jmeter.exception.JMeterPluginException;

public class JMeterSensor implements Sensor { // , GeneratesViolations {

  public static final Logger LOG = LoggerFactory.getLogger(JMeterSensor.class);

  public boolean shouldExecuteOnProject(Project project) {
    // this sensor is executed only if test config is set
    String config = (String) project
        .getProperty(JMeterPluginConst.CONFIG_PROPERTY);
    return StringUtils.isNotBlank(config);
  }

  public void analyse(Project project, SensorContext context) {

    LOG.debug("START JMeterSensor");

    try {

      JMeterReportClient client = getReportClient(project);
      String config = getTestConfigName(project);

      GlobalSummary summary = client.getGlobalSummary(config);

      context.saveMeasure(JMeterMetrics.requestErrorPercent, summary
          .getRequestsErrorPercent());
      context.saveMeasure(new Measure(JMeterMetrics.testDesc, summary
          .getTestDesc()));
      context.saveMeasure(JMeterMetrics.duration, new Double(summary
          .getTestDuration()));
      context.saveMeasure(JMeterMetrics.usersLogged, new Double(summary
          .getUsersLogged()));
      context.saveMeasure(JMeterMetrics.requestTotal, new Double(summary
          .getRequestsTotal()));
      context.saveMeasure(JMeterMetrics.transTotal, new Double(summary
          .getTransTotal()));

      if (summary.getRequestsTotal() > 0) {
        context.saveMeasure(JMeterMetrics.requestResponseTimeOkAvg, new Double(
            summary.getRequestsResponseTimeOkAvg()));
        context.saveMeasure(JMeterMetrics.requestResponseTimeOkDevPercent,
            new Double(summary.getRequestsResponseTimeOkAvgDevPercent()));
        context.saveMeasure(JMeterMetrics.requestOkPerMinute, new Double(
            summary.getRequestsOkPerMinute()));
        context.saveMeasure(JMeterMetrics.requestOkPerMinuteAndUser,
            new Double(summary.getRequestsOkPerMinuteAndUser()));
      }

      if (summary.getTransTotal() > 0) {
        context.saveMeasure(JMeterMetrics.transResponseTimeOkAvg, new Double(
            summary.getTransResponseTimeOkAvg()));
        context.saveMeasure(JMeterMetrics.transResponseTimeOkDevPercent,
            new Double(summary.getTransBytesOkAvgDevPercent()));
        context.saveMeasure(JMeterMetrics.transOkPerMinute, new Double(summary
            .getTransOkPerMinute()));
        context.saveMeasure(JMeterMetrics.transOkPerMinuteAndUser, new Double(
            summary.getTransOkPerMinuteAndUser()));

        // transMapResponseTimeOkAvg
        PropertiesBuilder<String, Double> transMapResponseTimeOkAvgPropBuild = new PropertiesBuilder<String, Double>(
            JMeterMetrics.transMapResponseTimeOkAvg, summary
                .getTransMapResponseTimeOkAvg());
        context.saveMeasure(new Measure(
            JMeterMetrics.transMapResponseTimeOkAvg,
            transMapResponseTimeOkAvgPropBuild.buildData()));

        // transMapResponseTimeOkDevPercent
        PropertiesBuilder<String, Double> transMapResponseTimeOkDevPropBuild = new PropertiesBuilder<String, Double>(
            JMeterMetrics.transMapResponseTimeOkDevPercent, summary
                .getTransMapResponseTimeOkAvgDevPercent());
        context.saveMeasure(new Measure(
            JMeterMetrics.transMapResponseTimeOkDevPercent,
            transMapResponseTimeOkDevPropBuild.buildData()));
      }

    } catch (Exception e) {
      LOG.error("Cannot analyse project '" + project.getName() + "'", e);
    }

    LOG.debug("END JMeterSensor");
  }

  /**
   * Gets a jmeter report client connected to the configured jmeter report
   * server
   */
  public JMeterReportClient getReportClient(Project project) {

    String host = (String) project.getProperty(JMeterPluginConst.HOST_PROPERTY);
    if (StringUtils.isBlank(host)) {
      throw new JMeterPluginException(
          "You must set the HOST in sonar-jmeter-plugin config "
              + "for the project '" + project.getName() + "'");
    }

    String port = (String) project.getProperty(JMeterPluginConst.PORT_PROPERTY);
    if (StringUtils.isBlank(port) || Integer.parseInt(port) == 0) {
      LOG.warn("Null or invalid jmeter-report-server PORT. "
          + "Using default '{}'", JMeterReportConst.DEFAULT_PORT);
      port = Integer.toString(JMeterReportConst.DEFAULT_PORT);
    }

    return new JMeterReportClient(host, Integer.parseInt(port));
  }

  /**
   * Gets the configured jmeter test configuration name
   */
  public String getTestConfigName(Project project) {
    String config = (String) project
        .getProperty(JMeterPluginConst.CONFIG_PROPERTY);
    if (StringUtils.isBlank(config)) {
      throw new JMeterPluginException(
          "You must set test CONFIG in sonar-jmeter-plugin "
              + "for the project '" + project.getName() + "'");
    } else {
      return config;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
