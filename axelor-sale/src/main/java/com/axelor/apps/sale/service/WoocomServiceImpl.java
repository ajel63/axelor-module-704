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
package com.axelor.apps.sale.service;

import com.axelor.apps.base.AxelorException;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.ShipmentApiConfig;
import com.axelor.apps.sale.db.repo.ShipmentApiConfigRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class WoocomServiceImpl implements WoocomService {

  @Transactional
  @Override
  public String updateStatusOnWoocom(SaleOrder saleOrder) throws AxelorException {

    String url = "";
    String token = "";
    ShipmentApiConfig shipmentApiConfig =
        Beans.get(ShipmentApiConfigRepository.class).all().fetchOne();
    url = shipmentApiConfig.getBaseUrl();
    token = shipmentApiConfig.getApiKey();

    if (url == null || url == "" || token == null || token == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please configure WooComerce API or Shipmet API: base Urls and API key.");
    }

    if (saleOrder.getWoocommerceOrder() == null || saleOrder.getWoocommerceOrder() == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please Add WooComerce Id on SaleOrder for update status on woocommerce.");
    }

    String woocommerceStatus = "";

    if (saleOrder.getStatusSelect() == 2) {
      woocommerceStatus = "pending";
    } else if (saleOrder.getStatusSelect() == 3) {
      woocommerceStatus = "processing";
    } else if (saleOrder.getStatusSelect() == 4) {
      woocommerceStatus = "completed";
    } else if (saleOrder.getStatusSelect() == 5) {
      woocommerceStatus = "cancelled";
    }

    url =
        url
            + "/orders/woocommerce/"
            + saleOrder
                .getWoocommerceOrder(); // "https://axelor-api.zdental.com/orders/woocommerce/24218"
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPut httpRequest = new HttpPut(url);
    httpRequest.setHeader("X-Api-Key", token);
    httpRequest.addHeader("Content-Type", "application/json");

    try {
      StringEntity params = new StringEntity("{\"status\":\"" + woocommerceStatus + "\"}");
      httpRequest.setEntity(params);

      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        System.err.println(result);
        JSONObject jsonObj = null;
        try {
          jsonObj = new JSONObject(result);
        } catch (Exception e) {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_NO_VALUE,
              "Status does not update in Woocommerce system due to invalid order ID please update menually.");
        }
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            return "Woocommerce status updated.";
          }
        } else {
          throw new AxelorException(
              TraceBackRepository.CATEGORY_NO_VALUE,
              "Status does not update in Woocommerce system please update menually.");
        }
      }
    } catch (Exception e) {
      System.err.println(e);
    }
    return "";
  }
}
