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
import com.axelor.apps.sale.db.ShipmentApiConfig;
import com.axelor.apps.sale.db.ShippService;
import com.axelor.apps.sale.db.repo.ShipmentApiConfigRepository;
import com.axelor.apps.sale.db.repo.ShippServiceRepository;
import com.axelor.inject.Beans;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class PurchaseLableServiceImpl implements PurchaseLableService {

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
    String addressStr2 = address.getAddressL3();

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

    String customerName = saleOrder.getClientPartner().getSimpleFullName();
    String company = saleOrder.getCompany().getName();

    List<PurchaseLabelRateLine> purchaseLabelRateLineList = new ArrayList<PurchaseLabelRateLine>();
    String url = "";
    String token = "";
    ShipmentApiConfig shipmentApiConfig =
        Beans.get(ShipmentApiConfigRepository.class).all().fetchOne();
    if (shipmentApiConfig == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please configure Shipment API: base Urls and API key.");
    }
    url = shipmentApiConfig.getBaseUrl();
    token = shipmentApiConfig.getApiKey();

    if (url == null || url == "" || token == null || token == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please configure Shipment API: base Urls and API key.");
    }

    url = url + "/shipping/cost";

    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost httpRequest = new HttpPost(url);
    httpRequest.setHeader("X-Api-Key", token);
    httpRequest.addHeader("Content-Type", "application/json");
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
                  + "\",\"address2\":\""
                  + addressStr2
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

  @Override
  public Map<String, String> confirmShippingService(
      PurchaseLabel purchaseLabel, SaleOrder saleOrder) throws AxelorException {

    Map<String, String> map = new HashMap<>();
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
    String addressStr2 = address.getAddressL3();

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

    String customerName = saleOrder.getClientPartner().getSimpleFullName();
    Long custumerId = saleOrder.getClientPartner().getId();
    String company = saleOrder.getCompany().getName();
    Long orderId = saleOrder.getId();

    String carrier = "";
    String carrierService = "";
    String carrierServiceToken = "";

    for (PurchaseLabelRateLine purchaseLabelRateLine : purchaseLabel.getPurchaseLabelRateLine()) {
      if (purchaseLabelRateLine.getIsServiceSelected()) {
        carrier = purchaseLabelRateLine.getCarrier();
        carrierService = purchaseLabelRateLine.getCarrierService();
        carrierServiceToken = purchaseLabelRateLine.getCarrierServiceToken();
      }
    }

    if (carrier == ""
        || carrier == null
        || carrierService == ""
        || carrierService == null
        || carrierServiceToken == ""
        || carrierServiceToken == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Carrier, Carrier Service or Carrier Service token not found in request.");
    }

    String url = "";
    String token = "";
    ShipmentApiConfig shipmentApiConfig =
        Beans.get(ShipmentApiConfigRepository.class).all().fetchOne();
    url = shipmentApiConfig.getBaseUrl();
    token = shipmentApiConfig.getApiKey();

    if (url == null || url == "" || token == null || token == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please configure Shipment API: base Urls and API key.");
    }

    url = url + "/shipping/order";
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost httpRequest = new HttpPost(url);

    try {
      String str =
          "{\"weight\":\""
              + saleOrder.getWeight()
              + "\",\"length\":\""
              + saleOrder.getSizeL()
              + "\",\"width\":\""
              + saleOrder.getSizeW()
              + "\",\"height\":\""
              + saleOrder.getSizeH()
              + "\",\"city\":\""
              + city
              + "\",\"customer\":\""
              + custumerId
              + "\",\"name\":\""
              + customerName
              + "\",\"company\":\""
              + company
              + "\",\"address\":\""
              + addressStr
              + "\",\"address2\":\""
              + addressStr2
              + "\",\"zipCode\":\""
              + pincode
              + "\",\"state\":\""
              + state
              + "\",\"phone\":\""
              + purchaseLabel.getPhoneNumber()
              + "\",\"mail\":\""
              + purchaseLabel.getEmailAddress()
              + "\",\"orderId\":"
              + orderId
              + ",\"carrier\":\""
              + carrier.toLowerCase()
              + "\",\"servicelevel_token\":\""
              + carrierServiceToken
              + "\",\"serviceName\":\""
              + carrierService
              + "\"}";

      StringEntity params = new StringEntity(str);
      httpRequest.addHeader("Content-Type", "application/json");
      httpRequest.setHeader("X-Api-Key", token);
      httpRequest.setEntity(params);
      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            JSONObject dataObj = (JSONObject) jsonObj.get("data");
            JSONObject shippingOrderObj = (JSONObject) dataObj.get("shippingOrder");
            JSONObject shippoOrderObj = (JSONObject) dataObj.get("shippoOrder");

            String trackingNumber = (String) shippingOrderObj.get("trackingNumber").toString();
            String labelUrl = (String) shippoOrderObj.get("label_url").toString();

            if (trackingNumber == ""
                || trackingNumber == null
                || labelUrl == ""
                || labelUrl == null) {
              String errMessage = shippoOrderObj.get("message").toString();
              throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, errMessage);
            }

            map.put("trackingNumber", trackingNumber);
            map.put("labelUrl", labelUrl);

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
    return map;
  }

  @Transactional
  @Override
  public ShippService setSelectedCarrier(PurchaseLabel purchaseLabel) {
    ShippService shippService = null;
    for (PurchaseLabelRateLine purchaseLabelRateLine : purchaseLabel.getPurchaseLabelRateLine()) {
      if (purchaseLabelRateLine.getIsServiceSelected()) {
        shippService =
            Beans.get(ShippServiceRepository.class).findByName(purchaseLabelRateLine.getCarrier());
        if (shippService == null) {
          shippService = new ShippService();
          shippService.setName(purchaseLabelRateLine.getCarrier());
          shippService.setCode(purchaseLabelRateLine.getCarrier());
          shippService = Beans.get(ShippServiceRepository.class).save(shippService);
        }
      }
    }
    return shippService;
  }

  @Override
  public BigDecimal getSelectedCarrierPrice(PurchaseLabel purchaseLabel) {
    for (PurchaseLabelRateLine purchaseLabelRateLine : purchaseLabel.getPurchaseLabelRateLine()) {
      if (purchaseLabelRateLine.getIsServiceSelected()) {
        return purchaseLabelRateLine.getRate();
      }
    }
    return new BigDecimal(0);
  }
}
