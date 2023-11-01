/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.apps.sale.web;

import com.axelor.apps.ReportFactory;
import com.axelor.apps.base.AxelorException;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.SaleReport;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.inject.Beans;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SaleReportController {

  public void printReport(ActionRequest request, ActionResponse response) throws AxelorException {
    SaleReport saleReport = request.getContext().asType(SaleReport.class);
    String reportName = saleReport.getReportType();

    String idsStr = "";
    List<SaleOrder> saleOrderList = Beans.get(SaleOrderRepository.class).all().fetch();
    List<Long> idList = saleOrderList.stream().map(SaleOrder::getId).collect(Collectors.toList());
    idsStr = Joiner.on(",").join(idList);
    System.err.println(idsStr);

    LocalDate fromDate = saleReport.getFromDate();
    LocalDate toDate = saleReport.getToDate();

    String fileLink =
        ReportFactory.createReport(reportName + ".rptdesign", reportName + "-${date}")
            .addParam("ids", idsStr)
            .addParam("fromDate", fromDate.toString())
            .addParam("toDate", toDate.toString())
            .generate()
            .getFileLink();

    response.setView(ActionView.define("Sale Report").add("html", fileLink).map());
  }
}
