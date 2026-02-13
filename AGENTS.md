# Project Overview: codeexamples-ide

This project is a collection of Eclipse IDE plugins maintained by [vogella GmbH](https://www.vogella.com).
It serves as a reference for modern Eclipse development practices, covering both the classic IDE extension points and the newer Eclipse 4 (e4) application model.

## Key Technologies and Architecture

- **Eclipse Tycho**: Used for building Eclipse plugins, features, and products with Maven.
- **Eclipse 4 (e4)**: Demonstrates dependency injection (Jakarta Inject), the application model (`.e4xmi`), and CSS styling.
- **Generic Editor & TM4E**: Examples of extending the Eclipse Generic Editor with TextMate grammars (TM4E) for languages like Asciidoc.
- **Language Server Protocol (LSP)**: Integrations for Asciidoc via Language Servers.
- **OSGi**: The underlying modular system for all Eclipse plugins.
- **Java 25**: The project is configured to use JavaSE-25.

## Project Structure

The repository is organized into many OSGi bundles (plugins):

- `com.vogella.ide.first`: Core UI components and product definition.
- `com.vogella.ide.editor.*`: Custom editors and extensions for the Generic Editor (Asciidoc, Gradle, Shell, Tasks).
- `com.vogella.tasks.*`: A sample task management application split into model, services, and UI.
<<<<<<< Upstream, based on origin/main
- `com.vogella.lsp.asciidoc.client` and `com.vogella.lsp.asciidoc.server` LSP client and server implementations.
- `target-platform`: Contains the target definition file (`target-platform.target`) which specifies the Eclipse release and dependencies used for the build.
- `updatesite`: The Tycho-generated p2 repository for distributing the plugins.

## Building and Running

### Prerequisites

- **Java 25**: Ensure you have a JDK 25 installed and your `JAVA_HOME` is set.
- **Maven**: The project includes a Maven wrapper (`mvnw`).

### Building the Project

To build all bundles and the update site, run the following command from the root directory:

```bash
./mvnw clean verify
=======
- `com.vogella.languageserver.*` & `com.vogella.lsp.*`: LSP client and server implementations.
- `target-platform`: Contains the target definition file (`target-platform.target`) which specifies the Eclipse release and dependencies used for the build.
- `updatesite`: The Tycho-generated p2 repository for distributing the plugins.

## Building and Running

### Prerequisites

- **Java 21**: Ensure you have a JDK 21 installed and your `JAVA_HOME` is set.
- **Maven**: The project includes a Maven wrapper (`mvnw`).

### Building the Project

To build all bundles and the update site, run the following command from the root directory:

```bash
./mvnw clean install
>>>>>>> 7b2984b Adding support for different AI engines
```

### Running the Application

1.  **Import into Eclipse**:
    - Use an Eclipse IDE (e.g., Eclipse IDE for RCP and RAP Developers).
    - File -> Import... -> Existing Maven Projects.
    - Select the root directory and import all modules.
2.  **Target Platform**:
    - Open `target-platform/target-platform.target`.
    - Click "Set as Active Target Platform" in the top-right corner of the editor.
3.  **Launch**:
    - You can launch a runtime workbench from the `com.vogella.ide.product/ide.product` file by clicking the "Launch an Eclipse application" link in the Overview tab.

## Development Conventions

- **OSGi Metadata**: Always manage dependencies in `META-INF/MANIFEST.MF`.
- **Extensions**: UI contributions are defined in `plugin.xml`.
- **Asciidoc Functionality**: As much as possible, Asciidoc functionality should be implemented via the LSP (Language Server Protocol) functionality in the `com.vogella.lsp.asciidoc.*` bundles.
- **Dependency Injection**: Prefer `@Inject` (Jakarta) for accessing services and UI components in e4-based bundles.
- **Coding Style**: Follow standard Eclipse/Java conventions. The project uses `UTF-8` encoding.
- **Testing**: Test fragments (e.g., `com.vogella.tasks.services.tests`) use JUnit and are executed by `tycho-surefire-plugin` during the build.
