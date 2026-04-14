package com.fluffy.batch.aggregator;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class AggregatorDashboardRedirect {

    @GetMapping(value = {"/fluffy-aggregator", "/fluffy-aggregator/"})
    public ResponseEntity<Void> redirectToDashboard() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/fluffy-aggregator/index.html")
                .build();
    }
}
