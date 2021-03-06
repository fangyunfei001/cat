package com.dianping.cat.report.task.reload.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import com.dianping.cat.configuration.NetworkInterfaceManager;
import com.dianping.cat.consumer.problem.ProblemAnalyzer;
import com.dianping.cat.consumer.problem.ProblemReportMerger;
import com.dianping.cat.consumer.problem.model.entity.ProblemReport;
import com.dianping.cat.consumer.problem.model.transform.DefaultNativeBuilder;
import com.dianping.cat.core.dal.HourlyReport;
import com.dianping.cat.report.ReportManager;
import com.dianping.cat.report.task.reload.AbstractReportReloader;
import com.dianping.cat.report.task.reload.ReportReloadEntity;
import com.dianping.cat.report.task.reload.ReportReloader;

@Named(type = ReportReloader.class, value = ProblemAnalyzer.ID)
public class ProblemReportReloader extends AbstractReportReloader {

	@Inject(ProblemAnalyzer.ID)
	protected ReportManager<ProblemReport> m_reportManager;

	private List<ProblemReport> buildMergedReports(Map<String, List<ProblemReport>> mergedReports) {
		List<ProblemReport> results = new ArrayList<ProblemReport>();

		for (Entry<String, List<ProblemReport>> entry : mergedReports.entrySet()) {
			String domain = entry.getKey();
			ProblemReport report = new ProblemReport(domain);
			ProblemReportMerger merger = new ProblemReportMerger(report);

			report.setStartTime(report.getStartTime());
			report.setEndTime(report.getEndTime());

			for (ProblemReport r : entry.getValue()) {
				r.accept(merger);
			}
			results.add(merger.getProblemReport());
		}

		return results;
	}

	@Override
	public String getId() {
		return ProblemAnalyzer.ID;
	}

	@Override
	public List<ReportReloadEntity> loadReport(long time) {
		List<ReportReloadEntity> results = new ArrayList<ReportReloadEntity>();
		Map<String, List<ProblemReport>> mergedReports = new HashMap<String, List<ProblemReport>>();

		for (int i = 0; i < getAnalyzerCount(); i++) {
			Map<String, ProblemReport> reports = m_reportManager.loadLocalReports(time, i);

			for (Entry<String, ProblemReport> entry : reports.entrySet()) {
				String domain = entry.getKey();
				ProblemReport r = entry.getValue();
				List<ProblemReport> rs = mergedReports.get(domain);

				if (rs == null) {
					rs = new ArrayList<ProblemReport>();

					mergedReports.put(domain, rs);
				}
				rs.add(r);
			}
		}

		List<ProblemReport> reports = buildMergedReports(mergedReports);

		for (ProblemReport r : reports) {
			HourlyReport report = new HourlyReport();

			report.setCreationDate(new Date());
			report.setDomain(r.getDomain());
			report.setIp(NetworkInterfaceManager.INSTANCE.getLocalHostAddress());
			report.setName(getId());
			report.setPeriod(new Date(time));
			report.setType(1);

			byte[] content = DefaultNativeBuilder.build(r);
			ReportReloadEntity entity = new ReportReloadEntity(report, content);

			results.add(entity);
		}
		return results;
	}
}
