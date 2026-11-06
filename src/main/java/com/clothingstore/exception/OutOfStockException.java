package com.clothingstore.exception;

/** Được ném khi giỏ hàng hoặc đơn hàng đòi nhiều hơn số lượng size đang có trong kho. Thông báo cho biết món nào thiếu. */
public final class OutOfStockException extends ApplicationException {
    private static final long serialVersionUID = 1L;

    /** Size bị thiếu hàng. */
    private final long sizeId;

    public OutOfStockException(long sizeId, String productName, String size, int requested, int available) {
        super(available == 0
                ? "Sản phẩm này đã hết hàng: " + productName + " (size " + size + ")."
                : "Sản phẩm " + productName + " (size " + size + ") chỉ còn " + available
                        + " sản phẩm; bạn đang yêu cầu " + requested + " sản phẩm.");
        this.sizeId = sizeId;
    }

    public long getSizeId() { return sizeId; }
}
