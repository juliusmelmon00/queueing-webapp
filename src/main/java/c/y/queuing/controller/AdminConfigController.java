package c.y.queuing.controller;
import org.springframework.beans.factory.annotation.Value;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import c.y.queuing.entity.AppUser;
import c.y.queuing.entity.ClientFormField;
import c.y.queuing.entity.Department;
import c.y.queuing.entity.RoutingRule;
import c.y.queuing.entity.SpecificTransaction;
import c.y.queuing.entity.SystemSetting;
import c.y.queuing.entity.TransactionType;
import c.y.queuing.repository.AppUserRepository;
import c.y.queuing.repository.ClientFormFieldRepository;
import c.y.queuing.repository.DepartmentRepository;
import c.y.queuing.repository.RoutingRuleRepository;
import c.y.queuing.repository.SpecificTransactionRepository;
import c.y.queuing.repository.SystemSettingRepository;
import c.y.queuing.repository.TicketHistoryRepository;
import c.y.queuing.repository.TicketRepository;
import c.y.queuing.repository.TransactionTypeRepository;

@Controller
public class AdminConfigController {

    private final DepartmentRepository departmentRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpecificTransactionRepository specificTransactionRepository;
    private final ClientFormFieldRepository clientFormFieldRepository;
    private final SystemSettingRepository systemSettingRepository;

