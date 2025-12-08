package com.vogella.ide.debugtools.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import jakarta.inject.Named;

public class VisualizedUserInterface {

  private Random random = new Random();

  private List<Color> createdColors = new ArrayList<>();

  @Execute
  public void execute(@Named(IServiceConstants.ACTIVE_SHELL) final Shell s) {
    // Check for flagged containers and print ASCII art layout
    checkForLayoutPrintFlag(s);

    // Original colorization
    colorizeComposites(s);
  }

  private void checkForLayoutPrintFlag(Control control) {
    if (control.getData("LAYOUT_PRINT") != null) {
      System.out.println("--- Box Layout Ascii Art Start ---");
      printBoxTree(control, "");
      System.out.println("--- Box Layout Ascii Art End ---");
      // If we found the flag, we print this subtree and stop searching this branch
      return;
    }

    if (control instanceof Composite) {
      for (Control child : ((Composite) control).getChildren()) {
        checkForLayoutPrintFlag(child);
      }
    }
  }


  private void printBoxTree(Control control, String indent) {
    int maxWidth = 100;
    // Determine current width based on indentation
    int currentWidth = maxWidth - indent.length();
    if (currentWidth < 20) {
      currentWidth = 20; // Minimum width safeguard
    }

    String info = getSimpleControlInfo(control);

    // Prepare strings
    String horizontalBorder = "+" + repeat("-", currentWidth - 2) + "+";
    String emptyLine = "|" + repeat(" ", currentWidth - 2) + "|";

    // Print Top Border
    System.out.println(indent + horizontalBorder);

    // Print Info Line
    String content = "| " + info;
    int padding = currentWidth - 1 - content.length();
    if (padding < 0) {
      // Truncate if too long
      content = content.substring(0, currentWidth - 4) + "...";
      padding = 1;
    }
    System.out.println(indent + content + repeat(" ", padding) + "|");

    // Print Spacer
    System.out.println(indent + emptyLine);

    // Print Children
    if (control instanceof Composite) {
      // The indentation for children includes the vertical bar of the parent
      String childIndent = indent + "|  ";
      
      // Recursively print children
      for (Control child : ((Composite) control).getChildren()) {
        printBoxTree(child, childIndent);
      }
      
      // Optional: Print a spacer after children before closing bottom border?
      // System.out.println(indent + emptyLine); 
    }

    // Print Bottom Border
    System.out.println(indent + horizontalBorder);
  }

  private String getSimpleControlInfo(Control control) {
    StringBuilder sb = new StringBuilder();
    sb.append(control.getClass().getSimpleName());
    
    if (control instanceof Composite) {
      Layout layout = ((Composite) control).getLayout();
      if (layout != null) {
        sb.append(" (").append(layout.getClass().getSimpleName()).append(")");
      } else {
        sb.append(" (No Layout)");
      }
    }
    
    // Add bounds for extra detail
    sb.append(" ").append(control.getBounds());
    
    return sb.toString();
  }

  private String repeat(String str, int count) {
    if (count <= 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(str.length() * count);
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }
    return sb.toString();
  }

  private void colorizeComposites(final Composite composite) {
    // Apply random color to this composite
    Color color = createDistinctColor(composite);
    composite.setBackground(color);

    // Add tooltip with layout information
    String tooltip = buildTooltip(composite);
    composite.setToolTipText(tooltip);

    // Recursively process all children
    for (Control child : composite.getChildren()) {
      if (child instanceof Composite) {
        colorizeComposites((Composite) child);
      }
    }

    // Force redraw
    composite.redraw();
  }

