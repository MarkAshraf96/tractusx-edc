/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.tractusx.edc.api.cp.adapter;

import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.tractusx.edc.api.cp.adapter.dto.NegotiateEdrRequestDto;
import org.eclipse.tractusx.edc.spi.cp.adapter.model.NegotiateEdrRequest;
import org.eclipse.tractusx.edc.spi.cp.adapter.service.AdapterTransferProcessService;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Path("/adapter/edrs")
public class AdapterEdrController implements AdapterEdrApi {

    private final AdapterTransferProcessService adapterTransferProcessService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonLd jsonLdService;

    public AdapterEdrController(AdapterTransferProcessService adapterTransferProcessService, JsonLd jsonLdService, TypeTransformerRegistry transformerRegistry) {
        this.adapterTransferProcessService = adapterTransferProcessService;
        this.jsonLdService = jsonLdService;
        this.transformerRegistry = transformerRegistry;
    }

    @POST
    @Override
    public JsonObject initiateEdrNegotiation(JsonObject requestObject) {
        var edrNegotiationRequest = jsonLdService.expand(requestObject)
                .compose(expanded -> transformerRegistry.transform(expanded, NegotiateEdrRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, NegotiateEdrRequest.class))
                .orElseThrow(InvalidRequestException::new);

        var contractNegotiation = adapterTransferProcessService.initiateEdrNegotiation(edrNegotiationRequest).orElseThrow(exceptionMapper(NegotiateEdrRequest.class));

        var responseDto = IdResponseDto.Builder.newInstance()
                .id(contractNegotiation.getId())
                .createdAt(contractNegotiation.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .compose(jsonLdService::compact)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }
}
