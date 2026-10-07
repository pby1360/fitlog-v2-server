package com.fitlog.fitlogv2server.domain.dashboard.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    @Test
    void toPercent_convertsRatioToPercentage() {
        assertThat(DashboardService.toPercent(1.0)).isEqualTo(100.0);
        assertThat(DashboardService.toPercent(0.5)).isEqualTo(50.0);
        assertThat(DashboardService.toPercent(0.0)).isEqualTo(0.0);
        assertThat(DashboardService.toPercent(2.0 / 3.0)).isEqualTo(66.7);
    }

    @Test
    void toPercent_returnsZeroWhenNoCompletedSessions() {
        assertThat(DashboardService.toPercent(null)).isEqualTo(0.0);
    }
}
