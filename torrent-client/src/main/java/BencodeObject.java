public class BencodeObject<T> {

    private final T value;

    public BencodeObject(T val) {
        this.value = val;
    }

    /**
     * @return the value of this BencodeObject.
     */
    public final T get() {
        return this.value;
    }
}
