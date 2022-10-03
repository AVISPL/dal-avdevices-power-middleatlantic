/*
 * Copyright (c) 2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.middleatlantic.statistics;

import java.util.Arrays;
import java.util.Optional;

/**
 * DynamicStatisticsDefinitions contains definitions for Dynamic property candidates.
 *
 * @author Maksym.Rossiitsev / Symphony Dev Team<br>
 * @since 1.1.0
 * */
public enum DynamicStatisticsDefinitions {
    InletActivePower("Inlet Active Power"),
    InletLineFrequency("Inlet Line Frequency"),
    InletRMSCurrent("Inlet RMS Current"),
    InletRMSVoltage("Inlet RMS Voltage"),
    Outlet1RMSCurrent("Outlet 1 RMS Current"),
    Outlet2RMSCurrent("Outlet 2 RMS Current"),
    Outlet3RMSCurrent("Outlet 3 RMS Current"),
    Outlet4RMSCurrent("Outlet 4 RMS Current"),
    Outlet5RMSCurrent("Outlet 5 RMS Current"),
    Outlet6RMSCurrent("Outlet 6 RMS Current"),
    Outlet7RMSCurrent("Outlet 7 RMS Current"),
    Outlet8RMSCurrent("Outlet 8 RMS Current");

    private final String name;
    DynamicStatisticsDefinitions(final String name) {
        this.name = name;
    }

    /**
     * Retrieves {@link #name}
     *
     * @return value of {@link #name}
     */
    public String getName() {
        return name;
    }

    /**
     * Check if dynamic property definition exists, by name.
     *
     * @param name of the property to check
     * @return true if definition exists, false otherwise
     * */
    public static boolean checkIfExists(String name) {
        Optional<DynamicStatisticsDefinitions> dynamicStatisticsProperty = Arrays.stream(DynamicStatisticsDefinitions.values()).filter(c -> name.contains(c.getName())).findFirst();
        if (dynamicStatisticsProperty.isPresent()) {
            return true;
        }
        return false;
    }
}
