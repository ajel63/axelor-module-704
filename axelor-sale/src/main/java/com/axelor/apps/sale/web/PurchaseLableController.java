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

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.axelor.apps.sale.db.PurchaseLabel;
import com.axelor.apps.sale.db.PurchaseLabelRateLine;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.repo.PurchaseLabelRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.service.PurchaseLableService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class PurchaseLableController {

  public void getPurchaseLableRates(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    PurchaseLabel purchaseLabel = request.getContext().asType(PurchaseLabel.class);

    SaleOrder saleOrder = null;
    if (context.get("_parent") != null) {

      Map<String, Object> _parent = (Map<String, Object>) context.get("_parent");

      String id = _parent.get("id").toString();
      saleOrder = Beans.get(SaleOrderRepository.class).find(Long.parseLong(id));
    }

    List<PurchaseLabelRateLine> purchaseLabelRateLine =
        Beans.get(PurchaseLableService.class)
            .getShipmentRates(
                Beans.get(PurchaseLabelRepository.class).find(purchaseLabel.getId()), saleOrder);

    response.setValue("purchaseLabelRateLine", purchaseLabelRateLine);
  }

  public void setAttributes(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    PurchaseLabel purchaseLabel = context.asType(PurchaseLabel.class);

    Partner partner = null;
    if (context.get("_parent") != null
        && ((Map) context.get("_parent")).get("clientPartner") != null) {

      Map<String, Object> _parent = (Map<String, Object>) context.get("_parent");

      partner =
          Beans.get(PartnerRepository.class)
              .find(Long.parseLong(((Map) _parent.get("clientPartner")).get("id").toString()));

      String emailAddress = "";
      if (partner.getEmailAddress() != null && partner.getEmailAddress().getAddress() != null) {
        emailAddress = partner.getEmailAddress().getAddress();
      }
      System.err.println("djnvk");
      response.setValue("phoneNumber", partner.getFixedPhone());
      response.setValue("emailAddress", emailAddress);
    }
  }
}
