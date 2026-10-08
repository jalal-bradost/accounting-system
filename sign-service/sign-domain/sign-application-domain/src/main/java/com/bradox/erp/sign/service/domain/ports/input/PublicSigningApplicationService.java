package com.bradox.erp.sign.service.domain.ports.input;

import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.PublicRefuseCommand;
import com.bradox.erp.sign.service.domain.dto.PublicResultResponse;
import com.bradox.erp.sign.service.domain.dto.PublicSigningView;
import com.bradox.erp.sign.service.domain.dto.PublicSubmitCommand;

/** Everything a signer can do with only a link. No user is signed in; the token is the whole credential. */
public interface PublicSigningApplicationService {

    /** Marks the signer as having viewed on first open. Throws NotFound for any bad, expired or closed link. */
    PublicSigningView open(String token, String ip, String userAgent);

    PageImage page(String token, int page);

    PublicResultResponse submit(String token, PublicSubmitCommand command, String ip, String userAgent);

    PublicResultResponse refuse(String token, PublicRefuseCommand command, String ip, String userAgent);

    /** The finished PDF, for a signer of a completed request. */
    FileContent download(String token, String ip, String userAgent);
}
