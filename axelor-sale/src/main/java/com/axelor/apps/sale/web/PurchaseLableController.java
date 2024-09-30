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
import com.axelor.apps.sale.db.MultiShipmentPackageLine;
import com.axelor.apps.sale.db.PurchaseLabel;
import com.axelor.apps.sale.db.PurchaseLabelRateLine;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.ShipmentLine;
import com.axelor.apps.sale.db.ShippService;
import com.axelor.apps.sale.db.repo.PurchaseLabelRepository;
import com.axelor.apps.sale.db.repo.SaleOrderRepository;
import com.axelor.apps.sale.db.repo.ShipmentLineRepository;
import com.axelor.apps.sale.service.PurchaseLableService;
import com.axelor.inject.Beans;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;
import com.google.inject.Singleton;
import java.math.BigDecimal;
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
      response.setValue("phoneNumber", partner.getFixedPhone());
      response.setValue("emailAddress", emailAddress);
    }
  }

  public void confirmShippingServiceValidate(ActionRequest request, ActionResponse response) {
    Context context = request.getContext();
    PurchaseLabel purchaseLabel = context.asType(PurchaseLabel.class);

    int serviceCount = 0;
    for (PurchaseLabelRateLine purchaseLabelRateLine : purchaseLabel.getPurchaseLabelRateLine()) {
      if (purchaseLabelRateLine.getIsServiceSelected()) {
        serviceCount = serviceCount + 1;
      }
    }

    if (serviceCount == 0) {
      response.setError("Please Select the shipping service.");
    }

    if (serviceCount > 1) {
      response.setError("Please Select only one shippig service.");
    }
  }

  public void confirmShippingService(ActionRequest request, ActionResponse response)
      throws AxelorException {
    Context context = request.getContext();
    PurchaseLabel purchaseLabel = request.getContext().asType(PurchaseLabel.class);

    SaleOrder saleOrder = null;
    if (context.get("_parent") != null) {

      Map<String, Object> _parent = (Map<String, Object>) context.get("_parent");

      String id = _parent.get("id").toString();
      saleOrder = Beans.get(SaleOrderRepository.class).find(Long.parseLong(id));
    }

    List<MultiShipmentPackageLine> multiShipmentPackageLine =
        Beans.get(PurchaseLableService.class)
            .confirmShippingService(
                Beans.get(PurchaseLabelRepository.class).find(purchaseLabel.getId()), saleOrder);

    if (!multiShipmentPackageLine.isEmpty()) {
      response.setValue("multiShipmentPackageLine", multiShipmentPackageLine);
      response.setValue("isShippingConfirm", true);
    }
    //    if (map.size() > 0) {
    //      response.setValue("trackingNumber", map.get("trackingNumber"));
    //      response.setValue("lableUrl", map.get("labelUrl"));
    //      response.setValue("isShippingConfirm", true);
    //    }
  }

  public void setSelectedCarrier(ActionRequest request, ActionResponse response)
      throws AxelorException {
    PurchaseLabel purchaseLabel = request.getContext().asType(PurchaseLabel.class);
    ShippService shiSservice =
        Beans.get(PurchaseLableService.class)
            .setSelectedCarrier(
                Beans.get(PurchaseLabelRepository.class).find(purchaseLabel.getId()));
    BigDecimal selectedRate =
        Beans.get(PurchaseLableService.class)
            .getSelectedCarrierPrice(
                Beans.get(PurchaseLabelRepository.class).find(purchaseLabel.getId()));
    response.setValue("carrier", shiSservice);
    response.setValue("selectedRate", selectedRate);
    response.setValue(
        "totalMultiShipmentCost",
        selectedRate.multiply(new BigDecimal(purchaseLabel.getTotalPackage())));
  }

  public void openPrintingLable(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ShipmentLine shipmentLine = request.getContext().asType(ShipmentLine.class);

    String os = System.getProperty("os.name").toLowerCase();

    try {
      if (os.contains("win")) {
        // For Windows
        Runtime.getRuntime()
            .exec("rundll32 url.dll,FileProtocolHandler " + shipmentLine.getLableUrl());
      } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
        // For Unix/Linux/Mac
        Runtime.getRuntime().exec("xdg-open " + shipmentLine.getLableUrl());
      } else {
        System.out.println("Unsupported operating system: " + os);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendEmail(ActionRequest request, ActionResponse response) throws AxelorException {
    ShipmentLine shipmentLine = request.getContext().asType(ShipmentLine.class);

    String responsStr =
        Beans.get(PurchaseLableService.class)
            .sendEmailApi(Beans.get(ShipmentLineRepository.class).find(shipmentLine.getId()));

    if (responsStr.equals("Email send successfully")) {
      response.setValue("isEmailSent", true);
      response.setNotify(responsStr);
    } else {
      response.setError(responsStr);
    }
  }

  public void returnPurchaseLable(ActionRequest request, ActionResponse response)
      throws AxelorException {
    ShipmentLine shipmentLine = request.getContext().asType(ShipmentLine.class);

    Boolean returnedLable =
        Beans.get(PurchaseLableService.class)
            .returnShipmentLable(
                Beans.get(ShipmentLineRepository.class).find(shipmentLine.getId()));
    response.setValue("isReturnConfirm", returnedLable);
  }
}
