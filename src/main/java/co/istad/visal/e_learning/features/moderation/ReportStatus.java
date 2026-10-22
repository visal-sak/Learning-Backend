package co.istad.visal.e_learning.features.moderation;

/**
 * OPEN      waiting in the moderation queue
 * DISMISSED an admin looked and kept the review
 * (A removed review takes its reports with it — see ReviewReport.review.)
 */
public enum ReportStatus {
    OPEN,
    DISMISSED
}
