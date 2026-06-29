package com.lms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {
    // Admin / Super Admin metrics
    private long todaySamples;
    private long pendingAssignments;
    private long pendingQAReviews;
    private long pendingSignOff;
    private long reportsReleasedToday;
    private long invoicesPending;
    private long activeCustomers;
    private long totalLaboratories;
    private long totalSystemUsers;
    private long totalProjects;
    private long totalRegisteredSamples;
    private long todayActiveTests;

    // Lab Manager metrics
    private long awaitingAssignment;
    private long testsInProgress;
    private long testingComplete;
    private long pendingQEReview;
    private long overdueTests;
    private long completedToday;

    // Reception metrics
    private long todayWorkOrders;
    private long registeredToday;
    private long pendingPhysicalReceipt;
    private long pendingRegistrations;
    private List<ReceptionQueueDto> receptionIncomingQueue;

    // Technician metrics
    private long assignedTests;
    private long activeTesting;
    private long draftResults;
    private long completedTodayTech;
    private List<TechnicianTestDto> technicianActiveTestsList;

    // Quality Engineer metrics
    private long qePendingReviews;
    private long qeRejectedResults;
    private long qeApprovedToday;
    private long qeReportsReady;
    private long qeOverdueReviews;
    private List<QEPendingReviewDto> qePendingReviewsList;

    // Client Viewer metrics
    private long clientProjectsCount;
    private long clientActiveWorkOrdersCount;
    private long clientSamplesSubmittedCount;
    private long clientReportsAvailableCount;
    private long clientInvoicesCount;
    private List<ClientSampleDto> clientRecentSamplesList;
    private List<ClientDownloadDto> clientReportDownloadsList;

    // Charts
    private Map<String, Long> samplesByMaterial;
    private Map<String, Long> workflowProgression;

    // Tables (Lab Manager)
    private List<SampleDto> pendingSamplesList;
    private List<TechWorkloadDto> technicianWorkloadList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechWorkloadDto {
        private String name;
        private long assigned;
        private long active;
        private long completed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReceptionQueueDto {
        private String client;
        private String project;
        private String status;
        private String due;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianTestDto {
        private String sampleTestId;
        private String sampleCode;
        private String testName;
        private String priority;
        private String dueDate;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QEPendingReviewDto {
        private String testResultId;
        private String sampleCode;
        private String testName;
        private String technicianName;
        private String submittedDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientSampleDto {
        private String id;
        private String materialName;
        private String projectName;
        private String status;
        private String date;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClientDownloadDto {
        private String id;
        private String title;
        private String projectName;
        private String date;
    }
}
