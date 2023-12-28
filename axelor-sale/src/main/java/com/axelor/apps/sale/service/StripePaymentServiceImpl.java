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
import com.axelor.apps.sale.db.StripePaymentConfig;
import com.axelor.apps.sale.db.StripePaymentLine;
import com.axelor.apps.sale.db.repo.StripePaymentConfigRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class StripePaymentServiceImpl implements StripePaymentService {

  @Transactional
  @Override
  public List<StripePaymentLine> getStripePaymentRecord(SaleOrder saleOrder) throws AxelorException {
    List<StripePaymentLine> stripePaymentLineList = new ArrayList<StripePaymentLine>();

    String url = "";
    StripePaymentConfig stripePaymentConfig =
        Beans.get(StripePaymentConfigRepository.class).all().fetchOne();
    
    if(stripePaymentConfig.getRetrivePaymentBaseUrl() == "" || stripePaymentConfig.getRetrivePaymentBaseUrl() == null) {
    	throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, "Please configure stripe payment URL.");
    }
    
    url = stripePaymentConfig.getRetrivePaymentBaseUrl();
    url =
        url
            + "/api/payments/data/"
            + saleOrder.getId(); // "https://axelor-api.zdental.com/api/payments/data/24218"

    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    HttpGet httpRequest = new HttpGet(url);
    httpRequest.setHeader("X-Api-Key", "justtestkey");

    try {
      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            JSONArray paymentArray = jsonObj.getJSONArray("payment");

            for (int i = 0; i < paymentArray.length(); i++) {
              StripePaymentLine stripePaymentLine = new StripePaymentLine();
              JSONObject paymentObject = paymentArray.getJSONObject(i);
              stripePaymentLine.setStripeId((String) paymentObject.get("id").toString());
              stripePaymentLine.setOrderId(paymentObject.get("order_id").toString());
              stripePaymentLine.setPaymentId(paymentObject.get("payment_id").toString());
              stripePaymentLine.setCustomerId(paymentObject.get("customer_id").toString());
              stripePaymentLine.setCustomerName(paymentObject.get("customer_name").toString());
              stripePaymentLine.setCustomerEmail(paymentObject.get("customer_email").toString());
              stripePaymentLine.setCost(paymentObject.get("cost").toString());
              stripePaymentLine.setStatus(paymentObject.get("status").toString());
              stripePaymentLine.setCard(paymentObject.get("card").toString());
              stripePaymentLine.setStripeCreatedAt(paymentObject.get("created_at").toString());
              stripePaymentLine.setStripeUpdatedAt(paymentObject.get("updated_at").toString());

              stripePaymentLineList.add(stripePaymentLine);
            }
          }
        } else {
        }
      }
    } catch (Exception e) {
    	throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, e.toString());
    }

    return stripePaymentLineList;
  }
}
