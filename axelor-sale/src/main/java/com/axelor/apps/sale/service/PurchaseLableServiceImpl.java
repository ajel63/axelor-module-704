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
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.repo.TraceBackRepository;
import com.axelor.apps.sale.db.PurchaseLabel;
import com.axelor.apps.sale.db.PurchaseLabelRateLine;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.StripePaymentConfig;
import com.axelor.apps.sale.db.StripePaymentLine;
import com.axelor.apps.sale.db.repo.StripePaymentConfigRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PurchaseLableServiceImpl implements PurchaseLableService {

  @Transactional
  public List<StripePaymentLine> getStripePaymentRecord(SaleOrder saleOrder) {
    List<StripePaymentLine> stripePaymentLineList = new ArrayList<StripePaymentLine>();

    String url = "";
    StripePaymentConfig stripePaymentConfig =
        Beans.get(StripePaymentConfigRepository.class).all().fetchOne();
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
      System.err.println(e);
    }

    return stripePaymentLineList;
  }

  @Transactional
  @Override
  public List<PurchaseLabelRateLine> getShipmentRates(
      PurchaseLabel purchaseLabel, SaleOrder saleOrder) throws AxelorException {

    if (!(saleOrder.getWeight().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add weight in shipping tab.");
    }

    if (!(saleOrder.getSizeL().compareTo(new BigDecimal(0)) == 1)
        || !(saleOrder.getSizeW().compareTo(new BigDecimal(0)) == 1)
        || !(saleOrder.getSizeH().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add size of L, W and H values in shipping tab.");
    }

    if (saleOrder.getDeliveryAddress() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add Delivery address on Sale Order");
    }

    Address address = saleOrder.getDeliveryAddress();
    if (address.getPostalCode() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add postal code on Delivery address of Sale Order");
    }
    String pincode = address.getPostalCode().getCode();
    String city = address.getCityName();
    String state = address.getState();
    String addressStr = address.getAddressL4();

    if (pincode == null
        || pincode == ""
        || city == null
        || city == ""
        || state == null
        || state == ""
        || addressStr == null
        || addressStr == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add Address line, city, state and pincode on address");
    }

    String customerName = saleOrder.getClientPartner().getName();
    String company = saleOrder.getCompany().getName();

    List<PurchaseLabelRateLine> purchaseLabelRateLineList = new ArrayList<PurchaseLabelRateLine>();
    String url = "https://axelor-api.zdental.com/api/shipping/cost";

    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost httpRequest = new HttpPost(url);
    httpRequest.setHeader("X-Api-Key", "justtestkey");

    try {

      StringEntity params =
          new StringEntity(
              "{\"city\":\""
                  + city
                  + "\",\"name\":\""
                  + customerName
                  + "\",\"company\":\""
                  + company
                  + "\",\"address\":\""
                  + addressStr
                  + "\",\"zipCode\":\""
                  + pincode
                  + "\",\"state\":\""
                  + state
                  + "\",\"phone\":\""
                  + purchaseLabel.getPhoneNumber()
                  + "\",\"mail\":\""
                  + purchaseLabel.getEmailAddress()
                  + "\",\"weight\":\""
                  + saleOrder.getWeight()
                  + "\",\"length\":\""
                  + saleOrder.getSizeL()
                  + "\",\"width\":\""
                  + saleOrder.getSizeW()
                  + "\",\"height\":\""
                  + saleOrder.getSizeH()
                  + "\"}");
      httpRequest.addHeader("Content-Type", "application/json");
      httpRequest.setEntity(params);

      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            JSONObject shippingRatesObj = (JSONObject) jsonObj.get("shippingRates");

            JSONObject ratesObj = (JSONObject) shippingRatesObj.get("rates");

            Iterator<String> keysIterator = ratesObj.keys();

            while (keysIterator.hasNext()) {
              String serviceKey = keysIterator.next();

              JSONArray serviceTypeArray = (JSONArray) ratesObj.get(serviceKey);

              for (int i = 0; i < serviceTypeArray.length(); i++) {
                PurchaseLabelRateLine purchaseLabelRateLine = new PurchaseLabelRateLine();
                JSONObject serviceTypeObj = serviceTypeArray.getJSONObject(i);
                purchaseLabelRateLine.setCarrier(
                    (String) serviceTypeObj.get("provider").toString());

                JSONObject servicelevelObj = (JSONObject) serviceTypeObj.get("servicelevel");
                purchaseLabelRateLine.setCarrierService(
                    (String) servicelevelObj.get("name").toString());
                purchaseLabelRateLine.setCarrierServiceToken(
                    (String) servicelevelObj.get("token").toString());

                String amountStr = (String) serviceTypeObj.get("amount").toString();
                purchaseLabelRateLine.setRate(new BigDecimal(amountStr));

                purchaseLabelRateLineList.add(purchaseLabelRateLine);
              }
            }
          } else {
            if (jsonObj.has("type")) {
              if (jsonObj.get("type").equals("error")) {
                String errorMessage = jsonObj.get("message").toString();
                throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, errorMessage);
              }
            }
          }
        }
      }
    } catch (Exception e) {
      throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, e.toString());
    }

    return purchaseLabelRateLineList;
  }
}
