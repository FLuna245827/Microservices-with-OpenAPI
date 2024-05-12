package com.company.demo.rest.impl;

import com.x.openapi.template.generated.api.RestApiDelegate;
import com.x.openapi.template.generated.model.ServiceFeaturesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RestApiDelegateImpl implements RestApiDelegate {

  private static final Logger lOGGER = LoggerFactory.getLogger(RestApiDelegateImpl.class);

  @Override
  public ResponseEntity<ServiceFeaturesDto> getFeatures(String xApiKey, String xAuthorizedUserId, String xApplicationClient) {
    return RestApiDelegate.super.getFeatures(xApiKey, xAuthorizedUserId, xApplicationClient);
  }

  @Override
  public ResponseEntity<Void> updateFeatures(String userId, String xApiKey, String xAuthorizedUserId, ServiceFeaturesDto serviceFeaturesDto) {
    return RestApiDelegate.super.updateFeatures(userId, xApiKey, xAuthorizedUserId, serviceFeaturesDto);
  }
}