    public AdminConfigController(
            DepartmentRepository departmentRepository,
            TransactionTypeRepository transactionTypeRepository,
            RoutingRuleRepository routingRuleRepository,
            TicketRepository ticketRepository,
            TicketHistoryRepository ticketHistoryRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            SpecificTransactionRepository specificTransactionRepository,
            ClientFormFieldRepository clientFormFieldRepository,
            SystemSettingRepository systemSettingRepository) {
        this.departmentRepository = departmentRepository;
        this.transactionTypeRepository = transactionTypeRepository;
        this.routingRuleRepository = routingRuleRepository;
        this.ticketRepository = ticketRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.specificTransactionRepository = specificTransactionRepository;
        this.clientFormFieldRepository = clientFormFieldRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    @Value("${app.admin.clear-key}")
    private String clearTicketSecretKey;

    @Value("${app.admin.reset-key}")
    private String resetSystemSecretKey;

    @GetMapping("/admin/config")
    public String adminConfigPage(Model model) {
        String companyName = systemSettingRepository.findBySettingKey("company_name")
                .map(SystemSetting::getSettingValue)
                .orElse("");

        String companyLogoUrl = systemSettingRepository.findBySettingKey("company_logo")
                .map(SystemSetting::getSettingValue)
                .orElse("");

        model.addAttribute("companyName", companyName);
        model.addAttribute("companyLogoUrl", companyLogoUrl);

        model.addAttribute("departments", departmentRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("transactionTypes", transactionTypeRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("users", appUserRepository.findAllByOrderByUsernameAsc());
        model.addAttribute("clientFields", clientFormFieldRepository.findAllByOrderByDisplayOrderAsc());

        List<SpecificTransaction> specificTransactions = specificTransactionRepository.findAllByOrderByDisplayOrderAsc();
        model.addAttribute("specificTransactions", specificTransactions);

        List<Map<String, Object>> groupedSpecificTransactions = buildGroupedSpecificTransactions(specificTransactions);
        model.addAttribute("groupedSpecificTransactions", groupedSpecificTransactions);

        List<RoutingRule> routingRules = routingRuleRepository.findAllByOrderByTransactionTypeCodeAscPriorityAsc();
        model.addAttribute("routingRules", routingRules);

        List<Map<String, Object>> groupedRoutingRules = buildGroupedRoutingRules(routingRules);
        model.addAttribute("groupedRoutingRules", groupedRoutingRules);

        return "admin-config";
    }

    // ================= GROUPING HELPERS =================
    private List<Map<String, Object>> buildGroupedRoutingRules(List<RoutingRule> routingRules) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();

        for (RoutingRule rule : routingRules) {
            String deptCode = rule.getTargetDepartmentCode() == null
                    ? "-"
                    : rule.getTargetDepartmentCode().trim().toUpperCase();

            Map<String, Object> group = grouped.get(deptCode);

            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("targetDepartmentCode", deptCode);
                group.put("targetDepartmentName", resolveDepartmentName(deptCode));
                group.put("rules", new ArrayList<RoutingRule>());
                group.put("totalCount", 0);
                group.put("activeCount", 0);
                grouped.put(deptCode, group);
            }

            @SuppressWarnings("unchecked")
            List<RoutingRule> rules = (List<RoutingRule>) group.get("rules");
            rules.add(rule);

            int totalCount = (int) group.get("totalCount");
            group.put("totalCount", totalCount + 1);

            if (Boolean.TRUE.equals(rule.getIsActive())) {
                int activeCount = (int) group.get("activeCount");
                group.put("activeCount", activeCount + 1);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    private List<Map<String, Object>> buildGroupedSpecificTransactions(List<SpecificTransaction> list) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();

        for (SpecificTransaction st : list) {
            String txCode = st.getTransactionTypeCode() == null
                    ? "-"
                    : st.getTransactionTypeCode().trim().toUpperCase();

            Map<String, Object> group = grouped.get(txCode);

            if (group == null) {
                group = new LinkedHashMap<>();
                group.put("transactionTypeCode", txCode);
                group.put("transactionTypeName", resolveTransactionTypeName(txCode));
                group.put("items", new ArrayList<SpecificTransaction>());
                group.put("totalCount", 0);
                group.put("activeCount", 0);
                grouped.put(txCode, group);
            }

            @SuppressWarnings("unchecked")
            List<SpecificTransaction> items = (List<SpecificTransaction>) group.get("items");
            items.add(st);

            int total = (int) group.get("totalCount");
            group.put("totalCount", total + 1);

            if (Boolean.TRUE.equals(st.getIsActive())) {
                int active = (int) group.get("activeCount");
                group.put("activeCount", active + 1);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    private String resolveDepartmentName(String deptCode) {
        if (deptCode == null || deptCode.isBlank()) {
            return "-";
        }

        return departmentRepository.findByCode(deptCode.trim().toUpperCase())
                .map(Department::getName)
                .orElse(deptCode);
    }

    private String resolveTransactionTypeName(String code) {
        if (code == null || code.isBlank()) {
            return "-";
        }

        return transactionTypeRepository.findByCode(code.trim().toUpperCase())
                .map(TransactionType::getName)
                .orElse(code);
    }

    // ================= COMPANY BRANDING =================
    @PostMapping("/admin/config/branding")
    public String saveBranding(
            @RequestParam String companyName,
            @RequestParam(required = false) String companyLogoUrl) {

        String cleanCompanyName = companyName == null ? "" : companyName.trim();
        String cleanCompanyLogoUrl = companyLogoUrl == null ? "" : companyLogoUrl.trim();

        if (cleanCompanyName.isEmpty()) {
            return "redirect:/admin/config?brandingInvalid";
        }

        systemSettingRepository.save(new SystemSetting("company_name", cleanCompanyName));
        systemSettingRepository.save(new SystemSetting("company_logo", cleanCompanyLogoUrl));

        return "redirect:/admin/config?brandingSaved";
    }

    // ================= ROUTING RULES =================
    @PostMapping("/admin/config/routing-rules")
    public String addRoutingRule(
            @RequestParam String transactionTypeCode,
            @RequestParam(required = false) java.util.List<String> specificTransactionCodes,
            @RequestParam String targetDepartmentCode,
            @RequestParam Integer priority,
            @RequestParam(defaultValue = "true") boolean isActive) {

        if (isBlank(transactionTypeCode) || isBlank(targetDepartmentCode) || priority == null || priority < 1) {
            return "redirect:/admin/config?ruleInvalid";
        }

        String txCode = transactionTypeCode.trim().toUpperCase();
        String deptCode = targetDepartmentCode.trim().toUpperCase();

        boolean txExists = transactionTypeRepository.findByCode(txCode).isPresent();
        boolean deptExists = departmentRepository.findByCode(deptCode).isPresent();

        if (!txExists || !deptExists) {
            return "redirect:/admin/config?ruleInvalidReference";
        }

        if (specificTransactionCodes == null || specificTransactionCodes.isEmpty()) {
            if (routingRuleCombinationExists(txCode, null, deptCode)) {
                return "redirect:/admin/config?ruleExists";
            }

            RoutingRule rule = new RoutingRule();
            rule.setTransactionTypeCode(txCode);
            rule.setSpecificTransactionCode(null);
            rule.setOperatorNumber(null);
            rule.setTargetDepartmentCode(deptCode);
            rule.setPriority(priority);
            rule.setIsActive(isActive);

            routingRuleRepository.save(rule);
            return "redirect:/admin/config?ruleSaved";
        }

        int created = 0;

        for (String code : specificTransactionCodes) {
            String specificCode = normalizeSpecificTransactionCode(code);

            if (!isSpecificTransactionValidForType(txCode, specificCode)) {
                continue;
            }

            if (routingRuleCombinationExists(txCode, specificCode, deptCode)) {
                continue;
            }

            RoutingRule rule = new RoutingRule();
            rule.setTransactionTypeCode(txCode);
            rule.setSpecificTransactionCode(specificCode);
            rule.setOperatorNumber(null);
            rule.setTargetDepartmentCode(deptCode);
            rule.setPriority(priority);
            rule.setIsActive(isActive);

            routingRuleRepository.save(rule);
            created++;
        }

        if (created == 0) {
            return "redirect:/admin/config?ruleExists";
        }

        return "redirect:/admin/config?ruleSaved";
    }

    // ================= TRANSACTION TYPES =================
    @PostMapping("/admin/config/transaction-types")
    public String addTransactionType(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive) {

        if (isBlank(code) || isBlank(name) || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?txInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String cleanName = name.trim();
        String cleanDescription = description == null ? null : description.trim();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?txInvalidCode";
        }

        if (transactionTypeRepository.existsByCode(codeUpper)) {
            return "redirect:/admin/config?txExists";
        }

        TransactionType tx = new TransactionType();
        tx.setCode(codeUpper);
        tx.setName(cleanName);
        tx.setDescription(cleanDescription);
        tx.setDisplayOrder(displayOrder);
        tx.setIsActive(isActive);

        transactionTypeRepository.save(tx);

        return "redirect:/admin/config?txSaved";
    }

    // ================= SPECIFIC TRANSACTIONS =================
    @GetMapping("/admin/config/specific-transactions/template")
    public ResponseEntity<byte[]> downloadSpecificTransactionCsvTemplate() {
        String csv = """
    code,name,transactionTypeCode,displayOrder,isActive
    TR4,BBC ALBERTA,TR4,1,true
    TR5,XYZ PROCESS,TR4,2,true
    """;

        byte[] content = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=specific-transactions-template.csv")
                .contentType(new MediaType("text", "csv"))
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("/admin/config/specific-transactions")
    public String addSpecificTransaction(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam String transactionTypeCode,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive) {

        if (isBlank(code) || isBlank(name) || isBlank(transactionTypeCode)
                || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?stInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String nameClean = name.trim();
        String txCode = transactionTypeCode.trim().toUpperCase();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?stInvalidCode";
        }

        boolean txExists = transactionTypeRepository.findByCode(txCode).isPresent();
        if (!txExists) {
            return "redirect:/admin/config?stInvalid";
        }

        boolean duplicateCodeInSameType =
                specificTransactionRepository.existsByTransactionTypeCodeAndCode(txCode, codeUpper);

        if (duplicateCodeInSameType) {
            return "redirect:/admin/config?stExists";
        }

        boolean duplicateNameInSameType =
                specificTransactionRepository.existsByTransactionTypeCodeAndNameIgnoreCase(txCode, nameClean);

        if (duplicateNameInSameType) {
            return "redirect:/admin/config?stExists";
        }

        SpecificTransaction st = new SpecificTransaction();
        st.setCode(codeUpper);
        st.setName(nameClean);
        st.setTransactionTypeCode(txCode);
        st.setDisplayOrder(displayOrder);
        st.setIsActive(isActive);

        specificTransactionRepository.save(st);

        return "redirect:/admin/config?stSaved";
    }

    @PostMapping("/admin/config/specific-transactions/{id}/activate")
    public String activateSpecificTransaction(@PathVariable Long id) {
        SpecificTransaction st = specificTransactionRepository.findById(id).orElseThrow();
        st.setIsActive(true);
        specificTransactionRepository.save(st);
        return "redirect:/admin/config?stActivated";
    }

    @PostMapping("/admin/config/specific-transactions/{id}/deactivate")
    public String deactivateSpecificTransaction(@PathVariable Long id) {
        SpecificTransaction st = specificTransactionRepository.findById(id).orElseThrow();

        boolean isUsed = routingRuleRepository.existsBySpecificTransactionCode(st.getCode());
        if (isUsed) {
            return "redirect:/admin/config?stInUse";
        }

        st.setIsActive(false);
        specificTransactionRepository.save(st);
        return "redirect:/admin/config?stDeactivated";
    }

    @GetMapping("/admin/config/specific-transactions/{id}/edit")
    public String editSpecificTransactionForm(@PathVariable Long id, Model model) {
        SpecificTransaction st = specificTransactionRepository.findById(id).orElseThrow();
        model.addAttribute("specificTransaction", st);
        model.addAttribute("transactionTypes", transactionTypeRepository.findAllByOrderByDisplayOrderAsc());
        return "edit-specific-transaction";
    }

    @PostMapping("/admin/config/specific-transactions/{id}/edit")
    public String updateSpecificTransaction(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam String transactionTypeCode,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive) {

        SpecificTransaction st = specificTransactionRepository.findById(id).orElseThrow();

        if (isBlank(code) || isBlank(name) || isBlank(transactionTypeCode)
                || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?stInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String nameClean = name.trim();
        String txCode = transactionTypeCode.trim().toUpperCase();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?stInvalidCode";
        }

        boolean txExists = transactionTypeRepository.findByCode(txCode).isPresent();
        if (!txExists) {
            return "redirect:/admin/config?stInvalid";
        }

        Optional<SpecificTransaction> existingCode =
                specificTransactionRepository.findByTransactionTypeCodeAndCode(txCode, codeUpper);

        if (existingCode.isPresent() && !existingCode.get().getId().equals(id)) {
            return "redirect:/admin/config?stExists";
        }

        Optional<SpecificTransaction> existingName =
                specificTransactionRepository.findByTransactionTypeCodeAndNameIgnoreCase(txCode, nameClean);

        if (existingName.isPresent() && !existingName.get().getId().equals(id)) {
            return "redirect:/admin/config?stExists";
        }

        st.setCode(codeUpper);
        st.setName(nameClean);
        st.setTransactionTypeCode(txCode);
        st.setDisplayOrder(displayOrder);
        st.setIsActive(isActive);

        specificTransactionRepository.save(st);

        return "redirect:/admin/config?stSaved";
    }

    // ================= CSV IMPORT =================
    @PostMapping("/admin/config/specific-transactions/import")
    public String importSpecificTransactions(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return "redirect:/admin/config?stImportError";
        }

        int success = 0;
        int duplicates = 0;
        int invalid = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {

                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length < 5) {
                    invalid++;
                    continue;
                }

                String code = parts[0].trim().toUpperCase();
                String name = parts[1].trim();
                String txCode = parts[2].trim().toUpperCase();
                Integer displayOrder;
                Boolean isActive;

                try {
                    displayOrder = Integer.parseInt(parts[3].trim());
                    isActive = Boolean.parseBoolean(parts[4].trim());
                } catch (Exception e) {
                    invalid++;
                    continue;
                }

                if (code.isBlank() || name.isBlank() || txCode.isBlank() || displayOrder < 1) {
                    invalid++;
                    continue;
                }

                if (code.contains(" ")) {
                    invalid++;
                    continue;
                }

                boolean txExists = transactionTypeRepository.findByCode(txCode).isPresent();
                if (!txExists) {
                    invalid++;
                    continue;
                }

                boolean duplicateCode =
                        specificTransactionRepository.existsByTransactionTypeCodeAndCode(txCode, code);

                boolean duplicateName =
                        specificTransactionRepository.existsByTransactionTypeCodeAndNameIgnoreCase(txCode, name);

                if (duplicateCode || duplicateName) {
                    duplicates++;
                    continue;
                }

                SpecificTransaction st = new SpecificTransaction();
                st.setCode(code);
                st.setName(name);
                st.setTransactionTypeCode(txCode);
                st.setDisplayOrder(displayOrder);
                st.setIsActive(isActive);

                specificTransactionRepository.save(st);
                success++;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/admin/config?stImportError";
        }

        return "redirect:/admin/config?stImportSuccess&ok=" + success + "&dup=" + duplicates + "&bad=" + invalid;
    }

    // ================= CLIENT FIELDS =================
    @PostMapping("/admin/config/client-fields")
    public String addClientField(
            @RequestParam String label,
            @RequestParam String fieldCode,
            @RequestParam String fieldType,
            @RequestParam(required = false) String placeholder,
            @RequestParam(defaultValue = "false") boolean isRequired,
            @RequestParam(defaultValue = "true") boolean isActive,
            @RequestParam Integer displayOrder) {

        if (label == null || label.trim().isEmpty()
                || fieldCode == null || fieldCode.trim().isEmpty()
                || fieldType == null || fieldType.trim().isEmpty()
                || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?clientFieldInvalid";
        }

        String cleanLabel = label.trim();
        String cleanFieldCode = fieldCode.trim().toLowerCase();
        String cleanFieldType = fieldType.trim().toUpperCase();
        String cleanPlaceholder = placeholder == null ? "" : placeholder.trim();

        if (cleanFieldCode.contains(" ")) {
            return "redirect:/admin/config?clientFieldInvalidCode";
        }

        if (clientFormFieldRepository.existsByFieldCode(cleanFieldCode)) {
            return "redirect:/admin/config?clientFieldExists";
        }

        if (!"TEXT".equals(cleanFieldType)
                && !"NUMBER".equals(cleanFieldType)
                && !"DATE".equals(cleanFieldType)) {
            return "redirect:/admin/config?clientFieldInvalid";
        }

        ClientFormField field = new ClientFormField();
        field.setLabel(cleanLabel);
        field.setFieldCode(cleanFieldCode);
        field.setFieldType(cleanFieldType);
        field.setPlaceholder(cleanPlaceholder);
        field.setIsRequired(isRequired);
        field.setIsActive(isActive);
        field.setDisplayOrder(displayOrder);

        clientFormFieldRepository.save(field);

        return "redirect:/admin/config?clientFieldSaved";
    }

    @PostMapping("/admin/config/client-fields/{id}/activate")
    public String activateClientField(@PathVariable Long id) {
        ClientFormField field = clientFormFieldRepository.findById(id).orElseThrow();
        field.setIsActive(true);
        clientFormFieldRepository.save(field);
        return "redirect:/admin/config?clientFieldActivated";
    }

    @PostMapping("/admin/config/client-fields/{id}/deactivate")
    public String deactivateClientField(@PathVariable Long id) {
        ClientFormField field = clientFormFieldRepository.findById(id).orElseThrow();
        field.setIsActive(false);
        clientFormFieldRepository.save(field);
        return "redirect:/admin/config?clientFieldDeactivated";
    }

    @GetMapping("/admin/config/client-fields/{id}/edit")
    public String editClientField(@PathVariable Long id, Model model) {
        ClientFormField field = clientFormFieldRepository.findById(id).orElseThrow();
        model.addAttribute("clientField", field);
        return "edit-client-field";
    }

    @PostMapping("/admin/config/client-fields/{id}/edit")
    public String updateClientField(
            @PathVariable Long id,
            @RequestParam String label,
            @RequestParam String fieldCode,
            @RequestParam String fieldType,
            @RequestParam(required = false) String placeholder,
            @RequestParam(defaultValue = "false") boolean isRequired,
            @RequestParam(defaultValue = "true") boolean isActive,
            @RequestParam Integer displayOrder) {

        ClientFormField field = clientFormFieldRepository.findById(id).orElseThrow();

        if (label == null || label.trim().isEmpty()
                || fieldCode == null || fieldCode.trim().isEmpty()
                || fieldType == null || fieldType.trim().isEmpty()
                || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?clientFieldInvalid";
        }

        String cleanLabel = label.trim();
        String cleanFieldCode = fieldCode.trim().toLowerCase();
        String cleanFieldType = fieldType.trim().toUpperCase();
        String cleanPlaceholder = placeholder == null ? "" : placeholder.trim();

        if (cleanFieldCode.contains(" ")) {
            return "redirect:/admin/config?clientFieldInvalidCode";
        }

        if (!field.getFieldCode().equalsIgnoreCase(cleanFieldCode)
                && clientFormFieldRepository.existsByFieldCode(cleanFieldCode)) {
            return "redirect:/admin/config?clientFieldExists";
        }

        if (!"TEXT".equals(cleanFieldType)
                && !"NUMBER".equals(cleanFieldType)
                && !"DATE".equals(cleanFieldType)) {
            return "redirect:/admin/config?clientFieldInvalid";
        }

        field.setLabel(cleanLabel);
        field.setFieldCode(cleanFieldCode);
        field.setFieldType(cleanFieldType);
        field.setPlaceholder(cleanPlaceholder);
        field.setIsRequired(isRequired);
        field.setIsActive(isActive);
        field.setDisplayOrder(displayOrder);

        clientFormFieldRepository.save(field);

        return "redirect:/admin/config?clientFieldSaved";
    }

    // ================= DEPARTMENTS =================
    @PostMapping("/admin/config/departments")
    public String addDepartment(
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive,
            @RequestParam(defaultValue = "true") boolean allowRouting,
            @RequestParam(defaultValue = "true") boolean oneActiveTicketOnly) {

        if (isBlank(code) || isBlank(name) || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?deptInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String cleanName = name.trim();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?deptInvalidCode";
        }

        if (departmentRepository.existsByCode(codeUpper)) {
            return "redirect:/admin/config?deptExists";
        }

        Department dept = new Department();
        dept.setCode(codeUpper);
        dept.setName(cleanName);
        dept.setDisplayOrder(displayOrder);
        dept.setIsActive(isActive);
        dept.setAllowRouting(allowRouting);
        dept.setOneActiveTicketOnly(oneActiveTicketOnly);

        departmentRepository.save(dept);
        return "redirect:/admin/config?deptSaved";
    }

    // ================= ACTIVATE / DEACTIVATE TRANSACTION TYPE =================
    @PostMapping("/admin/config/transaction-types/{id}/activate")
    public String activateTransactionType(@PathVariable Long id) {
        TransactionType tx = transactionTypeRepository.findById(id).orElseThrow();
        tx.setIsActive(true);
        transactionTypeRepository.save(tx);
        return "redirect:/admin/config?txActivated";
    }

    @PostMapping("/admin/config/transaction-types/{id}/deactivate")
    public String deactivateTransactionType(@PathVariable Long id) {
        TransactionType tx = transactionTypeRepository.findById(id).orElseThrow();

        boolean isUsed = routingRuleRepository.existsByTransactionTypeCode(tx.getCode());
        if (isUsed) {
            return "redirect:/admin/config?txInUse";
        }

        tx.setIsActive(false);
        transactionTypeRepository.save(tx);
        return "redirect:/admin/config?txDeactivated";
    }

    // ================= ACTIVATE / DEACTIVATE DEPARTMENT =================
    @PostMapping("/admin/config/departments/{id}/activate")
    public String activateDepartment(@PathVariable Long id) {
        Department dept = departmentRepository.findById(id).orElseThrow();
        dept.setIsActive(true);
        departmentRepository.save(dept);
        return "redirect:/admin/config?deptActivated";
    }

    @PostMapping("/admin/config/departments/{id}/deactivate")
    public String deactivateDepartment(@PathVariable Long id) {
        Department dept = departmentRepository.findById(id).orElseThrow();

        boolean isUsed = routingRuleRepository.existsByTargetDepartmentCode(dept.getCode());
        if (isUsed) {
            return "redirect:/admin/config?deptInUse";
        }

        dept.setIsActive(false);
        departmentRepository.save(dept);
        return "redirect:/admin/config?deptDeactivated";
    }

    // ================= ACTIVATE / DEACTIVATE ROUTING RULE =================
    @PostMapping("/admin/config/routing-rules/{id}/activate")
    public String activateRoutingRule(@PathVariable Long id) {
        RoutingRule rule = routingRuleRepository.findById(id).orElseThrow();
        rule.setIsActive(true);
        routingRuleRepository.save(rule);
        return "redirect:/admin/config?ruleActivated";
    }

    @PostMapping("/admin/config/routing-rules/{id}/deactivate")
    public String deactivateRoutingRule(@PathVariable Long id) {
        RoutingRule rule = routingRuleRepository.findById(id).orElseThrow();
        rule.setIsActive(false);
        routingRuleRepository.save(rule);
        return "redirect:/admin/config?ruleDeactivated";
    }

    // ================= EDIT DEPARTMENT =================
    @GetMapping("/admin/config/departments/{id}/edit")
    public String editDepartmentForm(@PathVariable Long id, Model model) {
        Department dept = departmentRepository.findById(id).orElseThrow();
        model.addAttribute("department", dept);
        return "edit-department";
    }

    @PostMapping("/admin/config/departments/{id}/edit")
    public String updateDepartment(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive,
            @RequestParam(defaultValue = "true") boolean allowRouting,
            @RequestParam(defaultValue = "true") boolean oneActiveTicketOnly) {

        Department dept = departmentRepository.findById(id).orElseThrow();

        if (isBlank(code) || isBlank(name) || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?deptInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String cleanName = name.trim();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?deptInvalidCode";
        }

        Optional<Department> existingDept = departmentRepository.findByCode(codeUpper);
        if (existingDept.isPresent() && !existingDept.get().getId().equals(id)) {
            return "redirect:/admin/config?deptExists";
        }

        dept.setCode(codeUpper);
        dept.setName(cleanName);
        dept.setDisplayOrder(displayOrder);
        dept.setIsActive(isActive);
        dept.setAllowRouting(allowRouting);
        dept.setOneActiveTicketOnly(oneActiveTicketOnly);

        departmentRepository.save(dept);
        return "redirect:/admin/config?deptSaved";
    }

    // ================= EDIT TRANSACTION TYPE =================
    @GetMapping("/admin/config/transaction-types/{id}/edit")
    public String editTransactionTypeForm(@PathVariable Long id, Model model) {
        TransactionType tx = transactionTypeRepository.findById(id).orElseThrow();
        model.addAttribute("transactionType", tx);
        return "edit-transaction-type";
    }

    @PostMapping("/admin/config/transaction-types/{id}/edit")
    public String updateTransactionType(
            @PathVariable Long id,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer displayOrder,
            @RequestParam(defaultValue = "true") boolean isActive) {

        TransactionType tx = transactionTypeRepository.findById(id).orElseThrow();

        if (isBlank(code) || isBlank(name) || displayOrder == null || displayOrder < 1) {
            return "redirect:/admin/config?txInvalid";
        }

        String codeUpper = code.trim().toUpperCase();
        String cleanName = name.trim();
        String cleanDescription = description == null ? null : description.trim();

        if (hasSpaces(codeUpper)) {
            return "redirect:/admin/config?txInvalidCode";
        }

        Optional<TransactionType> existing = transactionTypeRepository.findByCode(codeUpper);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return "redirect:/admin/config?txExists";
        }

        tx.setCode(codeUpper);
        tx.setName(cleanName);
        tx.setDescription(cleanDescription);
        tx.setDisplayOrder(displayOrder);
        tx.setIsActive(isActive);

        transactionTypeRepository.save(tx);

        return "redirect:/admin/config?txSaved";
    }

    // ================= EDIT ROUTING RULE =================
    @GetMapping("/admin/config/routing-rules/{id}/edit")
    public String editRoutingRuleForm(@PathVariable Long id, Model model) {
        RoutingRule rule = routingRuleRepository.findById(id).orElseThrow();

        model.addAttribute("rule", rule);
        model.addAttribute("transactionTypes", transactionTypeRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("departments", departmentRepository.findAllByOrderByDisplayOrderAsc());
        model.addAttribute("specificTransactions", specificTransactionRepository.findAllByOrderByDisplayOrderAsc());

        return "edit-routing-rule";
    }

    @PostMapping("/admin/config/routing-rules/{id}/edit")
    public String updateRoutingRule(
            @PathVariable Long id,
            @RequestParam String transactionTypeCode,
            @RequestParam(required = false) String specificTransactionCode,
            @RequestParam String targetDepartmentCode,
            @RequestParam Integer priority,
            @RequestParam(defaultValue = "true") boolean isActive) {

        RoutingRule rule = routingRuleRepository.findById(id).orElseThrow();

        if (isBlank(transactionTypeCode) || isBlank(targetDepartmentCode) || priority == null || priority < 1) {
            return "redirect:/admin/config?ruleInvalid";
        }

        String txCode = transactionTypeCode.trim().toUpperCase();
        String deptCode = targetDepartmentCode.trim().toUpperCase();
        String specificCode = normalizeSpecificTransactionCode(specificTransactionCode);

        boolean txExists = transactionTypeRepository.findByCode(txCode).isPresent();
        boolean deptExists = departmentRepository.findByCode(deptCode).isPresent();

        if (!txExists || !deptExists) {
            return "redirect:/admin/config?ruleInvalidReference";
        }

        if (!isSpecificTransactionValidForType(txCode, specificCode)) {
            return "redirect:/admin/config?ruleInvalidReference";
        }

        boolean sameAsCurrent =
                txCode.equals(rule.getTransactionTypeCode())
                && deptCode.equals(rule.getTargetDepartmentCode())
                && ((specificCode == null && rule.getSpecificTransactionCode() == null)
                    || (specificCode != null && specificCode.equals(rule.getSpecificTransactionCode())));

        if (!sameAsCurrent && routingRuleCombinationExists(txCode, specificCode, deptCode)) {
            return "redirect:/admin/config?ruleExists";
        }

        rule.setTransactionTypeCode(txCode);
        rule.setSpecificTransactionCode(specificCode);
        rule.setOperatorNumber(null);
        rule.setTargetDepartmentCode(deptCode);
        rule.setPriority(priority);
        rule.setIsActive(isActive);

        routingRuleRepository.save(rule);

        return "redirect:/admin/config?ruleSaved";
    }

    // ================= ROUTING RULE HELPERS =================
    private String normalizeSpecificTransactionCode(String specificTransactionCode) {
        if (specificTransactionCode == null || specificTransactionCode.trim().isEmpty()) {
            return null;
        }
        return specificTransactionCode.trim().toUpperCase();
    }

    private boolean isSpecificTransactionValidForType(String transactionTypeCode, String specificTransactionCode) {
        if (specificTransactionCode == null || specificTransactionCode.trim().isEmpty()) {
            return true;
        }

        String txCode = transactionTypeCode.trim().toUpperCase();
        String specificCode = specificTransactionCode.trim().toUpperCase();

        return specificTransactionRepository
                .findByTransactionTypeCodeAndCode(txCode, specificCode)
                .isPresent();
    }

    private boolean routingRuleCombinationExists(String transactionTypeCode, String specificTransactionCode, String targetDepartmentCode) {
        if (specificTransactionCode == null) {
            return routingRuleRepository.existsByTransactionTypeCodeAndSpecificTransactionCodeIsNullAndTargetDepartmentCode(
                    transactionTypeCode, targetDepartmentCode);
        }

        return routingRuleRepository.existsByTransactionTypeCodeAndSpecificTransactionCodeAndTargetDepartmentCode(
                transactionTypeCode, specificTransactionCode, targetDepartmentCode);
    }

    // ================= VALIDATION HELPERS =================
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasSpaces(String value) {
        return value != null && value.contains(" ");
    }

    // ================= RESET MASTER DATA =================
    @PostMapping("/admin/config/reset")
    public String resetMasterData(
            @RequestParam String confirmText,
            @RequestParam String secretKey) {

        if (!"RESET".equals(confirmText)) {
            return "redirect:/admin/config?resetInvalid";
        }

        if (secretKey == null || !secretKey.equals(resetSystemSecretKey)) {
            return "redirect:/admin/config?resetUnauthorized";
        }

        if (ticketRepository.count() > 0) {
            return "redirect:/admin/config?resetBlocked";
        }

        appUserRepository.findAll().stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getUsername()))
                .forEach(appUserRepository::delete);

        routingRuleRepository.deleteAll();
        specificTransactionRepository.deleteAll();
        transactionTypeRepository.deleteAll();
        departmentRepository.deleteAll();
        clientFormFieldRepository.deleteAll();
        systemSettingRepository.deleteAll();

        return "redirect:/admin/config?resetSuccess";
    }

    // ================= CLEAR TICKETS =================
    @PostMapping("/admin/config/clear-tickets")
    public String clearTickets(
            @RequestParam String confirmText,
            @RequestParam String secretKey) {

        if (!"CLEAR".equals(confirmText)) {
            return "redirect:/admin/config?clearInvalid";
        }

        if (secretKey == null || !secretKey.equals(clearTicketSecretKey)) {
            return "redirect:/admin/config?clearUnauthorized";
        }

        ticketHistoryRepository.deleteAll();
        ticketRepository.deleteAll();

        return "redirect:/admin/config?ticketsCleared";
    }

    // ================= CREATE USER =================
    @PostMapping("/admin/config/users")
    public String addUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "true") boolean isActive) {

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()
                || role == null || role.trim().isEmpty()) {
            return "redirect:/admin/config?userInvalid";
        }

        String cleanUsername = username.trim();
        String cleanRole = role.trim().toUpperCase();

        if (!"ADMIN".equals(cleanRole) && !"STAFF".equals(cleanRole)) {
            return "redirect:/admin/config?userInvalid";
        }

        if (cleanUsername.contains(" ")) {
            return "redirect:/admin/config?userInvalidUsername";
        }

        if (appUserRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            return "redirect:/admin/config?userExists";
        }

        AppUser user = new AppUser();
        user.setUsername(cleanUsername);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(cleanRole);
        user.setIsActive(isActive);

        if ("STAFF".equals(cleanRole)) {
            if (departmentId == null) {
                return "redirect:/admin/config?userDeptRequired";
            }

            Department department = departmentRepository.findById(departmentId).orElse(null);
            if (department == null) {
                return "redirect:/admin/config?userDeptInvalid";
            }

            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        appUserRepository.save(user);

        return "redirect:/admin/config?userSaved";
    }

    // ================= ACTIVATE USER =================
    @PostMapping("/admin/config/users/{id}/activate")
    public String activateUser(@PathVariable Long id) {
        AppUser user = appUserRepository.findById(id).orElseThrow();
        user.setIsActive(true);
        appUserRepository.save(user);
        return "redirect:/admin/config?userActivated";
    }

    // ================= DEACTIVATE USER =================
    @PostMapping("/admin/config/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id) {
        AppUser user = appUserRepository.findById(id).orElseThrow();

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            return "redirect:/admin/config?userProtected";
        }

        user.setIsActive(false);
        appUserRepository.save(user);
        return "redirect:/admin/config?userDeactivated";
    }