  private String buildTooltip(final Composite composite) {
    StringBuilder sb = new StringBuilder();

    // Class name
    sb.append("Class: ").append(composite.getClass().getSimpleName()).append("\n");

    // Layout information
    Layout layout = composite.getLayout();
    if (layout != null) {
      sb.append("Layout: ").append(layout.getClass().getSimpleName()).append("\n");

      if (layout instanceof GridLayout) {
        GridLayout gl = (GridLayout) layout;
        sb.append("  Columns: ").append(gl.numColumns).append("\n");
        sb.append("  Make columns equal: ").append(gl.makeColumnsEqualWidth).append("\n");
        sb.append("  Margins: T=").append(gl.marginTop).append(" B=").append(gl.marginBottom).append(" L=")
            .append(gl.marginLeft).append(" R=").append(gl.marginRight).append("\n");
        sb.append("  Spacing: H=").append(gl.horizontalSpacing).append(" V=").append(gl.verticalSpacing).append("\n");
      } else if (layout instanceof FillLayout) {
        FillLayout fl = (FillLayout) layout;
        sb.append("  Type: ").append(fl.type == 256 ? "HORIZONTAL" : "VERTICAL").append("\n");
        sb.append("  Margin: W=").append(fl.marginWidth).append(" H=").append(fl.marginHeight).append("\n");
        sb.append("  Spacing: ").append(fl.spacing).append("\n");
      } else if (layout instanceof RowLayout) {
        RowLayout rl = (RowLayout) layout;
        sb.append("  Type: ").append(rl.type == 256 ? "HORIZONTAL" : "VERTICAL").append("\n");
        sb.append("  Wrap: ").append(rl.wrap).append("\n");
        sb.append("  Pack: ").append(rl.pack).append("\n");
        sb.append("  Justify: ").append(rl.justify).append("\n");
        sb.append("  Margin: W=").append(rl.marginWidth).append(" H=").append(rl.marginHeight).append("\n");
        sb.append("  Spacing: ").append(rl.spacing).append("\n");
      } else if (layout instanceof FormLayout) {
        FormLayout fl = (FormLayout) layout;
        sb.append("  Margin: T=").append(fl.marginTop).append(" B=").append(fl.marginBottom).append(" L=")
            .append(fl.marginLeft).append(" R=").append(fl.marginRight).append("\n");
        sb.append("  Spacing: ").append(fl.spacing).append("\n");
      } else {
        sb.append("  Custom layout\n");
      }
    } else {
      sb.append("Layout: NONE\n");
    }

    // Bounds information
    sb.append("Bounds: x=").append(composite.getBounds().x).append(" y=").append(composite.getBounds().y).append(" w=")
        .append(composite.getBounds().width).append(" h=").append(composite.getBounds().height).append("\n");

    // Children count
    sb.append("Children: ").append(composite.getChildren().length);

    return sb.toString();
  }

  private Color createDistinctColor(final Composite composite) {
    // Generate a bright, distinct color
    int hue = random.nextInt(360);
    float saturation = 0.6f + random.nextFloat() * 0.4f; // 0.6 to 1.0
    float brightness = 0.7f + random.nextFloat() * 0.3f; // 0.7 to 1.0

    RGB rgb = hsbToRgb(hue, saturation, brightness);
    Color color = new Color(composite.getDisplay(), rgb);
    createdColors.add(color);

    return color;
  }

  private RGB hsbToRgb(final int hue, final float saturation, final float brightness) {
    float h = hue / 360f;
    float s = saturation;
    float b = brightness;

    int r = 0, g = 0, bl = 0;
    if (s == 0) {
      r = g = bl = (int) (b * 255.0f + 0.5f);
    } else {
      float h6 = (h - (float) Math.floor(h)) * 6.0f;
      float f = h6 - (float) Math.floor(h6);
      float p = b * (1.0f - s);
      float q = b * (1.0f - s * f);
      float t = b * (1.0f - (s * (1.0f - f)));

      switch ((int) h6) {
      case 0:
        r = (int) (b * 255.0f + 0.5f);
        g = (int) (t * 255.0f + 0.5f);
        bl = (int) (p * 255.0f + 0.5f);
        break;
      case 1:
        r = (int) (q * 255.0f + 0.5f);
        g = (int) (b * 255.0f + 0.5f);
        bl = (int) (p * 255.0f + 0.5f);
        break;
      case 2:
        r = (int) (p * 255.0f + 0.5f);
        g = (int) (b * 255.0f + 0.5f);
        bl = (int) (t * 255.0f + 0.5f);
        break;
      case 3:
        r = (int) (p * 255.0f + 0.5f);
        g = (int) (q * 255.0f + 0.5f);
        bl = (int) (b * 255.0f + 0.5f);
        break;
      case 4:
        r = (int) (t * 255.0f + 0.5f);
        g = (int) (p * 255.0f + 0.5f);
        bl = (int) (b * 255.0f + 0.5f);
        break;
      case 5:
        r = (int) (b * 255.0f + 0.5f);
        g = (int) (p * 255.0f + 0.5f);
        bl = (int) (q * 255.0f + 0.5f);
        break;
      }
    }

    return new RGB(r, g, bl);
  }
}