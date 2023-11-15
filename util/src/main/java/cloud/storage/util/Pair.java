package cloud.storage.util;

/**
 * Pair class that containing two variables of type F and S respectively.
 *
 * @param <F> type of first variable
 * @param <S> type of second variable
 */
public class Pair<F, S> {
    private final F first;
    private final S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
