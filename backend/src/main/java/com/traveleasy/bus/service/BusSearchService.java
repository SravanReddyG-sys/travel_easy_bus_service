package com.traveleasy.bus.service;

import com.traveleasy.bus.dto.request.BusSearchRequest;
import com.traveleasy.bus.dto.response.BusSearchPageResponse;

public interface BusSearchService {

    /**
     * Core Bus Search Query Engine:
     * Searches schedules matching origin, destination, and travel date,
     * applies facet filtering (bus type, operator, price, boarding/dropping point, amenities),
     * calculates live available seat count and lowest fare,
     * generates filter facet summaries, sorts, and returns paginated search results.
     */
    BusSearchPageResponse searchBuses(BusSearchRequest request);
}
