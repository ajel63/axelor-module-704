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
import com.axelor.apps.sale.db.MultiShipmentPackageLine;
import com.axelor.apps.sale.db.PurchaseLabel;
import com.axelor.apps.sale.db.PurchaseLabelRateLine;
import com.axelor.apps.sale.db.SaleOrder;
import com.axelor.apps.sale.db.ShipmentApiConfig;
import com.axelor.apps.sale.db.ShipmentLine;
import com.axelor.apps.sale.db.ShippService;
import com.axelor.apps.sale.db.repo.ShipmentApiConfigRepository;
import com.axelor.apps.sale.db.repo.ShippServiceRepository;
import com.axelor.common.StringUtils;
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

    if (!(purchaseLabel.getWeight().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add weight in shipping tab.");
    }

    if (!(purchaseLabel.getSizeL().compareTo(new BigDecimal(0)) == 1)
        || !(purchaseLabel.getSizeW().compareTo(new BigDecimal(0)) == 1)
        || !(purchaseLabel.getSizeH().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add size of L, W and H values in shipping tab.");
    }

    if (purchaseLabel.getShippingToAddress() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add Shipping To Address");
    }

    Address address = purchaseLabel.getShippingToAddress();
    if (address.getPostalCode() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add postal code on Shipping To Address");
    }

    String RecipientDetails = address.getAddressL2();
    if (RecipientDetails == null || RecipientDetails == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add Recipient Details on Shiping To Address");
    }

    String pincode = address.getPostalCode().getCode();
    String city = address.getCityName();
    String state = address.getState();
    String addressStr = address.getAddressL4();
    String addressStr2 =
        (StringUtils.isBlank(address.getAddressL3())) ? "" : address.getAddressL3();

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
          "Please add Address line, city, state and pincode on Shipping TO address");
    }

    // String customerName = saleOrder.getClientPartner().getSimpleFullName();
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

    url = url + "/shipping/easypost/cost"; // url = url + "/shipping/cost"; // (Shipo)

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
                  + RecipientDetails
                  + "\",\"company\":\""
                  + company
                  + "\",\"address\":\""
                  + addressStr
                  + "\",\"address_2\":\""
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
                  + purchaseLabel.getWeight()
                  + "\",\"length\":\""
                  + purchaseLabel.getSizeL()
                  + "\",\"width\":\""
                  + purchaseLabel.getSizeW()
                  + "\",\"height\":\""
                  + purchaseLabel.getSizeH()
                  + "\"}");
      httpRequest.setEntity(params);

      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            JSONObject ratesObj = (JSONObject) jsonObj.get("shippingRates");

            //            JSONObject ratesObj = (JSONObject) shippingRatesObj.get("rates");

            Iterator<String> keysIterator = ratesObj.keys();

            while (keysIterator.hasNext()) {
              String serviceKey = keysIterator.next();

              JSONArray serviceTypeArray = (JSONArray) ratesObj.get(serviceKey);

              for (int i = 0; i < serviceTypeArray.length(); i++) {
                PurchaseLabelRateLine purchaseLabelRateLine = new PurchaseLabelRateLine();
                JSONObject serviceTypeObj = serviceTypeArray.getJSONObject(i);
                purchaseLabelRateLine.setCarrier((String) serviceTypeObj.get("carrier").toString());
                purchaseLabelRateLine.setCarrierService(
                    (String) serviceTypeObj.get("service").toString());

                //                JSONObject servicelevelObj = (JSONObject)
                // serviceTypeObj.get("service");
                //                purchaseLabelRateLine.setCarrierService(
                //                    (String) servicelevelObj.get("name").toString());
                //                purchaseLabelRateLine.setCarrierServiceToken(
                //                    (String) servicelevelObj.get("token").toString());

                String amountStr = (String) serviceTypeObj.get("rate").toString();
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
  public List<MultiShipmentPackageLine> confirmShippingService(
      PurchaseLabel purchaseLabel, SaleOrder saleOrder) throws AxelorException {

    List<MultiShipmentPackageLine> MultiShipmentPackageLineList =
        new ArrayList<MultiShipmentPackageLine>();
    Map<String, String> map = new HashMap<>();
    if (!(purchaseLabel.getWeight().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, "Please add weight.");
    }

    if (!(purchaseLabel.getSizeL().compareTo(new BigDecimal(0)) == 1)
        || !(purchaseLabel.getSizeW().compareTo(new BigDecimal(0)) == 1)
        || !(purchaseLabel.getSizeH().compareTo(new BigDecimal(0)) == 1)) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add size of L, W and H values.");
    }

    if (purchaseLabel.getShippingToAddress() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add Shipping To Address");
    }

    Address address = purchaseLabel.getShippingToAddress();
    if (address.getPostalCode() == null) {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE, "Please add postal code on Shipping To Address");
    }

    String RecipientDetails = address.getAddressL2();
    if (RecipientDetails == null || RecipientDetails == "") {
      throw new AxelorException(
          TraceBackRepository.CATEGORY_NO_VALUE,
          "Please add Recipient Details on Shiping To Address");
    }
    String customerAccountId = "";
    if (purchaseLabel.getCustomerShipmentInfo() != null) {
      customerAccountId = purchaseLabel.getCustomerShipmentInfo().getId().toString();
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

    //    String customerName = saleOrder.getClientPartner().getSimpleFullName();
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

    if (carrier == "" || carrier == null || carrierService == "" || carrierService == null) {
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

    url = url + "/shipping/easypost/orders";
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost httpRequest = new HttpPost(url);

    try {
      String singleObj =
          "{\"weight\":\""
              + purchaseLabel.getWeight()
              + "\",\"length\":\""
              + purchaseLabel.getSizeL()
              + "\",\"width\":\""
              + purchaseLabel.getSizeW()
              + "\",\"height\":\""
              + purchaseLabel.getSizeH()
              + "\",\"city\":\""
              + city
              + "\",\"customer\":\""
              + custumerId
              + "\",\"name\":\""
              + RecipientDetails
              + "\",\"company\":\""
              + company
              + "\",\"address\":\""
              + addressStr
              + "\",\"address_2\":\""
              + addressStr2
              + "\",\"zipCode\":\""
              + pincode
              + "\",\"state\":\""
              + state
              + "\",\"phone\":\""
              + purchaseLabel.getPhoneNumber()
              + "\",\"send_email\":"
              + purchaseLabel.getIsEmailSend()
              + ",\"mail\":\""
              + purchaseLabel.getEmailAddress()
              + "\",\"orderId\":"
              + orderId
              + ",\"carrier\":\""
              + carrier.toLowerCase()
              + "\",\"serviceName\":\""
              + carrierService
              //              + "\",\"customerAccountId\":\""
              //              + customerAccountId
              + "\"}";
      String requestBody = singleObj;
      for (int i = 1; i < purchaseLabel.getTotalPackage(); i++) {
        requestBody = requestBody + "," + singleObj;
      }
      requestBody = "[" + requestBody + "]";
      StringEntity params = new StringEntity(requestBody);
      httpRequest.addHeader("Content-Type", "application/json");
      httpRequest.setHeader("X-Api-Key", token);
      httpRequest.setEntity(params);
      CloseableHttpResponse httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        System.err.println(result);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            JSONArray resArray = (JSONArray) jsonObj.get("data");

            for (int i = 0; i < resArray.length(); i++) {
              MultiShipmentPackageLine purchaseLabelRateLine = new MultiShipmentPackageLine();

              JSONObject dataObj = resArray.getJSONObject(i);
              if (dataObj.get("status").equals("Error")) {
                throw new AxelorException(
                    TraceBackRepository.CATEGORY_NO_VALUE, dataObj.toString());
              }

              JSONObject shippingOrderObj = (JSONObject) dataObj.get("shippingOrder");
              //              JSONObject shippoOrderObj = (JSONObject) dataObj.get("shippoOrder");

              String trackingNumber = (String) shippingOrderObj.get("trackingNumber").toString();
              //              String trackingLink = (String)
              // shippingOrderObj.get("trackingLink").toString();
              String trackingLink = (String) shippingOrderObj.get("lableUrl").toString();

              if (trackingNumber == ""
                  || trackingNumber == null
                  || trackingLink == ""
                  || trackingLink == null) {
                throw new AxelorException(
                    TraceBackRepository.CATEGORY_NO_VALUE, "Traking number or Lable not found.");
              }

              purchaseLabelRateLine.setTrackingNumber(trackingNumber);
              purchaseLabelRateLine.setLableUrl(trackingLink);

              MultiShipmentPackageLineList.add(purchaseLabelRateLine);
            }

            /**
             * For Single shipment
             *
             * <p>JSONObject dataObj = (JSONObject) jsonObj.get("data"); JSONObject shippingOrderObj
             * = (JSONObject) dataObj.get("shippingOrder"); JSONObject shippoOrderObj = (JSONObject)
             * dataObj.get("shippoOrder");
             *
             * <p>String trackingNumber = (String)
             * shippingOrderObj.get("trackingNumber").toString(); String labelUrl = (String)
             * shippoOrderObj.get("label_url").toString();
             *
             * <p>if (trackingNumber == "" || trackingNumber == null || labelUrl == "" || labelUrl
             * == null) { String errMessage = shippoOrderObj.get("message").toString(); throw new
             * AxelorException(TraceBackRepository.CATEGORY_NO_VALUE, errMessage); }
             *
             * <p>map.put("trackingNumber", trackingNumber); map.put("labelUrl", labelUrl);
             */
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
    return MultiShipmentPackageLineList;
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

  @Override
  public String sendEmailApi(ShipmentLine shipmentLine) {
    try {
      String url = "https://axelor-api.zdental.com/api/shipping/email";

      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost httpRequest = new HttpPost(url);
      httpRequest.addHeader("Content-Type", "application/json");
      String shippingId = shipmentLine.getId().toString();
      //    shippingId = "4352";
      String requestBody = "{\"shippingId\":" + shippingId + "}";

      StringEntity params = new StringEntity(requestBody);
      httpRequest.setEntity(params);
      CloseableHttpResponse httpRresponse;
      httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        return EntityUtils.toString(entity);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Error during sending Email";
  }

  @Override
  public Boolean returnShipmentLable(ShipmentLine shipmentLine) throws AxelorException {
    try {
      String url = "https://axelor-api.zdental.com/api/shipping/refund";

      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost httpRequest = new HttpPost(url);
      httpRequest.addHeader("Content-Type", "application/json");
      String shippingId = shipmentLine.getId().toString();
      String requestBody = "{\"shippingId\":" + shippingId + "}";

      StringEntity params = new StringEntity(requestBody);
      httpRequest.setEntity(params);
      CloseableHttpResponse httpRresponse;
      httpRresponse = httpClient.execute(httpRequest);
      HttpEntity entity = httpRresponse.getEntity();
      if (entity != null) {
        String result = EntityUtils.toString(entity);
        System.err.println(result);
        JSONObject jsonObj = new JSONObject(result);
        if (jsonObj.has("type")) {
          if (jsonObj.get("type").equals("success")) {
            return true;
          } else {
            throw new AxelorException(0, result);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new AxelorException(0, e.toString());
    }
    throw new AxelorException(0, "Error in return shipment lable.");
  }
}
