package com.bradox.erp.sign.application.rest;

import com.bradox.erp.sign.service.domain.dto.FileContent;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.dto.PublicRefuseCommand;
import com.bradox.erp.sign.service.domain.dto.PublicResultResponse;
import com.bradox.erp.sign.service.domain.dto.PublicSigningView;
import com.bradox.erp.sign.service.domain.dto.PublicSubmitCommand;
import com.bradox.erp.sign.service.domain.ports.input.PublicSigningApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * The only anonymous routes of the Sign module (SIG-03). The link token in the path is the whole credential. Every
 * response is {@code no-store}, and the token is never logged by this class.
 */
@RestController
@RequestMapping(value = "/api/v1/public/sign/{token}", produces = "application/json")
public class PublicSignController {

    private final PublicSigningApplicationService signing;

    public PublicSignController(PublicSigningApplicationService signing) {
        this.signing = signing;
    }

    @GetMapping
    public ResponseEntity<PublicSigningView> open(@PathVariable String token, HttpServletRequest request) {
        return noStore(signing.open(token, ip(request), agent(request)));
    }

    @GetMapping(value = "/pages/{page}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> page(@PathVariable String token, @PathVariable int page) {
        PageImage image = signing.page(token, page);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(image.png());
    }

    @PostMapping("/submit")
    public ResponseEntity<PublicResultResponse> submit(@PathVariable String token, @RequestBody PublicSubmitCommand command,
                                                       HttpServletRequest request) {
        return noStore(signing.submit(token, command, ip(request), agent(request)));
    }

    @PostMapping("/refuse")
    public ResponseEntity<PublicResultResponse> refuse(@PathVariable String token, @RequestBody PublicRefuseCommand command,
                                                       HttpServletRequest request) {
        return noStore(signing.refuse(token, command, ip(request), agent(request)));
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String token, HttpServletRequest request) {
        FileContent f = signing.download(token, ip(request), agent(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(f.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(f.bytes());
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    /** The first address of X-Forwarded-For when behind the reverse proxy, else the socket address. */
    private static String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String agent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua == null ? null : ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
