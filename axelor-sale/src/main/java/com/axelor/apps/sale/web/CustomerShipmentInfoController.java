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

import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PartnerAddress;
import com.axelor.apps.sale.db.CustomerShipmentInfo;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class CustomerShipmentInfoController {

  public void setCustomerInvoiceAddress(ActionRequest request, ActionResponse response) {
    CustomerShipmentInfo customerShipmentInfo =
        request.getContext().asType(CustomerShipmentInfo.class);

    if (customerShipmentInfo.getCustomer() == null) {
      response.setValue("shipmentBillingAddress", null);
      return;
    }

    Partner customer = customerShipmentInfo.getCustomer();
    if (customer.getPartnerAddressList().size() == 0) {
      response.setValue("shipmentBillingAddress", null);
      return;
    }

    List<PartnerAddress> partnerAddressList = customer.getPartnerAddressList();
    Address address = null;
    for (PartnerAddress PartnerAddress : partnerAddressList) {
      address = PartnerAddress.getAddress();
      if (PartnerAddress.getIsInvoicingAddr()) {
        address = PartnerAddress.getAddress();
        break;
      }
    }

    response.setValue("shipmentBillingAddress", address);
  }
}
