package com.rentflow.report.port.in;

import com.rentflow.report.model.ExportReportCommand;

public interface ExportReportUseCase {
    byte[] export(ExportReportCommand command);
}
