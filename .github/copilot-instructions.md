# Copilot Instructions for codeexamples-ide Repository

## Repository Overview

This repository contains Eclipse IDE plugin examples and educational code samples from vogella company. It demonstrates various Eclipse RCP (Rich Client Platform) and IDE plugin development techniques including custom editors, language servers, SWT widgets, and OSGi services.

### High-Level Repository Details

- **Purpose**: Collection of Eclipse IDE plugin examples and educational samples
- **Project Type**: Eclipse Plugin Development (PDE) using Tycho Maven build system
- **Size**: ~103 Java files, ~44 XML files, 3.5MB total
- **Target Runtime**: Eclipse IDE 2024-03, Java SE-21
- **Languages**: Java (primary), XML (configuration), Properties files
- **Frameworks**: Eclipse RCP, OSGi, SWT, JFace, LSP4J (Language Server Protocol)
- **Build System**: Maven 3.9+ with Eclipse Tycho 5.0.0

## Build Instructions

### Environment Requirements

**CRITICAL: Always ensure correct Java version is set before building:**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64  # Linux
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Must show Java 21
```

**System Prerequisites:**
- Java 21 (OpenJDK or Oracle JDK) - **Required, not optional**
- Maven 3.9+ 
- For UI testing: Xvfb (headless X server) on Linux
- Internet connection for Eclipse P2 repositories

### Core Build Commands

**Build the working modules (excludes problematic dependencies):**
```bash
# Clean build of stable modules
mvn clean compile -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page,com.vogella.ide.feature,com.vogella.tasks.events,com.vogella.tasks.services,com.vogella.swt.widgets,com.vogella.tasks.ui

# Run tests on stable modules
mvn test -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page,com.vogella.ide.feature,com.vogella.tasks.events,com.vogella.tasks.services,com.vogella.swt.widgets,com.vogella.tasks.ui
```

**Full build attempt (has known issues):**
```bash
# Will fail on certain modules due to dependency conflicts
mvn clean compile  # Expect failures on com.vogella.ide.editor.asciidoc and com.vogella.contribute.parts
```

### Build Issues & Workarounds

**Known build failures and resolutions:**

1. **TextMate dependency issues (com.vogella.ide.editor.asciidoc):**
   - **Error**: Missing `org.eclipse.tm4e.registry`
   - **Workaround**: Module temporarily disabled in pom.xml
   - **Root cause**: Eclipse 2024-03 target platform lacks compatible TextMate bundles

2. **Eclipse workbench version conflicts (com.vogella.contribute.parts):**
   - **Error**: Missing `org.eclipse.ui.workbench 3.134.0`
   - **Workaround**: Skip this module or use newer target platform
   - **Resolution**: Update target platform to Eclipse 2024-12 (requires dependency resolution)

3. **Java version mismatches:**
   - **Error**: `UnsupportedClassVersionError` class file version conflicts (class file version 65.0 vs 61.0)
   - **Root cause**: Tycho 5.0.0 components compiled with newer Java than runtime
   - **Solution**: Always verify `JAVA_HOME` points to Java 21 before building
   - **Commands**: 
     ```bash
     export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
     export PATH=$JAVA_HOME/bin:$PATH
     java -version  # Must show Java 21
     ```
   - **Note**: Using Java 17 will cause "Type TychoFeatureMapping not present" errors

### Test Execution

**Test Infrastructure:**
- Uses JUnit 5 (Jupiter API)
- Fragment host pattern for testing OSGi services
- Headless UI testing with Xvfb

**Running tests:**
```bash
# Setup for headless testing (Linux)
sudo apt-get install xvfb
Xvfb :99 &
export DISPLAY=:99

