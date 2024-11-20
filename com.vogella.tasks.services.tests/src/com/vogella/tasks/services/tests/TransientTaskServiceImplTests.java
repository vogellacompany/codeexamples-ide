package com.vogella.tasks.services.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vogella.tasks.services.internal.TransientTaskServiceImpl;

class TransientTaskServiceImplTests {

    @Test 
    @DisplayName("TransientTaskServiceImpl can be initialized")
    void assertThatTaskServiceCanBeInitialized() {
        TransientTaskServiceImpl service = new TransientTaskServiceImpl();
        assertNotNull(service);
        assertTrue(service.getAll().size()>0);
    }

    
    @Test
    @DisplayName("TransientTaskServiceImpl provides at least one task ")
    void assertThatTaskServiceProvidesData() {
        TransientTaskServiceImpl service = new TransientTaskServiceImpl();
        assertTrue(service.getAll().size()>0);
    }

}