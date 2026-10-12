package com.example.eshop.auth.mapper;
import com.example.eshop.auth.dto.request.AddressRequest;
import com.example.eshop.auth.dto.response.AddressResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AddressMapperTest {
 @Test void mapsDeliveryFieldsAndPreservesThemForLegacyUpdates() {
  AddressRequest request = AddressRequest.builder().addressLine1("Street 271").city("Phnom Penh").zipCode("120101").country("Cambodia").label("Home").recipientName("Customer").phoneNumber("+85512345678").district("Chamkar Mon").commune("Tuol Tumpung Ti Muoy").additionalInformation("Floor 2").isDefault(true).build();
  var entity = AddressMapper.toEntity(request);
  var response = (AddressResponse) AddressMapper.toResponse(entity).object();
  assertEquals("Home", response.getLabel()); assertEquals("Customer", response.getRecipientName()); assertEquals("+85512345678", response.getPhoneNumber()); assertEquals("Chamkar Mon", response.getDistrict()); assertEquals("Tuol Tumpung Ti Muoy", response.getCommune()); assertEquals("Floor 2", response.getAdditionalInformation());
  var legacy = AddressRequest.builder().addressLine1("New Street").city("Phnom Penh").zipCode("120101").country("Cambodia").isDefault(false).build();
  AddressMapper.updateEntity(entity, legacy);
  assertEquals("Customer", entity.getRecipientName()); assertEquals("Home", entity.getLabel()); assertEquals("New Street", entity.getAddressLine1());
  request.setAdditionalInformation(""); AddressMapper.updateEntity(entity, request); assertEquals("", entity.getAdditionalInformation());
 }
}