    // ================= EDIT USER =================
    @GetMapping("/admin/config/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        AppUser user = appUserRepository.findById(id).orElseThrow();
        model.addAttribute("appUser", user);
        model.addAttribute("departments", departmentRepository.findAllByOrderByDisplayOrderAsc());
        return "edit-user";
    }

    // ================= UPDATE USER =================
    @PostMapping("/admin/config/users/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam String role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "true") boolean isActive) {

        AppUser user = appUserRepository.findById(id).orElseThrow();

        if (username == null || username.trim().isEmpty()
                || role == null || role.trim().isEmpty()) {
            return "redirect:/admin/config?userInvalid";
        }

        String cleanUsername = username.trim();
        String cleanRole = role.trim().toUpperCase();

        if (!"ADMIN".equals(cleanRole) && !"STAFF".equals(cleanRole)) {
            return "redirect:/admin/config?userInvalid";
        }

        if (cleanUsername.contains(" ")) {
            return "redirect:/admin/config?userInvalidUsername";
        }

        var existing = appUserRepository.findByUsernameIgnoreCase(cleanUsername);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            return "redirect:/admin/config?userExists";
        }

        if ("admin".equalsIgnoreCase(user.getUsername()) && !isActive) {
            return "redirect:/admin/config?userProtected";
        }

