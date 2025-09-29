package com.vogella.ide.editor.asciidoc.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vogella.ide.editor.asciidoc.IncludeHyperlinkDetector;

/**
 * Test class for the IncludeHyperlinkDetector.
 * 
 * This test class focuses on testing the static utility methods and basic initialization
 * of the IncludeHyperlinkDetector. More complex integration tests that require a full
 * Eclipse workbench context would need to be run in an Eclipse environment.
 */
class IncludeHyperlinkDetectorTests {

	@Test
	@DisplayName("IncludeHyperlinkDetector can be initialized")
	void testCanBeInitialized() {
		IncludeHyperlinkDetector detector = new IncludeHyperlinkDetector();
		assertNotNull(detector, "IncludeHyperlinkDetector should be successfully instantiated");
	}

	@Test
	@DisplayName("containsSubfolder method detects subfolders correctly")
	void testContainsSubfolder() {
		// Test with subfolder path after "../"
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../exercises/exercise1.adoc"), 
				"Should detect subfolder in '../exercises/exercise1.adoc'");
		
		// Test without subfolder after "../"
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("../exercise_settings.adoc"), 
				"Should not detect subfolder in '../exercise_settings.adoc'");
		
		// Test with relative path with subfolder after "../"
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../common/header.adoc"), 
				"Should detect subfolder in '../common/header.adoc'");
		
		// Test with multiple subfolders
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../exercises/chapter1/intro.adoc"), 
				"Should detect subfolder in '../exercises/chapter1/intro.adoc'");
		
		// Test with current directory subfolder - this method only handles "../" not "./"
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("./subfolder/file.adoc"), 
				"Method only handles '../' patterns, not './' patterns");
		
		// Test with just filename
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("file.adoc"), 
				"Should not detect subfolder in 'file.adoc'");
	}

	@Test
	@DisplayName("containsSubfolder handles Windows-style paths")
	void testContainsSubfolderWindowsPaths() {
		// Test with Windows-style backslashes
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("..\\exercises\\exercise1.adoc"), 
				"Should detect subfolder in Windows-style path '..\\exercises\\exercise1.adoc'");
		
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("..\\exercise_settings.adoc"), 
				"Should not detect subfolder in '..\\exercise_settings.adoc'");
		
		// Test mixed separators (Windows/Unix)
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../exercises\\chapter1/intro.adoc"), 
				"Should detect subfolder with mixed separators '../exercises\\chapter1/intro.adoc'");
	}

	@Test
	@DisplayName("containsSubfolder handles edge cases")
	void testContainsSubfolderEdgeCases() {
		// Empty string
		assertFalse(IncludeHyperlinkDetector.containsSubfolder(""), 
				"Empty string should not contain subfolder");
		
		// Just path separators
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("/"), 
				"Single forward slash should not contain subfolder");
		
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("\\"), 
				"Single backslash should not contain subfolder");
		
		// Just parent directory
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("../"), 
				"Parent directory with trailing slash should not contain subfolder");
		
		// Multiple parent directories but no subfolder
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("../../file.adoc"), 
				"Multiple parent directories without subfolder should not contain subfolder");
		
		// Multiple parent directories with subfolder
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../../parent/file.adoc"), 
				"Multiple parent directories with subfolder should contain subfolder");
	}

	@Test
	@DisplayName("containsSubfolder handles complex path scenarios")
	void testContainsSubfolderComplexPaths() {
		// Test deep nested paths
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../level1/level2/level3/file.adoc"), 
				"Deep nested path should contain subfolder");
		
		// Test paths with special characters in directory names
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../my-folder/my_file.adoc"), 
				"Path with special characters should contain subfolder");
		
		// Test paths that start with current directory - method only handles "../"
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("./docs/readme.adoc"), 
				"Method only handles '../' patterns, not './' patterns");
		
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("./readme.adoc"), 
				"Current directory without subfolder should not contain subfolder");
	}

	@Test
	@DisplayName("containsSubfolder verifies normalization behavior")
	void testContainsSubfolderNormalization() {
		// The method should normalize backslashes to forward slashes
		// Test that backslashes are properly handled
		String windowsPath = "..\\exercises\\test.adoc";
		String normalizedPath = windowsPath.replace("\\", "/");
		
		// Both should give same result after normalization
		boolean windowsResult = IncludeHyperlinkDetector.containsSubfolder(windowsPath);
		boolean unixResult = IncludeHyperlinkDetector.containsSubfolder(normalizedPath);
		
		assertEquals(windowsResult, unixResult, 
				"Windows and Unix-style paths should produce same result after normalization");
		assertTrue(windowsResult, "Both paths should contain subfolders");
	}
	
	@Test
	@DisplayName("containsSubfolder validates lastIndexOfParent logic")
	void testContainsSubfolderParentLogic() {
		// Test the specific logic: lastIndexOfParent and subsequent slash search
		
		// Path with multiple ../ but no subfolder after the last one
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("../../file.adoc"), 
				"No subfolder after last parent directory reference");
		
		// Path with multiple ../ and subfolder after the last one
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../../folder/file.adoc"), 
				"Subfolder exists after last parent directory reference");
		
		// Path with ../ in the middle but not at the end
		assertTrue(IncludeHyperlinkDetector.containsSubfolder("../parent/../child/file.adoc"), 
				"Complex path with parent navigation should contain subfolder");
	}

	@Test
	@DisplayName("containsSubfolder handles simple relative paths")
	void testContainsSubfolderSimpleRelativePaths() {
		// Test the issue case: simple relative paths without ./ or ../
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("res/practical/asciidoc_validator.py"), 
				"containsSubfolder method is designed only for ../ patterns, not simple relative paths");
		
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("folder/file.adoc"), 
				"containsSubfolder method is designed only for ../ patterns, not simple relative paths");
		
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("deep/nested/path/file.txt"), 
				"containsSubfolder method is designed only for ../ patterns, not simple relative paths");
		
		// Test backslashes too
		assertFalse(IncludeHyperlinkDetector.containsSubfolder("folder\\file.adoc"), 
				"containsSubfolder method is designed only for ../ patterns, not simple relative paths");
	}
}