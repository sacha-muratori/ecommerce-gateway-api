package com.ecommerce.service.master.batch;

import java.util.List;

public interface BatchEventListener {
    void onBatchReady(String endpoint, List<String> batch);
}
