package com.vogella.ide.editor.gradle;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class GradleKeywordRule extends WordRule {

	String[] tagWrods = new String[] { "android", "compileSdkVersion", "buildToolsVersion", "defaultConfig",
			"applicationId", "targetSdkVersion", "applicationId", "versionCode", "minSdkVersion", "versionName",
			"testInstrumentationRunner", "buildTypes", "release", "dependencies", "androidTestCompile", "minifyEnabled",
			"proguardFiles", "compile", "testCompile", "apply", "plugin", "launcherArgs" };

	private IToken wordToken = new Token(new TextAttribute(new Color(Display.getCurrent(), new RGB(139,0,139))));
	
	public GradleKeywordRule() {
		super(new Detector());
		for (String word : tagWrods) {
			this.addWord(word, wordToken);
		}
	}
}
