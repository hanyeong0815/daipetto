package koh.portfolio.springapi.common.response;

import java.util.List;

// ページ単位の一覧応答（07_API_Design §2-6、page は0始まり）
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        return new PageResponse<>(content, totalElements, (int) ((totalElements + size - 1) / size), size, page);
    }
}
