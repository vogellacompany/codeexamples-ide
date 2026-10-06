package com.vogella.todo.tips;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class TipsTipProvider extends org.eclipse.tips.core.TipProvider {

	private TipImage fImage48;

	@Override
	public TipImage getImage() {
		if (fImage48 == null) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());
			try {
				fImage48 = new TipImage(bundle.getEntry("icons/48/tips.png")).setAspectRatio(1);
			} catch (IOException e) {
				getManager().log(Status.error("Could not load the tip image", e));
			}
		}
		return fImage48;
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor, "Loading Tips", 1);
		List<Tip> tips = new ArrayList<>();
		tips.add(new WelcomeTip(getID()));
		setTips(tips);
		subMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public String getDescription() {
		return "Tips about Tips";
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public void dispose() {

	}
}
