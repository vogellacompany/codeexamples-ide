package com.vogella.ide.editor.bytecode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/** Lists the target languages, each entry runs the translate command with that language. */
public class TranslateMenu extends CompoundContributionItem {

	@Override
	protected IContributionItem[] getContributionItems() {
		List<IContributionItem> items = new ArrayList<>();
		for (Language language : Language.DEFAULTS) {
			items.add(item(language.name(), Map.of(TranslateHandler.LANGUAGE_PARAMETER, language.name())));
		}
		items.add(new Separator());
		items.add(item("Other Language...", Map.of()));
		return items.toArray(IContributionItem[]::new);
	}

	private IContributionItem item(String label, Map<String, String> parameters) {
		var parameter = new CommandContributionItemParameter(PlatformUI.getWorkbench(), null,
				TranslateHandler.COMMAND_ID, CommandContributionItem.STYLE_PUSH);
		parameter.label = label;
		parameter.parameters = parameters;
		return new CommandContributionItem(parameter);
	}
}
