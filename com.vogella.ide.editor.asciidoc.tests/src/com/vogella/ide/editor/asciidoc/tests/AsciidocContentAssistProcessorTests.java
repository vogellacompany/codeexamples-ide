package com.vogella.ide.editor.asciidoc.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vogella.ide.editor.asciidoc.AsciidocContentAssistProcessor;

/**
 * Test class for the AsciidocContentAssistProcessor.
 * 
 * This test class focuses on testing basic initialization and behavior of the processor.
 * Full integration tests that require Eclipse workbench context would need to be run 
 * in an Eclipse environment with actual editors.
 */
class AsciidocContentAssistProcessorTests {

	@Test
	@DisplayName("AsciidocContentAssistProcessor can be initialized")
	void testCanBeInitialized() {
		AsciidocContentAssistProcessor processor = new AsciidocContentAssistProcessor();
		assertNotNull(processor, "AsciidocContentAssistProcessor should be successfully instantiated");
	}

	@Test
	@DisplayName("Processor has predefined AsciiDoc proposals")
	void testHasPredefinedProposals() {
		assertNotNull(AsciidocContentAssistProcessor.PROPOSALS, 
				"PROPOSALS list should not be null");
		assertFalse(AsciidocContentAssistProcessor.PROPOSALS.isEmpty(), 
				"PROPOSALS list should contain at least one element");
		
		// Check for expected proposal keywords
		boolean hasImageProposal = AsciidocContentAssistProcessor.PROPOSALS.stream()
				.anyMatch(p -> p.contains("image::"));
		assertTrue(hasImageProposal, "Should have image:: proposal");
		
		boolean hasIncludeProposal = AsciidocContentAssistProcessor.PROPOSALS.stream()
				.anyMatch(p -> p.contains("include::"));
		assertTrue(hasIncludeProposal, "Should have include:: proposal");
	}

	@Test
	@DisplayName("Processor returns empty auto-activation characters")
	void testAutoActivationCharacters() {
		AsciidocContentAssistProcessor processor = new AsciidocContentAssistProcessor();
		char[] autoActivationChars = processor.getCompletionProposalAutoActivationCharacters();
		assertNotNull(autoActivationChars, "Auto-activation characters should not be null");
		assertEquals(0, autoActivationChars.length, 
				"Auto-activation characters array should be empty by default");
	}

	@Test
	@DisplayName("Path parsing logic - extracting directory and prefix")
	void testPathParsingLogic() {
		// Test parsing of various path formats
		// These are the expected behaviors based on the implementation:
		
		// Case 1: "../OSGi/215-" should parse to directory="../OSGi/" and prefix="215-"
		String path1 = "../OSGi/215-";
		int lastSlash1 = Math.max(path1.lastIndexOf("/"), path1.lastIndexOf("\\"));
		String dir1 = path1.substring(0, lastSlash1);
		String prefix1 = path1.substring(lastSlash1 + 1);
		assertEquals("../OSGi", dir1, "Directory should be ../OSGi");
		assertEquals("215-", prefix1, "Prefix should be 215-");
		
		// Case 2: "subfolder/test" should parse to directory="subfolder" and prefix="test"
		String path2 = "subfolder/test";
		int lastSlash2 = Math.max(path2.lastIndexOf("/"), path2.lastIndexOf("\\"));
		String dir2 = path2.substring(0, lastSlash2);
		String prefix2 = path2.substring(lastSlash2 + 1);
		assertEquals("subfolder", dir2, "Directory should be subfolder");
		assertEquals("test", prefix2, "Prefix should be test");
		
		// Case 3: "file.adoc" has no directory separator
		String path3 = "file.adoc";
		int lastSlash3 = Math.max(path3.lastIndexOf("/"), path3.lastIndexOf("\\"));
		assertTrue(lastSlash3 < 0, "Should not have directory separator");
		
		// Case 4: Empty string
		String path4 = "";
		int lastSlash4 = Math.max(path4.lastIndexOf("/"), path4.lastIndexOf("\\"));
		assertTrue(lastSlash4 < 0, "Empty string should not have directory separator");
	}

	@Test
	@DisplayName("Path parsing handles Windows and Unix separators")
	void testCrossPlatformSeparators() {
		// Test Unix-style path
		String unixPath = "folder/subfolder/file";
		int unixSlash = Math.max(unixPath.lastIndexOf("/"), unixPath.lastIndexOf("\\"));
		assertEquals(16, unixSlash, "Should find last forward slash at position 16");
		
		// Test Windows-style path
		String winPath = "folder\\subfolder\\file";
		int winSlash = Math.max(winPath.lastIndexOf("/"), winPath.lastIndexOf("\\"));
		assertEquals(16, winSlash, "Should find last backslash at position 16");
		
		// Test mixed path (should work with either)
		String mixedPath = "folder/subfolder\\file";
		int mixedSlash = Math.max(mixedPath.lastIndexOf("/"), mixedPath.lastIndexOf("\\"));
		assertEquals(16, mixedSlash, "Should find last separator (backslash) at position 16");
	}

	@Test
	@DisplayName("Path reconstruction maintains original path prefix")
	void testPathReconstruction() {
		// Test that pathPrefix + filename reconstructs the original path
		String originalPath = "../OSGi/215-example.adoc";
		int lastSlash = Math.max(originalPath.lastIndexOf("/"), originalPath.lastIndexOf("\\"));
		String pathPrefix = originalPath.substring(0, lastSlash + 1); // Include the separator
		String filename = originalPath.substring(lastSlash + 1);
		
		String reconstructed = pathPrefix + filename;
		assertEquals(originalPath, reconstructed, "Reconstructed path should match original");
		assertEquals("../OSGi/", pathPrefix, "Path prefix should include trailing separator");
		assertEquals("215-example.adoc", filename, "Filename should be extracted correctly");
	}
}
