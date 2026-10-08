package com.bradox.erp.sign.service.domain.ports.input;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.platform.application.dto.PageResponse;
import com.bradox.erp.sign.service.domain.dto.CreateRequestCommand;
import com.bradox.erp.sign.service.domain.dto.DashboardResponse;
import com.bradox.erp.sign.service.domain.dto.EventResponse;
import com.bradox.erp.sign.service.domain.dto.ExtendCommand;
import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.ReasonCommand;
import com.bradox.erp.sign.service.domain.dto.ReminderTextResponse;
import com.bradox.erp.sign.service.domain.dto.RequestFilter;
import com.bradox.erp.sign.service.domain.dto.RequestResponse;
import com.bradox.erp.sign.service.domain.dto.RequestSummaryResponse;
import com.bradox.erp.sign.service.domain.dto.SendResultResponse;
import com.bradox.erp.sign.service.domain.dto.SignerInput;
import com.bradox.erp.sign.service.domain.dto.SignerLinkResponse;
import com.bradox.erp.sign.service.domain.dto.UpdateRequestCommand;

import java.util.List;
import java.util.UUID;

public interface RequestApplicationService {

    PageResponse<RequestSummaryResponse> list(CompanyId companyId, RequestFilter filter);

    RequestResponse get(CompanyId companyId, UUID id);

    RequestResponse create(CompanyId companyId, CreateRequestCommand command);

    RequestResponse update(CompanyId companyId, UUID id, UpdateRequestCommand command);

    void delete(CompanyId companyId, UUID id);

    SendResultResponse send(CompanyId companyId, UUID id, Integer validityDays);

    /** A new link for one signer; the old one stops working (BR-SIG-03). */
    SignerLinkResponse newLink(CompanyId companyId, UUID id, UUID signerId);

    RequestResponse replaceSigner(CompanyId companyId, UUID id, UUID signerId, SignerInput signer);

    RequestResponse cancel(CompanyId companyId, UUID id, ReasonCommand command);

    RequestResponse extend(CompanyId companyId, UUID id, ExtendCommand command);

    /** "Remind now": logs a reminder and returns a ready-made message with a fresh link (SIG-06 #4). */
    ReminderTextResponse remind(CompanyId companyId, UUID id, UUID signerId);

    List<EventResponse> events(CompanyId companyId, UUID id);

    PageImage page(CompanyId companyId, UUID id, int page);

    FileContent finalDocument(CompanyId companyId, UUID id);

    DashboardResponse dashboard(CompanyId companyId);

    List<RequestSummaryResponse> byRecord(CompanyId companyId, String recordModel, UUID recordId);
}
