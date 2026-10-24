package pl.wsb.likely.market.entity;

/**
 * The YES or NO key.
 *
 * <p>Used for the direction of a trade and for the final result. Cancelling is a question status,
 * not a result, so there is no third value.
 */
public enum Outcome {
    YES,
    NO
}
