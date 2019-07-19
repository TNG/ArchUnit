package com.tngtech.archunit.example.cycles.simplescenario.administration;

import java.util.UUID;

import com.tngtech.archunit.example.cycles.simplescenario.report.Report;
import com.tngtech.archunit.example.cycles.simplescenario.report.ReportService;

public class AdministrationService {
    private ReportService reportService;

    public void saveNewInvoice(Invoice invoice) {
        Report report = reportService.getReport(invoice.getCustomer());
        if (!report.isEmpty()) {
            throw new IllegalArgumentException("Invoice " + invoice + " is not new");
        }
        // save whatever
    }

    public UUID createCustomerId(String customer) {
        return UUID.randomUUID();
    }
}
