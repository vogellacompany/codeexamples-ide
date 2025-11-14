package z.ex.search;

import java.nio.file.Paths;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

	public static final String SCRIPT_PATH = "scriptPath";
	public static final String PDF_OUTPUT_PATH = "pdfOutputPath";

	// Default values
	public static final String DEFAULT_SCRIPT_PATH = Paths.get(System.getProperty("user.home"),
			"git", "content", "_scripts", "buildRCPScript.sh").toString();
	public static final String DEFAULT_PDF_OUTPUT_PATH = Paths.get(System.getProperty("user.home"),
			"git", "content", "output.pdf").toString();

}
