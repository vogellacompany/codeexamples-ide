package com.vogella.ide.editor.asciidoc.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vogella.ide.editor.asciidoc.AsciidocMergeViewerCreator;

/**
 * Test class for the AsciidocMergeViewerCreator.
 * 
 * This test class focuses on testing the basic initialization and structure
 * of the AsciidocMergeViewerCreator. Full integration tests that require a 
 * complete Eclipse workbench context would need to be run in an Eclipse environment.
 */
class AsciidocMergeViewerCreatorTests {

    @Test
    @DisplayName("AsciidocMergeViewerCreator can be initialized")
    void testCanBeInitialized() {
        AsciidocMergeViewerCreator creator = new AsciidocMergeViewerCreator();
        assertNotNull(creator, "AsciidocMergeViewerCreator should be successfully instantiated");
    }
    
    @Test
    @DisplayName("AsciidocMergeViewerCreator implements IViewerCreator")
    void testImplementsIViewerCreator() {
        AsciidocMergeViewerCreator creator = new AsciidocMergeViewerCreator();
        assertTrue(creator instanceof org.eclipse.compare.IViewerCreator, 
            "AsciidocMergeViewerCreator should implement IViewerCreator");
    }
}
