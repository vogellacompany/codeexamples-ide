# Asciidoc Editor Support for Eclipse

This plugin provides enhanced Asciidoc (.adoc) file editing support for Eclipse IDE.

## Features

### Editor Features
- Syntax highlighting using TextMate grammar
- Content assist for common Asciidoc syntax elements
- Hyperlink detection for:
  - `include::` statements - navigate to included files
  - `image::` statements - navigate to image files
  - `link:` statements - navigate to linked resources
- Image hover preview
- Language configuration support

### Compare Mode Support (NEW)
The plugin now supports Asciidoc files in Eclipse compare mode, providing the following features:

#### Hyperlink Navigation in Compare View
When comparing two versions of an Asciidoc file, you can:
- Click on `include::` statements to navigate to the included file
- Click on `image::` statements to navigate to the image file
- Click on `link:` statements to navigate to linked resources
- All hyperlinks are available in both the left and right panes of the compare editor

#### Syntax Highlighting in Compare Mode
The compare editor automatically provides color-coded highlighting for:
- **Red highlighting**: Deleted lines (content removed in the newer version)
- **Green highlighting**: Added lines (content added in the newer version)
- **Gray highlighting**: Unchanged lines

This makes it easy to review changes to Asciidoc documentation files using Eclipse's built-in compare functionality.

## Implementation Details

### Compare Mode Architecture
The compare mode support is implemented through:
1. `AsciidocMergeViewerCreator` - Factory class implementing `IViewerCreator`
2. `AsciidocMergeViewer` - Custom merge viewer extending `TextMergeViewer`
3. Extension point registration in `plugin.xml` via `org.eclipse.compare.contentMergeViewers`

The merge viewer:
- Extends Eclipse's standard `TextMergeViewer` for text comparison
- Configures hyperlink detectors for the compare view text viewers
- Installs hyperlink presenters with blue color (RGB 0,0,255) for clickable links
- Supports all three hyperlink types: include, image, and link statements

### Extension Point Configuration
The plugin registers the merge viewer for:
- File extension: `.adoc`
- Content type: `com.vogella.editor.asciidoc.contenttype`

This ensures that whenever Eclipse opens an Asciidoc file in compare mode (e.g., through Git compare, version history, or file comparison), the enhanced merge viewer is automatically used.

## Usage

### Opening Files in Compare Mode
To use the compare mode features:

1. **Git Integration**: Right-click on an Asciidoc file → Compare With → Previous Revision
2. **File Comparison**: Right-click on an Asciidoc file → Compare With → Other Resource
3. **Local History**: Right-click on an Asciidoc file → Compare With → Local History

### Navigating Hyperlinks in Compare Mode
1. Hold `Ctrl` (Windows/Linux) or `Cmd` (macOS)
2. Hover over an `include::`, `image::`, or `link:` statement
3. The hyperlink will be underlined in blue
4. Click to navigate to the referenced file

## Technical Dependencies
- `org.eclipse.compare` - Compare framework support
- `org.eclipse.jface.text` - Text editor framework
- `org.eclipse.ui.genericeditor` - Generic editor support
- `org.eclipse.tm4e.*` - TextMate grammar support

## Testing
Unit tests are provided in the `com.vogella.ide.editor.asciidoc.tests` fragment bundle:
- `AsciidocMergeViewerCreatorTests` - Tests for the merge viewer factory
- `IncludeHyperlinkDetectorTests` - Tests for hyperlink detection logic

## Future Enhancements
Potential improvements could include:
- Custom syntax highlighting rules for Asciidoc in compare mode
- Preview pane for image comparisons
- Enhanced diff annotations for Asciidoc-specific structures (headers, blocks, etc.)
