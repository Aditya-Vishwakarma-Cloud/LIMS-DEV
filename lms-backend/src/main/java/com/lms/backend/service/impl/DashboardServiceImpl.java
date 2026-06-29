package com.lms.backend.service.impl;

import com.lms.backend.dto.DashboardStatsDto;
import com.lms.backend.dto.SampleDto;
import com.lms.backend.entity.*;
import com.lms.backend.repository.*;
import com.lms.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SampleRepository sampleRepository;
    private final SampleTestRepository sampleTestRepository;
    private final TestResultRepository testResultRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ContactPersonRepository contactPersonRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        User currentUser = getCurrentUser();

        // 1. Calculate General / Admin metrics
        long todaySamples = sampleRepository.countByCreatedAtAfter(startOfToday);
        long pendingAssignments = sampleRepository.countByStatusInAndDeletedFalse(
                List.of(SampleStatus.RECEIVED, SampleStatus.PARTIALLY_ASSIGNED)
        );
        long pendingQAReviews = testResultRepository.countByStatusIn(
                List.of(ResultStatus.SUBMITTED, ResultStatus.UNDER_REVIEW)
        );
        long pendingSignOff = testResultRepository.countByStatus(ResultStatus.VERIFIED);
        
        long reportsReleasedToday = sampleRepository.countByStatusInAndUpdatedAtAfterAndDeletedFalse(
                List.of(SampleStatus.REPORT_GENERATED, SampleStatus.DELIVERED), startOfToday
        );

        long invoicesPending = sampleRepository.countByStatusAndDeletedFalse(SampleStatus.REPORT_GENERATED);
        long activeCustomers = customerRepository.count();
        long totalLaboratories = 3; 
        long totalSystemUsers = userRepository.count();
        long totalProjects = projectRepository.count();
        long totalRegisteredSamples = sampleRepository.count();
        long todayActiveTests = sampleTestRepository.countActiveTests();

        // 2. Calculate Lab Manager specific metrics
        long awaitingAssignment = pendingAssignments;
        long testsInProgress = todayActiveTests;
        long testingComplete = sampleTestRepository.countCompletedTests();
        long pendingQEReview = pendingQAReviews;
        long overdueTests = sampleTestRepository.countOverdueTests(LocalDate.now());
        long completedToday = sampleTestRepository.countCompletedTestsToday(startOfToday);

        // 3. Reception metrics
        long todayWorkOrders = workOrderRepository.countByCreatedAtAfter(startOfToday);
        long registeredToday = todaySamples;
        long pendingPhysicalReceipt = sampleRepository.countByStatusAndDeletedFalse(SampleStatus.REGISTERED);
        long pendingRegistrations = workOrderRepository.countByStatusAndDeletedFalse(WorkOrderStatus.OPEN);

        List<DashboardStatsDto.ReceptionQueueDto> receptionIncomingQueue = new ArrayList<>();
        List<WorkOrder> recentWorkOrders = workOrderRepository.findAll().stream()
                .filter(wo -> !wo.isDeleted())
                .sorted(Comparator.comparing(BaseEntity::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());

        for (WorkOrder wo : recentWorkOrders) {
            receptionIncomingQueue.add(DashboardStatsDto.ReceptionQueueDto.builder()
                    .client(wo.getCustomer().getCustomerName())
                    .project(wo.getProject().getProjectName())
                    .status(wo.getStatus().name())
                    .due(wo.getDueDate() != null ? wo.getDueDate().toString() : "N/A")
                    .build());
        }

        // 4. Technician metrics (personalized)
        long assignedTests = 0;
        long activeTesting = 0;
        long draftResults = 0;
        long completedTodayTech = 0;
        List<DashboardStatsDto.TechnicianTestDto> technicianActiveTestsList = new ArrayList<>();

        if (currentUser != null) {
            List<SampleTest> techTests = sampleTestRepository.findByTechnicianId(currentUser.getId());
            assignedTests = techTests.stream().filter(t -> t.getStatus() == SampleTestStatus.ASSIGNED || t.getStatus() == SampleTestStatus.PENDING).count();
            activeTesting = techTests.stream().filter(t -> t.getStatus() == SampleTestStatus.IN_PROGRESS).count();
            
            draftResults = testResultRepository.findBySampleTestTechnicianId(currentUser.getId()).stream()
                    .filter(r -> r.getStatus() == ResultStatus.DRAFT)
                    .count();

            completedTodayTech = techTests.stream()
                    .filter(t -> (t.getStatus() == SampleTestStatus.COMPLETED || t.getStatus() == SampleTestStatus.VERIFIED) && t.getUpdatedAt().isAfter(startOfToday))
                    .count();

            List<SampleTest> activeTechTests = techTests.stream()
                    .filter(t -> t.getStatus() == SampleTestStatus.ASSIGNED || t.getStatus() == SampleTestStatus.IN_PROGRESS)
                    .sorted(Comparator.comparing(BaseEntity::getUpdatedAt).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            for (SampleTest st : activeTechTests) {
                technicianActiveTestsList.add(DashboardStatsDto.TechnicianTestDto.builder()
                        .sampleTestId(st.getId().toString())
                        .sampleCode(st.getSample().getSampleId())
                        .testName(st.getTestDefinition().getTestName())
                        .priority(st.getSample().getPriority() != null ? st.getSample().getPriority() : "NORMAL")
                        .dueDate(st.getDueDate() != null ? st.getDueDate().toString() : "N/A")
                        .status(st.getStatus().name())
                        .build());
            }
        }

        // 5. Quality Engineer metrics
        long qePendingReviews = pendingQAReviews;
        long qeRejectedResults = testResultRepository.countByStatus(ResultStatus.REJECTED);
        long qeApprovedToday = testResultRepository.countByStatusIn(List.of(ResultStatus.VERIFIED, ResultStatus.APPROVED));
        long qeReportsReady = sampleRepository.countByStatusAndDeletedFalse(SampleStatus.APPROVED);
        long qeOverdueReviews = 0;

        List<DashboardStatsDto.QEPendingReviewDto> qePendingReviewsList = new ArrayList<>();
        List<TestResult> pendingReviews = testResultRepository.findByStatusIn(
                List.of(ResultStatus.SUBMITTED, ResultStatus.UNDER_REVIEW)
        );
        for (TestResult tr : pendingReviews) {
            SampleTest st = tr.getSampleTest();
            if (st != null) {
                if (st.getDueDate() != null && st.getDueDate().isBefore(LocalDate.now())) {
                    qeOverdueReviews++;
                }
                qePendingReviewsList.add(DashboardStatsDto.QEPendingReviewDto.builder()
                        .testResultId(tr.getId().toString())
                        .sampleCode(st.getSample().getSampleId())
                        .testName(st.getTestDefinition().getTestName())
                        .technicianName(st.getTechnician() != null ? st.getTechnician().getName() : "Unassigned")
                        .submittedDate(tr.getUpdatedAt() != null ? tr.getUpdatedAt().toLocalDate().toString() : "N/A")
                        .build());
            }
        }

        // 6. Client Viewer metrics (personalized)
        long clientProjectsCount = 0;
        long clientActiveWorkOrdersCount = 0;
        long clientSamplesSubmittedCount = 0;
        long clientReportsAvailableCount = 0;
        long clientInvoicesCount = 0;
        List<DashboardStatsDto.ClientSampleDto> clientRecentSamplesList = new ArrayList<>();
        List<DashboardStatsDto.ClientDownloadDto> clientReportDownloadsList = new ArrayList<>();

        if (currentUser != null) {
            UUID clientCustomerId = null;
            Optional<Customer> customerOpt = customerRepository.findByEmailId(currentUser.getEmail());
            if (customerOpt.isPresent()) {
                clientCustomerId = customerOpt.get().getId();
            } else {
                Optional<ContactPerson> contactOpt = contactPersonRepository.findByEmail(currentUser.getEmail());
                if (contactOpt.isPresent()) {
                    clientCustomerId = contactOpt.get().getCustomer().getId();
                }
            }

            if (clientCustomerId != null) {
                clientProjectsCount = projectRepository.countByCustomerId(clientCustomerId);
                clientActiveWorkOrdersCount = workOrderRepository.countByCustomerIdAndStatusAndDeletedFalse(clientCustomerId, WorkOrderStatus.OPEN) +
                        workOrderRepository.countByCustomerIdAndStatusAndDeletedFalse(clientCustomerId, WorkOrderStatus.IN_PROGRESS);
                clientSamplesSubmittedCount = sampleRepository.countByWorkOrderCustomerId(clientCustomerId);
                clientReportsAvailableCount = sampleRepository.countByWorkOrderCustomerIdAndStatusInAndDeletedFalse(
                        clientCustomerId, List.of(SampleStatus.REPORT_GENERATED, SampleStatus.DELIVERED)
                );
                clientInvoicesCount = workOrderRepository.countByCustomerIdAndStatusAndDeletedFalse(clientCustomerId, WorkOrderStatus.COMPLETED);

                List<Sample> clientSamples = sampleRepository.findByWorkOrderCustomerIdAndDeletedFalseOrderByCreatedAtDesc(clientCustomerId);
                
                clientSamples.stream().limit(5).forEach(s -> {
                    clientRecentSamplesList.add(DashboardStatsDto.ClientSampleDto.builder()
                            .id(s.getSampleId())
                            .materialName(s.getMaterial().getMaterialName())
                            .projectName(s.getWorkOrder().getProject().getProjectName())
                            .status(s.getStatus().name())
                            .date(s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : "N/A")
                            .build());
                });

                clientSamples.stream()
                        .filter(s -> s.getStatus() == SampleStatus.REPORT_GENERATED || s.getStatus() == SampleStatus.DELIVERED)
                        .limit(5)
                        .forEach(s -> {
                            clientReportDownloadsList.add(DashboardStatsDto.ClientDownloadDto.builder()
                                    .id(s.getSampleId())
                                    .title(s.getMaterial().getMaterialName() + " Compliance Certificate")
                                    .projectName(s.getWorkOrder().getProject().getProjectName())
                                    .date(s.getUpdatedAt() != null ? s.getUpdatedAt().toLocalDate().toString() : "N/A")
                                    .build());
                        });
            }
        }

        // 7. Compute Charts & Progress Bars
        Map<String, Long> samplesByMaterial = new HashMap<>();
        samplesByMaterial.put("CON", 0L);
        samplesByMaterial.put("SOI", 0L);
        samplesByMaterial.put("BIT", 0L);
        samplesByMaterial.put("STL", 0L);

        List<Object[]> materialStats = sampleRepository.countSamplesByMaterial();
        for (Object[] row : materialStats) {
            String code = (String) row[0];
            Long count = (Long) row[1];
            if (code != null) {
                String key = code.toUpperCase();
                if (key.contains("CON")) {
                    samplesByMaterial.put("CON", samplesByMaterial.getOrDefault("CON", 0L) + count);
                } else if (key.contains("SOI") || key.contains("SOL")) {
                    samplesByMaterial.put("SOI", samplesByMaterial.getOrDefault("SOI", 0L) + count);
                } else if (key.contains("BIT")) {
                    samplesByMaterial.put("BIT", samplesByMaterial.getOrDefault("BIT", 0L) + count);
                } else if (key.contains("STL")) {
                    samplesByMaterial.put("STL", samplesByMaterial.getOrDefault("STL", 0L) + count);
                } else {
                    samplesByMaterial.put(key, count);
                }
            }
        }

        Map<String, Long> workflowProgression = new HashMap<>();
        workflowProgression.put("completed", testingComplete);
        workflowProgression.put("testing", testsInProgress);
        workflowProgression.put("awaiting", awaitingAssignment);

        // 8. Lab Manager tables
        List<SampleDto> pendingSamplesList = sampleRepository.findByStatusInAndDeletedFalse(
                List.of(SampleStatus.RECEIVED, SampleStatus.PARTIALLY_ASSIGNED)
        ).stream()
                .limit(5)
                .map(this::mapSampleToDto)
                .collect(Collectors.toList());

        List<User> technicians = userRepository.findByRoleAndStatusAndNotDeleted("ROLE_TECHNICIAN", AccountStatus.ACTIVE);
        List<Object[]> workloadStats = sampleTestRepository.getTechnicianWorkloadStats();

        Map<UUID, Map<SampleTestStatus, Long>> techStatsMap = new HashMap<>();
        for (Object[] row : workloadStats) {
            UUID techId = (UUID) row[0];
            SampleTestStatus status = (SampleTestStatus) row[1];
            Long count = (Long) row[2];
            techStatsMap.computeIfAbsent(techId, k -> new HashMap<>()).put(status, count);
        }

        List<DashboardStatsDto.TechWorkloadDto> technicianWorkloadList = new ArrayList<>();
        for (User tech : technicians) {
            Map<SampleTestStatus, Long> stats = techStatsMap.getOrDefault(tech.getId(), Collections.emptyMap());
            long assigned = stats.getOrDefault(SampleTestStatus.PENDING, 0L) + stats.getOrDefault(SampleTestStatus.ASSIGNED, 0L);
            long active = stats.getOrDefault(SampleTestStatus.IN_PROGRESS, 0L);
            long completed = stats.getOrDefault(SampleTestStatus.COMPLETED, 0L) + stats.getOrDefault(SampleTestStatus.VERIFIED, 0L);

            technicianWorkloadList.add(DashboardStatsDto.TechWorkloadDto.builder()
                    .name(tech.getName() + " (Technician)")
                    .assigned(assigned)
                    .active(active)
                    .completed(completed)
                    .build());
        }

        return DashboardStatsDto.builder()
                .todaySamples(todaySamples)
                .pendingAssignments(pendingAssignments)
                .pendingQAReviews(pendingQAReviews)
                .pendingSignOff(pendingSignOff)
                .reportsReleasedToday(reportsReleasedToday)
                .invoicesPending(invoicesPending)
                .activeCustomers(activeCustomers)
                .totalLaboratories(totalLaboratories)
                .totalSystemUsers(totalSystemUsers)
                .totalProjects(totalProjects)
                .totalRegisteredSamples(totalRegisteredSamples)
                .todayActiveTests(todayActiveTests)
                .awaitingAssignment(awaitingAssignment)
                .testsInProgress(testsInProgress)
                .testingComplete(testingComplete)
                .pendingQEReview(pendingQEReview)
                .overdueTests(overdueTests)
                .completedToday(completedToday)
                .todayWorkOrders(todayWorkOrders)
                .registeredToday(registeredToday)
                .pendingPhysicalReceipt(pendingPhysicalReceipt)
                .pendingRegistrations(pendingRegistrations)
                .receptionIncomingQueue(receptionIncomingQueue)
                .assignedTests(assignedTests)
                .activeTesting(activeTesting)
                .draftResults(draftResults)
                .completedTodayTech(completedTodayTech)
                .technicianActiveTestsList(technicianActiveTestsList)
                .qePendingReviews(qePendingReviews)
                .qeRejectedResults(qeRejectedResults)
                .qeApprovedToday(qeApprovedToday)
                .qeReportsReady(qeReportsReady)
                .qeOverdueReviews(qeOverdueReviews)
                .qePendingReviewsList(qePendingReviewsList)
                .clientProjectsCount(clientProjectsCount)
                .clientActiveWorkOrdersCount(clientActiveWorkOrdersCount)
                .clientSamplesSubmittedCount(clientSamplesSubmittedCount)
                .clientReportsAvailableCount(clientReportsAvailableCount)
                .clientInvoicesCount(clientInvoicesCount)
                .clientRecentSamplesList(clientRecentSamplesList)
                .clientReportDownloadsList(clientReportDownloadsList)
                .samplesByMaterial(samplesByMaterial)
                .workflowProgression(workflowProgression)
                .pendingSamplesList(pendingSamplesList)
                .technicianWorkloadList(technicianWorkloadList)
                .build();
    }

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ?
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        return userRepository.findByEmail(username).orElse(null);
    }

    private SampleDto mapSampleToDto(Sample s) {
        return SampleDto.builder()
                .id(s.getId())
                .sampleId(s.getSampleId())
                .workOrderId(s.getWorkOrder().getId())
                .workOrderNumber(s.getWorkOrder().getWorkOrderNumber())
                .customerName(s.getWorkOrder().getCustomer().getCustomerName())
                .projectName(s.getWorkOrder().getProject().getProjectName())
                .materialId(s.getMaterial().getId())
                .materialName(s.getMaterial().getMaterialName())
                .materialCode(s.getMaterial().getMaterialCode())
                .quantity(s.getQuantity())
                .unit(s.getUnit())
                .collectionDate(s.getCollectionDate())
                .collectionLocation(s.getCollectionLocation())
                .status(s.getStatus().name())
                .priority(s.getPriority())
                .remarks(s.getRemarks())
                .build();
    }
}
