package com.bradox.erp.repair.service.domain;

import com.bradox.erp.domain.valueobject.CompanyId;
import com.bradox.erp.repair.domain.core.model.InspectionTemplate;
import com.bradox.erp.repair.domain.core.model.LaborCategory;
import com.bradox.erp.repair.domain.core.model.Settings;
import com.bradox.erp.repair.service.domain.ports.output.repository.InspectionRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.LaborRepository;
import com.bradox.erp.repair.service.domain.ports.output.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared helpers: company settings with first-use seeding of the default data (D17), and "not found". */
@Component
class RepairSupport {

    private static final List<String> CATEGORIES = List.of("Mechanical", "Electrical", "Body and paint",
            "Diagnostic and scan", "Service and maintenance");

    private static final String[][] INSPECTION = {
            {"Engine and fluids", "Engine oil level and condition", "Coolant level", "Brake fluid level", "Power steering fluid",
                    "Washer fluid", "Leaks (engine, transmission)", "Drive belts and hoses", "Air filter"},
            {"Brakes", "Front pads", "Rear pads", "Front discs", "Rear discs", "Brake lines and hoses", "Handbrake"},
            {"Suspension and steering", "Shock absorbers", "Control arm bushings", "Ball joints and tie rods",
                    "Wheel bearing play", "Steering play"},
            {"Tires and wheels", "Tread depth (each tire)", "Tire pressure", "Tire damage or age", "Pulling to one side"},
            {"Lights and electrics", "Headlights", "Brake lights", "Indicators", "Horn", "Wipers", "Dashboard warning lights",
                    "Battery condition and voltage", "Charging system"},
            {"Exhaust", "Leaks and noise", "Mounts"},
            {"A/C and cabin", "A/C cooling", "Cabin filter"},
            {"Scan", "Fault codes read (attach report)"},
            {"Body and glass", "Visible damage", "Rust", "Windshield cracks"}};

    private final SettingsRepository settings;
    private final LaborRepository labor;
    private final InspectionRepository inspections;

    RepairSupport(SettingsRepository settings, LaborRepository labor, InspectionRepository inspections) {
        this.settings = settings;
        this.labor = labor;
        this.inspections = inspections;
    }

    /** The company's settings; the first call creates the defaults plus the seeded categories and checklist. */
    Settings settings(CompanyId companyId) {
        return settings.find(companyId).orElseGet(() -> seed(companyId));
    }

    private Settings seed(CompanyId companyId) {
        UUID company = companyId.getId();
        for (String name : CATEGORIES) {
            labor.save(new LaborCategory(UUID.randomUUID(), company, name, null, true));
        }
        List<InspectionTemplate.Item> items = new ArrayList<>();
        int seq = 0;
        for (String[] section : INSPECTION) {
            for (int i = 1; i < section.length; i++) {
                items.add(new InspectionTemplate.Item(UUID.randomUUID(), section[0], section[i], seq++));
            }
        }
        InspectionTemplate template = inspections.save(
                new InspectionTemplate(UUID.randomUUID(), company, "General inspection", null, true, items));
        Settings s = Settings.defaults(company);
        return settings.save(new Settings(s.id(), company, s.defaultHourlyRate(), s.verbalLimitAdvisor(), s.verbalLimitSupervisor(),
                s.warrantyDays(), s.warrantyKm(), s.emergencyStartLimit(), s.quoteValidityDays(), s.advisorDiscountLimitPercent(),
                s.comebackDays(), s.partsTolerancePercent(), template.id(), null));
    }

    static ResponseStatusException notFound(String what) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found");
    }
}
