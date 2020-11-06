 
package com.vogella.nattable.parts;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.data.IDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByHeaderLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.CompositeLayer;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.sort.SortHeaderLayer;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.viewport.ViewportLayer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.vogella.tasks.model.Task;
import com.vogella.tasks.model.TaskService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TransformedList;

public class NattableExampleGroupBy {
	
	private BodyLayerStack<Task> bodyLayerStack;


	Map<String, String> propertyToLabelMap = Map.of("id", "ID", "summary", "Summary", "description", "Description",
			"done", "Done", "dueDate", "Due Date");

	@PostConstruct
	public void postConstruct(Composite parent, TaskService taskService) {
		parent.setLayout(new GridLayout());

		ConfigRegistry configRegistry = new ConfigRegistry();
		IColumnPropertyAccessor<Task> accessor = new TaskColumnPropertyAccessor();

		// create the body stack
		bodyLayerStack = new BodyLayerStack<>(taskService.getAll(), accessor);


		// create the column header layer stack
		IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(
				TaskColumnPropertyAccessor.propertyNames.toArray(new String[0]), propertyToLabelMap);
		DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		ColumnHeaderLayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer,
				bodyLayerStack.getBodyDataLayer(),
				bodyLayerStack.getSelectionLayer());
		
		
		SortHeaderLayer<Task> sortHeaderLayer = new SortHeaderLayer<>(columnHeaderLayer,
				new GlazedListsSortModel<>(bodyLayerStack.getSortedList(), accessor, configRegistry,
						columnHeaderDataLayer));
		
		// create the row header layer stack
		IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyLayerStack.getBodyDataProvider());
		DataLayer rowHeaderDataLayer = new DataLayer(rowHeaderDataProvider, 40, 20);
		ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayerStack,
				bodyLayerStack.getSelectionLayer());

		
		// build the corner layer stack
		IDataProvider cornerDataProvider =  new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);



		// create the grid layer composed with the prior created layer stacks
		ILayer gridLayer = new GridLayer(bodyLayerStack, sortHeaderLayer, rowHeaderLayer, cornerLayer);

		// set the group by header on top of the grid
		CompositeLayer compositeGridLayer = new CompositeLayer(1, 2);
		GroupByHeaderLayer groupByHeaderLayer = new GroupByHeaderLayer(bodyLayerStack.getGroupByModel(), gridLayer,
				columnHeaderDataProvider, columnHeaderLayer);
		compositeGridLayer.setChildLayer(GroupByHeaderLayer.GROUP_BY_REGION, groupByHeaderLayer, 0, 0);
		compositeGridLayer.setChildLayer("Grid", gridLayer, 0, 1);

		NatTable natTable = new NatTable(parent, compositeGridLayer, false);
		natTable.setConfigRegistry(configRegistry);
		natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
		natTable.addConfiguration(new SingleClickSortConfiguration());
		natTable.addConfiguration(new EditConfiguration());
		natTable.configure();

		GridDataFactory.fillDefaults().grab(true, true).applyTo(natTable);
	}

	private void setColumnWidth(DataLayer bodyDataLayer) {
		// activate percentage sizing for all columns
		bodyDataLayer.setColumnPercentageSizing(true);

		// Use percentage values for the first three columns column
		bodyDataLayer.setColumnWidthPercentageByPosition(0, 5);
		bodyDataLayer.setColumnWidthPercentageByPosition(1, 30);
		bodyDataLayer.setColumnWidthPercentageByPosition(2, 40);

		// deactivate percentage sizing for the fourth column (isDone)
		bodyDataLayer.setColumnPercentageSizing(3, false);
		// apply a fixed size for the fourth column
		bodyDataLayer.setColumnWidthByPosition(3, 50);

		// also use percentage sizing for the dueDate column
		bodyDataLayer.setColumnWidthPercentageByPosition(4, 25);
	}
	
	/**
	 * Always encapsulate the body layer stack in an AbstractLayerTransform to
	 * ensure that the index transformations are performed in later commands.
	 *
	 * @param <T>
	 */
	class BodyLayerStack<T> extends AbstractLayerTransform {

		private final SortedList<T> sortedList;

		private final IDataProvider bodyDataProvider;

		private final SelectionLayer selectionLayer;

		private final GroupByDataLayer<T> bodyDataLayer;

		private final GroupByModel groupByModel = new GroupByModel();

		private EventList<T> eventList;

		private final TreeLayer treeLayer;

		public BodyLayerStack(List<T> values, IColumnPropertyAccessor<T> columnPropertyAccessor) {
			eventList = GlazedLists.eventList(values);
			TransformedList<T, T> rowObjectsGlazedList = GlazedLists.threadSafeList(eventList);

			// use the SortedList constructor with 'null' for the Comparator
			// because the Comparator
			// will be set by configuration
			this.sortedList = new SortedList<>(rowObjectsGlazedList, null);

			// Use the GroupByDataLayer instead of the default DataLayer
			bodyDataLayer = new GroupByDataLayer<>(getGroupByModel(), this.sortedList, columnPropertyAccessor);

//			ColumnOverrideLabelAccumulator columnLabelAccumulator = new ColumnOverrideLabelAccumulator(bodyDataLayer);
//			bodyDataLayer.setConfigLabelAccumulator(columnLabelAccumulator);
//			registerColumnLabels(columnLabelAccumulator);

			// get the IDataProvider that was created by the GroupByDataLayer
			this.bodyDataProvider = bodyDataLayer.getDataProvider();
			// layer for event handling of GlazedLists and PropertyChanges
			GlazedListsEventLayer<T> glazedListsEventLayer = new GlazedListsEventLayer<>(bodyDataLayer,
					this.sortedList);

			this.selectionLayer = new SelectionLayer(glazedListsEventLayer);

			// add a tree layer to visualise the grouping
			treeLayer = new TreeLayer(this.selectionLayer, bodyDataLayer.getTreeRowModel());

			ViewportLayer viewportLayer = new ViewportLayer(treeLayer);

			setUnderlyingLayer(viewportLayer);
		}

		public SelectionLayer getSelectionLayer() {
			return this.selectionLayer;
		}

		public SortedList<T> getSortedList() {
			return this.sortedList;
		}

		public GroupByDataLayer<T> getBodyDataLayer() {
			return this.bodyDataLayer;
		}

		public TreeLayer getTreeLayer() {
			return this.treeLayer;
		}

		public IDataProvider getBodyDataProvider() {
			return this.bodyDataProvider;
		}

		public GroupByModel getGroupByModel() {
			return this.groupByModel;
		}

		public EventList<T> getList() {
			return eventList;
		}
	}
	
	
	
}