        user.setUsername(cleanUsername);
        user.setRole(cleanRole);
        user.setIsActive(isActive);

        if ("STAFF".equals(cleanRole)) {
            if (departmentId == null) {
                return "redirect:/admin/config?userDeptRequired";
            }

            Department department = departmentRepository.findById(departmentId).orElse(null);
            if (department == null) {
                return "redirect:/admin/config?userDeptInvalid";
            }

            user.setDepartment(department);
        } else {
            user.setDepartment(null);
        }

        appUserRepository.save(user);

        return "redirect:/admin/config?userSaved";
    }

    // ================= PASSWORD ENDPOINTS =================
    @GetMapping("/admin/config/users/{id}/password")
    public String editUserPasswordForm(@PathVariable Long id, Model model) {
        AppUser user = appUserRepository.findById(id).orElseThrow();
        model.addAttribute("appUser", user);
        return "edit-user-password";
    }

    @PostMapping("/admin/config/users/{id}/password")
    public String updateUserPassword(
            @PathVariable Long id,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword) {

        AppUser user = appUserRepository.findById(id).orElseThrow();

        String cleanPassword = newPassword == null ? "" : newPassword.trim();
        String cleanConfirm = confirmPassword == null ? "" : confirmPassword.trim();

        if (cleanPassword.isEmpty() || cleanPassword.length() < 4) {
            return "redirect:/admin/config?userPasswordInvalid";
        }

        if (!cleanPassword.equals(cleanConfirm)) {
            return "redirect:/admin/config?userPasswordMismatch";
        }

        user.setPassword(passwordEncoder.encode(cleanPassword));
        appUserRepository.save(user);

        return "redirect:/admin/config?userPasswordSaved";
    }

    // ================= AUDIT TRAIL =================
    @GetMapping("/admin/tickets/history")
    public String viewDailyTicketHistory(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Model model) {

        java.time.LocalDate selectedDate;

        if (date == null || date.isBlank()) {
            selectedDate = java.time.LocalDate.now();
        } else {
            selectedDate = java.time.LocalDate.parse(date);
        }

        java.time.LocalDateTime start = selectedDate.atStartOfDay();
        java.time.LocalDateTime end = selectedDate.plusDays(1).atStartOfDay();

        var tickets = (status == null || status.isBlank())
                ? ticketRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end)
                : ticketRepository.findByCreatedAtBetweenAndStatusOrderByCreatedAtAsc(
                        start, end, status.toUpperCase().trim());

        long totalToday = ticketRepository.countByCreatedAtBetween(start, end);
        long doneToday = ticketRepository.countByCreatedAtBetweenAndStatus(start, end, "DONE");
        long waitingToday = ticketRepository.countByCreatedAtBetweenAndStatus(start, end, "WAITING");
        long inServiceToday = ticketRepository.countByCreatedAtBetweenAndStatus(start, end, "IN_SERVICE");
        long calledToday = ticketRepository.countByCreatedAtBetweenAndStatus(start, end, "CALLED");

        var departmentSummaryRaw = ticketRepository.countTicketsPerDepartment(start, end);

        java.util.List<java.util.Map<String, Object>> departmentSummary = new java.util.ArrayList<>();

        for (Object[] row : departmentSummaryRaw) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("department", row[0]);
            item.put("count", row[1]);
            departmentSummary.add(item);
        }

        Map<String, String> txNameMap = transactionTypeRepository.findAll()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        tx -> tx.getCode().trim().toUpperCase(),
                        tx -> tx.getName(),
                        (a, b) -> a
                ));

        Map<String, String> specTxNameMap = specificTransactionRepository.findAll()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        st -> st.getCode().trim().toUpperCase(),
                        st -> st.getName(),
                        (a, b) -> a
                ));

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedStatus", status == null ? "" : status);

        model.addAttribute("totalToday", totalToday);
        model.addAttribute("doneToday", doneToday);
        model.addAttribute("waitingToday", waitingToday);
        model.addAttribute("inServiceToday", inServiceToday);
        model.addAttribute("calledToday", calledToday);

        model.addAttribute("departmentSummary", departmentSummary);
        model.addAttribute("txNameMap", txNameMap);
        model.addAttribute("specTxNameMap", specTxNameMap);

        return "admin-ticket-history";
    }

    // =============== FOR CLONING ROUTING RULES ===========================
    @PostMapping("/admin/config/routing-rules/clone-pattern")
    public String cloneRoutingPattern(
            @RequestParam String sourceTransactionTypeCode,
            @RequestParam String targetTransactionTypeCode) {

        String sourceTx = sourceTransactionTypeCode.trim().toUpperCase();
        String targetTx = targetTransactionTypeCode.trim().toUpperCase();

        List<RoutingRule> sourceRules =
                routingRuleRepository.findByTransactionTypeCodeOrderByPriorityAsc(sourceTx);

        List<SpecificTransaction> sourceSpecs =
                specificTransactionRepository.findByTransactionTypeCodeOrderByDisplayOrderAsc(sourceTx);

        List<SpecificTransaction> targetSpecs =
                specificTransactionRepository.findByTransactionTypeCodeOrderByDisplayOrderAsc(targetTx);

        int size = Math.min(sourceSpecs.size(), targetSpecs.size());

        int created = 0;
        int skipped = 0;

        for (int i = 0; i < size; i++) {
            String sourceCode = sourceSpecs.get(i).getCode();
            String targetCode = targetSpecs.get(i).getCode();

            for (RoutingRule r : sourceRules) {

                if (sourceCode.equals(r.getSpecificTransactionCode())) {

                    boolean exists = routingRuleRepository
                            .existsByTransactionTypeCodeAndSpecificTransactionCodeAndTargetDepartmentCode(
                                    targetTx,
                                    targetCode,
                                    r.getTargetDepartmentCode()
                            );

                    if (exists) {
                        skipped++;
                        continue;
                    }

                    RoutingRule newRule = new RoutingRule();
                    newRule.setTransactionTypeCode(targetTx);
                    newRule.setSpecificTransactionCode(targetCode);
                    newRule.setTargetDepartmentCode(r.getTargetDepartmentCode());
                    newRule.setPriority(r.getPriority());
                    newRule.setIsActive(r.getIsActive());

                    routingRuleRepository.save(newRule);
                    created++;
                }
            }
        }

        return "redirect:/admin/config?cloneSuccess&created=" + created + "&skipped=" + skipped;
    }
}