# Run specific test modules
mvn test -pl com.vogella.tasks.services.tests
```

**Test Coverage:**
- Only 2 test files exist: `TransientTaskServiceImplTests.java` and `TransientTaskServiceImplMoreTests.java`
- Tests validate OSGi service instantiation and basic functionality
- No UI test automation currently implemented

## Project Architecture & Layout

### Module Structure

**Core Application Modules:**
- `com.vogella.tasks.model/` - Domain model and interfaces
- `com.vogella.tasks.services/` - OSGi service implementations  
- `com.vogella.tasks.ui/` - Eclipse 4 UI components and handlers
- `com.vogella.ide.feature/` - Eclipse feature definition

**Custom Editors:**
- `com.vogella.ide.editor.tasks/` - Task file editor with content assist
- `com.vogella.ide.editor.gradle/` - Gradle build file syntax highlighting
- `com.vogella.ide.editor.shell/` - Shell script editor
- `com.vogella.ide.editor.asciidoc/` - AsciiDoc editor (disabled due to dependencies)

**Language Server Implementations:**
- `com.vogella.languageserver.asciidoc/` - AsciiDoc language server
- `com.vogella.lsp.asciidoc.server/` - LSP4J-based AsciiDoc server
- `com.vogella.languageserver.dart/` - Dart language server integration

**SWT & UI Components:**
- `com.vogella.swt.widgets/` - Custom SWT widget examples
- `com.vogella.contribute.parts/` - Eclipse 4 parts and contribution
- `com.vogella.eclipse.css/` - CSS styling for Eclipse themes

**Utility Modules:**
- `com.vogella.resources/` - Resource management examples
- `com.vogella.adapters/` - Adapter pattern implementations
- `com.vogella.preferences.page/` - Preference page contributions

### Configuration Files

**Build Configuration:**
- `pom.xml` - Root Maven POM with Tycho configuration
- `target-platform/target-platform.target` - Eclipse target platform definition
- `updatesite/` - P2 update site generation

**Per-Module Configuration:**
- `META-INF/MANIFEST.MF` - OSGi bundle manifest
- `plugin.xml` - Eclipse plugin extensions and contributions
- `build.properties` - Tycho build properties
- `.project` / `.classpath` - Eclipse project configuration

### Validation Pipeline

**GitHub Actions CI Pipeline (.github/workflows/maven.yaml):**
```yaml
# Triggered on: push/PR to main branch
# Environment: Ubuntu latest, Java 21, Maven 3.9.9
# Special setup: Xvfb for headless UI testing
# Command: mvn clean verify -ntp
```

**CI Environment Setup:**
1. Java 21 (Temurin distribution)
2. Maven 3.9.9 via stCarolas/setup-maven
3. Xvfb installation and startup
4. Maven cache configuration

**Local Validation Steps:**
```bash
# 1. Environment check (CRITICAL - must be Java 21)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Verify Java 21 - Java 17 will fail with Tycho 5.0.0
mvn -version   # Verify Maven 3.9+

# 2. Clean build on working modules
mvn clean compile -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page,com.vogella.ide.feature,com.vogella.tasks.events,com.vogella.tasks.services,com.vogella.swt.widgets,com.vogella.tasks.ui

# 3. Run tests (if applicable)
mvn test -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page

# 4. Verify packaging
mvn package -pl com.vogella.tasks.model,com.vogella.ide.first,com.vogella.ide.editor.gradle,com.vogella.ide.editor.tasks,com.vogella.preferences.page
```

## Key Dependencies & Technologies

**Eclipse Platform Dependencies:**
- Eclipse Platform 4.31+ (2024-03 release)
- OSGi Framework 1.10+
- SWT/JFace UI toolkit
- Eclipse JDT (Java Development Tools)
- Eclipse PDE (Plugin Development Environment)

**Language Server Protocol:**
- LSP4J library for language server implementations
- Eclipse LSP4E client integration
- TextMate grammar support (tm4e)

**Testing Framework:**
- JUnit 5 (Jupiter API)
- OSGi fragment host testing pattern

**External Dependencies:**
- Google Gson 2.11.0 (JSON processing)
- Various Eclipse ecosystem plugins

## Important Notes for Coding Agents

### What Always Works
- Java model classes and OSGi services compile reliably
- Basic Eclipse plugin structure and manifest files
- SWT widget examples and UI components
- Core task management functionality

### Known Problematic Areas
- TextMate/tm4e integration requires careful dependency management
- Eclipse workbench version compatibility issues
- Language server modules need LSP4E dependencies
- Headless testing requires proper Xvfb setup

### Best Practices
- **Always validate Java 21 environment** before any build operations
- Use the working module subset for reliable builds
- Check target platform compatibility when adding new dependencies
- Test changes with `mvn clean compile` on working modules first
- For new language servers, model after existing `com.vogella.lsp.asciidoc.server`

### Debugging Build Issues
1. Verify Java version: `java -version`
2. Check Maven version: `mvn -version` 
3. Clean workspace: `mvn clean`
4. Build incrementally by module: `mvn compile -pl <module-name>`
5. Check target platform resolution: Look for "Cannot resolve target definition" errors

**Trust these instructions** - they are based on actual build testing and validation. Only perform additional searches if you encounter errors not documented here or need specific implementation details not covered.