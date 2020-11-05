 
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
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsSortModel;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByDataLayer;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.groupBy.GroupByModel;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultColumnHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.ColumnHeaderLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.selection.config.DefaultRowSelectionLayerConfiguration;
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

public class NattableExample {
	
	Map<String, String> propertyToLabelMap = Map.of("id", "ID", "summary", "Summary", "description", "Description",
			"done", "Done", "dueDate", "Due Date");

	@PostConstruct
	public void postConstruct(Composite parent, TaskService taskService) {
		parent.setLayout(new GridLayout());

		ConfigRegistry configRegistry = new ConfigRegistry();

		EventList<Task> tasks = GlazedLists.eventList(taskService.getAll());
		SortedList<Task> sortedList = new SortedList<>(tasks, null);

		IColumnPropertyAccessor<Task> accessor = new TaskColumnPropertyAccessor();
		IDataProvider bodyDataProvider = new ListDataProvider<Task>(sortedList,
				accessor);


		// create the body layer stack consisting out of:
		// data layer
		// selection layer
		// viewport layer
		// DataLayer bodyDataLayer = new DataLayer(bodyDataProvider);
		// Use the GroupByDataLayer instead of the default DataLayer
		GroupByDataLayer bodyDataLayer = new GroupByDataLayer<>(new GroupByModel(), sortedList, accessor);

		setColumnWidth(bodyDataLayer);


		// Apply a ColumnLabelAccumulator to address the columns in the
		// EditConfiguration class
		ColumnLabelAccumulator columnLabelAccumulator = new ColumnLabelAccumulator(bodyDataProvider);
		bodyDataLayer.setConfigLabelAccumulator(columnLabelAccumulator);

		// selection layer
		SelectionLayer selectionLayer = new SelectionLayer(bodyDataLayer);
		// Enable full selection for rows
		selectionLayer.addConfiguration(new DefaultRowSelectionLayerConfiguration());


		// view port layer
		ViewportLayer viewportLayer = new ViewportLayer(selectionLayer);



		// create the column header layer stack
		IDataProvider columnHeaderDataProvider = new DefaultColumnHeaderDataProvider(
				TaskColumnPropertyAccessor.propertyNames.toArray(new String[0]), propertyToLabelMap);
		DataLayer columnHeaderDataLayer = new DataLayer(columnHeaderDataProvider);
		ILayer columnHeaderLayer = new ColumnHeaderLayer(columnHeaderDataLayer, viewportLayer, selectionLayer);
		
		
		SortHeaderLayer<Task> sortHeaderLayer = new SortHeaderLayer<>(columnHeaderLayer,
				new GlazedListsSortModel<>(sortedList, accessor, configRegistry, columnHeaderDataLayer));
		
		// create the row header layer stack
		IDataProvider rowHeaderDataProvider = new DefaultRowHeaderDataProvider(bodyDataProvider);
		DataLayer rowHeaderDataLayer = new DataLayer(rowHeaderDataProvider, 40, 20);
		ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, viewportLayer, selectionLayer);

		
		// build the corner layer stack
		IDataProvider cornerDataProvider =  new DefaultCornerDataProvider(columnHeaderDataProvider, rowHeaderDataProvider);
		DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);



		// create the grid layer composed with the prior created layer stacks
		GridLayer gridLayer = new GridLayer(viewportLayer, sortHeaderLayer, rowHeaderLayer, cornerLayer);

		NatTable natTable = new NatTable(parent, gridLayer, false);
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