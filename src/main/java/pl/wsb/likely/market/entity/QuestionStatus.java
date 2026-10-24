package pl.wsb.likely.market.entity;

/**
 * Life cycle of a question: OPEN -> CLOSED -> RESOLVED or CANCELLED.
 *
 * <p>CLOSED only records that {@code closeAt} has passed. It is planned that a scheduled job will
 * set it. Code that accepts trades must still compare the date itself.
 */
public enum QuestionStatus {
    OPEN,
    CLOSED,
    RESOLVED,
    CANCELLED
}
