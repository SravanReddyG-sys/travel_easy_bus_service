package com.traveleasy.bus.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusSearchPageResponse {

    @Builder.Default
    private List<BusSearchResultCardResponse> content = new ArrayList<>();

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    private BusSearchFilterSummaryResponse filterSummary;
}
