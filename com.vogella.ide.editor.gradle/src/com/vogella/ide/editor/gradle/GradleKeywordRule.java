package com.vogella.ide.editor.gradle;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class GradleKeywordRule extends WordRule {

	private static final String[] KEYWORDS = { "allprojects", "subprojects", "buildscript", "plugins", "id",
			"apply", "plugin", "repositories", "mavenCentral", "google", "mavenLocal", "maven", "dependencies",
			"implementation", "api", "compileOnly", "runtimeOnly", "testImplementation", "testRuntimeOnly",
			"androidTestImplementation", "android", "namespace", "compileSdk", "defaultConfig", "applicationId",
			"minSdk", "targetSdk", "versionCode", "versionName", "testInstrumentationRunner", "buildTypes", "release",
			"minifyEnabled", "proguardFiles", "tasks", "register" };

	private IToken wordToken = new Token(new TextAttribute(new Color(new RGB(0, 0, 255))));

	public GradleKeywordRule() {
		super(new Detector());
		for (String word : KEYWORDS) {
			this.addWord(word, wordToken);
		}
	}
}
