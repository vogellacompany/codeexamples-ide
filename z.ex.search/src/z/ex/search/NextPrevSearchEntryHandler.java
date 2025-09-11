package z.ex.search;

import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

public class NextPrevSearchEntryHandler extends AbstractHandler implements IExecutableExtension {
	private String searchCommand = IWorkbenchCommandConstants.NAVIGATE_NEXT;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ICommandService cs = (ICommandService) window.getService(ICommandService.class);

		Command showView = cs.getCommand(IWorkbenchCommandConstants.VIEWS_SHOW_VIEW);
		HashMap<String, Object> parms = new HashMap<String, Object>();
		parms.put(IWorkbenchCommandConstants.VIEWS_SHOW_VIEW_PARM_ID, "org.eclipse.search.ui.views.SearchView");
		ParameterizedCommand showSearchView = ParameterizedCommand.generateCommand(showView, parms);

		IHandlerService hs = window.getService(IHandlerService.class);
		try {
			hs.executeCommand(showSearchView, (Event)event.getTrigger());
			hs.executeCommand(searchCommand, (Event)event.getTrigger());
			hs.executeCommand(IWorkbenchCommandConstants.WINDOW_ACTIVATE_EDITOR, (Event)event.getTrigger());
		} catch (NotDefinedException e) {
			throw new ExecutionException(e.getMessage(), e);
		} catch (NotEnabledException e) {
			throw new ExecutionException(e.getMessage(), e);
		} catch (NotHandledException e) {
			throw new ExecutionException(e.getMessage(), e);
		}

		return null;
	}

	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		if ("previous".equals(data)) {
			searchCommand = IWorkbenchCommandConstants.NAVIGATE_PREVIOUS;
		}
	}

}
