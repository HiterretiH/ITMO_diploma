package com.logistic.backend.typedata;

import com.logistic.backend.catalog.CustomerPlaceKeys;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TypeDataCachedSuggestService {

    private final TypeDataAddressSuggestClient delegate;

    @Cacheable(
            cacheNames = "typedataAddressSuggest",
            key = "T(com.logistic.backend.catalog.CustomerPlaceKeys).normalizeForKey(#query)")
    public List<String> suggest(String query) {
        return delegate.fetchSuggestions(query);
    }
}
