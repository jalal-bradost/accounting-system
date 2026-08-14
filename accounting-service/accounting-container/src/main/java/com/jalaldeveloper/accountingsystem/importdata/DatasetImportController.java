package com.jalaldeveloper.accountingsystem.importdata;

import com.jalaldeveloper.accountingsystem.domain.valueobject.CompanyId;
import com.jalaldeveloper.accountingsystem.platform.security.RequiresPermission;
import com.jalaldeveloper.accountingsystem.platform.web.CurrentCompany;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/v1/import", produces = MediaType.APPLICATION_JSON_VALUE)
public class DatasetImportController {

    private final DatasetImportService datasetImportService;

    public DatasetImportController(DatasetImportService datasetImportService) {
        this.datasetImportService = datasetImportService;
    }

    @PostMapping("/grocery")
    @RequiresPermission("dataset.import")
    public ResponseEntity<DatasetImportReport> importBundledGrocery(@CurrentCompany CompanyId companyId)
            throws IOException {
        return ResponseEntity.ok(datasetImportService.importGroceryClasspath(companyId.getId()));
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiresPermission("dataset.import")
    public ResponseEntity<DatasetImportReport> importCsv(
            @CurrentCompany CompanyId companyId,
            @RequestPart(value = "categories", required = false) MultipartFile categories,
            @RequestPart(value = "partners", required = false) MultipartFile partners,
            @RequestPart(value = "products", required = false) MultipartFile products,
            @RequestPart(value = "purchases", required = false) MultipartFile purchases,
            @RequestPart(value = "purchase_lines", required = false) MultipartFile purchaseLines,
            @RequestPart(value = "sales_orders", required = false) MultipartFile salesOrders,
            @RequestPart(value = "sale_lines", required = false) MultipartFile saleLines,
            @RequestPart(value = "pos_sessions", required = false) MultipartFile posSessions,
            @RequestPart(value = "pos_tickets", required = false) MultipartFile posTickets,
            @RequestPart(value = "pos_lines", required = false) MultipartFile posLines) throws IOException {
        return ResponseEntity.ok(datasetImportService.importMultipart(
                companyId.getId(), categories, partners, products, purchases, purchaseLines,
                salesOrders, saleLines, posSessions, posTickets, posLines));
    }
}
